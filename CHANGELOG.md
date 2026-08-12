# Changelog

This file records maintained changes to KRouter. The pre-revival history remains available in Git.

## [0.2.0-beta.1] - 2026-08-12

This prerelease revives source builds and local publication checks. Any GitHub release attachments are for evaluation only; no public Maven repository or production-stable artifact is available.

### Added

- Open-source contribution and vulnerability-reporting guidance.
- Compiler integration coverage for generated provider registries, multi-round processing, and Java/Kotlin injection contracts.
- Gradle TestKit coverage for plugin configuration, KAPT arguments, generated marker assets, and configuration-cache reuse.
- Credential-free repository-local publications for the maintained libraries and Gradle plugin.
- A least-privilege GitHub Actions workflow for JDK 17 builds, tests, Android assembly, and lint.
- Code ownership, weekly dependency-update checks, and pull-request dependency review.

### Changed

- Updated the build to Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.3.21, and Android SDK 35.
- Raised the minimum supported Android API level to 21 while keeping library bytecode compatible with Java 8.
- Reworked annotation-processor type conversion to use supported KotlinPoet APIs and explicit Java-to-Kotlin mappings.
- Accumulated annotations across processing rounds and made `Inject.isRequired` control missing-value failures while optional fields preserve defaults only when no value or Provider exists.
- Aligned generated injector names with JVM binary names for nested Java/Kotlin classes and rejected blank route paths during compilation.
- Made present-but-invalid injected values fail with field/key context while preserving serialization-parser causes.
- Kept embedded `KRouter_` text in module names when discovering generated loaders and removed only the marker prefix.
- Moved the KRouter Gradle plugin to a standalone included build using supported Android variant APIs and cacheable marker generation.
- Mapped Kotlin plugin IDs directly to the verified implementation module and removed stale generated marker files after module renames.
- Packaged Apache-2.0 license metadata in published archives and Maven POMs.
- Reported normal Activity arrival exactly once after the main-thread launch succeeds, and routed asynchronous launch failures to the failure callback.

### Security

- Removed mutable remote Gradle scripts and retired JCenter/Bintray build and publication configuration.
- Added macOS, Linux, and Windows SHA-256 verification metadata for the complete Gradle dependency and plugin graph.
- Staged Robolectric's Android runtime through Gradle dependency verification and disabled its test-time network downloader.
- Escaped generated provider keys and rejected blank or duplicate keys during annotation processing.
- Declared Android component export state explicitly for the modern Android manifest model.
- Made external deep links deny-all by default and bound each allowlisted URI to an exact, enabled, non-exported manifest Activity.
- Honored runtime component-disable overrides during external-policy binding, rejected Activity aliases, and verified that rejecting interceptors launch nothing.
- Rejected duplicate route paths, Provider keys, and interceptor priorities transactionally; external dispatch rejects ambiguous URIs, unknown or duplicate query keys, ClipData, and selectors while stripping incoming extras and flags.
- Added an isolated consumer that strictly verifies third-party dependencies, runs the workflow-local plugin and annotation processor, compiles the public API, and checks reflection entry points and injected target names after shrinking.
- Preserved generated `TypeToken<T>` signatures and the concrete Provider, interceptor, and Fragment constructors used by reflection through R8 full mode, including Providers that do not implement `IProvider`; verified those contracts in the minified release DEX.

### Compatibility notes

- Legacy Bintray/JCenter coordinates are not a supported installation path.
- Upgrade the annotation, compiler, and API modules together and run a clean rebuild; injector names changed for nested classes and target binary names containing underscores.
- `KRouter.create(String)` now removes the URI query from the route key and decodes query values into extras, matching the `Uri` overload.
- External deep-link allow rules must name the exact target `Activity`; the legacy overload without an expected target was removed before this beta to avoid ambiguous authorization.
- Module marker parsing accepts only filenames beginning with `KRouter_`; other inputs now fail explicitly instead of being treated as generated tables.
- The revival branch does not yet promise API or binary stability.
