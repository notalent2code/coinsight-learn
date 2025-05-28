package id.co.bankbsi.coinsight.ocr.exception;

public class TextExtractionException extends RuntimeException {
    public TextExtractionException(String message) {
        super(message);
    }
    
    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}