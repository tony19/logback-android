# Component catalogue

One entry per Java package under `logback-android/src/main/java`. The heading's backticked
path is what `scripts/check-architecture-docs.sh` matches on, so it must be exact. Each entry
says what the package is responsible for, and anything that must stay true of it.

Most packages are ports of the same package in upstream
[logback](https://github.com/qos-ch/logback) (1.2.x line). "Android-only" marks code with no
upstream counterpart.

See [README.md](README.md) for the map these fit into.

## SLF4J binding

### `org/slf4j/impl` — `org.slf4j.impl`

The SLF4J entry point. `LoggerServiceProvider` is the SLF4J 2.x `SLF4JServiceProvider`
registered in `src/main/resources/META-INF/services`; it creates the `LoggerContext` and runs
`ContextInitializer`. The `Static*Binder` classes keep SLF4J 1.7 bindings working. Only this
package and `classic` may depend on SLF4J types in public signatures.

## classic — the SLF4J-facing logger

### `ch/qos/logback/classic` — `ch.qos.logback.classic`

`Logger`, `LoggerContext`, `Level`, `AsyncAppender`, `PatternLayout`, `BasicConfigurator`.
The logger tree, level inheritance, and the hot path of a log call (see README §3). Thread
safety of `Logger` and `LoggerContext` is load-bearing: they are called from every app thread.

### `ch/qos/logback/classic/android` — `ch.qos.logback.classic.android`

Android-only appenders: `LogcatAppender`, `SQLiteAppender` (+ `SQLiteLogCleaner`), and
`BasicLogcatConfigurator`, which an app calls to log to logcat without a `logback.xml`. One of the few
packages allowed to import `android.*`.

### `ch/qos/logback/classic/boolex` — `ch.qos.logback.classic.boolex`

Boolean evaluators over `ILoggingEvent` (e.g. `OnMarkerEvaluator`, `OnErrorEvaluator`), used by
`EvaluatorFilter` and `SMTPAppender` triggers. Upstream's Janino evaluator is not ported.

### `ch/qos/logback/classic/db` — `ch.qos.logback.classic.db`

`SQLBuilder`: the SQL statements `SQLiteAppender` uses to create and fill its tables.

### `ch/qos/logback/classic/db/names` — `ch.qos.logback.classic.db.names`

Table and column name strategies for the SQLite schema, so users can rename tables/columns.

### `ch/qos/logback/classic/encoder` — `ch.qos.logback.classic.encoder`

`PatternLayoutEncoder`, the default encoder for file and stream appenders.

### `ch/qos/logback/classic/filter` — `ch.qos.logback.classic.filter`

`LevelFilter` and `ThresholdFilter`.

### `ch/qos/logback/classic/html` — `ch.qos.logback.classic.html`

`HTMLLayout` and its CSS builders for logging events.

### `ch/qos/logback/classic/joran` — `ch.qos.logback.classic.joran`

`JoranConfigurator` for classic: the rule set that maps `logback.xml` elements to actions.
Any new configuration element needs a rule here and an entry in `logback.xsd`.

### `ch/qos/logback/classic/joran/action` — `ch.qos.logback.classic.joran.action`

Classic-specific Joran actions: `<configuration>`, `<logger>`, `<root>`, `<level>`,
`<contextName>`, `<receiver>`, etc.

### `ch/qos/logback/classic/layout` — `ch.qos.logback.classic.layout`

`TTLLLayout`, a fixed-format layout.

### `ch/qos/logback/classic/log4j` — `ch.qos.logback.classic.log4j`

`XMLLayout`, emitting events in log4j's XML format.

### `ch/qos/logback/classic/net` — `ch.qos.logback.classic.net`

Network appenders and receivers for logging events: `SocketAppender`, `SSLSocketAppender`,
`SyslogAppender`, `SMTPAppender`, `SocketReceiver`, and the simple socket servers. Blocking I/O
here must stay off the caller's thread.

### `ch/qos/logback/classic/net/server` — `ch.qos.logback.classic.net.server`

Server-socket appenders and receivers (`ServerSocketAppender`, `ServerSocketReceiver`), and
`HardenedLoggingEventInputStream`, which restricts deserialization to known classes. Do not
widen that allow-list without a security review.

### `ch/qos/logback/classic/pattern` — `ch.qos.logback.classic.pattern`

The conversion words of `PatternLayout` (`%logger`, `%msg`, `%thread`, `%caller`, …) and the
throwable proxies' converters. Converters run on every log call; keep them allocation-light.

### `ch/qos/logback/classic/pattern/color` — `ch.qos.logback.classic.pattern.color`

`HighlightingCompositeConverter`: level-based ANSI colouring.

### `ch/qos/logback/classic/selector` — `ch.qos.logback.classic.selector`

`ContextSelector` implementations (default and static).

### `ch/qos/logback/classic/sift` — `ch.qos.logback.classic.sift`

`SiftingAppender` for events, and its discriminators (MDC, context name).

### `ch/qos/logback/classic/spi` — `ch.qos.logback.classic.spi`

`ILoggingEvent`, `LoggingEvent`, `LoggingEventVO`, throwable proxies, caller data, and
`LoggerContextListener`. `LoggingEventVO` is serialized over the network: its shape is a wire
format.

### `ch/qos/logback/classic/turbo` — `ch.qos.logback.classic.turbo`

`TurboFilter`s, evaluated before a `LoggingEvent` is built (`MDCFilter`, `MarkerFilter`,
`DuplicateMessageFilter`, `ReconfigureOnChangeFilter`, …).

### `ch/qos/logback/classic/util` — `ch.qos.logback.classic.util`

`ContextInitializer` (configuration discovery: the `logback.configurationFile` system
property, then `assets/logback.xml`), `LogbackMDCAdapter`, and small helpers.

## core — the logger-agnostic framework

`core` knows nothing of `classic`; `scripts/check-architecture-docs.sh` enforces it.

### `ch/qos/logback/core` — `ch.qos.logback.core`

`Context`, `Appender`, `AppenderBase`, `UnsynchronizedAppenderBase`, `OutputStreamAppender`,
`FileAppender`, `ConsoleAppender`, `Layout`, `CoreConstants`. The appender base classes decide
the locking model of every appender built on them.

### `ch/qos/logback/core/android` — `ch.qos.logback.core.android`

Android-only: `AndroidContextUtil` (resolves `DATA_DIR`, `EXT_DIR`, package name, etc. for
configuration properties) and `SystemPropertiesProxy`. Lookups are lazy so configuration does
not read disk on the main thread (see #383).

### `ch/qos/logback/core/boolex` — `ch.qos.logback.core.boolex`

`EventEvaluator` and its base classes.

### `ch/qos/logback/core/encoder` — `ch.qos.logback.core.encoder`

`Encoder`, `EncoderBase`, `LayoutWrappingEncoder`, `EchoEncoder`.

### `ch/qos/logback/core/filter` — `ch.qos.logback.core.filter`

`Filter`, `AbstractMatcherFilter`, `EvaluatorFilter`.

### `ch/qos/logback/core/helpers` — `ch.qos.logback.core.helpers`

`CyclicBuffer`, `NOPAppender`, HTML escaping, and similar helpers.

### `ch/qos/logback/core/hook` — `ch.qos.logback.core.hook`

Shutdown hooks (`DefaultShutdownHook`) installed by `<shutdownHook>`.

### `ch/qos/logback/core/html` — `ch.qos.logback.core.html`

`HTMLLayoutBase` and CSS builder interfaces.

### `ch/qos/logback/core/joran` — `ch.qos.logback.core.joran`

`GenericConfigurator` and `JoranConfiguratorBase`: the XML configuration engine shared by
`classic`. Configuration XML is untrusted input: fail with a status message, not an exception.

### `ch/qos/logback/core/joran/action` — `ch.qos.logback.core.joran.action`

Generic Joran actions: `<appender>`, `<property>`, `<define>`, `<include>`, `<if>`,
nested components, implicit bean properties.

### `ch/qos/logback/core/joran/event` — `ch.qos.logback.core.joran.event`

SAX recording of configuration XML into replayable events.

### `ch/qos/logback/core/joran/spi` — `ch.qos.logback.core.joran.spi`

The interpreter, rule store, element paths, and `ConfigurationWatchList`.

### `ch/qos/logback/core/joran/util` — `ch.qos.logback.core.joran.util`

Bean introspection (`PropertySetter`, `BeanDescriptionCache`) used to apply XML attributes to
components. Reflection lives here, which is why `consumer-rules.pro` keeps all of
`ch.qos.logback.**` in consuming apps' R8/ProGuard builds; a new reflective lookup outside that
namespace (e.g. of an app class) needs a keep rule there too.

### `ch/qos/logback/core/net` — `ch.qos.logback.core.net`

Transport bases for socket, syslog and SMTP appenders; `HardenedObjectInputStream`.

### `ch/qos/logback/core/net/server` — `ch.qos.logback.core.net.server`

Server-socket runner, listeners and client handling for server-socket appenders.

### `ch/qos/logback/core/net/ssl` — `ch.qos.logback.core.net.ssl`

SSL configuration beans (`<ssl>`: keystore, truststore, protocols, cipher suites, hostname
verification). Imports `android.os.Build` to guard API-level-dependent socket settings
(hostname verification needs API 24).

### `ch/qos/logback/core/pattern` — `ch.qos.logback.core.pattern`

`PatternLayoutBase`, `Converter` chain and formatting.

### `ch/qos/logback/core/pattern/color` — `ch.qos.logback.core.pattern.color`

ANSI colour converters (`%red`, `%boldGreen`, …).

### `ch/qos/logback/core/pattern/parser` — `ch.qos.logback.core.pattern.parser`

Tokenizer, parser and compiler for pattern strings.

### `ch/qos/logback/core/pattern/util` — `ch.qos.logback.core.pattern.util`

Escape utilities for patterns and regexes.

### `ch/qos/logback/core/property` — `ch.qos.logback.core.property`

`PropertyDefiner`s such as `FileExistsPropertyDefiner`.

### `ch/qos/logback/core/read` — `ch.qos.logback.core.read`

`CyclicBufferAppender`, an in-memory appender for reading recent events back.

### `ch/qos/logback/core/recovery` — `ch.qos.logback.core.recovery`

`ResilientOutputStreamBase`, `ResilientFileOutputStream`, `ResilientSyslogOutputStream` and
`RecoveryCoordinator`: reopen-on-failure streams used by file and syslog appenders.

### `ch/qos/logback/core/rolling` — `ch.qos.logback.core.rolling`

`RollingFileAppender` and its rolling/triggering policies (time-based, size-and-time-based,
fixed-window, size-based). Known open defects live here (#135, #371).

### `ch/qos/logback/core/rolling/helper` — `ch.qos.logback.core.rolling.helper`

File-name patterns, date/int token converters, compression, archive removal, and renaming.

### `ch/qos/logback/core/sift` — `ch.qos.logback.core.sift`

`SiftingAppenderBase`, `AppenderTracker`, and discriminator interfaces.

### `ch/qos/logback/core/spi` — `ch.qos.logback.core.spi`

Lifecycle, `ContextAware`, `AppenderAttachable(Impl)`, `FilterAttachable`, `PropertyContainer`,
`ScanException`. `AppenderAttachableImpl` is the fan-out on every log call.

### `ch/qos/logback/core/status` — `ch.qos.logback.core.status`

Internal status messages (`StatusManager`, listeners, `OnConsoleStatusListener`). The way the
library reports its own problems without throwing into the app.

### `ch/qos/logback/core/subst` — `ch.qos.logback.core.subst`

`${var:-default}` variable substitution in configuration.

### `ch/qos/logback/core/util` — `ch.qos.logback.core.util`

Shared utilities: `OptionHelper`, `FileSize`, `Duration`, `FileUtil`, `Loader`,
`CachingDateFormatter`, `EnvUtil`, `ExecutorServiceUtil`.
