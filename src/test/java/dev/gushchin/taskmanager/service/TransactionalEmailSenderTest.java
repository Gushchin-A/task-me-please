package dev.gushchin.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.gushchin.taskmanager.config.AppProperties;
import dev.gushchin.taskmanager.exception.TransactionalEmailSendingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class TransactionalEmailSenderTest {
    private static final String API_KEY = "test-api-key";
    private static final String API_URL = "https://api.brevo.test/v3/smtp/email";

    private MockRestServiceServer server;
    private TransactionalEmailSender emailSender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AppProperties appProperties = new AppProperties();
        appProperties.getBrevo().setApiKey(API_KEY);
        appProperties.getBrevo().setApiUrl(API_URL);
        appProperties.getMail().setFrom("no-reply@test.com");
        emailSender = new TransactionalEmailSender(builder.build(), appProperties);
    }

    @Test
    void sendShouldBuildExpectedBrevoRequest() {
        server.expect(once(), requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                        content()
                                .json(
                                        """
                        {
                          "sender": {"email": "no-reply@test.com"},
                          "to": [{"email": "user@test.com"}],
                          "subject": "Subject",
                          "htmlContent": "<p>Message html</p>",
                          "textContent": "Message text"
                        }
                        """))
                .andRespond(withSuccess("{\"messageId\":\"message-id\"}", MediaType.APPLICATION_JSON));

        emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text");

        server.verify();
    }

    @Test
    void sendShouldWrapClientErrorAndLogSafeDiagnostics(CapturedOutput output) {
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API delivery failed"));
        assertTrue(output.getAll().contains("status=400"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    @Test
    void sendShouldWrapServerErrorAndLogSafeDiagnostics(CapturedOutput output) {
        server.expect(requestTo(API_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API delivery failed"));
        assertTrue(output.getAll().contains("status=503"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    @Test
    void sendShouldWrapTimeoutAndLogSafeDiagnostics(CapturedOutput output) {
        server.expect(requestTo(API_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API delivery failed"));
        assertTrue(output.getAll().contains("SocketTimeoutException"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    @Test
    void sendShouldWrapNetworkErrorAndLogSafeDiagnostics(CapturedOutput output) {
        server.expect(requestTo(API_URL)).andRespond(withException(new ConnectException("Connection refused")));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API delivery failed"));
        assertTrue(output.getAll().contains("ConnectException"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    @Test
    void sendShouldRejectSuccessfulResponseWithoutMessageId(CapturedOutput output) {
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess("{\"unexpected\":\"value\"}", MediaType.APPLICATION_JSON));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API returned an invalid success response"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    @Test
    void sendShouldWrapMalformedResponseAndLogSafeDiagnostics(CapturedOutput output) {
        server.expect(requestTo(API_URL)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThrows(
                TransactionalEmailSendingException.class,
                () -> emailSender.send("user@test.com", "Subject", "<p>Message html</p>", "Message text"));

        assertTrue(output.getAll().contains("Brevo API delivery failed"));
        assertSafeDiagnostics(output);
        server.verify();
    }

    private void assertSafeDiagnostics(CapturedOutput output) {
        assertFalse(output.getAll().contains(API_KEY));
        assertFalse(output.getAll().contains("user@test.com"));
        assertFalse(output.getAll().contains("Message text"));
    }
}
