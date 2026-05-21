package dev.gushchin.taskmanager.controller;

import dev.gushchin.taskmanager.dto.ErrorResponse;
import dev.gushchin.taskmanager.exception.TeamMemberAlreadyExistsException;
import dev.gushchin.taskmanager.exception.TeamMemberNotFoundException;
import dev.gushchin.taskmanager.exception.TeamNotFoundException;
import dev.gushchin.taskmanager.exception.UserAlreadyExistsException;
import dev.gushchin.taskmanager.exception.UserNotFoundByEmailException;
import dev.gushchin.taskmanager.exception.UserNotFoundByIdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    private static final String TEAM_NOT_FOUND = "TEAM_NOT_FOUND";
    private static final String TEAM_MEMBER_NOT_FOUND = "TEAM_MEMBER_NOT_FOUND";
    private static final String TEAM_MEMBER_ALREADY_EXISTS = "TEAM_MEMBER_ALREADY_EXISTS";
    private static final String BAD_REQUEST = "BAD_REQUEST";

    @ExceptionHandler(UserNotFoundByIdException.class)
    public ResponseEntity<ErrorResponse> handlerUserNotFoundById(UserNotFoundByIdException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(USER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundByEmailException.class)
    public ResponseEntity<ErrorResponse> handlerUserNotFoundByEmailException(UserNotFoundByEmailException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(USER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlerUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(USER_ALREADY_EXISTS, ex.getMessage()));
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerTeamNotFoundException(TeamNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(TEAM_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(TeamMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerTeamMemberNotFoundException(TeamMemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(TEAM_MEMBER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(TeamMemberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlerTeamMemberAlreadyExistsException(TeamMemberAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(TEAM_MEMBER_ALREADY_EXISTS, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(BAD_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(BAD_REQUEST, "Malformed request body"));
    }
}
