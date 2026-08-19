param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$masterPath = Join-Path $ProjectRoot 'tool\branding\ic_launcher_master.png'
$storeRoot = Join-Path $ProjectRoot 'store-assets\app-icons'
$universalRoot = Join-Path $storeRoot 'universal'
$androidRoot = Join-Path $storeRoot 'android'
$iosRoot = Join-Path $storeRoot 'ios\AppIcon.appiconset'

foreach ($directory in @($storeRoot, $universalRoot, $androidRoot, $iosRoot)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

$master = [System.Drawing.Bitmap]::new($masterPath)

function Export-RgbPng {
    param(
        [int]$Size,
        [string]$Path
    )

    $target = [System.Drawing.Bitmap]::new(
        $Size,
        $Size,
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($target)

    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(255, 194, 59, 50))
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.DrawImage(
            $master,
            [System.Drawing.Rectangle]::new(0, 0, $Size, $Size),
            0,
            0,
            $master.Width,
            $master.Height,
            [System.Drawing.GraphicsUnit]::Pixel
        )
        $temporaryPath = "$Path.tmp.png"
        $target.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Move-Item -LiteralPath $temporaryPath -Destination $Path -Force
    }
    finally {
        $graphics.Dispose()
        $target.Dispose()
    }
}

try {
    foreach ($size in @(2048, 1024, 512, 432, 216, 192, 180, 167, 152, 144, 128, 120, 96, 87, 80, 76, 72, 64, 60, 58, 48, 40, 29, 20)) {
        Export-RgbPng -Size $size -Path (Join-Path $universalRoot "app-icon-$size.png")
    }

    $androidIcons = [ordered]@{
        'google-play-512.png' = 512
        'ic_launcher-mdpi-48.png' = 48
        'ic_launcher-hdpi-72.png' = 72
        'ic_launcher-xhdpi-96.png' = 96
        'ic_launcher-xxhdpi-144.png' = 144
        'ic_launcher-xxxhdpi-192.png' = 192
        'huawei-appgallery-216.png' = 216
        'store-universal-512.png' = 512
        'store-universal-1024.png' = 1024
    }
    foreach ($entry in $androidIcons.GetEnumerator()) {
        Export-RgbPng -Size $entry.Value -Path (Join-Path $androidRoot $entry.Key)
    }

    $iosIcons = [ordered]@{
        'Icon-App-20x20@1x.png' = 20
        'Icon-App-20x20@2x.png' = 40
        'Icon-App-20x20@3x.png' = 60
        'Icon-App-29x29@1x.png' = 29
        'Icon-App-29x29@2x.png' = 58
        'Icon-App-29x29@3x.png' = 87
        'Icon-App-40x40@1x.png' = 40
        'Icon-App-40x40@2x.png' = 80
        'Icon-App-40x40@3x.png' = 120
        'Icon-App-60x60@2x.png' = 120
        'Icon-App-60x60@3x.png' = 180
        'Icon-App-76x76@1x.png' = 76
        'Icon-App-76x76@2x.png' = 152
        'Icon-App-83.5x83.5@2x.png' = 167
        'Icon-App-Store-1024x1024@1x.png' = 1024
    }
    foreach ($entry in $iosIcons.GetEnumerator()) {
        Export-RgbPng -Size $entry.Value -Path (Join-Path $iosRoot $entry.Key)
    }
}
finally {
    $master.Dispose()
}

Write-Output $storeRoot
