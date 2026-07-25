package dev.gushchin.taskmanager.exception;

public class TransactionalEmailSendingException extends RuntimeException {
    public TransactionalEmailSendingException(Throwable cause) {
        super(cause);
    }

    public TransactionalEmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
