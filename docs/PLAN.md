# logback-android — Plan

What this library is for, the constraints every change works within, the milestones, and the
requirements they trace to. The architecture — packages, dependency direction, where a change
goes — is in [`docs/architecture/README.md`](architecture/README.md).

## 1. Product summary

`logback-android` is a lite port of [logback](https://logback.qos.ch) for Android: an SLF4J
implementation configured from `assets/logback.xml`, logging to several destinations at once —
logcat, files (with rolling), SQLite, sockets, syslog and email. It ships inside other people's
apps, so it has to be small, quiet on failure, safe from any thread, and stable across
releases.

## 2. Constraints

- **Two artifacts, one source tree.** `logback-android` (Java 11, minSdk 26) and
  `logback-android-jdk8` (Java 8, minSdk 21). Code targets the lower floor.
- **Upstream parity.** Ported classes stay close to upstream logback in shape and naming, so
  fixes can move between the two. Deliberate divergence is commented at the site.
- **No new runtime dependencies** beyond `slf4j-api`. Optional integrations are `compileOnly`.
- **Public API is a contract.** Breaking changes need a `@Deprecated` transition and a
  major version (`feat!:` / `build!:`).
- **Configuration is untrusted input.** Bad XML produces a status message, not a crash.

## 3. Milestones

> **This list is the plan, not the whole backlog.** Work also gets deferred into open GitHub
> issues, into `docs/design/<feature>/SPEC.md`, and into the open questions of merged design
> PRs — none of which are visible from the milestone lines below. Read those before starting a
> milestone and fold in whatever it already touches, so a milestone does not edit a file and
> leave a known defect sitting in it. `.claude/skills/phase-start/SKILL.md` is the checklist.

Status: ✅ done · 🔶 in progress · ⬜ planned. A milestone is one reviewable PR (or a small,
named set of them). When one ships, tick it here and in §4 in the same PR.

### Phase 0 — Foundations

- ✅ **M0.1 JDK-specific artifacts.** Publish `logback-android` (Java 11) and
  `logback-android-jdk8` from flavors of one module; modernize the toolchain. #413
- ✅ **M0.2 Consumer R8/ProGuard rules.** Ship `consumer-rules.pro` in the AAR; fix the
  `logback.xsd` regex escape. #418
- ✅ **M0.3 Rollover durability.** No malformed archives or lost logs on abrupt app death;
  keep non-expired period dirs during async cleanup. #427, #415
- ✅ **M0.4 Android storage properties.** `EXT_FILES_DIR`/`EXT_CACHE_DIR`; tolerate broken
  external storage; lazy Android props (no main-thread disk reads). #430, #431, #434, #435
- ✅ **M0.5 In-repo static analysis.** Checkstyle, PMD and CodeQL replace Codacy. #436, #444
- ✅ **M0.6 Draft-aware CI and docs preview.** #474
- 🔶 **M0.7 Agent-ready repository.** `docs/PLAN.md`, `docs/architecture/` with its check,
  `docs/design/`, and the `phase-start` / `architecture-sync` skills.

### Phase 1 — Close the open-issue backlog

Each of these issues is older than a merged fix in the same area. The milestone is to
reproduce the report against the current code with a test, then either close it as fixed by
the earlier PR or fix what remains.

- ⬜ **M1.1 External storage paths.** #366 (can't write under `getExternalFilesDir()`), #223
  (`EXT_DIR` depends on Android version). Related: M0.4.
- ⬜ **M1.2 Rolling file reliability.** #135 (`SizeBasedTriggeringPolicy` with relative
  paths), #371 (logs lost across device restart). Related: M0.3.
- ⬜ **M1.3 Minified release builds.** #344 (`DateTokenConverter` not found), #352 (release-only
  exception in a Compose app). Related: M0.2 — `consumer-rules.pro` already cites both.

Later phases are not planned yet. Add them here — with a design spec in `docs/design/` first
for anything larger than a fix.

## 4. Requirements traceability

| Req | Summary | Milestone | Proof | Status |
|---|---|---|---|---|
| R1 | SLF4J 2.x provider, with SLF4J 1.7 binder compatibility | — | `org.slf4j.impl.*Test` (`InitializationOutputTest`, `RecursiveInitializationTest`) | ✅ |
| R2 | Auto-configuration from `assets/logback.xml` or `logback.configurationFile` | — | `ContextInitializerTest` | ✅ |
| R3 | Configuration schema published and valid | M0.2 | `LogbackSchemaTest` | ✅ |
| R4a | Logcat destination | — | `LogcatAppenderTest` | ✅ |
| R4b | File destination with rolling | M0.3, M1.2 | `FileAppenderTest`, `RollingFileAppenderTest`, `TimeBasedRollingTest`, `SizeBasedRollingTest` | 🔶 |
| R4c | SQLite destination | — | `SQLiteAppenderTest` | ✅ |
| R4d | Socket destinations | — | `SocketAppenderMessageLossTest`, `ServerSocketReceiverTest`, `HardenedObjectInputStreamTest`, `SSLHostnameVerificationTest` | ✅ |
| R4e | Syslog destination | — | `SyslogAppenderTest`, `SyslogAppenderBaseTest` | ✅ |
| R4f | Email destination | — | `SMTPAppender_GreenTest`, `SMTPAppenderBaseTest` | ✅ |
| R5 | Android storage properties resolve on every supported API level | M0.4, M1.1 | `AndroidContextUtilTest` | 🔶 |
| R6 | No disk reads on the main thread during initialization | M0.4 | `Issue383StrictModeTest` | ✅ |
| R7 | Works in R8/ProGuard-minified release builds | M0.2, M1.3 | `consumer-rules.pro`; no automated minified-build test yet | 🔶 |
| R8 | Both artifacts build and pass tests at their own floor | M0.1 | CI `testJdk11DebugUnitTest`, `testJdk8DebugUnitTest`, logback-test-app | ✅ |
