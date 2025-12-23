package com.driveguard.rule_engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class AppLogger {

    private final Logger logger;

    private AppLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    public void info(String message) {
        logger.info(message);
    }


    public void warn(String message) {
        // The {} is a placeholder that will be replaced by tripId.
        logger.warn(message);
    }

    // This is the standard and correct way to log exceptions.
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    // You can keep this method if you sometimes need to log an error with a file path.
    public void error(String message, Path tripFile) {
        logger.error(String.format("%s [File: %s]", message, tripFile));
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void trace(String message) {
        logger.trace(message);
    }
}
