package id.co.bankbsi.coinsight.ocr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class OcrProcessingException extends RuntimeException {

  public OcrProcessingException(String message) {
    super(message);
  }

  public OcrProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
