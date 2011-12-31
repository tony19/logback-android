<h1><a href="http://tony19.github.com/logback-android/"><img src="http://tony19.github.com/logback-android/img/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/></a>logback-android</h1>

*Advanced logging library for Android*


Overview
--------

**Logback-Android** brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications.

The current version is **1.0.0-4**.

Download
--------
 * [logback-android-1.0.0-4.jar][13] (MD5: `656296323340b7e4fc87cd1212138a7d`)
 * [slf4j-api-1.6.4.jar][14] (MD5: `a134d83e0c12a9611824284c855ffb13`)

Quickstart
----------
1. Add [Logback][13] and [SLF4J][14] to your project classpath.
2. Enter configuration XML in one of the following locations. Note that Logback-Android checks them in exact order and uses the first one found.
	* SD card (`/sdcard/logback/logback.xml`)
	* Your application's `AndroidManifest.xml`
	* Your application's `assets/logback.xml` (create dir if necessary)

	If not found, a simple LogcatAppender is used by default.

3. Ready to run!

See examples at http://tony19.github.com/logback-android


Features Supported
------------------
Logback-Android supports only a subset of Logback's features (i.e., the logback-core and logback-classic modules). It does **NOT** support the following:

* logback-access
* Groovy configuration
* Evaluators and conditionals in the configuration XML
* JMS, JMX, JNDI, SMTP, and Servlets

Documentation
-------------
* [Logback-Android Javadoc][8]
* [Logback manual][7]
* [Why switch from log4j?][2]
* [FAQ][6]

For help with using **Logback-Android**, ask the mailing list: [logback-user AT qos DOT ch][9].

Build
-----
Logback-Android is built from [Ant][16], using the Android SDK.

1. Download the [Android SDK][15], Revision 14 (or newer).
2. Change directory to `${logback-android-src}/build/ant`.
2. Edit `ant.properties`:
	* Set `sdk.dir` to the root directory of the Android SDK.
	* Set `slf4j.jar` to the path of [SLF4J API library][14].
3. Enter `ant` to begin the build. The JAR is created at `bin/logback-android-1.0.0-4.jar`.


 [1]: http://logback.qos.ch
 [2]: http://logback.qos.ch/reasonsToSwitch.html
 [6]: http://logback.qos.ch/faq.html
 [7]: http://logback.qos.ch/manual/index.html
 [8]: http://tony19.github.com/logback-android/doc/1.0.0-3/
 [9]: mailto:logback-user@qos.ch
 [13]: https://github.com/downloads/tony19/logback-android/logback-android-1.0.0-4.jar 
 [14]: https://github.com/downloads/tony19/logback-android/slf4j-api-1.6.4.jar
 [15]: http://developer.android.com/sdk/index.html
 [16]: http://ant.apache.org/
