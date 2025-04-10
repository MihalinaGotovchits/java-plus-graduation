package ru.practicum.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.dto.error.ErrorResponse;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.DataIntegrityViolationException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.UncorrectedParametersException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ErrorExceptionHandler {

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    private ErrorResponse handleDataIntegrityViolationException(Exception e) {
        return ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.toString())
                .reason("Integrity constraint has been violated.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    private ErrorResponse handleThrowable(Throwable e) {
        return ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
                .reason("Unexpected reason")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    private ErrorResponse handleNotFoundException(NotFoundException e) {
        return ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.toString())
                .reason("The required object was not found.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, UncorrectedParametersException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlerIncorrectParametersException(Exception e) {

        String detailedMessage = e instanceof MethodArgumentNotValidException
                ? "Validation failed for some fields."
                : e.getMessage();

        return ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.toString())
                .message(detailedMessage)
                .reason("Incorrect parameters")
                .timestamp(LocalDateTime.now())
                .build();
    }

}