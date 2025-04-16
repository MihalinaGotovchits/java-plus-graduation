package ru.practicum.ewm.error.exception;

public class ConflictDataException extends RuntimeException {
    public ConflictDataException(String message) {
        super(message);
    }
}