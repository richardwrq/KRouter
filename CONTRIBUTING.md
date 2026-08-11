# Contributing to KRouter

Thank you for helping maintain KRouter. The repository is being revived after a period of limited activity. Historical releases were distributed through Bintray/JCenter, which are no longer active distribution channels. The current revival beta and any GitHub prerelease attachments are intended for source evaluation and compatibility testing; no public Maven artifact is declared production-stable.

## Development baseline

The maintained development baseline is:

- JDK 17
- Android SDK Platform 35
- a Unix-like shell, or PowerShell/cmd with `gradlew.bat` on Windows

Confirm that Gradle is using the intended JDK before diagnosing build failures:

```shell
./gradlew --version
```

The primary verification command is:

```shell
./gradlew --no-daemon clean check assembleDebug lint
```

Report any failure with the exact commit, command, JDK version, Android SDK details, operating system, and relevant error output.

## Making a change

1. Open an issue before making a broad API, routing-semantics, or publishing change.
2. Keep each pull request focused and add tests for behavior changes and bug fixes.
3. Run the verification commands above, or state clearly which checks could not run and why.
4. Update documentation when public APIs, annotations, Gradle configuration, or security boundaries change.
5. Use clear commit messages and do not include generated build output, local SDK paths, credentials, signing material, or IDE-specific secrets.

Pull requests should explain the user-visible effect, compatibility impact, tests performed, and any remaining risk. A passing local build does not by itself authorize publication; release credentials and publishing actions remain maintainer-only operations.

## Security-sensitive areas

KRouter participates in both build-time code generation and runtime Android component dispatch. Changes in these areas need explicit adversarial tests and review:

- Gradle plugins, wrapper files, repositories, dependencies, and publishing configuration execute or resolve code in a developer or CI environment. Do not add mutable remote scripts or unpinned executable downloads.
- Annotation values become generated Kotlin/Java source and route tables. Treat annotation content as untrusted input and use structured escaping rather than string interpolation.
- Deep links and external intents may provide route paths and query parameters that become component arguments. Test malformed, encoded, duplicated, oversized, and unexpected values, and preserve the host application's allowlist boundary.
- Reflection, provider construction, interceptors, and generated registries affect which application classes can be instantiated or launched. Avoid widening access beyond explicitly registered routes.
- CI workflows and dependency updates can alter the software supply chain. Keep workflow permissions minimal and review third-party action and dependency changes carefully.

Do not open a public issue containing an exploit, secret, or detailed vulnerability report. Follow [SECURITY.md](SECURITY.md) instead.

## Licensing

By submitting a contribution, you agree that it may be distributed under the [Apache License 2.0](LICENSE), as described in section 5 of that license.
