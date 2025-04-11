package ru.practicum.exception;

public class UncorrectedParametersException extends RuntimeException {
    public UncorrectedParametersException(String message) {
        super(message);
    }
}