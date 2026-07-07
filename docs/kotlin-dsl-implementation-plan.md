# Kotlin Configuration DSL for logback-android — Implementation Plan

**Status:** Proposed
**Target artifact:** `com.github.tony19:logback-android-ktx`
**Audience:** This plan is written so that it can be executed step-by-step with minimal
ambiguity. Every file to create is listed with its full path and a near-complete code
skeleton. Follow the phases in order; each phase ends with a verification command that
must pass before moving on.

---

## 1. Goal

Give Android/Kotlin users a type-safe, IDE-discoverable way to configure logback-android
in code, replacing both `assets/logback.xml` and verbose manual bean wiring
(see `BasicLogcatConfigurator` for how verbose that is today:
`logback-android/src/main/java/ch/qos/logback/classic/android/BasicLogcatConfigurator.java:55-75`).

### Target end-user API (this is the spec — the DSL must make exactly this compile)

```kotlin
// In Application.onCreate()
logbackAndroid {
  val logcat = logcatAppender {
    encoder { pattern = "%msg" }
    tagEncoder { pattern = "%logger{0}" }
  }

  val file = rollingFileAppender {
    file = "$dataDir/logs/app.log"
    sizeAndTimeBasedRollingPolicy {
      fileNamePattern = "$dataDir/logs/app.%d{yyyy-MM-dd}.%i.log"
      maxFileSize = "5MB"
      maxHistory = 7
      totalSizeCap = "50MB"
    }
    encoder { pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n" }
  }

  root(Level.DEBUG) {
    appender(logcat)
    appender(file)
  }

  logger("com.example.noisy.lib", Level.WARN)
}
```

Design principles (these resolve most ambiguity during implementation):

1. **Real object references, not string refs.** Appender builder functions *return* the
   started appender; `root {}` / `logger {}` blocks attach the returned instance. This is
   type-safe and removes the XML `appender-ref` indirection entirely.
2. **Lifecycle is handled by the DSL, never by the user.** Every builder sets the
   logback `Context`, calls `start()` in the correct order (encoder → policy → appender),
   and validates required fields, reporting errors through the context's `StatusManager`.
3. **No magic.** An appender not attached via `root {}`/`logger {}` logs nowhere (same as
   XML). The DSL emits a `WarnStatus` if the block ends with zero appenders attached.
4. **The core module stays 100% Java.** All Kotlin lives in a new `logback-android-ktx`
   Gradle module so Java-only consumers never pull in the Kotlin stdlib.

---

## 2. Constraints and existing facts (verified against the repo)

| Fact | Where |
|---|---|
| Single Gradle module `:logback-android`, all Java | `settings.gradle` |
| Gradle wrapper 8.1.1, AGP 7.4.2 | `gradle/wrapper/gradle-wrapper.properties`, root `build.gradle:9` |
| Core module: `minSdkVersion 9`, Java 6 source/target | `logback-android/build.gradle:9-15` |
| SLF4J 2.0.7 is `compileOnly`; consumers add it themselves | `logback-android/build.gradle:74`, `gradle.properties:14` |
| Init path: SLF4J `ServiceLoader` → `LoggerServiceProvider` → `ContextInitializer.autoConfig()` → sysprop `logback.configurationFile`, else `assets/logback.xml`, else **silence** | `org/slf4j/impl/LoggerServiceProvider.java:55-62`, `ch/qos/logback/classic/util/ContextInitializer.java:121-147` |
| Programmatic config today = manual bean wiring | `BasicLogcatConfigurator.java` |
| Android property values (`DATA_DIR`, `EXT_DIR`, `PACKAGE_NAME`, `VERSION_CODE`, `VERSION_NAME`) come from `AndroidContextUtil.setupProperties(Context)` (logback `Context`, *not* `android.content.Context`) | `ch/qos/logback/core/android/AndroidContextUtil.java:62-72` |
| `FileSize.valueOf(String)` parses `"5MB"` etc. | `ch/qos/logback/core/util/FileSize.java:59` |
| Tests are JVM/Robolectric 4.10.3 + JUnit 4 | `logback-android/build.gradle:54` |

Key class signatures the DSL wraps (all already exist, do not modify them):

- `LogcatAppender` — `setEncoder(PatternLayoutEncoder)`, `setTagEncoder(PatternLayoutEncoder)`,
  `setCheckLoggable(boolean)` (`ch/qos/logback/classic/android/LogcatAppender.java`)
- `FileAppender<E>` — `setFile(String)`, `setAppend(boolean)`, `setPrudent(boolean)`,
  `setLazy(boolean)`, `setBufferSize(FileSize)`, `setEncoder(Encoder<E>)`
- `RollingFileAppender<E>` — adds `setRollingPolicy(RollingPolicy)`,
  `setTriggeringPolicy(TriggeringPolicy<E>)`
- `TimeBasedRollingPolicy<E>` — `setFileNamePattern(String)`, `setMaxHistory(int)`,
  `setTotalSizeCap(FileSize)`, `setCleanHistoryOnStart(boolean)`, `setParent(FileAppender<?>)`
- `SizeAndTimeBasedRollingPolicy<E>` — adds `setMaxFileSize(FileSize)`
- `FixedWindowRollingPolicy` — `setMinIndex(int)`, `setMaxIndex(int)`, `setFileNamePattern(String)`
- `SizeBasedTriggeringPolicy<E>` — `setMaxFileSize(FileSize)`
- `PatternLayoutEncoder` — `setPattern(String)`
- `ch.qos.logback.classic.Logger` — `setLevel(Level)`, `setAdditive(boolean)`,
  `addAppender(Appender<ILoggingEvent>)`
- `LoggerContext` — `reset()`, `getLogger(String)`, `putProperty(String, String)`

---

## 3. Phase 0 — Build infrastructure

### 0.1 Add the Kotlin Gradle plugin to the root build

Edit root `build.gradle`. In the `buildscript.dependencies` block (where the AGP classpath
is declared, around line 9), add:

```groovy
classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22'
```

Kotlin 1.8.22 is chosen because it is compatible with both Gradle 8.1.1 and AGP 7.4.2.
Do **not** upgrade Gradle or AGP in this change.

### 0.2 Register the new module

Edit `settings.gradle` (currently a single line) to:

```groovy
include ':logback-android'
include ':logback-android-ktx'
```

### 0.3 Create the module

Create directory `logback-android-ktx/` with this `build.gradle`:

```groovy
apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'

android {
    namespace 'com.github.tony19.logback.android.ktx'
    compileSdkVersion 31
    buildToolsVersion '30.0.3'

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = '1.8'
        freeCompilerArgs += ['-Xexplicit-api=strict']
    }

    defaultConfig {
        minSdkVersion 14
        targetSdkVersion 31
        versionCode VERSION_CODE.toInteger()
        versionName VERSION_NAME
    }

    testOptions {
        unitTests {
            includeAndroidResources = true
        }
    }
}

dependencies {
    api project(':logback-android')
    compileOnly "org.slf4j:slf4j-api:${slf4jVersion}"

    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.robolectric:robolectric:4.10.3'
    testImplementation "org.slf4j:slf4j-api:${slf4jVersion}"
}

ext {
    PUBLISH_GROUP_ID = GROUP
    PUBLISH_VERSION = VERSION_NAME
    PUBLISH_ARTIFACT_ID = 'logback-android-ktx'
}

apply from: "${rootProject.projectDir}/gradle/publish-module.gradle"
```

Notes:
- `minSdkVersion 14` (not 9): the Kotlin stdlib and current tooling do not meaningfully
  support pre-14, and AGP only requires a *consumer's* minSdk to be ≥ the library's, so
  the Java core keeps its minSdk 9 story intact.
- `-Xexplicit-api=strict` forces explicit `public` and return types — this is a published
  library API.
- If `gradle/publish-module.gradle` hardcodes anything specific to the core module
  (e.g. `POM_ARTIFACT_ID`), read it first; override per-module via the `ext` block above
  or add `POM_ARTIFACT_ID=logback-android-ktx` to a new
  `logback-android-ktx/gradle.properties` — match whatever mechanism the existing script
  uses. If publishing wiring turns out to be complex, defer it: **comment out the
  `apply from … publish-module.gradle` line and file it as a follow-up**; do not block
  the DSL on publishing plumbing.

Create the source directories:

```
logback-android-ktx/src/main/kotlin/com/github/tony19/logback/android/dsl/
logback-android-ktx/src/test/kotlin/com/github/tony19/logback/android/dsl/
```

(If AGP doesn't pick up `src/main/kotlin` automatically, add
`sourceSets { main.java.srcDirs += 'src/main/kotlin'; test.java.srcDirs += 'src/test/kotlin' }`
inside the `android {}` block.)

### 0.4 Verify

```
./gradlew :logback-android-ktx:assembleDebug
```

Must succeed (empty module). Also run `./gradlew :logback-android:test` once now to
confirm the baseline is green before any changes; core-module tests must be equally green
at the end.

---

## 4. Phase 1 — Core DSL

All files below go in
`logback-android-ktx/src/main/kotlin/com/github/tony19/logback/android/dsl/`
with `package com.github.tony19.logback.android.dsl`. Add the repository's Apache-2.0
license header (copy from any core Java file) to every new file.

### 1.1 `LogbackDsl.kt` — the `@DslMarker`

```kotlin
@DslMarker
public annotation class LogbackDsl
```

Why: prevents accidentally calling an outer builder's method from a nested block
(e.g. calling `logcatAppender {}` from inside an `encoder {}` block becomes a compile
error).

### 1.2 `EncoderDsl.kt` — pattern encoder builder

```kotlin
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.Context

@LogbackDsl
public class PatternEncoderDsl internal constructor(private val context: Context) {
  /** A logback pattern, e.g. `"%d %-5level [%thread] %logger{36} - %msg%n"` */
  public var pattern: String? = null

  /** Charset name, e.g. `"UTF-8"`. Optional. */
  public var charset: String? = null

  internal fun build(): PatternLayoutEncoder {
    val e = PatternLayoutEncoder()
    e.context = context
    e.pattern = requireNotNull(pattern) { "encoder { pattern = ... } is required" }
    charset?.let { e.charset = java.nio.charset.Charset.forName(it) }
    e.start()
    return e
  }
}
```

Note: `PatternLayoutEncoder` inherits `setCharset(Charset)` from `LayoutWrappingEncoder`.
If the compiler can't resolve `e.charset`, call `e.setCharset(...)` explicitly.

### 1.3 `LogcatAppenderDsl.kt`

```kotlin
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.core.Context

@LogbackDsl
public class LogcatAppenderDsl internal constructor(private val context: Context) {
  /**
   * When true, honors `Log.isLoggable(tag, level)` (and truncates tags to 23 chars).
   * Default false = always log.
   */
  public var checkLoggable: Boolean = false

  private var encoderDsl: (PatternEncoderDsl.() -> Unit) = { pattern = "%msg" }
  private var tagEncoderDsl: (PatternEncoderDsl.() -> Unit)? = null

  public fun encoder(block: PatternEncoderDsl.() -> Unit) { encoderDsl = block }

  /** Pattern for the logcat *tag*, e.g. `"%logger{0}"`. Defaults to the logger name. */
  public fun tagEncoder(block: PatternEncoderDsl.() -> Unit) { tagEncoderDsl = block }

  internal fun build(name: String): LogcatAppender {
    val a = LogcatAppender()
    a.context = context
    a.name = name
    a.checkLoggable = checkLoggable
    a.encoder = PatternEncoderDsl(context).apply(encoderDsl).build()
    tagEncoderDsl?.let { a.tagEncoder = PatternEncoderDsl(context).apply(it).build() }
    a.start()
    return a
  }
}
```

Default encoder pattern is `"%msg"` (no trailing `%n` — logcat adds the newline), matching
`BasicLogcatConfigurator`.

### 1.4 `RollingPolicyDsl.kt` — rolling/triggering policy builders

```kotlin
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingPolicy
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.spi.ContextAware
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.classic.spi.ILoggingEvent

@LogbackDsl
public class TimeBasedPolicyDsl internal constructor(private val context: Context) {
  /** e.g. `"$dataDir/logs/app.%d{yyyy-MM-dd}.log"` */
  public var fileNamePattern: String? = null
  /** Max number of archived periods to keep. 0 = unlimited (default). */
  public var maxHistory: Int = 0
  /** e.g. `"100MB"`. Optional total cap across all archives. */
  public var totalSizeCap: String? = null
  public var cleanHistoryOnStart: Boolean = false

  internal open fun build(parent: FileAppender<ILoggingEvent>): TimeBasedRollingPolicy<ILoggingEvent> =
    configure(TimeBasedRollingPolicy(), parent)

  protected fun <T : TimeBasedRollingPolicy<ILoggingEvent>> configure(p: T, parent: FileAppender<ILoggingEvent>): T {
    p.context = context
    p.fileNamePattern = requireNotNull(fileNamePattern) { "fileNamePattern is required" }
    p.maxHistory = maxHistory
    totalSizeCap?.let { p.setTotalSizeCap(FileSize.valueOf(it)) }
    p.isCleanHistoryOnStart = cleanHistoryOnStart
    p.setParent(parent)
    // NOTE: start() is deferred — RollingFileAppenderDsl.build() starts the policy
    // AFTER it is set on the appender, then starts the appender (same order Joran uses).
    return p
  }
}

@LogbackDsl
public class SizeAndTimeBasedPolicyDsl internal constructor(context: Context) :
  TimeBasedPolicyDsl(context) {
  /** e.g. `"5MB"`. Required. The fileNamePattern must contain `%i`. */
  public var maxFileSize: String? = null

  override fun build(parent: FileAppender<ILoggingEvent>): SizeAndTimeBasedRollingPolicy<ILoggingEvent> {
    val p = configure(SizeAndTimeBasedRollingPolicy(), parent)
    p.setMaxFileSize(FileSize.valueOf(requireNotNull(maxFileSize) { "maxFileSize is required" }))
    return p
  }
}

@LogbackDsl
public class FixedWindowPolicyDsl internal constructor(private val context: Context) {
  /** e.g. `"$dataDir/logs/app.%i.log"` — must contain `%i`. */
  public var fileNamePattern: String? = null
  public var minIndex: Int = 1
  public var maxIndex: Int = 7
  /** Triggering threshold, e.g. `"5MB"`. Required. */
  public var maxFileSize: String? = null

  internal fun buildRolling(parent: FileAppender<ILoggingEvent>): FixedWindowRollingPolicy { /* set context, pattern, indices, parent */ }
  internal fun buildTriggering(): SizeBasedTriggeringPolicy<ILoggingEvent> { /* set context + FileSize.valueOf(maxFileSize) */ }
}
```

Implementation detail for the elided bodies: mirror the pattern above — construct, set
`context`, copy each property (with `requireNotNull` for required ones), set parent, and
return **without** calling `start()`; the appender DSL starts policies in Joran's order.

If `TimeBasedPolicyDsl` being `open`/subclassed fights the `internal constructor` +
explicit-api mode, it is acceptable to make the two classes independent (duplicate the
four shared properties) — simpler is fine here.

### 1.5 `FileAppenderDsl.kt`

```kotlin
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.util.FileSize

@LogbackDsl
public open class FileAppenderDsl internal constructor(protected val context: Context) {
  /** Absolute path of the active log file, e.g. `"$dataDir/logs/app.log"`. Required. */
  public var file: String? = null
  public var append: Boolean = true
  /** Defer file creation until first log event (useful before storage is ready). */
  public var lazy: Boolean = false
  /** e.g. `"8KB"`. Optional. */
  public var bufferSize: String? = null

  protected var encoderDsl: (PatternEncoderDsl.() -> Unit)? = null
  public fun encoder(block: PatternEncoderDsl.() -> Unit) { encoderDsl = block }

  internal open fun build(name: String): FileAppender<ILoggingEvent> {
    val a = FileAppender<ILoggingEvent>()
    configureCommon(a, name)
    a.start()
    return a
  }

  protected fun configureCommon(a: FileAppender<ILoggingEvent>, name: String) {
    a.context = context
    a.name = name
    a.file = requireNotNull(file) { "file is required" }
    a.isAppend = append
    a.setLazy(lazy)
    bufferSize?.let { a.setBufferSize(FileSize.valueOf(it)) }
    a.encoder = PatternEncoderDsl(context)
      .apply(requireNotNull(encoderDsl) { "encoder { pattern = ... } is required" })
      .build()
  }
}

@LogbackDsl
public class RollingFileAppenderDsl internal constructor(context: Context) : FileAppenderDsl(context) {
  private var policyFactory: ((RollingFileAppender<ILoggingEvent>) -> Unit)? = null

  public fun timeBasedRollingPolicy(block: TimeBasedPolicyDsl.() -> Unit) {
    policyFactory = { appender ->
      val p = TimeBasedPolicyDsl(context).apply(block).build(appender)
      appender.rollingPolicy = p   // TimeBasedRollingPolicy is also the triggering policy
      p.start()
    }
  }

  public fun sizeAndTimeBasedRollingPolicy(block: SizeAndTimeBasedPolicyDsl.() -> Unit) {
    policyFactory = { appender ->
      val p = SizeAndTimeBasedPolicyDsl(context).apply(block).build(appender)
      appender.rollingPolicy = p
      p.start()
    }
  }

  public fun fixedWindowRollingPolicy(block: FixedWindowPolicyDsl.() -> Unit) {
    policyFactory = { appender ->
      val dsl = FixedWindowPolicyDsl(context).apply(block)
      val rolling = dsl.buildRolling(appender)
      val triggering = dsl.buildTriggering()
      appender.rollingPolicy = rolling
      appender.triggeringPolicy = triggering
      rolling.start()
      triggering.start()
    }
  }

  override fun build(name: String): RollingFileAppender<ILoggingEvent> {
    val a = RollingFileAppender<ILoggingEvent>()
    configureCommon(a, name)
    requireNotNull(policyFactory) { "a rolling policy is required (e.g. timeBasedRollingPolicy { ... })" }
      .invoke(a)
    a.start()
    return a
  }
}
```

Ordering is critical and mirrors Joran: encoder started first, policy set on appender →
`policy.start()` → `appender.start()`.

### 1.6 `LoggerDsl.kt`

```kotlin
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender

@LogbackDsl
public class LoggerDsl internal constructor(private val logger: Logger, private val onAttach: () -> Unit) {
  public fun appender(appender: Appender<ILoggingEvent>) {
    logger.addAppender(appender)
    onAttach()
  }
}
```

(`onAttach` is a callback into `LogbackConfigDsl` used only to track "at least one
appender was attached" for the end-of-block warning.)

### 1.7 `LogbackConfigDsl.kt` — the root builder

```kotlin
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.android.AndroidContextUtil
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.spi.ContextAware
import ch.qos.logback.core.spi.LifeCycle
import ch.qos.logback.core.status.OnConsoleStatusListener
import ch.qos.logback.core.status.WarnStatus

@LogbackDsl
public class LogbackConfigDsl internal constructor(public val context: LoggerContext) {

  init {
    // Populate DATA_DIR / EXT_DIR / PACKAGE_NAME / VERSION_CODE / VERSION_NAME
    // exactly the way XML substitution does (AndroidContextUtil no-arg ctor
    // resolves the app Context reflectively; safe under Robolectric too).
    AndroidContextUtil().setupProperties(context)
  }

  private var attachedCount = 0

  /** `${DATA_DIR}` — the app's files directory. */
  public val dataDir: String get() = prop(CoreConstants.DATA_DIR_KEY)
  /** `${EXT_DIR}` — mounted external storage dir; empty string if unmounted. */
  public val extDir: String get() = prop(CoreConstants.EXT_DIR_KEY)
  public val packageName: String get() = prop(CoreConstants.PACKAGE_NAME_KEY)
  public val versionCode: String get() = prop(CoreConstants.VERSION_CODE_KEY)
  public val versionName: String get() = prop(CoreConstants.VERSION_NAME_KEY)

  private fun prop(key: String): String = context.getProperty(key) ?: ""

  /** Echo logback's own status messages (config errors etc.) to System.out. */
  public var debug: Boolean = false
    set(value) {
      field = value
      if (value) OnConsoleStatusListener.addNewInstanceToContext(context)
    }

  /** Define a context property usable in patterns via `%property{key}`. */
  public fun property(key: String, value: String) { context.putProperty(key, value) }

  public fun logcatAppender(
    name: String = "logcat",
    block: LogcatAppenderDsl.() -> Unit = {}
  ): LogcatAppender = LogcatAppenderDsl(context).apply(block).build(name)

  public fun fileAppender(
    name: String = "file",
    block: FileAppenderDsl.() -> Unit
  ): FileAppender<ILoggingEvent> = FileAppenderDsl(context).apply(block).build(name)

  public fun rollingFileAppender(
    name: String = "rollingFile",
    block: RollingFileAppenderDsl.() -> Unit
  ): RollingFileAppender<ILoggingEvent> =
    (RollingFileAppenderDsl(context).apply(block).build(name)) as RollingFileAppender<ILoggingEvent>

  /**
   * Escape hatch for any other/custom appender: wires context, applies [block],
   * then starts it.
   */
  public fun <T : Appender<ILoggingEvent>> appender(instance: T, name: String, block: T.() -> Unit = {}): T {
    instance.context = context
    instance.name = name
    instance.block()
    instance.start()
    return instance
  }

  public fun root(level: Level = Level.DEBUG, block: LoggerDsl.() -> Unit = {}) {
    val r = context.getLogger(Logger.ROOT_LOGGER_NAME)
    r.level = level
    LoggerDsl(r) { attachedCount++ }.block()
  }

  public fun logger(
    name: String,
    level: Level? = null,
    additive: Boolean = true,
    block: LoggerDsl.() -> Unit = {}
  ) {
    val l = context.getLogger(name)
    level?.let { l.level = it }
    l.isAdditive = additive
    LoggerDsl(l) { attachedCount++ }.block()
  }

  internal fun finish() {
    if (attachedCount == 0) {
      context.statusManager.add(
        WarnStatus("No appenders were attached to any logger; nothing will be logged. " +
          "Attach appenders inside root { } or logger(...) { }.", context)
      )
    }
  }
}
```

If `RollingFileAppenderDsl.build`'s return type already is
`RollingFileAppender<ILoggingEvent>` (recommended — use covariant override), drop the cast.

### 1.8 `Logback.kt` — entry points

```kotlin
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

/**
 * Configures the given [LoggerContext] with a type-safe DSL.
 *
 * @param reset when true (default), wipes any configuration previously loaded
 *   (e.g. from `assets/logback.xml`) before applying this one.
 */
public fun LoggerContext.configure(reset: Boolean = true, block: LogbackConfigDsl.() -> Unit) {
  if (reset) reset()
  val dsl = LogbackConfigDsl(this)
  dsl.block()
  dsl.finish()
}

/**
 * Configures the default logger context (the one SLF4J's `LoggerFactory` uses).
 * Call from `Application.onCreate()`.
 */
public fun logbackAndroid(reset: Boolean = true, block: LogbackConfigDsl.() -> Unit) {
  (LoggerFactory.getILoggerFactory() as LoggerContext).configure(reset, block)
}
```

`reset = true` by default because the ServiceLoader init path may already have loaded
`assets/logback.xml`; without a reset the DSL config would *add to* it, which surprises
users. Document this in KDoc (done above).

### 1.9 Verify Phase 1

```
./gradlew :logback-android-ktx:assembleDebug
```

Must compile with zero warnings from `-Xexplicit-api=strict`. Then write a scratch
`Main.kt` **in the test source set** that contains exactly the target API from §1 and
confirm it compiles (it becomes a real test in Phase 3).

---

## 5. Phase 2 — Kotlin logger extensions (ecosystem parity features)

These are small but high-value additions borrowed from kotlin-logging / Square logcat /
Timber. Same module, new package file
`logback-android-ktx/src/main/kotlin/com/github/tony19/logback/android/LoggerExt.kt`
(package `com.github.tony19.logback.android`):

```kotlin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Lazy message evaluation: the lambda runs only if DEBUG is enabled. */
public inline fun Logger.debug(message: () -> String) {
  if (isDebugEnabled) debug(message())
}
public inline fun Logger.trace(message: () -> String) { if (isTraceEnabled) trace(message()) }
public inline fun Logger.info(message: () -> String) { if (isInfoEnabled) info(message()) }
public inline fun Logger.warn(message: () -> String) { if (isWarnEnabled) warn(message()) }
public inline fun Logger.error(message: () -> String) { if (isErrorEnabled) error(message()) }
public inline fun Logger.error(t: Throwable, message: () -> String) {
  if (isErrorEnabled) error(message(), t)
}

/**
 * Logger with a name inferred from the receiver's class, unwrapping companion objects:
 *
 *     class MyService { private val log = logger() }
 */
public fun Any.logger(): Logger {
  val clazz = this::class.java
  val name = clazz.name
    .removeSuffix("\$Companion")
    .substringBefore("\$\$")          // strip lambda/synthetic suffixes
  return LoggerFactory.getLogger(name)
}

/** Top-level or explicit-name variant: `val log = logger("Sync")` */
public fun logger(name: String): Logger = LoggerFactory.getLogger(name)
```

Keep this file small and boring. Do **not** add fluent builders, markers, or MDC helpers
in this pass.

Verify: `./gradlew :logback-android-ktx:assembleDebug`.

---

## 6. Phase 3 — Tests (Robolectric, JUnit 4)

Location: `logback-android-ktx/src/test/kotlin/com/github/tony19/logback/android/dsl/`.
Annotate each class with `@RunWith(RobolectricTestRunner::class)` and
`@Config(sdk = [28])` (import from `org.robolectric.annotation.Config`).

Common test scaffolding: each test creates a **fresh** `LoggerContext()` directly (do not
use `LoggerFactory` singletons except in the one test for `logbackAndroid`), calls
`context.configure(...) { ... }`, and inspects the resulting object graph. Helper:

```kotlin
private fun LoggerContext.rootAppenders(): List<Appender<ILoggingEvent>> =
  getLogger(Logger.ROOT_LOGGER_NAME).iteratorForAppenders().asSequence().toList()
```

### `ConfigureDslTest.kt`

1. `logcatAppender with defaults produces started appender named "logcat" with %msg pattern`
   — assert `appender.isStarted`, `appender.name == "logcat"`,
   `(appender.encoder as PatternLayoutEncoder).pattern == "%msg"`.
2. `root attaches appenders and sets level` — configure root(Level.INFO) with a logcat
   appender; assert `rootAppenders().size == 1` and root logger level is INFO.
3. `logger() sets level and additivity` — `logger("com.example", Level.WARN, additive = false)`;
   assert on `context.getLogger("com.example")`.
4. `reset=true wipes previous appenders` — attach one appender via a first `configure`,
   run a second `configure(reset = true)` attaching a different one; assert only the
   second remains.
5. `reset=false preserves previous appenders` — same, assert both remain.
6. `no attached appenders adds WarnStatus` — empty block; assert
   `context.statusManager.copyOfStatusList.any { it.message.contains("No appenders") }`.
7. `property() is usable` — `property("k", "v")`; assert `context.getProperty("k") == "v"`.
8. `dataDir is non-empty under Robolectric` — assert `dataDir.isNotEmpty()`.

### `FileAppenderDslTest.kt`

Use `org.junit.rules.TemporaryFolder` (`@get:Rule`) for file paths.

1. `fileAppender writes a log line` — configure fileAppender with
   `file = tmp.root.resolve("app.log").path`, pattern `"%msg%n"`, attach to root, log
   `"hello"` via `context.getLogger("t").info("hello")`, then `context.stop()` and assert
   the file content contains `"hello"`.
2. `fileAppender without file fails with IllegalArgumentException` — assert
   `assertThrows(IllegalArgumentException::class.java) { ... }` (from JUnit 4.13).
3. `fileAppender without encoder fails`.
4. `rollingFileAppender with sizeAndTimeBasedRollingPolicy starts` — assert appender
   `isStarted`, `rollingPolicy is SizeAndTimeBasedRollingPolicy<*>`, policy `isStarted`,
   `maxHistory` propagated.
5. `rollingFileAppender without policy fails`.
6. `fixedWindowRollingPolicy sets both rolling and triggering policies`.

### `LoggerExtTest.kt`

1. `debug lambda not evaluated when level is INFO` — set root level INFO, use a
   `var evaluated = false; log.debug { evaluated = true; "x" }`, assert `!evaluated`.
2. `debug lambda evaluated when level is DEBUG`.
3. `logger() strips Companion` — inside a class with a companion object, assert
   `MyClass.log.name == "…MyClass"` (define the fixture class in the test file).

### Verify

```
./gradlew :logback-android-ktx:test :logback-android:test
```

All green. If Robolectric fails to initialize in the new module, compare
`testOptions`/dependencies against the core module's `build.gradle` (it already runs
Robolectric 4.10.3 successfully) and mirror any missing piece.

---

## 7. Phase 4 — CI, docs, sample

1. **CI:** `.circleci/config.yml` runs bare `./gradlew test` / `assembleDebug` /
   `assembleRelease` style tasks. Confirm the new module is picked up (unqualified task
   names run for all modules). If the config names `:logback-android:` tasks explicitly,
   add the ktx equivalents.
2. **README.md:** add a "Kotlin DSL" section right after the existing Quick Start:
   the Gradle coordinate `com.github.tony19:logback-android-ktx:$version`, the §1 example
   verbatim, and one sentence noting that the DSL replaces `assets/logback.xml` and must
   be called in `Application.onCreate()` **before** any logging occurs (or with the
   default `reset = true`, at any time to reconfigure).
3. **KDoc:** every public symbol already requires docs under explicit-api; ensure each
   builder property lists its XML equivalent (e.g. `file` ↔ `<file>`), so users can
   migrate from existing XML configs mechanically.

---

## 8. Acceptance criteria (final checklist)

- [ ] `./gradlew build` green from a clean checkout (both modules, lint included).
- [ ] The §1 target snippet compiles verbatim against the ktx module (it exists as a test).
- [ ] Core module diff is **empty** except (possibly) root `build.gradle` +
      `settings.gradle` — no Java files modified.
- [ ] No Kotlin stdlib dependency leaks into `:logback-android`'s POM.
- [ ] New module publishes as `com.github.tony19:logback-android-ktx` (or publishing is
      explicitly deferred with a TODO in the module's `build.gradle`).
- [ ] README documents the DSL.

## 9. Known pitfalls (read before starting)

1. **`ch.qos.logback.core.Context` vs `android.content.Context`** — never import the
   Android one in DSL files; every `Context` in this plan is logback's.
2. **Start order matters.** Encoder must be started before the appender; rolling policy
   must be `setParent(appender)`-ed and set on the appender before `policy.start()`, and
   the appender started last. Getting this wrong doesn't throw — it emits status errors
   and silently drops events, which is exactly the debugging misery the DSL exists to prevent.
3. **`SizeAndTimeBasedRollingPolicy` requires `%i`** in `fileNamePattern` and
   `FixedWindowRollingPolicy` requires `%i`; `TimeBasedRollingPolicy` requires `%d`.
   Don't validate this in the DSL — logback already reports it via status — but use
   correct patterns in tests.
4. **Robolectric + `AndroidContextUtil`**: the no-arg constructor resolves the app context
   via reflection on `android.app.AppGlobals`; under Robolectric this works, but if
   `dataDir` comes back empty in tests, construct the DSL's property init defensively
   (wrap `setupProperties` in try/catch and emit a `WarnStatus` instead of crashing —
   recommended regardless, since host-JVM usage of the library shouldn't crash).
5. **Don't call `start()` twice.** `LifeCycle.start()` is not always idempotent; the DSL
   owns lifecycle, so builders must not let users start components.
6. **Kotlin/AGP version drift.** If `assembleDebug` fails with a Kotlin/AGP compatibility
   error, adjust the Kotlin plugin within the 1.8.x line only; do not touch AGP/Gradle.
