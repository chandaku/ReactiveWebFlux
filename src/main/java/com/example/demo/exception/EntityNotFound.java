package com.example.demo.exception;

public class EntityNotFound extends RuntimeException {

    public EntityNotFound(final String message) {
        super(message);
    }
}
