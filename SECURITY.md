# Security Policy

## Support status

KRouter is currently undergoing maintenance and revival. No currently published package version is declared supported, and historical Bintray/JCenter artifacts are unsupported. Security reports about the current source are still welcome and will help shape the next maintained release.

| Version or source | Status |
| --- | --- |
| Current default branch | Under maintenance; reports accepted |
| Historical Bintray/JCenter artifacts | Unsupported |

## Reporting a vulnerability

If this repository has GitHub private vulnerability reporting enabled, use **Security → Advisories → Report a vulnerability**. This keeps the report and follow-up work private.

If that option is not available, contact the maintainer through the GitHub repository or the maintainer's GitHub profile to request a private reporting channel. Do not include exploit instructions, proof-of-concept code, credentials, personal data, or other sensitive details in a public issue or discussion.

In the private report, include:

- the affected commit and modules;
- the required attacker access and trust boundary;
- reproducible steps or a minimal proof of concept;
- the observed and expected behavior;
- the potential impact; and
- any suggested mitigation.

Reports will be acknowledged when maintainer capacity permits. Because the project is in revival and has no funded response team or response-time guarantee, no service-level deadline is promised. Please coordinate public disclosure with the maintainer so users have a reasonable opportunity to update.

## Security boundaries

The following areas receive particular scrutiny:

- **Build supply chain:** Gradle plugins and scripts execute with developer or CI privileges. Repository changes, wrapper downloads, dependency substitutions, publishing logic, and third-party CI actions can introduce malicious code or unauthorized network access. Mutable remote scripts and exposed publishing credentials are not acceptable.
- **Generated code:** Route, provider, interceptor, and injection annotations feed a compiler plugin/annotation processor. Unescaped annotation values can corrupt generated source or inject unintended code, while malformed registries can expose classes that were not meant to be routable.
- **Deep links and intents:** An exported proxy activity may accept attacker-controlled URI paths and query parameters. Those values can influence route selection and component extras, so validation, decoding, duplicate parameters, unexpected schemes/hosts, and explicit-intent bypasses require testing.
- **Runtime dispatch:** Registered routes can launch activities or services, create fragments or providers, invoke interceptors, and inject values through reflection. A change that weakens route registration or type checks can cross application authorization boundaries.
- **Secrets and network behavior:** Local properties, CI secrets, signing keys, repository credentials, and API tokens must never be committed, logged, embedded in artifacts, or sent to an undeclared endpoint.
- **Third-party contributions:** Pull requests can modify build logic, generated-code templates, manifests, CI workflows, or dependency coordinates. Such changes may execute during review or later compromise downstream builds, so they require least-privilege CI and human review before merge.

Security review should consider both the library's runtime behavior and the privileges of build tooling. Do not run an untrusted pull request with release credentials, signing keys, or write-enabled repository tokens.

## Out of scope

General support requests and failures limited to unsupported historical artifacts are not security vulnerabilities. Reports generated solely by an automated scanner should include a concrete, reproducible impact in KRouter's code or build process.
