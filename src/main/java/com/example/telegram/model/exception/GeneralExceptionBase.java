package com.example.telegram.model.exception;

public class GeneralExceptionBase extends RuntimeException {
    public GeneralExceptionBase() {
        super();
    }

    public GeneralExceptionBase(String message) {
        super(message);
    }
}