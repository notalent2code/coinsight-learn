package id.co.bankbsi.coinsight.ocr.exception;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(String message) {
        super(message);
    }
    
    public FileTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }
}