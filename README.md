<h1><img src="https://github.com/tony19/logback-android/raw/master/logback-site/src/site/resources/images/logos/lblogo-72x72.png" width="64" height="64" hspace="4" vspace="4" valign="middle"/>logback-android</h1>

*Advanced logging library for Android*


Overview
--------

**Logback-Android** brings the power of *Logback* to Android. [*Logback*][1] is a reliable, generic, fast, and flexible logging library for Java applications written by the creator of the popular (but now defunct) Apache log4j project. Logback-Android provides a richer API than `android.util.Log` (including automatic log file compression). Additionally, Logback-Android together with [SLF4J-Android][3] allows for greater logging flexibility and portability across Java platforms.


Quickstart
----------

 1. Configure your Android project's *Java Build Path*:

     * Include [logback-android-0.9.30.jar][18] and [slf4j-api-1.6.1.jar][19].
     * Exclude all other SLF4J bindings/libraries (i.e., *log4j-over-slf4j.jar*, *slf4j-android-1.5.8.jar*, etc).

 1. Load configuration XML from pre-determined location (e.g., `/sdcard/logback-test.xml`). 

**NOTE**: If no configuration is loaded, the default level is set to `DEBUG` and default appender is the Console *stdout* (which can be seen from Android *logcat*).

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

	I/System.out( 6948): 03:41:26.403 [main] INFO  s.testapp.HelloAndroidActivity - Hello Android!
	I/System.out( 6948): 03:41:26.453 [main] TRACE s.testapp.HelloAndroidActivity - toString() entered
	I/System.out( 6948): 03:41:26.499 [main] DEBUG s.testapp.HelloAndroidActivity - toString: slf4jandroid.testapp.HelloAndroidActivity

#### Output of /sdcard/test.log:

	2506 [main] INFO  s.testapp.HelloAndroidActivity - Hello Android!
	2556 [main] TRACE s.testapp.HelloAndroidActivity - toString() entered
	2602 [main] DEBUG s.testapp.HelloAndroidActivity - toString: slf4jandroid.testapp.HelloAndroidActivity

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
Logback-Android uses the same license as Logback. See [LICENSE][17].

Build
-----

### *Eclipse*
Logback-Android is currently built from Eclipse (without Maven or any other plugins). The goal is to create `logback-android.jar`, containing only the `logback-core` and `logback-classic` modules with select features omitted. The toughest part is setting up your Eclipse project as outlined below.


 1. Create new Android project.

	* Name the project "logback-android".
	* For **Contents**, select **Create new project in workspace** (this is the default).
	* You can specify any root source directory (does not have to be Logback's src).
	* Pick the **Build Target**.
	* For **Package Name**, enter **ch.qos.logback**.
	* Uncheck the box for **Create Activity**.
	* Click **Finish**.

 1. Edit **Project Properties > Android**. 

	* Check the box for **Is Library**.

 1. Edit **Project Properties > Java Build Path**. 

 1. Click **Libraries** tab:

	* Click **Add External JARs**.
	* Browse to `slf4j-api-1.6.1.jar`, and click **OK**
   
 1. Click **Source** tab:

	* Select `logback-android/src`, and click **Remove**
	* Click **Link Source**.
	* Browse to `${logback-android-src}/logback-classic/src/main/java`
	* Click **Next**
	* For **Exclusion pattterns**, enter the following:

		* `ch/qos/logback/classic/boolex/GEventEvaluator.java`
		* `ch/qos/logback/classic/boolex/JaninoEventEvaluator.java`
		* `ch/qos/logback/classic/gaffer/`
		* `ch/qos/logback/classic/helpers/`
		* `ch/qos/logback/classic/jmx/`
		* `ch/qos/logback/classic/joran/action/EvaluatorAction.java`
		* `ch/qos/logback/classic/joran/action/InsertFromJNDIAction.java`
		* `ch/qos/logback/classic/joran/action/JMXConfiguratorAction.java`
		* `ch/qos/logback/classic/net/JMSQueueAppender.java`
		* `ch/qos/logback/classic/net/JMSQueueSink.java`
		* `ch/qos/logback/classic/net/JMSTopicAppender.java`
		* `ch/qos/logback/classic/net/JMSTopicSink.java`
		* `ch/qos/logback/classic/net/SMTPAppender.java`
		* `ch/qos/logback/classic/selector/ContextJNDISelector.java`
		* `ch/qos/logback/classic/selector/servlet/`
		* `ch/qos/logback/classic/util/JNDIUtil.java`
		* `ch/qos/logback/classic/ViewStatusMessagesServlet.java`

	* For **Folder name**, enter **logback-classic**
	* Click **Link Source** again.
	* Browse to `${logback-android-src}/logback-core/src/main/java`, and click **Next**
	* For **Exclusion pattterns**, enter the following:

		* `ch/qos/logback/core/boolex/JaninoEventEvaluatorBase.java`
		* `ch/qos/logback/core/db/BindDataSourceToJNDIAction.java`
		* `ch/qos/logback/core/db/JNDIConnectionSource.java`
		* `ch/qos/logback/core/net/JMSAppenderBase.java`
		* `ch/qos/logback/core/net/LoginAuthenticator.java`
		* `ch/qos/logback/core/net/SMTPAppenderBase.java`
		* `ch/qos/logback/core/status/ViewStatusMessagesServletBase.java`
  
	* For **Folder name**, enter **logback-core**
	* Click **Finish**. Eclipse should begin building automatically, and if no errors, `logback-android.jar` is created in the project's output directory.


Future Work
-----------
Tentative upcoming plans include:

 * Add script-able builds (one of [Ant][11], [Maven][12], or [Gradle][13]) (medium)
 * Add [support][14] for `SMTPAppender` (easy)
 * Add [support][15] for Groovy configuration (hard)

 [1]: http://logback.qos.ch
 [2]: http://logback.qos.ch/reasonsToSwitch.html
 [3]: http://www.slf4j.org/android
 [4]: http://logback.qos.ch/consolePlugin.html
 [5]: http://logback.qos.ch/codes.html
 [6]: http://logback.qos.ch/faq.html
 [7]: http://logback.qos.ch/manual/index.html
 [8]: http://logback.qos.ch/apidocs/index.html
 [9]: mailto:logback-user@qos.ch
 [10]: http://code.google.com/p/maven-android-plugin/
 [11]: http://www.androidengineer.com/2010/06/using-ant-to-automate-building-android.html
 [12]: http://code.google.com/p/maven-android-plugin/
 [13]: https://github.com/jvoegele/gradle-android-plugin/wiki/
 [14]: http://stackoverflow.com/questions/2020088/sending-email-in-android-using-javamail-api-without-using-the-default-android-app
 [15]: http://thediscobot.blogspot.com/2009/07/howto-run-groovy-on-android.html
 [16]: http://www.slf4j.org/download.html
 [17]: https://github.com/tony19/logback-android/blob/master/LICENSE.md
 [18]: https://github.com/tony19/logback-android/blob/master/bin/logback-android-0.9.30-RC1.jar
 [19]: https://github.com/tony19/logback-android/blob/master/bin/slf4j-api-1.6.1.jar
