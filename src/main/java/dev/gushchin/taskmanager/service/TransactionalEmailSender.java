package dev.gushchin.taskmanager.service;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalEmailSender {
    private static final String API_KEY_HEADER = "api-key";
    private static final String INVALID_RESPONSE_MESSAGE = "Brevo API returned an invalid success response";

    private final RestClient brevoRestClient;
    private final AppProperties appProperties;

    public void send(String recipient, String subject, String htmlContent, String textContent) {
        validateApiKey();
        BrevoEmailRequest request = new BrevoEmailRequest(
                new EmailAddress(appProperties.getMail().getFrom()),
                List.of(new EmailAddress(recipient)),
                subject,
                htmlContent,
                textContent);
        BrevoEmailResponse response;

        try {
            response = brevoRestClient
                    .post()
                    .uri(appProperties.getBrevo().getApiUrl())
                    .header(API_KEY_HEADER, appProperties.getBrevo().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BrevoEmailResponse.class);
        } catch (RestClientResponseException ex) {
            logHttpFailure(ex);
            throw new TransactionalEmailSendingException(ex);
        } catch (RestClientException ex) {
            logClientFailure(ex);
            throw new TransactionalEmailSendingException(ex);
        }

        if (response == null || !StringUtils.hasText(response.messageId())) {
            log.warn(INVALID_RESPONSE_MESSAGE);
            throw new TransactionalEmailSendingException(
                    INVALID_RESPONSE_MESSAGE, new IllegalStateException("Missing messageId"));
        }
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(appProperties.getBrevo().getApiKey())) {
            throw new TransactionalEmailSendingException(
                    "Brevo API key is not configured", new IllegalStateException("Missing Brevo API key"));
        }
    }

    private void logHttpFailure(RestClientResponseException exception) {
        if (log.isWarnEnabled()) {
            log.warn(
                    "Brevo API delivery failed: status={}, errorType={}",
                    exception.getStatusCode().value(),
                    exception.getClass().getSimpleName());
        }
    }

    private void logClientFailure(RestClientException exception) {
        if (log.isWarnEnabled()) {
            Throwable rootCause = getRootCause(exception);
            log.warn(
                    "Brevo API delivery failed: errorType={}",
                    rootCause.getClass().getSimpleName());
        }
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && !rootCause.getCause().equals(rootCause)) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }

    private record BrevoEmailRequest(
            EmailAddress sender, List<EmailAddress> to, String subject, String htmlContent, String textContent) {}

    private record BrevoEmailResponse(String messageId) {}

    private record EmailAddress(String email) {}
}
