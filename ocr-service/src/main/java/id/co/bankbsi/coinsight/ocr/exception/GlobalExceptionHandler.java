package id.co.bankbsi.coinsight.ocr.exception;

import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import id.co.bankbsi.coinsight.ocr.dto.ErrorResponse;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(TextExtractionException.class)
  public ResponseEntity<ErrorResponse> handleTextExtractionException(TextExtractionException e, HttpServletRequest request) {
    log.warn("Text extraction failed: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Text Extraction Failed",
        "No text could be extracted from the image. Please ensure the image is clear and contains readable text.",
        request.getRequestURI()
    );
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(InvalidFileFormatException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFileFormatException(InvalidFileFormatException e, HttpServletRequest request) {
    log.warn("Invalid file format: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Invalid File Format",
        "Please upload a valid image file (JPG, PNG, PDF)",
        request.getRequestURI()
    );
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(FileTooLargeException.class)
  public ResponseEntity<ErrorResponse> handleFileTooLargeException(FileTooLargeException e, HttpServletRequest request) {
    log.warn("File too large: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "File Too Large",
        "File size exceeds maximum allowed limit. Please upload a smaller file.",
        request.getRequestURI()
    );
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
    log.warn("Upload size exceeded: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "File Too Large",
        "File size exceeds maximum allowed limit",
        request.getRequestURI()
    );
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(OcrProcessingException.class)
  public ResponseEntity<ErrorResponse> handleOcrProcessingException(OcrProcessingException e, HttpServletRequest request) {
    log.error("OCR processing error: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "OCR Processing Error",
        "Failed to process the document. Please try again or contact support if the problem persists.",
        request.getRequestURI()
    );
    return ResponseEntity.internalServerError().body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
    log.warn("Invalid argument: {}", e.getMessage());
    ErrorResponse error = createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Invalid Request",
        e.getMessage(),
        request.getRequestURI()
    );
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
    log.error("Unexpected error in OCR service: {}", e.getMessage(), e);
    ErrorResponse error = createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "An unexpected error occurred while processing your request",
        request.getRequestURI()
    );
    return ResponseEntity.internalServerError().body(error);
  }

  private ErrorResponse createErrorResponse(HttpStatus status, String error, String message, String path) {
    return ErrorResponse.builder()
        .status(status.value())
        .error(error)
        .message(message)
        .path(path)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
