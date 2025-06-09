package ch.luimo.flashsale.flashsaleeventsapi.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
