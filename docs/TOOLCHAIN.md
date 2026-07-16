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

## Repository compatibility

The tracked sources currently require:

- Android Gradle Plugin `9.2.1` and Gradle Wrapper `9.6.0`;
- Kotlin `2.2.10`;
- compile SDK 37, target SDK 36, and minimum SDK 24;
- Java 11 source/target bytecode;
- a compatible Android Studio bundled JBR selected and validated dynamically by the wrapper.

These values come from `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and module build files. Installed SDK package revisions, JBR patch versions, Android Studio connection state, and connected devices are machine state: inspect them with `android info` and `tools/android-doctor.ps1` rather than copying them into tracked documentation.

Some Windows setups persist `JAVA_HOME` or `STUDIO_GRADLE_JDK` as `<android-studio-jbr>\bin`. Gradle requires the JBR root. `tools/android-env.ps1` considers valid configured candidates first and can recover the parent JBR as a final process-local compatibility fallback. It never rewrites the machine setting.

Run `android describe` after dot-sourcing `tools/android-env.ps1` because the command invokes the repository Gradle Wrapper and must inherit the resolved JBR. No permanent environment setting is required.

The wrappers intentionally use raw `$args`; advanced PowerShell parameter binding consumed single-dash adb options such as `-W` and `-a`. When a new outer `powershell -File` process must pass a Gradle `-P...` property literally, put `--%` before the Gradle task/arguments, for example:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.qtpie.simplepuzzle.benchmark.StartupBenchmark
```

`--%` is a PowerShell stop-parsing marker for the invoking shell; it is not stored in scripts and does not change the machine environment.

## Commands

```powershell
android info
android sdk list "platform-tools|build-tools|platforms" --all-versions

powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\android-doctor.ps1
powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\tools\android-env.ps1; android describe --project_dir=."
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 lint
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\adb.ps1 devices -l
```

Do not commit `local.properties`, machine reports, SDK/JDK absolute paths, or changes made only to accommodate one workstation.
