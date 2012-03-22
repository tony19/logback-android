/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 2011-2012, Anthony Trinh. All rights reserved.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package chapters.appenders.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * This activity sends an email when the user presses a button on the main page.
 * Note this application requires the Android Java Mail jars from
 * http://code.google.com/p/javamail-android/ to be in the classpath
 * or else an error occurs. Also remember to set the INTERNET permission in AndroidManifest.xml:
 * <pre>
 * &lt;uses-permission android:name="android.permission.INTERNET"/>
 * </pre>
 * <p>
 * Example logback configuration:
 * <pre>
 * &lt;configuration>
 * 	&lt;appender name="SMTP" class="ch.qos.logback.classic.net.SMTPAppender">
 * 		&lt;evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
 * 			&lt;marker>TOO_BIG_ACCELERATION&lt;/marker>
 * 		&lt;/evaluator>
 * 		&lt;cyclicBufferTracker class="ch.qos.logback.core.spi.CyclicBufferTrackerImpl">
 * 			&lt;!-- send just one log entry per email -->
 * 			&lt;bufferSize>1&lt;/bufferSize>
 * 		&lt;/cyclicBufferTracker>
 * 		&lt;smtpHost>smtp.gmail.com&lt;/smtpHost>
 * 		&lt;smtpPort>465&lt;/smtpPort>
 * 		&lt;SSL>true&lt;/SSL>
 * 		&lt;username>foo@gmail.com&lt;/username>
 * 		&lt;password>foo123&lt;/password>
 * 		&lt;to>tony19@gmail.com&lt;/to>
 * 		&lt;from>anyone@gmail.com&lt;/from>
 * 		&lt;subject>%date{yyyyMMdd'T'HH:mm:ss.SSS}; %-5level; %msg&lt;/subject>
 * 		&lt;layout class="ch.qos.logback.classic.PatternLayout">
 * 			&lt;pattern>%date{yyyyMMdd'T'HH:mm:ss.SSS}; %-5level; %msg%n&lt;/pattern>
 * 		&lt;/layout>
 * 	&lt;/appender>
 * 
 * 	&lt;root level="INFO">
 * 		&lt;appender-ref ref="SMTP" />
 * 	&lt;/root>
 * &lt;/configuration>
 * </pre>
 * </p>
 * 
 * @author Enrico Spinielli
 */
public class Marked_EMail_Activity extends Activity {
	
    static private final Logger LOG = LoggerFactory.getLogger(Marked_EMail_Activity.class);
    private static final Marker MARKER = MarkerFactory.getMarker("TOO_BIG_ACCELERATION");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /* Get the button declared in our layout.xml, and
         * set its click-event handler to log an error message
         * (we had configured the logger to send an email).
         */
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	LOG.info(MARKER, "on marker email");
            }
    	});

        LOG.info("end of onCreate");        
    }
}

