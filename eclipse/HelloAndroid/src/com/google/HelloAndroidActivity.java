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

        configureLog();
        LOG.info("Hello Android!");

        // this.toString() is only called if the DEBUG level is enabled
        LOG.debug("toString: {}", this);
    }

    /**
     * Checks the application assets for logback config XML file. If not found,
     * this function does nothing.
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
