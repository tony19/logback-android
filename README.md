<h1><a href="http://tony19.github.com/logback-android/"><img src="https://github.com/tony19/logback-android/raw/master/logback-site/src/site/resources/images/logos/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/></a>logback-android</h1>

*Advanced logging library for Android*


Overview
--------

**Logback-Android** brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications. 

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

**NOTE**: If no configuration is loaded, the default level is set to `DEBUG` and the default appender is `LogcatAppender`.

#### Example AndroidManifest.xml:

	<manifest xmlns:android="http://schemas.android.com/apk/res/android"
		package="com.example"
		android:versionCode="1"
		android:versionName="1.0" >
		
		<!-- {...} -->
		
		<logback>
			<configuration>
				<appender
					name="LOGCAT"
					class="ch.qos.logback.core.android.LogcatAppender" >
					<tagEncoder>
						<pattern>%logger{0}</pattern>
					</tagEncoder>
					<encoder>
						<pattern>[%method] > %msg%n</pattern>
					</encoder>
				</appender>
		
				<root level="TRACE" >
					<appender-ref ref="LOGCAT" />
				</root>
			</configuration>
		</logback>
	</manifest>


#### Example Android Activity:

	package com.example;
	
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;
	import com.google.R;
	import android.app.Activity;
	import android.os.Bundle;
	
	public class HelloAndroidActivity extends Activity {
	
		static private final Logger LOG = LoggerFactory
										   .getLogger(HelloAndroidActivity.class);
										   
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);
	
			LOG.info("Hello Android!");
			LOG.debug("reply: {}", Example.hello());
		}
	}
	
	class Example {
		static private final Logger LOG = LoggerFactory.getLogger(Example.class);
	  
		static public String hello() {
			LOG.trace("entered hello()");
			return "Hi there!";
		}
	}

#### Output of Android logcat:

	I/ActivityManager(   76): Start proc com.example for activity com.example/com.example.HelloAndroidActivity: pid=1142 uid=10040 gids={1015}
	D/dalvikvm( 1142): GC_CONCURRENT freed 353K, 5% free 9183K/9607K, paused 4ms+4ms
	I/HelloAndroidActivity( 1142): [onCreate] Hello Android!
	V/Example ( 1142): [hello] entered hello()
	D/HelloAndroidActivity( 1142): [onCreate] reply: Hi there!

See the sample project in the `build/eclipse/HelloAndroid` subdirectory.

Features
--------
Logback-Android currently supports only the **logback-core** and **logback-classic** modules **excluding** the following features:

* logback-access
* Groovy configuration
* Evaluators and conditionals in the configuration XML
* JMS, JMX, JNDI, SMTP, and Servlets

Documentation
-------------
* [Logback-Android Javadoc][8]
* [Logback manual][7]
* [Reasons to switch to logback from log4j][2]
* [Frequently Asked Questions (FAQ)][6]

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
 [6]: http://logback.qos.ch/faq.html
 [7]: http://logback.qos.ch/manual/index.html
 [8]: http://tony19.github.com/logback-android/doc/1.0.0-3/
 [9]: mailto:logback-user@qos.ch
 [10]: http://stackoverflow.com/questions/2020088/sending-email-in-android-using-javamail-api-without-using-the-default-android-app
 [11]: http://thediscobot.blogspot.com/2009/07/howto-run-groovy-on-android.html
 [12]: https://github.com/tony19/logback-android/blob/master/LICENSE.md
 [13]: https://github.com/downloads/tony19/logback-android/logback-android-1.0.0-3.jar 
 [14]: https://github.com/downloads/tony19/logback-android/slf4j-api-1.6.4.jar
 [15]: http://developer.android.com/sdk/index.html
 [16]: http://ant.apache.org/
