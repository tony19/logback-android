<h1><img src="https://github.com/tony19/logback-android/raw/master/logback-site/src/site/resources/images/logos/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/>logback-android</h1>

*Advanced logging library for Android*


Overview
--------

**Logback-Android** brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications written by the creator of the popular (but now defunct) Apache log4j project. Logback-Android provides a richer API than `android.util.Log` (including automatic log file compression). Additionally, Logback-Android together with [*SLF4J*][3] allows for greater logging flexibility and portability across Java platforms.

The current version is **1.0.0-2**.

Quickstart
----------

 1. Configure your Android project's *Java Build Path*:

     * Include [logback-android-1.0.0-2.jar][13] and [slf4j-api-1.6.4.jar][14].
     * Exclude all other SLF4J bindings/libraries (i.e., *log4j-over-slf4j.jar*, *slf4j-android-1.5.8.jar*, etc).

 1. Load configuration XML from pre-determined location (e.g., `/sdcard/logback-test.xml`). 

**NOTE**: If no configuration is loaded, the default level is set to `DEBUG` and the default appender is `LogcatAppender`.

#### Example config file:

	<configuration> 
	  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender"> 
	    <!-- encoders are  by default assigned the type
		 ch.qos.logback.classic.encoder.PatternLayoutEncoder -->
	    <encoder>
	      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
	    </encoder>
	  </appender>

	  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
	    <file>/sdcard/test.log</file>
	    <append>true</append>
	    <!-- encoders are assigned the type
		 ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
	    <encoder>
	      <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
	    </encoder>
	  </appender>

	  <root level="TRACE">
	    <appender-ref ref="FILE" />
	    <appender-ref ref="CONSOLE" />
	  </root>
	</configuration>


#### Example Android Activity:

	/* (other imports not shown for brevity) */
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;

	public class HelloAndroidActivity extends Activity {
		static private final Logger LOG = LoggerFactory.getLogger(HelloAndroidActivity.class);
		static private final String CONFIG_FILE = "/sdcard/logback.xml";
		static private final String TEST_CONFIG_FILE = "/sdcard/logback-test.xml";
	
		/** Called when the activity is first created. */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);
		
			configureLog();
			LOG.info("Hello Android!");
		
			// this.toString() is only called if the DEBUG level is enabled
			LOG.debug("toString: {}", this);
		}
	
		/**
		 * Checks the SD card for logback config XML files (first for test config
		 * and if not found, checks for normal config). If no config files
		 * exist in the root of SD card, this function does nothing.
		 */
		private void configureLog() {
			File xml = new File(TEST_CONFIG_FILE); 
			if (!xml.exists()) {
				xml = new File(CONFIG_FILE);
				if (!xml.exists()) {
					return; // no configuration files found
				}
			}
		
			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			try {
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(lc);
				lc.reset();
				configurator.doConfigure(xml);
			} catch (JoranException je) {
				// StatusPrinter will handle this
			}
			StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
		}

		@Override
		public String toString() {
			LOG.trace("toString() entered");
			return HelloAndroidActivity.class.getName();
		}
	}

#### Output of Android logcat:

	I/System.out( 6948): 03:41:26.403 [main] INFO  com.google.HelloAndroidActivity - Hello Android!
	I/System.out( 6948): 03:41:26.453 [main] TRACE com.google.HelloAndroidActivity - toString() entered
	I/System.out( 6948): 03:41:26.499 [main] DEBUG com.google.HelloAndroidActivity - toString: com.google.HelloAndroidActivity

#### Output of /sdcard/test.log:

	2506 [main] INFO  com.google.HelloAndroidActivity - Hello Android!
	2556 [main] TRACE com.google.HelloAndroidActivity - toString() entered
	2602 [main] DEBUG com.google.HelloAndroidActivity - toString: com.google.HelloAndroidActivity

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

1. Download the [Android SDK][15].
2. Change directory to `${logback-android-src}/build/ant`.
2. Edit `build.properties`:
	* Edit the path to the root directory of the Android SDK.
	* Edit the path to the [SLF4J API library][14].
3. Enter `ant` to begin the build. The JAR is created at `bin/logback-android-1.0.0-2.jar`.

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
 [13]: https://github.com/downloads/tony19/logback-android/logback-android-1.0.0-2.jar 
 [14]: https://github.com/downloads/tony19/logback-android/slf4j-api-1.6.4.jar
 [15]: http://developer.android.com/sdk/index.html
 [16]: http://ant.apache.org/