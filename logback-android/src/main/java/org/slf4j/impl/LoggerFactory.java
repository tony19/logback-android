package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.qos.logback.classic.LoggerContext;

/**
 * Based on https://github.com/qos-ch/slf4j/blob/master/slf4j-simple/src/main/java/org/slf4j/simple/SimpleLoggerFactory.java
 */
public class LoggerFactory implements ILoggerFactory {

    ConcurrentMap<String, Logger> loggerMap;
    LoggerContext loggerContext;

    public LoggerFactory() {
        loggerMap = new ConcurrentHashMap<String, Logger>();
        loggerContext = new LoggerContext();
    }

    @Override
    public Logger getLogger(String name) {
        Logger logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        } else {
            Logger newInstance =  loggerContext.getLogger(name);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }
}