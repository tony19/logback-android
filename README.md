# logback-android [![GitHub release](https://img.shields.io/github/release/tony19/logback-android.svg?maxAge=2592000)](https://github.com/tony19/logback-android/releases/) <a href="https://android-arsenal.com/api?level=9"><img alt="API" src="https://img.shields.io/badge/API-9%2B-brightgreen.svg?style=flat"/></a> [![CircleCI](https://circleci.com/gh/tony19/logback-android/tree/main.svg?style=svg)](https://circleci.com/gh/tony19/logback-android/tree/main) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/4fc7dae87f034dd181e4228acec33221)](https://www.codacy.com/gh/tony19/logback-android/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tony19/logback-android&amp;utm_campaign=Badge_Grade)

Overview
--------
`logback-android` is a lite version of [`logback`](http://logback.qos.ch) that runs on Android. This library provides a highly configurable logging framework for Android apps, supporting multiple log destinations simultaneously:

 * files
 * SQLite databases
 * logcat
 * sockets
 * syslog
 * email

See [Wiki](https://github.com/tony19/logback-android/wiki) for documentation.

*For `logback-android@1.x`, see the [`1.x` branch](https://github.com/tony19/logback-android/tree/1.x).*

Quick Start
-----------
1. Create a new "Basic Activity" app in [Android Studio](http://developer.android.com/sdk/index.html).
2. In `app/build.gradle`, add the following dependencies:

    ```groovy
    dependencies {
      implementation 'org.slf4j:slf4j-api:2.0.3'
      implementation 'com.github.tony19:logback-android:2.0.1'
    }
    ```

   If using `logback-android` in unit tests, **either** [use Robolectric](https://github.com/tony19/logback-android/issues/151#issuecomment-466276739), **or** use this config instead:

    ```groovy
    dependencies {
      implementation 'org.slf4j:slf4j-api:2.0.3'
      implementation 'com.github.tony19:logback-android:2.0.1'
      testImplementation 'ch.qos.logback:logback-classic:1.2.11'
    }

    configurations.testImplementation {
      exclude module: 'logback-android'
    }
    ```

3. Create `app/src/main/assets/logback.xml` containing:

    ```xml
    <configuration
      xmlns="https://tony19.github.io/logback-android/xml"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://tony19.github.io/logback-android/xml https://cdn.jsdelivr.net/gh/tony19/logback-android/logback.xsd"
    >
      <appender name="logcat" class="ch.qos.logback.classic.android.LogcatAppender">
        <tagEncoder>
          <pattern>%logger{12}</pattern>
        </tagEncoder>
        <encoder>
          <pattern>[%-20thread] %msg</pattern>
        </encoder>
      </appender>

      <root level="DEBUG">
        <appender-ref ref="logcat" />
      </root>
    </configuration>
    ```

4. In `MainActivity.java`, add the following imports:

    ```java
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    ```

5. ...and modify `onOptionsItemSelected()` to log "hello world":

    ```java
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Logger log = LoggerFactory.getLogger(MainActivity.class);
        log.info("hello world");
        // ...
    }
    ```

6. Build and start the app.
7. Open logcat for your device (via the _Android Monitor_ tab in Android Studio).
8. Click the app menu, and select the menu-option. You should see "hello world" in logcat.


Download
--------
_Gradle_ **release**

```groovy
dependencies {
  implementation 'org.slf4j:slf4j-api:2.0.3'
  implementation 'com.github.tony19:logback-android:2.0.1'
}
```

_Gradle_ **snapshot (unstable)**

```groovy
repositories {
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  implementation 'org.slf4j:slf4j-api:2.0.3'
  implementation 'com.github.tony19:logback-android:2.0.2-SNAPSHOT'
}
```

Build
-----
Use these commands to create the AAR:

    git clone git://github.com/tony19/logback-android.git
    cd logback-android
    scripts/makejar.sh

The file is output to: `./build/logback-android-2.0.1-debug.aar`

