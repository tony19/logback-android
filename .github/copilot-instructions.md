# Copilot instructions for logback-android

These instructions guide GitHub Copilot code review and Copilot Chat for this
repository. `logback-android` is a lite port of [logback](http://logback.qos.ch)
for Android, providing a configurable logging framework (files, SQLite, logcat,
sockets, syslog, email).

## Project facts

- **Language / build:** Java, built with Gradle (`./gradlew`). The library module
  lives under `logback-android/`.
- **Minimum Android API:** 21+ (older docs mention API 9). Keep code compatible
  with the project's configured `minSdk`; avoid APIs newer than the supported
  range without guarding them.
- **Java level:** Match the source/target compatibility already set in the Gradle
  build. Do not introduce language features above that level.
- **Tests:** JUnit tests live under `logback-android/src/test/java`. Run with
  `./gradlew test`.
- **Upstream parity:** Much of the code mirrors upstream logback. Prefer changes
  that stay close to upstream behavior and naming so the port remains easy to
  sync.

## What to focus on in reviews

- **Correctness of logging behavior:** appenders, encoders, layouts, filters, and
  configuration (`logback.xml` / Joran) parsing. Watch for regressions in log
  ordering, level filtering, and marker handling.
- **Thread safety:** logging is called from arbitrary threads. Flag shared mutable
  state without synchronization, and check that appenders handle concurrent
  `append()` calls safely.
- **Resource handling:** files, streams, sockets, and database handles must be
  closed (prefer try-with-resources). Flag leaks, especially in appenders and
  rolling policies.
- **Android constraints:** avoid heavy work on the main thread, avoid reflection
  that breaks under R8/ProGuard, and keep dependencies minimal. Flag use of
  desktop-JVM-only APIs not available on Android.
- **Null safety and input validation:** configuration comes from untrusted XML;
  validate and fail gracefully rather than throwing raw NPEs.
- **Performance:** logging is on hot paths. Flag unnecessary allocations, string
  concatenation in place of parameterized logging, and work done even when a log
  level is disabled.
- **Backward compatibility:** this is a published library. Flag breaking changes
  to public APIs, and note when a change needs a `@Deprecated` transition instead.

## Style

- Follow the surrounding code's formatting and naming; match upstream logback
  conventions where the file is a port.
- Keep public API changes documented (Javadoc) and covered by tests.
- Prefer small, focused changes; call out unrelated refactors mixed into a PR.

## What not to flag

- Existing upstream code style that predates the change under review.
- Generated or vendored files.
- Minor stylistic nits that a formatter would handle — focus on substance.
