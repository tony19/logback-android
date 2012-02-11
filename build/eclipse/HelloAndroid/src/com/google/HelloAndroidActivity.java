package com.google;

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
