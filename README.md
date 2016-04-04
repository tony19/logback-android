<h1><a href="http://tony19.github.com/logback-android/"><img src="https://github.com/tony19/logback-android/raw/gh-pages/img/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/></a> logback-android <a href='https://tony19.ci.cloudbees.com/job/logback-android/job/logback-android-gradle-build/'><img src='https://tony19.ci.cloudbees.com/buildStatus/icon?job=logback-android/logback-android-gradle-build'></a></h1>
<sup>v1.1.1-4</sup>

[![Codacy Badge](https://api.codacy.com/project/badge/grade/c1d818d1911440e3b6d685c20a425856)](https://www.codacy.com/app/tony19/logback-android)

Overview
--------
[`logback-android`][3] brings the power of [`logback`][1] to Android. This library provides a highly configurable logging framework for Android apps, supporting multiple log destinations simultaneously:

 * files
 * SQLite databases
 * logcat
 * sockets
 * syslog
 * email

Runs on Android 2.1 or higher.

Quick Start
-----------
1. Add [`logback-android`][9] and [`slf4j-api`][10] to your project class path.
2. Configure `logback-android` using either [`${project-root}/assets/logback.xml`](#configuration-via-xml) or [in-code statements](#configuration-in-code). Otherwise, logging is silently disabled.
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
    Logger log = LoggerFactory.getLogger(MainActivity.class);
    log.info("hello world");
  }
}
```


Download
--------

 * [logback-android-1.1.1-4.jar][9]
 * [slf4j-api-1.7.6.jar][10]

OR Maven users can simply add these dependencies to `pom.xml`:

```xml
<dependency>
  <groupId>com.github.tony19</groupId>
  <artifactId>logback-android-core</artifactId>
  <version>1.1.1-4</version>
</dependency>
<dependency>
  <groupId>com.github.tony19</groupId>
  <artifactId>logback-android-classic</artifactId>
  <version>1.1.1-4</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.6</version>
</dependency>
```


Build
-----
`logback-android` is built with Gradle 2.12+. Use these commands to create the uber JAR (with debug symbols).

    git clone git://github.com/tony19/logback-android.git
    cd logback-android
    ./makejar.sh

The jar would be in: `./builds/lib/logback-android-1.1.1-4.jar`

 [1]: http://logback.qos.ch
 [3]: http://tony19.github.com/logback-android
 [4]: https://github.com/tony19/logback-android/wiki/Changelog
 [5]: http://logback.qos.ch/manual/index.html
 [6]: http://tony19.github.com/logback-android/doc/1.1.1-4/
 [9]: https://bitbucket.org/tony19/logback-android-jar/downloads/logback-android-1.1.1-4.jar
 [10]: http://search.maven.org/remotecontent?filepath=org/slf4j/slf4j-api/1.7.6/slf4j-api-1.7.6.jar
 [11]: https://github.com/tony19/logback-android/wiki/Appender-Notes
 [12]: https://github.com/tony19/logback-android/wiki/FAQ
 [13]: https://github.com/tony19/logback-android/wiki/Appender-Notes#fileappender
 [14]: https://github.com/tony19/logback-android/wiki/Appender-Notes#smtpappender
 [15]: https://github.com/tony19/logback-android/wiki/Appender-Notes#socketappender-syslogappender
 [16]: https://github.com/tony19/logback-android/wiki/Appender-Notes#socketappender-syslogappender
 [17]: https://github.com/tony19/logback-android/wiki/Appender-Notes#dbappender
