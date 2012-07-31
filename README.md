<h1><a href="http://tony19.github.com/logback-android/"><img src="https://github.com/tony19/logback-android/raw/gh-pages/img/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/></a> logback-android</h1>


*Advanced logging library for Android applications*


Overview
--------

[*logback-android*][3] brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications. 

The current version is **1.0.6-1**.


Download
--------
 * [logback-android-1.0.6-1.jar][9] (MD5: `bbda40e50904590c52dc81945824daa2`)
 * [slf4j-api-1.6.4.jar][10] (MD5: `a134d83e0c12a9611824284c855ffb13`)

Quickstart
----------
1. Add [Logback][9] and [SLF4J][10] to your project classpath.
2. Enter configuration XML in one of the following locations. Note that logback-android checks them in exact order and uses the first one found.
	* SD card (`/sdcard/logback/logback.xml`)
	* Your application's `AndroidManifest.xml`
	* Your application's `assets/logback.xml` (create dir if necessary)

	If not found, a simple [`LogcatAppender`][8] is used by default.

3. Ready to run!

See **[examples][3]**


Features Supported
------------------
Runs on Android 2.1+

logback-android supports only a subset of Logback's features (i.e., the logback-core and logback-classic modules). It does **NOT** support the following:

* logback-access
* Groovy configuration
* Conditionals in the configuration XML
* JMS, JMX, JNDI, and Servlets

Documentation
-------------
* [logback-android Javadoc][6]
* [Logback manual][5]
* [Why switch from log4j?][2]
* [Changelog][4]

For help with using **logback-android**, ask the mailing list: [logback-user@qos.ch][7].

Build
-----
logback-android is built with Apache Maven. Use these commands to create the uber JAR.

    mvn install 
    mvn -f pom-uber.xml package


To include logback-android in your Maven project, add the following dependencies to your project's `pom.xml`:
 
    <dependency>
      <groupId>com.github.tony19</groupId>
      <artifactId>logback-android-core</artifactId>
      <version>1.0.6-1</version>
    </dependency>
    <dependency>
      <groupId>com.github.tony19</groupId>
      <artifactId>logback-android-classic</artifactId>
      <version>1.0.6-1</version>
    </dependency>


 [1]: http://logback.qos.ch
 [2]: http://logback.qos.ch/reasonsToSwitch.html
 [3]: http://tony19.github.com/logback-android
 [4]: http://tony19.github.com/logback-android/changelog.html
 [5]: http://logback.qos.ch/manual/index.html
 [6]: http://tony19.github.com/logback-android/doc/1.0.6-1/
 [7]: mailto:logback-user@qos.ch
 [8]: http://tony19.github.com/logback-android/doc/1.0.6-1/ch/qos/logback/classic/android/LogcatAppender.html
 [9]: https://github.com/downloads/tony19/logback-android/logback-android-1.0.6-1.jar 
 [10]: https://github.com/downloads/tony19/logback-android/slf4j-api-1.6.4.jar
