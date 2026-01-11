package com.driveguard.rule_engine.exception;

public class NoAlertsFoundException extends RuntimeException {
    public NoAlertsFoundException(String message) {
        super(message);
    }
}
