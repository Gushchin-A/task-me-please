package dev.gushchin.taskmanager.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ApplicationErrorController implements ErrorController {
    private static final String BAD_REQUEST_VIEW = "error/400";
    private static final String INTERNAL_SERVER_ERROR_VIEW = "error/500";
    private static final String NOT_FOUND_VIEW = "error/404";

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String htmlError(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = resolveStatus(request);

        response.setStatus(status.value());

        if (status == HttpStatus.BAD_REQUEST) {
            return BAD_REQUEST_VIEW;
        }
        if (status == HttpStatus.NOT_FOUND) {
            return NOT_FOUND_VIEW;
        }
        if (status.is4xxClientError()) {
            return BAD_REQUEST_VIEW;
        }

        return INTERNAL_SERVER_ERROR_VIEW;
    }

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jsonError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);

        return ResponseEntity.status(status).body(Map.of("status", status.value(), "error", status.getReasonPhrase()));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusAttribute instanceof Integer statusCode) {
            HttpStatus status = HttpStatus.resolve(statusCode);

            if (status != null) {
                return status;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
