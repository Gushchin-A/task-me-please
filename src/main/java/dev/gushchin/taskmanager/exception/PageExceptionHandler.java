package dev.gushchin.taskmanager.exception;

import dev.gushchin.taskmanager.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class PageExceptionHandler {
    private static final String NOT_FOUND_VIEW = "error/404";

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({
        AccessDeniedForTaskException.class,
        PageNotFoundException.class,
        TaskNotFoundException.class,
        TeamMemberNotFoundException.class,
        TeamNotFoundException.class
    })
    public String handleNotFound() {
        return NOT_FOUND_VIEW;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/users/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "Resource not found"));
        }

        return NOT_FOUND_VIEW;
    }
}
