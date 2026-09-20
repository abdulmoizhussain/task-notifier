# Notification Loss Investigation

Status: **open — root cause not yet proven, but now reproducible on an emulator (see §6.1)**
Last updated: 2026-09-20

Symptom as reported: ongoing task notifications disappear from the notification
shade after the app has been left alone for some days. Reproduced only on a
Samsung SM-A175F, One UI 8.5 / Android 16 (API 36). Not yet tried on any other
device.

Evidence base: the diagnostics export
`task-notifier-diagnostics-2026-09-13-163711` — 1037 events across both app
processes, 2026-08-14 to 2026-09-13, plus `app-state.json` captured at
2026-09-13 11:37:15 UTC.

---

## 1. Findings that are solid

### 1.1 The system reaps notifications at exactly 72.00 hours

Measuring the delta between each `NOTIFICATION_POST_REQUESTED` and the next
`NOTIFICATION_DISMISSED_RECEIVED` for the same id produces a tight cluster:

| Time (UTC) | Notification id | Delta |
| --- | --- | --- |
| 08-19 19:35:44 | 4 | 72.0001 h |
| 08-19 20:28:27 | 6 | 72.0028 h |
| 08-22 19:36:02 | 4 | 72.0050 h |
| 08-26 07:06:13 | 4 | 72.0067 h |
| 08-26 07:06:13 | 6 | 72.0067 h |
| 08-29 15:39:01 | 4 | 72.0039 h |
| 09-03 15:28:01 | 6 | 72.0000 h |
| 09-03 15:28:08 | 4 | 72.0000 h |
| 09-03 14:26:01 | 11 | 72.0260 h |

Sub-second accuracy over multiple independent notifications is not chance. The
platform (or One UI) cancels an app notification exactly three days after it is
posted.

**This 72 h reap is not itself the bug.** The delete intent *does* fire,
`NotificationDismissedBroadcastReceiver` *does* run, and the app *does* re-post.
The clock then restarts, so a healthy notification simply cycles every 3 days.
The user never sees a gap.

This does explain why the symptom takes days to appear: nothing at all happens
to a notification during its first 72 hours.

### 1.2 Two notifications died permanently on 2026-09-03 at 17:37

Notification ids 4 and 6 were posted at 09-03 17:37:25 and 17:37:31 and then
produced **no 72 h reap and no further event of any kind for 10 days**, until a
manual Restart on 09-13 11:37. Their tasks still had `inProgress: true`
throughout, so the app believed the notifications were up.

The absence of the 72 h reap is the key inference: a live notification
*always* produces one. No reap means the post never actually became a live
notification. This is race-free evidence — it does not depend on the unreliable
read-back described in §1.4.

Survival analysis over all posts old enough to judge:

| Channel | Survived (reaped at some point) | Vanished |
| --- | --- | --- |
| DEFAULT | 13 | 0 |
| SILENT | 22 | 2 |

So the silent channel is **not** inherently broken — 22 silent posts survived.
The loss is rare and situational, not systematic per channel.

### 1.3 Posting several notifications in one tight loop loses all but the last

The clearest single observation in the whole export. On 09-13 11:37:04 the
reviver loop posted three notifications in **10 milliseconds**:

```text
11:37:04.088  POST task 4   (SILENT, ongoing)
11:37:04.094  POST task 6   (SILENT, ongoing)
11:37:04.101  POST task 14  (SILENT, ongoing)
```

`app-state.json`, captured **11 seconds later**, reports:

```json
"activeNotificationIds": [14]
```

Only the last of the three survived. The same shape appears on 09-03 17:37:25,
where the reviver posted ids 4 and 6 twenty-six milliseconds apart and id 6 is
the one that vanished for ten days.

The responsible code is `TaskNotifierAndroidService.onStartCommand`, which
iterates with no spacing whatsoever:

```kotlin
tasks.forEach { task ->
    MyNotificationManager.notifySilently(...)
}
```

### 1.4 The diagnostic read-back is unreliable and has already misled

`MyNotificationManager.postAndRecord` calls `getActiveNotifications()` twice
immediately after `notify()` — once for `active`, once for
`activeNotificationIds`. Both directions of error are present in the data:

- **False negative:** 08-14 21:53:09 logged `active: false` while
  `activeNotificationIds` in the *same event* contained the id.
- **False positive risk:** on 09-13 11:37:04 all three posts read back `[]`,
  yet id 14 was demonstrably alive 11 seconds later.

Note also that successful burst posts read back correctly at gaps as small as
24 ms, so this is not a simple propagation delay — the field is just not
trustworthy. 25 of 120 posts logged `active: false`.

**Any conclusion previously drawn from the `active` field should be
re-derived.** A delayed re-check is needed before this field means anything.

---

## 2. Candidate root causes for §1.3

Ranked by how well they fit the evidence. None is yet proven; discriminating
between them needs device-level logcat, which the export does not contain.

### H1 — Rapid-succession posts are being coalesced or dropped (strongest fit)

Three posts in 10 ms, one survivor. Every confirmed vanish happens inside a
sub-100 ms burst from the reviver loop. No confirmed vanish happens to a post
that was issued in isolation.

Counter-evidence that must be explained: bursts *usually* work. 19 of 120 posts
were issued under 1 s after the previous notification op, and most survived
(08-15, 08-16, 08-23, 08-26 reviver runs all read back correctly). So a burst is
not *sufficient* on its own — something else has to be true at the same time,
which points at a rate limiter with memory of recent activity rather than a
fixed rule.

### H2 — Samsung treats repeated same-id re-posts as spam (user hypothesis)

Worth taking seriously. The two ids that died permanently are also by far the
most re-posted:

| Notification id | Total posts |
| --- | --- |
| 6 | 37 |
| 4 | 36 |
| 14 | 9 |
| 1 | 7 |
| 3 | 7 |
| others | ≤ 6 |

Ids 4 and 6 have 4× the re-post count of anything else, and they are exactly
the two that vanished. That correlation is real and should not be dismissed.

However it is **confounded**: 4 and 6 are also the longest-lived tasks, so they
accumulate re-posts simply by existing longer, and the 72 h reap/restore cycle
mechanically generates a re-post every three days. High post count may be a
consequence of longevity rather than a cause of suppression. The two can be
separated on-device by creating a long-lived task and re-posting it far more
often than the 3-day cycle requires.

Note that this hypothesis and H1 are not mutually exclusive, and One UI is known
to apply its own notification management on top of AOSP.

### H3 — Group key misuse

`keepTaskNotificationSeparate` assigns every notification a *unique* group
(`group.task.<id>`) with `setGroupSummary(false)` — i.e. every notification is a
lone group child with no summary. This is an unusual configuration and the
intent (keeping notifications un-bundled) is achievable without it. A launcher
or system UI that prunes childless/summary-less groups would produce exactly
this symptom. Cheap to test by removing the grouping.

### H4 — Restoring from inside the delete intent is racy

On 09-03 17:37:31 a restore was posted 2 seconds after the delete intent for the
same id. Re-posting an id while the system is still tearing the same id down is
a plausible way to have the new post swallowed by the in-flight cancel. This
would explain the 09-03 id 4 loss but not the id 6 loss in the same window, so
it is at best a contributing factor.

### H5 — Samsung deep-sleep / force-stop

Initially attractive given the multi-day timescale, but the data argues against
it as the primary cause. Only one reboot occurs in the whole period
(`elapsedRealtime` resets at 08-23 07:05:48), the boot receiver fired correctly
and revived notifications, and the 72 h reap continued to fire delete intents
throughout — which a force-stopped app would not receive. Demoted, not
eliminated.

---

## 3. Why loss is permanent once it happens

Independent of which hypothesis above is correct, a dropped post is never
noticed or retried. The reviver in `TaskNotifierAndroidService` runs only on:

- app launch,
- `BOOT_COMPLETED` / time / timezone change (`OnBootReceiver`), and
- an explicit **Restart** tap.

There is **no periodic self-heal**, and nothing ever compares
`getActiveNotifications()` against the set of tasks with `inProgress = true`.
The 09-03 → 09-13 gap contains only two events. Tasks 4 and 6 sat with
`inProgress: true` and no notification for ten days, and only the user's manual
Restart surfaced it.

This is why the symptom presents as "notifications disappear after a few days"
rather than "notifications flicker".

---

## 4. Unrelated defects noticed while investigating

Logged here so they are not lost; neither is believed to cause the main symptom.

- **Notification id 0 exists.** `activeNotificationIds` repeatedly contains `0`,
  but Room ids start at 1 and no task has id 0. The very first event in the log
  is `NOTIFY_NOW_CLICKED` with `taskId: 0`. Separately,
  `NotificationDismissedBroadcastReceiver` bails on `taskId < 1`, so an id 0
  notification could never be restored even in principle.

- **`AppStartup.createNotificationChannels()` configures the wrong channel.**
  After creating `silentChannel`, the lockscreen visibility, vibration and sound
  calls are all applied to `channel` (the default channel) instead of
  `silentChannel`. The `setSound(null, null)` call also happens *after*
  `createNotificationChannel(channel)`, so it is a no-op on an already-created
  channel. `app-state.json` confirms the channels ended up at importance 4 and
  2, so the visible damage is limited — but the code does not do what it reads
  as doing.

---

## 5. What would settle it

The export cannot discriminate between H1, H2 and H3, because it contains no
platform-side information. To close this out:

1. Fix the read-back instrumentation first (delayed re-check), so that
   "did this post land" becomes a trustworthy signal rather than a coin flip.
2. Capture `logcat` from the device around a reviver run.
   `NotificationManagerService` logs its own reasons for dropping or rate
   limiting a post, which would confirm or kill H1 immediately.
3. On an API 36 emulator, post N notifications in a tight loop and check
   `getActiveNotifications()` after a delay, varying only the inter-post gap.
   This isolates H1 from H2 cleanly.
4. Separately, re-post a single id far more often than the 3-day cycle requires
   to test H2 without the longevity confound.

Note that an emulator cannot reproduce One UI's own notification management, so
it can validate the *mechanics* of a fix but cannot confirm the root cause.

---

## 6. Instrumentation added 2026-09-20

The diagnostics were rebuilt around the gaps above. New and changed events:

| Event | Purpose |
| --- | --- |
| `NOTIFICATION_POST_REQUESTED` | now carries burst context (`millisSincePreviousPost`, `postsInLastSecond`, `postsInLastMinute`, `postsOfThisIdInProcess`, `postSequence`), the `group` key, and full device/channel state at post time |
| `NOTIFICATION_POST_RESULT` | `active` and `activeNotificationIds` now come from **one** snapshot, so the event can no longer contradict itself |
| `NOTIFICATION_POST_VERIFIED` / `NOTIFICATION_POST_LOST` | re-check 2.5 s after posting — this is the event that finally distinguishes "dropped" from "not surfaced yet" |
| `NOTIFICATION_RECONCILE` | on every reviver wake-up, expected ids vs actual ids, with `missingIds` |
| `NOTIFICATION_DISMISSED_RECEIVED` | now carries `ageMillis` / `ageHours`, so the 72 h reap is visible directly instead of being derived |

Device state captured at post time: `notificationsEnabled`, per-channel importance
and blocked flags, `currentInterruptionFilter`, `powerSaveMode`,
`deviceIdleMode`, `ignoringBatteryOptimizations`, `appStandbyBucket`.

`app-state.json` gained `missingNotificationIds`, `activeNotificationDetail`
(with each notification's `postTime` and age), `targetSdk`,
`canScheduleExactAlarms`, and the same environment block.

**Search `NOTIFICATION_POST_LOST` first in any future export.**

### 6.1 The symptom reproduced on an emulator immediately

On a clean API 37 emulator, tapping **Restart Service** made the reviver post
8 notifications in 150 ms. All 8 logged `NOTIFICATION_POST_LOST` 2.5 s later,
with `activeNotificationIds: []`, while `notificationsEnabled` was `true` and
neither channel was blocked.

A single **Notify Now** post 54 s later, with `postsInLastSecond: 1`, logged
`NOTIFICATION_POST_VERIFIED` with `activeNotificationIds: [113]`.

This is a much stronger result than expected — the loss is not One UI specific
and does not need days to appear. It is directly reproducible.

**Caveat before treating H1 as proven:** in this particular run the two cases
differ in *two* ways, not one. The 8 lost posts were all on the SILENT channel
via `notifySilently`; the surviving post was on the DEFAULT channel via
`notify`. Channel and burst are therefore confounded. The clean experiment is to
post on one channel only, varying nothing but the inter-post gap.

---

## Related

- [AlarmManager reliability analysis](ALARM_MANAGER_RELIABILITY_ANALYSIS.md) —
  covers the scheduling side of the same pipeline.
- [Project and target SDK analysis](ANALYSIS.md) — the app still targets SDK 30,
  which affects which notification behaviours apply.
