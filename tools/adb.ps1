$ErrorActionPreference = "Stop"

# Deliberately use PowerShell's automatic $args array instead of an advanced
# script parameter. Advanced parameter binding consumes adb flags such as -W,
# -a, and -d as PowerShell common parameters before adb can see them.
$AdbArguments = @($args)

$environmentScript = Join-Path $PSScriptRoot 'android-env.ps1'
. $environmentScript

& $AdbExecutable @AdbArguments
$adbExitCode = $LASTEXITCODE
exit $adbExitCode
