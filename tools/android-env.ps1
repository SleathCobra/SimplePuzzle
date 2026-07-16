$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot

function ConvertFrom-JavaPropertyValue {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Value
    )

    $result = $Value.Trim().Trim('"').Trim("'")
    $result = $result.Replace('\:', ':').Replace('\\', '\')
    return [Environment]::ExpandEnvironmentVariables($result)
}

function Get-JavaProperty {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Name
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }

    $pattern = '^\s*' + [regex]::Escape($Name) + '\s*[:=]\s*(.*?)\s*$'
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*[#!]') {
            continue
        }
        if ($line -match $pattern) {
            return ConvertFrom-JavaPropertyValue -Value $Matches[1]
        }
    }

    return $null
}

function ConvertTo-JdkRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Candidate,

        [Parameter(Mandatory = $true)]
        [string] $BasePath
    )

    $expanded = [Environment]::ExpandEnvironmentVariables($Candidate.Trim().Trim('"').Trim("'"))
    if ([string]::IsNullOrWhiteSpace($expanded)) {
        return $null
    }

    if (-not [IO.Path]::IsPathRooted($expanded)) {
        $expanded = Join-Path $BasePath $expanded
    }

    if ($expanded.EndsWith('java.exe', [StringComparison]::OrdinalIgnoreCase)) {
        $expanded = Split-Path -Parent (Split-Path -Parent $expanded)
    }

    try {
        return [IO.Path]::GetFullPath($expanded)
    } catch {
        return $null
    }
}

function Test-JdkRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Root
    )

    $java = Join-Path $Root 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        return $false
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $java -version 2>&1 | Out-Null
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return $exitCode -eq 0
}

function Add-JdkCandidate {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.List[object]] $Candidates,

        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.HashSet[string]] $Seen,

        [AllowNull()]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Source,

        [Parameter(Mandatory = $true)]
        [string] $BasePath
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    $root = ConvertTo-JdkRoot -Candidate $Path -BasePath $BasePath
    if ($null -eq $root -or -not $Seen.Add($root)) {
        return
    }

    $Candidates.Add([pscustomobject]@{
        Root = $root
        Source = $Source
    }) | Out-Null
}

function Get-AndroidStudioRegistryRoots {
    $uninstallRoots = @(
        'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall',
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall',
        'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall'
    )

    foreach ($root in $uninstallRoots) {
        if (-not (Test-Path -LiteralPath $root)) {
            continue
        }

        foreach ($entry in Get-ItemProperty -Path (Join-Path $root '*') -ErrorAction SilentlyContinue) {
            if ($entry.DisplayName -notmatch '(?i)Android Studio') {
                continue
            }

            if (-not [string]::IsNullOrWhiteSpace($entry.InstallLocation)) {
                Join-Path $entry.InstallLocation 'jbr'
            }

            if (-not [string]::IsNullOrWhiteSpace($entry.DisplayIcon)) {
                $iconPath = ($entry.DisplayIcon -replace ',\s*\d+\s*$', '').Trim('"')
                if (Test-Path -LiteralPath $iconPath -PathType Leaf) {
                    $studioRoot = Split-Path -Parent (Split-Path -Parent $iconPath)
                    Join-Path $studioRoot 'jbr'
                }
            }
        }
    }
}

function Get-AndroidStudioFallbackRoots {
    $roots = New-Object System.Collections.Generic.List[string]

    if (-not [string]::IsNullOrWhiteSpace($env:ProgramFiles)) {
        $roots.Add((Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'))
    }
    if (-not [string]::IsNullOrWhiteSpace(${env:ProgramFiles(x86)})) {
        $roots.Add((Join-Path ${env:ProgramFiles(x86)} 'Android\Android Studio\jbr'))
    }
    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $roots.Add((Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'))

        $toolboxRoot = Join-Path $env:LOCALAPPDATA 'JetBrains\Toolbox\apps\AndroidStudio'
        if (Test-Path -LiteralPath $toolboxRoot -PathType Container) {
            $patterns = @(
                (Join-Path $toolboxRoot '*\jbr'),
                (Join-Path $toolboxRoot '*\*\jbr'),
                (Join-Path $toolboxRoot '*\*\*\jbr')
            )
            foreach ($pattern in $patterns) {
                foreach ($directory in Get-Item -Path $pattern -ErrorAction SilentlyContinue) {
                    $roots.Add($directory.FullName)
                }
            }
        }
    }

    return $roots
}

$androidCommand = Get-Command android -ErrorAction SilentlyContinue
if ($null -eq $androidCommand) {
    throw "Android CLI is unavailable. Install it or add it to PATH, then rerun tools/android-doctor.ps1."
}
$AndroidCliExecutable = $androidCommand.Source

$androidInfoLines = @(& $AndroidCliExecutable info 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "'android info' failed. Verify the Android CLI installation before continuing."
}
$AndroidInfoOutput = ($androidInfoLines -join [Environment]::NewLine) -replace '\x1B\[[0-?]*[ -/]*[@-~]', ''

$sdkPatterns = @(
    '(?im)^\s*(?:android\s+)?sdk(?:\s+(?:location|root|path))?\s*[:=]\s*["'']?(.+?)["'']?\s*,?\s*$',
    '(?im)^\s*SDK\s+Location\s*[:=]\s*["'']?(.+?)["'']?\s*,?\s*$',
    '(?im)["'']sdk(?:Location|Root|Path)?["'']\s*:\s*["''](.+?)["'']'
)

$AndroidSdkRoot = $null
foreach ($pattern in $sdkPatterns) {
    $match = [regex]::Match($AndroidInfoOutput, $pattern)
    if ($match.Success) {
        $AndroidSdkRoot = $match.Groups[1].Value.Trim().Trim('"').Trim("'").TrimEnd(',')
        break
    }
}

if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot)) {
    foreach ($line in $androidInfoLines) {
        $candidate = ([string] $line).Trim().Trim('"').Trim("'")
        if ((Test-Path -LiteralPath $candidate -PathType Container) -and
            (Test-Path -LiteralPath (Join-Path $candidate 'platform-tools') -PathType Container)) {
            $AndroidSdkRoot = $candidate
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot)) {
    $fieldOutput = @(& $AndroidCliExecutable info sdk 2>&1)
    if ($LASTEXITCODE -eq 0) {
        foreach ($line in $fieldOutput) {
            $candidate = ([string] $line).Trim().Trim('"').Trim("'")
            if (Test-Path -LiteralPath $candidate -PathType Container) {
                $AndroidSdkRoot = $candidate
                break
            }
        }
    }
}

if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot) -or
    -not (Test-Path -LiteralPath $AndroidSdkRoot -PathType Container)) {
    throw "Unable to resolve a valid Android SDK root from 'android info'."
}
$AndroidSdkRoot = (Resolve-Path -LiteralPath $AndroidSdkRoot).Path

$AdbExecutable = Join-Path $AndroidSdkRoot 'platform-tools\adb.exe'
$EmulatorExecutable = Join-Path $AndroidSdkRoot 'emulator\emulator.exe'
if (-not (Test-Path -LiteralPath $AdbExecutable -PathType Leaf)) {
    throw "Android SDK platform-tools are missing. Expected adb at '<android-sdk>\platform-tools\adb.exe'."
}
if (-not (Test-Path -LiteralPath $EmulatorExecutable -PathType Leaf)) {
    throw "Android SDK emulator tools are missing. Expected emulator at '<android-sdk>\emulator\emulator.exe'."
}

$jdkCandidates = New-Object 'System.Collections.Generic.List[object]'
$seenJdkRoots = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)

$projectJdkFiles = @(
    (Join-Path $ProjectRoot '.gradle\config.properties'),
    (Join-Path $ProjectRoot 'gradle.properties'),
    (Join-Path $ProjectRoot 'gradle\gradle-daemon-jvm.properties')
)
foreach ($file in $projectJdkFiles) {
    foreach ($property in @('java.home', 'org.gradle.java.home', 'javaHome')) {
        $value = Get-JavaProperty -Path $file -Name $property
        Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $value -Source "project Gradle JDK configuration" -BasePath $ProjectRoot
    }
}

$ideaGradlePath = Join-Path $ProjectRoot '.idea\gradle.xml'
if (Test-Path -LiteralPath $ideaGradlePath -PathType Leaf) {
    $ideaGradleText = Get-Content -Raw -LiteralPath $ideaGradlePath
    $ideaMatch = [regex]::Match($ideaGradleText, '<option\s+name="gradleJvm"\s+value="([^"]+)"')
    if ($ideaMatch.Success) {
        $ideaValue = $ideaMatch.Groups[1].Value.Replace('$PROJECT_DIR$', $ProjectRoot)
        Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $ideaValue -Source "Android Studio project Gradle JDK configuration" -BasePath $ProjectRoot
    }
}

Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $env:STUDIO_GRADLE_JDK -Source 'STUDIO_GRADLE_JDK environment variable' -BasePath $ProjectRoot
Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $env:JAVA_HOME -Source 'JAVA_HOME environment variable' -BasePath $ProjectRoot

foreach ($process in Get-Process -Name studio64 -ErrorAction SilentlyContinue) {
    if (-not [string]::IsNullOrWhiteSpace($process.Path)) {
        $studioRoot = Split-Path -Parent (Split-Path -Parent $process.Path)
        Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path (Join-Path $studioRoot 'jbr') -Source 'running Android Studio process' -BasePath $ProjectRoot
    }
}

foreach ($root in Get-AndroidStudioRegistryRoots) {
    Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $root -Source 'Android Studio Windows installation information' -BasePath $ProjectRoot
}
foreach ($root in Get-AndroidStudioFallbackRoots) {
    Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $root -Source 'Android Studio installation-root fallback' -BasePath $ProjectRoot
}

# Some Windows setups accidentally persist JAVA_HOME or STUDIO_GRADLE_JDK as
# <android-studio-jbr>\bin. Gradle rejects that value because JAVA_HOME must be
# the JDK root. Only after all correctly configured and discoverable candidates
# have been considered, recover the parent JBR without changing the machine
# environment.
foreach ($malformedEnvironmentCandidate in @(
    [pscustomobject]@{ Value = $env:STUDIO_GRADLE_JDK; Name = 'STUDIO_GRADLE_JDK' },
    [pscustomobject]@{ Value = $env:JAVA_HOME; Name = 'JAVA_HOME' }
)) {
    if ([string]::IsNullOrWhiteSpace($malformedEnvironmentCandidate.Value)) {
        continue
    }
    $expandedValue = [Environment]::ExpandEnvironmentVariables($malformedEnvironmentCandidate.Value.Trim().Trim('"').Trim("'"))
    if ((Split-Path -Leaf $expandedValue) -ieq 'bin') {
        $parentRoot = Split-Path -Parent $expandedValue
        if ((Split-Path -Leaf $parentRoot) -ieq 'jbr') {
            Add-JdkCandidate -Candidates $jdkCandidates -Seen $seenJdkRoots -Path $parentRoot -Source "Android Studio jbr recovered from invalid $($malformedEnvironmentCandidate.Name) fallback" -BasePath $ProjectRoot
        }
    }
}

$JdkRoot = $null
$JdkSource = $null
foreach ($candidate in $jdkCandidates) {
    if (Test-JdkRoot -Root $candidate.Root) {
        $JdkRoot = $candidate.Root
        $JdkSource = $candidate.Source
        break
    }
}

if ([string]::IsNullOrWhiteSpace($JdkRoot)) {
    throw "No valid Gradle JDK was found. Configure a project JDK or install Android Studio with its bundled jbr; a standalone JDK is not required or installed by this tool."
}

$JavaExecutable = Join-Path $JdkRoot 'bin\java.exe'
$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    & $JavaExecutable -version 2>&1 | Out-Null
    $javaValidationExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($javaValidationExitCode -ne 0) {
    throw "The selected Android Studio JBR failed validation with java.exe -version."
}

$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$env:JAVA_HOME = $JdkRoot

$prependPaths = @(
    (Join-Path $JdkRoot 'bin'),
    (Join-Path $AndroidSdkRoot 'platform-tools'),
    (Join-Path $AndroidSdkRoot 'emulator')
)
$pathEntries = New-Object System.Collections.Generic.List[string]
foreach ($entry in @($prependPaths + ($env:Path -split ';'))) {
    if ([string]::IsNullOrWhiteSpace($entry)) {
        continue
    }
    if (-not ($pathEntries | Where-Object { $_ -ieq $entry })) {
        $pathEntries.Add($entry) | Out-Null
    }
}
$env:Path = $pathEntries -join ';'

$AndroidEnvironment = [pscustomobject]@{
    AndroidCliExecutable = $AndroidCliExecutable
    AndroidSdkRoot = $AndroidSdkRoot
    JdkRoot = $JdkRoot
    JdkSource = $JdkSource
    JavaExecutable = $JavaExecutable
    AdbExecutable = $AdbExecutable
    EmulatorExecutable = $EmulatorExecutable
}
