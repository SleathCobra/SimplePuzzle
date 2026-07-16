$ErrorActionPreference = "Stop"

$environmentScript = Join-Path $PSScriptRoot 'android-env.ps1'
. $environmentScript

function Invoke-CheckedNativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Executable,

        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]] $Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& $Executable @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        throw "Command failed: $Executable $($Arguments -join ' ')"
    }
    return $output
}

$androidVersion = Invoke-CheckedNativeCommand -Executable $AndroidCliExecutable '--version'
$javaVersion = Invoke-CheckedNativeCommand -Executable $JavaExecutable '-version'

$gradleWrapper = Join-Path $ProjectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "The repository Gradle Wrapper is missing."
}
$gradleOutput = Invoke-CheckedNativeCommand -Executable $gradleWrapper '--version'
$gradleVersion = $gradleOutput | Where-Object { [string] $_ -match '^Gradle\s+' } | Select-Object -First 1

$adbVersion = Invoke-CheckedNativeCommand -Executable $AdbExecutable 'version'
$platformToolsProperties = Join-Path $AndroidSdkRoot 'platform-tools\source.properties'
$platformToolsVersion = Get-JavaProperty -Path $platformToolsProperties -Name 'Pkg.Revision'
$buildToolsRoot = Join-Path $AndroidSdkRoot 'build-tools'
$buildToolsVersions = if (Test-Path -LiteralPath $buildToolsRoot -PathType Container) {
    Get-ChildItem -LiteralPath $buildToolsRoot -Directory | Sort-Object Name | Select-Object -ExpandProperty Name
} else {
    @()
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $devicesOutput = @(& $AdbExecutable devices -l 2>&1)
    $devicesExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($devicesExitCode -ne 0) {
    throw "adb device enumeration failed."
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $studioOutput = @(& $AndroidCliExecutable studio check 2>&1)
    $studioExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

Write-Output "Android CLI version: $($androidVersion -join ' ')"
Write-Output "SDK root: $AndroidSdkRoot"
Write-Output "JDK root: $JdkRoot"
Write-Output "JDK source: $JdkSource"
Write-Output "Java version: $($javaVersion -join ' ')"
Write-Output "Gradle Wrapper version: $gradleVersion"
Write-Output "adb version: $($adbVersion -join ' ')"
Write-Output "Installed platform-tools: $platformToolsVersion"
Write-Output "Installed build-tools: $($buildToolsVersions -join ', ')"
Write-Output 'Connected-device status:'
$devicesOutput | ForEach-Object { Write-Output "  $_" }
if ($studioExitCode -eq 0 -and ($studioOutput -join ' ') -notmatch '(?i)no running Studio instances') {
    Write-Output "Android Studio CLI connection: available ($($studioOutput -join ' '))"
} else {
    Write-Output "Android Studio CLI connection: unavailable/non-blocking ($($studioOutput -join ' '))"
}
