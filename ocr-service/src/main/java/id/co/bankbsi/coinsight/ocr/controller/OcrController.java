package id.co.bankbsi.coinsight.ocr.controller;

import id.co.bankbsi.coinsight.ocr.dto.OcrRequest;
import id.co.bankbsi.coinsight.ocr.dto.OcrResponse;
import id.co.bankbsi.coinsight.ocr.exception.FileTooLargeException;
import id.co.bankbsi.coinsight.ocr.exception.InvalidFileFormatException;
import id.co.bankbsi.coinsight.ocr.exception.OcrProcessingException;
import id.co.bankbsi.coinsight.ocr.exception.TextExtractionException;
import id.co.bankbsi.coinsight.ocr.service.OcrService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

  private final OcrService ocrService;

  @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<OcrResponse> processReceiptTransaction(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "categoryId", required = false) Integer categoryId,
      @RequestHeader("Authorization") String authToken) {

    Path tempFile = null;
    try {
      log.info("Processing OCR request for file: {}", file.getOriginalFilename());

      OcrResponse response = ocrService.processReceipt(file, categoryId, authToken);

      log.info("OCR processing completed successfully");
      return ResponseEntity.ok(response);

    } catch (TextExtractionException | InvalidFileFormatException | FileTooLargeException
        | IllegalArgumentException e) {
      // Re-throw specific exceptions to be handled by GlobalExceptionHandler
      throw e;
    } catch (OcrProcessingException e) {
      log.error("OCR processing error: {}", e.getMessage());
      throw e; // Let global exception handler deal with it
    } catch (Exception e) {
      log.error("Unexpected error in OCR controller: {}", e.getMessage(), e);
      throw new OcrProcessingException("Failed to process OCR request: " + e.getMessage());
    } finally {
      // Clean up temp file in finally block
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
          log.debug("Cleaned up temporary file: {}", tempFile);
        } catch (Exception e) {
          log.warn("Failed to delete temporary file: {}", e.getMessage());
        }
      }
    }
  }
}
