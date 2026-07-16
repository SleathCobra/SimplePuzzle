# Windows toolchain

Jigsaw Math uses Android CLI for Android SDK discovery, Android Studio's bundled JetBrains Runtime (JBR) for Java, and the repository Gradle Wrapper for builds. A standalone JDK, global Gradle installation, and bare SDK-tool commands are neither required nor supported by the repository workflow.

## Repository wrappers

- `tools/android-env.ps1` runs `android info`, validates the reported SDK, resolves a compatible JDK, and sets `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `JAVA_HOME`, and `PATH` only for the current process.
- `tools/gradle.ps1` initializes that environment and forwards its raw `$args` to `gradlew.bat`.
- `tools/adb.ps1` initializes that environment and forwards its raw `$args` to the exact `<android-sdk>\platform-tools\adb.exe`.
- `tools/android-doctor.ps1` validates and reports the Android CLI, SDK, JDK, Java, Gradle, adb, installed tools, device connection, and optional Android Studio connection.

None of these scripts writes permanent environment variables or contains a fixed machine path.

## Discovery order

The SDK root is parsed from `android info`; it is never guessed. The environment script then validates adb and emulator beneath that root.

The JDK is resolved independently, in this order:

1. project Gradle JDK configuration;
2. `STUDIO_GRADLE_JDK`;
3. a valid `JAVA_HOME`;
4. a running `studio64` process;
5. Windows Android Studio installation information;
6. dynamically expanded standard Android Studio and JetBrains Toolbox roots;
7. as a final compatibility recovery, the parent of a malformed environment value that names `<android-studio-jbr>\bin` rather than `<android-studio-jbr>`.

Every selected candidate must pass `<jdk>\bin\java.exe -version`. Gradle compatibility is then established with `tools/gradle.ps1 --version`.

## Verified baseline (2026-07-16)

- Android CLI: `1.0.15857036`
- SDK source: the root returned by `android info`
- installed platform-tools: `37.0.0`
- installed Build Tools: `36.0.0`, `36.1.0`, and `37.0.0`
- installed platforms: Android `36.1` and `37.0`
- JDK: Android Studio bundled JBR, OpenJDK `21.0.10`
- JDK discovery source: recovery from an existing malformed `JAVA_HOME` ending in `jbr\bin`
- Gradle Wrapper: `9.6.0`
- Gradle daemon criteria: Java 21
- connected device: `Medium_Phone` AVD (`emulator-5554`), Android 17/API 37, x86_64
- Android Studio CLI: installed; no running Studio instance, so IDE integration is currently unavailable but non-blocking

The SDK's optional `cmdline-tools` directory was not present. Android CLI and all packages required by the current build were available, so no SDK installation or broad update was performed.

An initial direct `android describe --project_dir=.` exposed the malformed ambient `JAVA_HOME`. Re-running the command after dot-sourcing `tools/android-env.ps1` completed successfully and located the debug APK. No permanent environment setting was changed.

The wrappers intentionally use raw `$args`; advanced PowerShell parameter binding consumed single-dash adb options such as `-W` and `-a`. When a new outer `powershell -File` process must pass a Gradle `-P...` property literally, put `--%` before the Gradle task/arguments, for example:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.qtpie.simplepuzzle.benchmark.StartupBenchmark
```

`--%` is a PowerShell stop-parsing marker for the invoking shell; it is not stored in scripts and does not change the machine environment.

## Commands

```powershell
android info
android describe --project_dir=.
android sdk list "platform-tools|build-tools|platforms" --all-versions

powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\android-doctor.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 lint
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\adb.ps1 devices -l
```

To describe the project when the machine's ambient Java configuration is invalid:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\tools\android-env.ps1; android describe --project_dir=."
```

Do not commit `local.properties`, machine reports, SDK/JDK absolute paths, or changes made only to accommodate one workstation.
