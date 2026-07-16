# ADR 0004: Android Studio embedded JBR

Status: accepted, 2026-07-15.

## Context

The workstation has no standalone JDK and its ambient Java setting points one directory too deep. SDK and Java discovery are separate concerns; committing workstation paths would make the project non-portable.

## Decision

Android CLI is authoritative for the SDK. Repository PowerShell wrappers parse `android info`, validate derived SDK executables, resolve a compatible Android Studio bundled JBR through ordered dynamic discovery, and set process-local environment variables only. All automated Gradle and adb calls use the wrappers.

## Consequences

- builds do not require installing or permanently configuring a JDK;
- no SDK/JDK absolute path is committed;
- wrapper failures are actionable;
- direct uninitialized `gradlew.bat`, bare `adb`, and bare global Gradle calls are unsupported.
