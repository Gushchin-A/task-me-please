package dev.gushchin.taskmanager.exception;

public class InvitationEmailSendingException extends RuntimeException {
    public InvitationEmailSendingException(Throwable cause) {
        super("Invitation email sending failed", cause);
    }
}
