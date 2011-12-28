package com.google;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class HelloAndroidActivity extends Activity {
    static private final Logger LOG = LoggerFactory.getLogger(HelloAndroidActivity.class);

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /*
         * If you don't specify a Logback config (either by loading a file
         * from code; or by using AndroidManifest.xml), Logback defaults to
         * the LogcatAppender. Note that Android has its own logging filters
         * that supersede all loggers, including Logback. So, if you don't
         * see an expected log message in logcat, your logcat filters are
         * likely blocking it.
         * 
         * See http://developer.android.com/guide/developing/tools/adb.html#filteringoutput
         */
        configureLog();
        LOG.info("Hello Android!");

        // this.toString() is only called if the DEBUG level is enabled
        LOG.debug("toString: {}", this);
    }

    /**
     * Configures Logback from the config XML in assets/logback.xml
     * <p>
     * Alternatively, you can enter the XML config in AndroidManifest.xml,
     * which obviates the need for any code-based configuration.
     * The Logback config should be inside a {@code <logback>} tag within 
     * the manifest like this:
     * <pre>
     * &lt;manifest  ...>
     *     ...
     *     
     *     &lt;logback>
     *        &lt;configuration>
     *           ...
     *        &lt;/configuration>
     *     &lt;/logback>
     * &lt;manifest>
     * </pre>
     */
    private void configureLog() {
        InputStream xml = null;
        try {
            xml = getAssets().open("logback.xml");
        } catch (IOException e) {
            return;
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
