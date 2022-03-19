# logback-android [![CircleCI](https://circleci.com/gh/tony19/logback-android/tree/main.svg?style=svg)](https://circleci.com/gh/tony19/logback-android/tree/main) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/4fc7dae87f034dd181e4228acec33221)](https://www.codacy.com/gh/tony19/logback-android/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tony19/logback-android&amp;utm_campaign=Badge_Grade)
<sup>v2.0.0</sup>

<a href="https://opencollective.com/logback-android/donate" target="_blank">
  <img src="https://opencollective.com/logback-android/donate/button@2x.png?color=blue" width=250 />
</a>

Overview
--------
[`logback-android`][2] brings the power of [`logback`][1] to Android. This library provides a highly configurable logging framework for Android apps, supporting multiple log destinations simultaneously:

 * files
 * SQLite databases
 * logcat
 * sockets
 * syslog
 * email

Runs on Android 2.3 (SDK 9) or higher. See [Wiki][4] for documentation.

*For `v1.x`, see the [`1.x` branch](https://github.com/tony19/logback-android/tree/1.x).*

Quick Start
-----------
1. Create a new "Basic Activity" app in [Android Studio][3].
2. In `app/build.gradle`, add the following dependencies:

    ```groovy
    dependencies {
      compile 'org.slf4j:slf4j-api:1.7.25'
      compile 'com.github.tony19:logback-android:2.0.0'
    }
    ```

3. Create `app/src/main/assets/logback.xml` containing:

    ```xml
    <configuration>
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
  implementation 'org.slf4j:slf4j-api:1.7.36'
  implementation 'com.github.tony19:logback-android:2.0.0'
}
```

_Gradle_ **snapshot (unstable)**

```groovy
repositories {
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  implementation 'org.slf4j:slf4j-api:1.7.36'
  implementation 'com.github.tony19:logback-android:2.0.1-SNAPSHOT'
}
```

Build
-----
Use these commands to create the AAR:

    git clone git://github.com/tony19/logback-android.git
    cd logback-android
    scripts/makejar.sh

The file is output to: `./build/logback-android-2.0.0-debug.aar`

 [1]: http://logback.qos.ch
 [2]: http://tony19.github.com/logback-android
 [3]: http://developer.android.com/sdk/index.html
 [4]: https://github.com/tony19/logback-android/wiki
