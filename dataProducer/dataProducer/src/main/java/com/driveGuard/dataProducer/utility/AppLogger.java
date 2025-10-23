package com.driveGuard.dataProducer.utility;

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

    // FIX 1: Use SLF4J's parameterized logging to include the tripId.
    public void warn(String message, String tripId) {
        // The {} is a placeholder that will be replaced by tripId.
        logger.warn(message + " [TripID: {}]", tripId);
    }

    // FIX 2: Overload the error method to accept a Throwable.
    // This is the standard and correct way to log exceptions.
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    // You can keep this method if you sometimes need to log an error with a file path.
    public void error(String message, Path tripFile) {
        // Also uses a placeholder for the path.
        logger.error(message + " [File: {}]", tripFile);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void trace(String message) {
        logger.trace(message);
    }
}
