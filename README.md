# logback-android [![Financial Contributors on Open Collective](https://opencollective.com/logback-android/all/badge.svg?label=financial+contributors)](https://opencollective.com/logback-android) [![CircleCI branch](https://img.shields.io/circleci/project/github/tony19/logback-android/master.svg)](https://circleci.com/gh/tony19/logback-android) [![Codacy Badge](https://api.codacy.com/project/badge/grade/c1d818d1911440e3b6d685c20a425856)](https://www.codacy.com/app/tony19/logback-android)
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
  compile 'org.slf4j:slf4j-api:1.7.25'
  compile 'com.github.tony19:logback-android:2.0.0'
}
```

_Gradle_ **snapshot (unstable)**

```groovy
repositories {
  maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
  compile 'org.slf4j:slf4j-api:1.7.25'
  compile 'com.github.tony19:logback-android:2.0.1-SNAPSHOT'
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

## Contributors

### Code Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/tony19/logback-android/graphs/contributors"><img src="https://opencollective.com/logback-android/contributors.svg?width=890&button=false" /></a>

### Financial Contributors

Become a financial contributor and help us sustain our community. [[Contribute](https://opencollective.com/logback-android/contribute)]

#### Individuals

<a href="https://opencollective.com/logback-android"><img src="https://opencollective.com/logback-android/individuals.svg?width=890"></a>

#### Organizations

Support this project with your organization. Your logo will show up here with a link to your website. [[Contribute](https://opencollective.com/logback-android/contribute)]

<a href="https://opencollective.com/logback-android/organization/0/website"><img src="https://opencollective.com/logback-android/organization/0/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/1/website"><img src="https://opencollective.com/logback-android/organization/1/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/2/website"><img src="https://opencollective.com/logback-android/organization/2/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/3/website"><img src="https://opencollective.com/logback-android/organization/3/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/4/website"><img src="https://opencollective.com/logback-android/organization/4/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/5/website"><img src="https://opencollective.com/logback-android/organization/5/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/6/website"><img src="https://opencollective.com/logback-android/organization/6/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/7/website"><img src="https://opencollective.com/logback-android/organization/7/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/8/website"><img src="https://opencollective.com/logback-android/organization/8/avatar.svg"></a>
<a href="https://opencollective.com/logback-android/organization/9/website"><img src="https://opencollective.com/logback-android/organization/9/avatar.svg"></a>
