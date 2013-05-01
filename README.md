<h1><a href="http://tony19.github.com/logback-android/"><img src="https://github.com/tony19/logback-android/raw/gh-pages/img/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/></a> logback-android</h1>
*Advanced logging library for Android*

[![Release](https://tony19.ci.cloudbees.com/job/logback-android/job/logback-android-RELEASE/badge/icon)](https://tony19.ci.cloudbees.com/job/logback-android/job/logback-android-RELEASE/)
[![Snapshot](https://tony19.ci.cloudbees.com/job/logback-android/job/logback-android-ANALYZE/badge/icon)](https://tony19.ci.cloudbees.com/job/logback-android/job/logback-android-ANALYZE/)

Overview
--------
[`logback-android`][3] brings the power of *logback* to Android. [`logback`][1] is a reliable, generic, fast, and flexible logging library for Java applications.

Runs on Android 2.1 or higher.

The current version is **1.0.10-1**.

Quick Start
-----------
1. Add [`logback-android`][9] and [`slf4j-api`][10] to your project class path.
2. Configure `logback-android` using an [XML file](#configuration-via-xml) or [in-code statements](#configuration-in-code). Otherwise, logging is silently disabled.
3. Use `slf4j-api` in your application to write logging statements as shown in the example below.

*Example:*

```java
package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // SLF4J
    Logger LOG = LoggerFactory.getLogger(MainActivity.class);
    LOG.info("hello world");
  }
}
```


Download
--------

 * [logback-android-1.0.10-1.jar][9]
 * [slf4j-api-1.7.5.jar][10]

OR Maven users can simply add these dependencies to `pom.xml`:

```xml
<dependency>
  <groupId>com.github.tony19</groupId>
  <artifactId>logback-android-core</artifactId>
  <version>1.0.10-1</version>
</dependency>
<dependency>
  <groupId>com.github.tony19</groupId>
  <artifactId>logback-android-classic</artifactId>
  <version>1.0.10-1</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.5</version>
</dependency>
```


Contents
--------
 * [Configuration via XML](#configuration-via-xml)
  * [AndroidManifest.xml](#androidmanifestxml)
  * [Initialization search path](#initialization-search-path)
 * [Configuration in code](#configuration-in-code)
 * [ProGuard](#proguard)
 * [Other Documentation](#other-documentation)
 * [Build](#build)



Configuration via XML
---------------------

`logback-android` can be configured simply by creating `assets/logback.xml`, containing [configuration XML](http://logback.qos.ch/manual/configuration.html#syntax). This file is read automatically upon loading the first logger from your code. Additional code configuration is *not* necessary.

*Example 1: Basic configuration (single destination)*

```xml
<configuration>
  <!-- Create a file appender for a log in the application's data directory -->
  <appender name="file" class="ch.qos.logback.core.FileAppender">
    <file>/data/data/com.example/files/log/foo.log</file>
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <!-- Write INFO (and higher-level) messages to the log file -->
  <root level="INFO">
    <appender-ref ref="file" />
  </root>
</configuration>
```

*Example 2: Advanced configuration (multiple destinations)*

```xml
<configuration>
  <property name="LOG_DIR" value="/data/data/com.example/files" />

  <!-- Create a logcat appender -->
  <appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
    <encoder>
      <pattern>%msg</pattern>
    </encoder>
  </appender>

  <!-- Create a file appender for TRACE-level messages -->
  <appender name="TraceLog" class="ch.qos.logback.core.FileAppender">
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
      <level>TRACE</level>
      <onMatch>ACCEPT</onMatch>
      <onMismatch>DENY</onMismatch>
    </filter>

    <file>${LOG_DIR}/trace.log</file>

    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <!-- Create a file appender for DEBUG-level messages -->
  <appender name="DebugLog" class="ch.qos.logback.core.FileAppender">
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
      <level>DEBUG</level>
      <onMatch>ACCEPT</onMatch>
      <onMismatch>DENY</onMismatch>
    </filter>

    <file>${LOG_DIR}/debug.log</file>

    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <!-- Write TRACE messages from class A to its own log -->
  <logger name="com.example.A" level="TRACE">
    <appender-ref ref="TraceLog" />
  </logger>

  <!-- Write DEBUG messages from class B to its own log -->
  <logger name="com.example.B" level="DEBUG">
    <appender-ref ref="DebugLog" />
  </logger>

  <!-- Write INFO (and higher-level) messages to logcat -->
  <root level="INFO">
    <appender-ref ref="logcat" />
  </root>
</configuration>
```

#### AndroidManifest.xml
`logback-android` also supports configuration XML within your application's `AndroidManifest.xml` (requires setting up your [initialization search path](#initialization-search-path)). Simply put the configuration XML inside the `<manifest>/<logback>` element as shown in the example below.

*Example:*

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk
        android:minSdkVersion="8"
        android:targetSdkVersion="16" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        <activity
            android:name="com.example.MainActivity"
            android:label="@string/app_name" >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

    <logback>
        <configuration>
          <appender
              name="LOGCAT"
              class="ch.qos.logback.classic.android.LogcatAppender" >
              <tagEncoder>
                  <pattern>%logger{0}</pattern>
              </tagEncoder>
              <encoder>
                  <pattern>[ %thread ] %msg%n</pattern>
              </encoder>
          </appender>

          <root level="WARN" >
              <appender-ref ref="LOGCAT" />
          </root>
        </configuration>
    </logback>

</manifest>
```

##### Android Lint Preferences
If you enter configuration XML in `AndroidManifest.xml`, the Android SDK r21+ emits an error message by default due to the `android` namespace prefix missing from the `logback-android` configuration elements. To resolve this, change the _Severity_ level for **MissingPrefix** in your project's Android Lint preferences from Eclipse.


#### Initialization search path
Even though `assets/logback.xml` is the first configuration loaded, this file could include other configuration files from within your JAR or from the host filesystem. This is achieved with the `<includes>` tag, containing a list of **optional** `<include>` tags (i.e., no error is thrown for nonexistent resources/files). The first `<include>` tag that points to an existent resource/file causes the remainder of the list to be ignored.

*Example:*

```xml
<configuration>
  <includes>
    <include file="/sdcard/foo.xml"/>
    <include resource="assets/config/test.xml"/>
    <include resource="AndroidManifest.xml"/>
  </includes>
</configuration>
```

Prior to `v1.0.8-1`, the initialization search path was hard-coded, and that can be recreated with this configuration:

```xml
<configuration>
  <includes>
    <include file="/sdcard/logback/logback-test.xml"/>
    <include file="/sdcard/logback/logback.xml"/>
    <include resource="AndroidManifest.xml"/>
    <include resource="assets/logback-test.xml"/>
    <include resource="assets/logback.xml"/>
  </includes>
</configuration>
```


Configuration in Code
---------------------

If you prefer code-based configuration instead of the XML method above, you can use the `logback` classes directly to initialize `logback-android` as shown in the following examples. Note the direct usage of `logback` classes removes the advantage of the facade provided by SLF4J.

*Example: Configures appenders directly*

```java
package com.example;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    configureLogbackDirectly();

    org.slf4j.Logger log = LoggerFactory.getLogger(MainActivity.class);
    for (int i = 0; i < 10; i++) {
      log.info("hello world");
    }
  }

  private void configureLogbackDirectly() {
    // reset the default context (which may already have been initialized)
    // since we want to reconfigure it
    LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
    lc.reset();

    // setup FileAppender
    PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
    encoder1.setContext(lc);
    encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    encoder1.start();

    FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
    fileAppender.setContext(lc);
    fileAppender.setFile(this.getFileStreamPath("app.log").getAbsolutePath());
    fileAppender.setEncoder(encoder1);
    fileAppender.start();

    // setup LogcatAppender
    PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
    encoder2.setContext(lc);
    encoder2.setPattern("[%thread] %msg%n");
    encoder2.start();

    LogcatAppender logcatAppender = new LogcatAppender();
    logcatAppender.setContext(lc);
    logcatAppender.setEncoder(encoder2);
    logcatAppender.start();

    // add the newly created appenders to the root logger;
    // qualify Logger to disambiguate from org.slf4j.Logger
    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.addAppender(fileAppender);
    root.addAppender(logcatAppender);
  }
}
```

*Example: Configures by XML file*

```java
// snip…

private void configureLogbackByFilePath() {
  // reset the default context (which may already have been initialized)
  // since we want to reconfigure it
  LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
  lc.reset();

  JoranConfigurator config = new JoranConfigurator();
  config.setContext(lc);

  try {
    config.doConfigure("/path/to/config.xml");
  } catch (JoranException e) {
    e.printStackTrace();
  }
}
```

*Example: Configures by in-memory XML string*

```java
// snip…

static final String LOGBACK_XML =
    "<configuration>" +
      "<appender name='FILE' class='ch.qos.logback.core.FileAppender'>" +
         "<file>foo.log</file>" +
         "<append>false</append>" +
         "<encoder>" +
           "<pattern>%-4r [%t] %-5p %c{35} - %m%n</pattern>" +
         "</encoder>" +
       "</appender>" +
       "<root level='DEBUG'>" +
         "<appender-ref ref='FILE' />" +
       "</root>" +
    "</configuration>"
    ;

private void configureLogbackByString() {
  // reset the default context (which may already have been initialized)
  // since we want to reconfigure it
  LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
  lc.reset();

  JoranConfigurator config = new JoranConfigurator();
  config.setContext(lc);

  InputStream stream = new ByteArrayInputStream(LOGBACK_XML.getBytes());
  try {
    config.doConfigure(stream);
  } catch (JoranException e) {
    e.printStackTrace();
  }
}
** Initialization search path
```


ProGuard
--------
When optimizing your application with [ProGuard](http://developer.android.com/tools/help/proguard.html), include the following rules to prevent `logback-android` and SLF4J calls from being removed (unless that's desired):

```
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
```


Other Documentation
-------------------
* [logback-android Javadoc][6]
* [logback manual][5]
* [Appender Notes][11]
  * [`FileAppender`][13]
  * [`SMTPAppender`][14]
  * [`SocketAppender`][15], [`SyslogAppender`][16]
  * [`DBAppender`][17]
* [Changelog][4]
* [FAQ][12]
 
For help with using **logback-android**, ask the mailing list: [logback-user@qos.ch](mailto:logback-user@qos.ch).


Build
-----
`logback-android` is built with Apache Maven 2+. Use these commands to create the uber JAR (with debug symbols).

    git clone git://github.com/tony19/logback-android.git
    cd logback-android
    ./makejar.sh

The jar would be in: `./target/logback-android-<version>.jar`

 [1]: http://logback.qos.ch
 [3]: http://tony19.github.com/logback-android
 [4]: https://github.com/tony19/logback-android/wiki/Changelog
 [5]: http://logback.qos.ch/manual/index.html
 [6]: http://tony19.github.com/logback-android/doc/1.0.10-1/
 [9]: https://bitbucket.org/tony19/logback-android-jar/downloads/logback-android-1.0.10-1.jar
 [10]: http://search.maven.org/remotecontent?filepath=org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar
 [11]: https://github.com/tony19/logback-android/wiki/Appender-Notes
 [12]: https://github.com/tony19/logback-android/wiki/FAQ
 [13]: https://github.com/tony19/logback-android/wiki/Appender-Notes#fileappender
 [14]: https://github.com/tony19/logback-android/wiki/Appender-Notes#smtpappender
 [15]: https://github.com/tony19/logback-android/wiki/Appender-Notes#socketappender-syslogappender
 [16]: https://github.com/tony19/logback-android/wiki/Appender-Notes#socketappender-syslogappender
 [17]: https://github.com/tony19/logback-android/wiki/Appender-Notes#dbappender
