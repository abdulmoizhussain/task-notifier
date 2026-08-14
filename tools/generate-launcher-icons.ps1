param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function New-RoundedRectanglePath {
    param([float]$X, [float]$Y, [float]$Width, [float]$Height, [float]$Radius)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = 2 * $Radius
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-BellCheck {
    param(
        [System.Drawing.Graphics]$Graphics,
        [float]$Scale,
        [System.Drawing.Color]$GlyphColor,
        [System.Drawing.Color]$BadgeColor,
        [System.Drawing.Color]$CheckColor
    )

    $glyphBrush = New-Object System.Drawing.SolidBrush($GlyphColor)
    $Graphics.FillEllipse($glyphBrush, [single](33*$Scale), [single](27*$Scale), [single](42*$Scale), [single](42*$Scale))
    $Graphics.FillRectangle($glyphBrush, [single](33*$Scale), [single](47*$Scale), [single](42*$Scale), [single](18*$Scale))
    [System.Drawing.PointF[]]$skirt = @(
        [System.Drawing.PointF]::new([single](34*$Scale), [single](58*$Scale)),
        [System.Drawing.PointF]::new([single](74*$Scale), [single](58*$Scale)),
        [System.Drawing.PointF]::new([single](82*$Scale), [single](72*$Scale)),
        [System.Drawing.PointF]::new([single](26*$Scale), [single](72*$Scale))
    )
    $Graphics.FillPolygon($glyphBrush, $skirt)
    $Graphics.FillEllipse($glyphBrush, [single](45.5*$Scale), [single](74*$Scale), [single](17*$Scale), [single](10*$Scale))

    $badgeBrush = New-Object System.Drawing.SolidBrush($BadgeColor)
    $Graphics.FillEllipse($badgeBrush, [single](55*$Scale), [single](54*$Scale), [single](32*$Scale), [single](32*$Scale))
    $checkBrush = New-Object System.Drawing.SolidBrush($CheckColor)
    [System.Drawing.PointF[]]$check = @(
        [System.Drawing.PointF]::new([single](59.25*$Scale), [single](72.95*$Scale)),
        [System.Drawing.PointF]::new([single](62.5*$Scale), [single](69.8*$Scale)),
        [System.Drawing.PointF]::new([single](67.85*$Scale), [single](75.15*$Scale)),
        [System.Drawing.PointF]::new([single](79.15*$Scale), [single](63*$Scale)),
        [System.Drawing.PointF]::new([single](82.5*$Scale), [single](66.1*$Scale)),
        [System.Drawing.PointF]::new([single](68*$Scale), [single](81.7*$Scale))
    )
    $Graphics.FillPolygon($checkBrush, $check)

    $checkBrush.Dispose()
    $badgeBrush.Dispose()
    $glyphBrush.Dispose()
}

function New-LauncherPng {
    param([int]$Size, [string]$OutputPath, [bool]$Round)
    $supersample = 4
    $canvasSize = $Size * $supersample
    $bitmap = New-Object System.Drawing.Bitmap($canvasSize, $canvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $background = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#263A7A'))
    if ($Round) {
        $graphics.FillEllipse($background, 0, 0, $canvasSize, $canvasSize)
    } else {
        $rounded = New-RoundedRectanglePath 0 0 $canvasSize $canvasSize (22*$supersample)
        $graphics.FillPath($background, $rounded)
        $rounded.Dispose()
    }
    Draw-BellCheck $graphics ($canvasSize / 108.0) ([System.Drawing.Color]::White) ([System.Drawing.ColorTranslator]::FromHtml('#57D6A0')) ([System.Drawing.ColorTranslator]::FromHtml('#263A7A'))
    $graphics.Dispose()
    $background.Dispose()

    $output = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $outGraphics = [System.Drawing.Graphics]::FromImage($output)
    $outGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $outGraphics.DrawImage($bitmap, 0, 0, $Size, $Size)
    $outGraphics.Dispose()
    $bitmap.Dispose()
    $output.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $output.Dispose()
}

$densities = [ordered]@{
    'mdpi' = 48
    'hdpi' = 72
    'xhdpi' = 96
    'xxhdpi' = 144
    'xxxhdpi' = 192
}

foreach ($density in $densities.Keys) {
    $directory = Join-Path $ProjectRoot "app\src\main\res\mipmap-$density"
    New-LauncherPng $densities[$density] (Join-Path $directory 'ic_launcher.png') $false
    New-LauncherPng $densities[$density] (Join-Path $directory 'ic_launcher_round.png') $true
}

# A large source preview is useful for reviews without installing the APK.
$artworkDirectory = Join-Path $ProjectRoot 'artwork'
New-Item -ItemType Directory -Path $artworkDirectory -Force | Out-Null
New-LauncherPng 512 (Join-Path $artworkDirectory 'bell-check-launcher-preview.png') $false
