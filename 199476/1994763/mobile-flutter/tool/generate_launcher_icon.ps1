param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$resourceRoot = Join-Path $ProjectRoot 'android\app\src\main\res'
$masterPath = Join-Path $ProjectRoot 'tool\branding\ic_launcher_master.png'
$master = [System.Drawing.Bitmap]::new($masterPath)
$sizes = [ordered]@{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

try {
    foreach ($entry in $sizes.GetEnumerator()) {
        $target = [System.Drawing.Bitmap]::new($entry.Value, $entry.Value)
        $targetGraphics = [System.Drawing.Graphics]::FromImage($target)

        try {
            $targetGraphics.InterpolationMode =
                [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $targetGraphics.PixelOffsetMode =
                [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $targetGraphics.DrawImage(
                $master,
                [System.Drawing.Rectangle]::new(0, 0, $entry.Value, $entry.Value),
                0,
                0,
                $master.Width,
                $master.Height,
                [System.Drawing.GraphicsUnit]::Pixel
            )
            $targetPath = Join-Path $resourceRoot "$($entry.Key)\ic_launcher.png"
            $temporaryPath = "$targetPath.tmp.png"
            $target.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Move-Item -LiteralPath $temporaryPath -Destination $targetPath -Force
        }
        finally {
            $targetGraphics.Dispose()
            $target.Dispose()
        }
    }
}
finally {
    $master.Dispose()
}
