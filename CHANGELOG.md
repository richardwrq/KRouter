# Changelog

This file records maintained changes to KRouter. The pre-revival history remains available in Git.

## [0.2.0-beta.1] - Unreleased

This is a source-only revival target. No `0.2.0-beta.1` artifact or GitHub Release has been published.

### Added

- Open-source contribution and vulnerability-reporting guidance.
- Compiler integration coverage for generated provider registries, including escaped keys.
- Gradle TestKit coverage for plugin configuration, KAPT arguments, generated marker assets, and configuration-cache reuse.
- Credential-free repository-local publications for the maintained libraries and Gradle plugin.
- A least-privilege GitHub Actions workflow for JDK 17 builds, tests, Android assembly, and lint.
- Code ownership, weekly dependency-update checks, and pull-request dependency review.

### Changed

- Updated the build to Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.3.21, and Android SDK 35.
- Raised the minimum supported Android API level to 21 while keeping library bytecode compatible with Java 8.
- Reworked annotation-processor type conversion to use supported KotlinPoet APIs and explicit Java-to-Kotlin mappings.
- Moved the KRouter Gradle plugin to a standalone included build using supported Android variant APIs and cacheable marker generation.

### Security

- Removed mutable remote Gradle scripts and retired JCenter/Bintray build and publication configuration.
- Escaped generated provider keys and rejected blank or duplicate keys during annotation processing.
- Declared Android component export state explicitly for the modern Android manifest model.
- Made external deep links deny-all by default and added an immutable exact URI/Activity allowlist.
- Rejected ambiguous external URIs, unknown or duplicate query keys, inherited extras, and unsafe Intent flags before dispatch.

### Compatibility notes

- Legacy Bintray/JCenter coordinates are not a supported installation path.
- The revival branch does not yet promise API or binary stability.
