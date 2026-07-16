$ErrorActionPreference = "Stop"

# Keep Gradle options byte-for-byte at the process boundary. An advanced
# PowerShell parameter would capture single-dash Gradle/JVM options itself.
$GradleArguments = @($args)

$environmentScript = Join-Path $PSScriptRoot 'android-env.ps1'
. $environmentScript

$gradleWrapper = Join-Path $ProjectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "The repository Gradle Wrapper was not found at '<project-root>\gradlew.bat'."
}

& $gradleWrapper @GradleArguments
$gradleExitCode = $LASTEXITCODE
exit $gradleExitCode
