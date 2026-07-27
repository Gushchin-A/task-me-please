package dev.gushchin.taskmanager.exception;

public class InvitationEmailSendingException extends TransactionalEmailSendingException {
    public InvitationEmailSendingException(Throwable cause) {
        super("Invitation email sending failed", cause);
    }
}
