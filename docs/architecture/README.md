# Architecture

The map of this repository: what gets published, how a log call and a configuration file move
through the code, which way dependencies point, and where a given kind of change belongs.

[`components.md`](components.md) is the catalogue: one entry per Java package.
`scripts/check-architecture-docs.sh` fails when a package has no entry, an entry names a
package that is gone, or a dependency rule in §4 is broken. It cannot check that the prose is
still true — that is review, and `.claude/skills/architecture-sync/SKILL.md` is how to do it.

## 1. What the system is

A single Android library module, `logback-android/`, that implements the SLF4J API on top of a
port of [logback](https://github.com/qos-ch/logback) 1.2.x, with Android-specific appenders
(logcat, SQLite) and Android-aware configuration (`assets/logback.xml`, `DATA_DIR`/`EXT_DIR`
properties). There is no app and no server; everything ships inside the consumer's APK.

## 2. What gets published

| Output | Built from | Notes |
|---|---|---|
| `com.github.tony19:logback-android` | `jdk11Release` variant | Java 11 bytecode, minSdk 26 |
| `com.github.tony19:logback-android-jdk8` | `jdk8Release` variant | Java 8 bytecode, minSdk 21 |
| `consumer-rules.pro` | inside both AARs | R8/ProGuard keep rules applied to consuming apps |
| `logback.xsd` | repo root, served from the `gh-pages` docs site | namespace `https://tony19.github.io/logback-android/xml` |
| Javadoc + docs site | `gh-pages` branch (`scripts/deploydocs.sh`) | previewed per PR by `docs-preview.yml` |

Both AARs come from one source tree (`logback-android/build.gradle`, flavor dimension `jdk`),
so all code must satisfy the **lower** floor: Java 8 language/library APIs and Android API 21.
`slf4j-api` is an `api` dependency; `android-mail`/`android-activation` are `compileOnly`
(`SMTPAppender` users bring them).

## 3. The shape of a log call

**Startup.** SLF4J finds `org.slf4j.impl.LoggerServiceProvider` through
`META-INF/services/org.slf4j.spi.SLF4JServiceProvider`. It creates the `LoggerContext` and runs
`ContextInitializer.autoConfig()`, which looks for the `logback.configurationFile` system
property, then `assets/logback.xml`. The file is parsed by `JoranConfigurator`
(`classic/joran` rules over the `core/joran` engine), which instantiates appenders, encoders
and policies **by class name via reflection** (`core/joran/util`) — hence the keep rules in
`consumer-rules.pro`. With no file, nothing is attached and logging is silent unless the app
calls `BasicLogcatConfigurator.configureDefaultContext()` itself.

**A call.** `Logger.info(...)` → `TurboFilterList` (`classic/turbo`) → effective-level check →
a `LoggingEvent` (`classic/spi`) → `AppenderAttachableImpl` (`core/spi`) up the logger tree
(respecting additivity) → each appender's filters (`core/filter`) → the appender, which encodes
(`core/encoder`, `classic/pattern`) and writes: logcat, a file (`core/rolling`,
`core/recovery`), SQLite, a socket, syslog, or SMTP.

**Failure.** The library never throws into the app for a configuration or I/O problem; it
records a status (`core/status`). Code paths that can fail report there.

Everything on the call path runs on the caller's thread, from any thread, on every log call:
thread safety and allocation cost matter there more than anywhere else.

## 4. Dependency direction

```
org.slf4j.impl  ──▶  ch.qos.logback.classic  ──▶  ch.qos.logback.core
```

- `core` never imports `classic`. (Checked.)
- `android.*` is imported only from `classic/android`, `core/android` and `core/net/ssl`.
  (Checked; the allow-list is in `scripts/check-architecture-docs.sh`.) Everything else is
  plain Java, which is what lets most tests run on the JVM without Robolectric.
- No new runtime dependencies: the library ships inside other people's apps. Anything beyond
  `slf4j-api` is `compileOnly` and optional.

A change that needs to break one of these is a design question, not a documentation one —
ask before writing it down.

## 5. Where a change goes

| Change | Where |
|---|---|
| A new appender for an Android facility | `classic/android` (+ tests under `src/test/.../classic/android`) |
| A new generic appender, encoder, layout, filter | the matching `core` package, then its `classic` subclass if event-specific |
| A new `%conversion` word | `classic/pattern`, registered in `PatternLayout` |
| A new `logback.xml` element or attribute | a Joran rule/action (`classic/joran`, `core/joran/action`) **and** `logback.xsd` |
| A new `${PROPERTY}` from the Android context | `core/android/AndroidContextUtil` |
| File rollover, naming, compression | `core/rolling`, `core/rolling/helper` |
| Reflection-reached class outside `ch.qos.logback.**` | a keep rule in `consumer-rules.pro` |
| Build flavors, publishing | `logback-android/build.gradle`, `gradle.properties` |
| CI | `.github/workflows/` |
| Planned work, requirements | [`docs/PLAN.md`](../PLAN.md); design for a feature in `docs/design/<feature>/SPEC.md` |

## 6. Keeping this map true

The map changes **in the same commit** as the code it describes. Adding, removing, renaming or
moving a package; changing a dependency direction; adding a published output or a runtime
dependency; or moving a responsibility between packages — each updates `components.md` and,
where visible here, this file. See `.claude/skills/architecture-sync/SKILL.md`.
