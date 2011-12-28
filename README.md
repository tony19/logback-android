<h1><img src="https://github.com/tony19/logback-android/raw/master/logback-site/src/site/resources/images/logos/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/>logback-android</h1>

*Advanced logging library for Android*


Overview
--------

**Logback-Android** brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications written by the creator of the popular (but now defunct) Apache log4j project. Logback-Android provides a richer API than `android.util.Log` (including automatic log file compression). Additionally, Logback-Android together with [*SLF4J*][3] allows for greater logging flexibility and portability across Java platforms.

The current version is **1.0.0-3**.

Download
--------
 * [logback-android-1.0.0-3.jar][13] (MD5: `f11370158aff171a37d8dc4c087bf7e8`)
 * [slf4j-api-1.6.4.jar][14] (MD5: `a134d83e0c12a9611824284c855ffb13`)

Quickstart
----------

 1. Edit your project's library references:

     * Include [logback-android-1.0.0-3.jar][13] and [slf4j-api-1.6.4.jar][14].
     * Exclude all other SLF4J bindings/libraries (i.e., *log4j-over-slf4j.jar*, *slf4j-android-1.5.8.jar*, etc).

 1. Edit AndroidManifest.xml with your Logback configuration (shown in example below).

**NOTE**: If no configuration is loaded, the default level is set to `DEBUG` and the default appender is `LogcatAppender`. However, Android has its own logging filters that supersede all loggers, including Logback. So, if you don't see an expected log message in logcat, your logcat filters are likely blocking it. See [Android documentation][17] for details on setting the logcat filters.

#### Example AndroidManifest.xml:

	<?xml version="1.0" encoding="utf-8"?>
	<manifest xmlns:android="http://schemas.android.com/apk/res/android"
		package="com.example"
		android:versionCode="1"
		android:versionName="1.0" >
		
		<uses-sdk android:minSdkVersion="15" />
		
		<application
			android:icon="@drawable/ic_launcher"
			android:label="@string/app_name" >
			<activity
				android:name=".HelloAndroidActivity"
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
					name="CONSOLE"
					class="ch.qos.logback.core.ConsoleAppender" >
					<encoder>
						<pattern>[%thread] %msg%n</pattern>
					</encoder>
				</appender>
	
				<root level="TRACE" >
					<appender-ref ref="CONSOLE" />
				</root>
			</configuration>
		</logback>
	
	</manifest>


#### Example Android Activity:

	package com.example;
	
	import android.app.Activity;
	import android.os.Bundle;

	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;

	public class HelloAndroidActivity extends Activity {
		static private final Logger LOG = LoggerFactory.getLogger(HelloAndroidActivity.class);
		
		/** Called when the activity is first created. */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);
		
			LOG.info("Hello Android!");
		
			// this.toString() is only called if the DEBUG level is enabled
			LOG.debug("toString: {}", this);
		}
	
		@Override
		public String toString() {
			LOG.trace("toString() entered");
			return HelloAndroidActivity.class.getName();
		}
	}

#### Output of Android logcat:

	I/System.out( 6948): 03:41:26.403 [main] Hello Android!
	I/System.out( 6948): 03:41:26.453 [main] toString() entered
	I/System.out( 6948): 03:41:26.499 [main] toString: com.example.HelloAndroidActivity

See the sample project in the `build/eclipse/HelloAndroid` subdirectory.

Features
--------
Logback-Android currently supports only the **logback-core** and **logback-classic** modules **excluding** the following features:

* Groovy configuration
* Conditionals in XML configuration files
* JMS, JMX, JNDI, SMTP, and Servlets

Documentation
-------------
* [Logback manual][7]
* [Reasons to switch to logback from log4j][2]
* [Frequently Asked Questions (FAQ)][6]
* [Logback error codes and their meanings][5]
* [Logback Console Plugin for Eclipse][4]
* [Original Logback Javadoc][8]

For help with using **Logback-Android**, ask the mailing list: [logback-user AT qos DOT ch][9].

License
-------
Logback-Android uses the same license as Logback. See [LICENSE][12].

Build
-----
Logback-Android is built from [Ant][16], using the Android SDK.

1. Download the [Android SDK][15], Revision 14 (or newer).
2. Change directory to `${logback-android-src}/build/ant`.
2. Edit `ant.properties`:
	* Set `sdk.dir` to the root directory of the Android SDK.
	* Set `slf4j.jar` to the path of [SLF4J API library][14].
3. Enter `ant` to begin the build. The JAR is created at `bin/logback-android-1.0.0-3.jar`.


Future Work
-----------
Tentative upcoming plans include:

 * Add [support][10] for `SMTPAppender` (easy)
 * Add [support][11] for Groovy configuration (hard)

 [1]: http://logback.qos.ch
 [2]: http://logback.qos.ch/reasonsToSwitch.html
 [3]: http://www.slf4j.org
 [4]: http://logback.qos.ch/consolePlugin.html
 [5]: http://logback.qos.ch/codes.html
 [6]: http://logback.qos.ch/faq.html
 [7]: http://logback.qos.ch/manual/index.html
 [8]: http://logback.qos.ch/apidocs/index.html
 [9]: mailto:logback-user@qos.ch
 [10]: http://stackoverflow.com/questions/2020088/sending-email-in-android-using-javamail-api-without-using-the-default-android-app
 [11]: http://thediscobot.blogspot.com/2009/07/howto-run-groovy-on-android.html
 [12]: https://github.com/tony19/logback-android/blob/master/LICENSE.md
 [13]: https://github.com/downloads/tony19/logback-android/logback-android-1.0.0-3.jar 
 [14]: https://github.com/downloads/tony19/logback-android/slf4j-api-1.6.4.jar
 [15]: http://developer.android.com/sdk/index.html
 [16]: http://ant.apache.org/
 [17]: http://developer.android.com/guide/developing/tools/adb.html#filteringoutput
