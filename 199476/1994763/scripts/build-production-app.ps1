param(
    [string]$KeystorePath = "$PSScriptRoot\..\mobile-flutter\shixianwen-release.jks",
    [string]$KeyAlias = "shixianwen"
)

$ErrorActionPreference = "Stop"

$apiBaseUrl = "https://www.inlightus.com/api"
$mobileDirectory = Resolve-Path "$PSScriptRoot\..\mobile-flutter"
$resolvedKeystorePath = Resolve-Path $KeystorePath

if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEYSTORE_PASSWORD)) {
    throw "ANDROID_KEYSTORE_PASSWORD is not configured."
}

if ([string]::IsNullOrWhiteSpace($env:ANDROID_KEY_PASSWORD)) {
    throw "ANDROID_KEY_PASSWORD is not configured."
}

$env:ANDROID_KEYSTORE_PATH = $resolvedKeystorePath.Path
$env:ANDROID_KEY_ALIAS = $KeyAlias

Push-Location $mobileDirectory.Path
try {
    flutter build apk `
        --release `
        --split-per-abi `
        --target-platform android-arm64 `
        --dart-define="API_BASE_URL=$apiBaseUrl"
} finally {
    Pop-Location
}
