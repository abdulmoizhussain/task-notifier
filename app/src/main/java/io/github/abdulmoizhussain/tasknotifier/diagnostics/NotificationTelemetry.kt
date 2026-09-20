package io.github.abdulmoizhussain.tasknotifier.diagnostics

import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.abdulmoizhussain.tasknotifier.common.Constants
import java.util.concurrent.atomic.AtomicLong

/**
 * Supporting detail for notification diagnostics.
 *
 * The 2026-09 investigation (see docs/NOTIFICATION_LOSS_INVESTIGATION.md) could not
 * distinguish between several candidate root causes because the logs recorded only
 * that a post had been requested, never the circumstances around it. Everything here
 * exists to make the next export answer those questions directly:
 *
 *  - how close together posts were issued (burst / rate limiting hypothesis),
 *  - how many times a given notification id had been re-posted (spam hypothesis),
 *  - what the device power and notification state was at the moment of the post,
 *  - whether the notification was still alive some seconds later, rather than
 *    immediately, which the old read-back could not tell us reliably.
 *
 * Counters are per-process and reset when the process dies. That is acceptable:
 * the cases of interest all happen inside a single process wake-up. The process
 * name is already on every event, so per-process scope is visible in the export.
 */
object NotificationTelemetry {

    /** Wait before re-checking a post. Long enough that a real post has settled. */
    const val VERIFY_DELAY_MILLIS = 2500L

    private val sequence = AtomicLong(0)

    /** Post timestamps within this process, newest last. Trimmed to the last minute. */
    private val recentPostMillis = ArrayList<Long>()

    /** Per notification id: number of posts, and when the last one happened. */
    private val postCountById = HashMap<Int, Int>()
    private val lastPostMillisById = HashMap<Int, Long>()

    private var lastPostMillis: Long? = null

    /**
     * Records that a post is about to be issued and returns the burst/rate context
     * for it. Call once per post, immediately before [NotificationManager.notify].
     */
    @Synchronized
    fun onPostRequested(notificationId: Int): Map<String, Any?> {
        val now = System.currentTimeMillis()
        val previous = lastPostMillis
        val previousForId = lastPostMillisById[notificationId]

        recentPostMillis.add(now)
        recentPostMillis.removeAll { now - it > 60_000L }

        val count = (postCountById[notificationId] ?: 0) + 1
        postCountById[notificationId] = count
        lastPostMillisById[notificationId] = now
        lastPostMillis = now

        return mapOf(
            "postSequence" to sequence.incrementAndGet(),
            "millisSincePreviousPost" to (previous?.let { now - it }),
            "millisSincePreviousPostOfThisId" to (previousForId?.let { now - it }),
            "postsInLastSecond" to recentPostMillis.count { now - it <= 1_000L },
            "postsInLastMinute" to recentPostMillis.size,
            "postsOfThisIdInProcess" to count,
        )
    }

    /**
     * A single [NotificationManager.getActiveNotifications] snapshot.
     *
     * The previous implementation called it twice per post and derived `active` and
     * `activeNotificationIds` from two different snapshots, which produced events
     * that contradicted themselves. Always derive both from one snapshot.
     */
    fun activeSnapshot(notificationManager: NotificationManager): List<StatusBarNotification> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyList()
        }
        return try {
            notificationManager.activeNotifications?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * The snapshot is always empty below API 23, so these accessors are unreachable
     * there. The explicit guard is what lets lint see that, given this module's
     * minSdk of 16.
     */
    fun activeIds(snapshot: List<StatusBarNotification>): List<Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyList()
        }
        return snapshot.map { it.id }
    }

    /**
     * Per-notification detail. `postTime` is the platform's own timestamp, which is
     * what makes the exact-72-hour reap visible without cross-referencing the logs.
     */
    fun describeActive(snapshot: List<StatusBarNotification>): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return emptyList()
        }
        val now = System.currentTimeMillis()
        return snapshot.map { sbn ->
            val ageMillis = now - sbn.postTime
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sbn.notification.channelId ?: "null"
            } else {
                "n/a"
            }
            val ongoing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sbn.isOngoing.toString()
            } else {
                "unknown"
            }
            val group = NotificationCompat.getGroup(sbn.notification)
            "id=${sbn.id} channel=$channel ongoing=$ongoing group=$group" +
                " postTime=${sbn.postTime} ageMillis=$ageMillis ageHours=${"%.3f".format(ageMillis / 3_600_000.0)}"
        }
    }

    /**
     * Device and app state that can suppress or drop a notification. Captured at post
     * time because the state days later, when the export is taken, says nothing about
     * why an individual post was lost.
     */
    fun environment(context: Context): Map<String, Any?> {
        val applicationContext = context.applicationContext
        val attributes = LinkedHashMap<String, Any?>()

        attributes["notificationsEnabled"] = try {
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        } catch (_: Exception) {
            null
        }

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            listOf(
                "default" to Constants.NOTIFICATION_CHANNEL_DEFAULT,
                "silent" to Constants.NOTIFICATION_CHANNEL_SILENT,
            ).forEach { (label, channelId) ->
                val channel = try {
                    notificationManager.getNotificationChannel(channelId)
                } catch (_: Exception) {
                    null
                }
                attributes["${label}ChannelImportance"] = channel?.importance
                attributes["${label}ChannelBlocked"] =
                    channel?.let { it.importance == NotificationManager.IMPORTANCE_NONE }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && notificationManager != null) {
            attributes["currentInterruptionFilter"] = try {
                notificationManager.currentInterruptionFilter
            } catch (_: Exception) {
                null
            }
        }

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                attributes["powerSaveMode"] = powerManager.isPowerSaveMode
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                attributes["deviceIdleMode"] = powerManager.isDeviceIdleMode
                attributes["ignoringBatteryOptimizations"] = try {
                    powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName)
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            attributes["appStandbyBucket"] = try {
                (applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)
                    ?.appStandbyBucket
            } catch (_: Exception) {
                null
            }
        }

        return attributes
    }

    /**
     * Compares the tasks the app believes are showing a notification against what is
     * actually in the shade.
     *
     * This is the check whose absence let notification ids 4 and 6 stay silently
     * missing for ten days in the 2026-09 export while their tasks still had
     * `inProgress = true`.
     */
    fun reconcile(
        context: Context,
        stage: String,
        expectedIds: List<Int>,
    ) {
        val notificationManager = context.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val snapshot = activeSnapshot(notificationManager)
        val actual = activeIds(snapshot)
        val missing = expectedIds.filterNot { actual.contains(it) }
        val unexpected = actual.filterNot { expectedIds.contains(it) }

        DiagnosticLog.record(
            context,
            "NOTIFICATION_RECONCILE",
            attributes = mapOf(
                "stage" to stage,
                "expectedIds" to expectedIds,
                "activeIds" to actual,
                "missingIds" to missing,
                "unexpectedIds" to unexpected,
                "missingCount" to missing.size,
                "activeDetail" to describeActive(snapshot),
            ),
        )
    }
}
