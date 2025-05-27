package id.co.bankbsi.coinsight.ocr.service;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.models.*;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import id.co.bankbsi.coinsight.ocr.client.CategoryClient;
import id.co.bankbsi.coinsight.ocr.client.TransactionServiceClient;
import id.co.bankbsi.coinsight.ocr.dto.CategoryDto;
import id.co.bankbsi.coinsight.ocr.dto.OcrRequest;
import id.co.bankbsi.coinsight.ocr.dto.OcrResponse;
import id.co.bankbsi.coinsight.ocr.dto.TransactionCreationRequest;
import id.co.bankbsi.coinsight.ocr.dto.TransactionResponse;
import id.co.bankbsi.coinsight.ocr.exception.OcrProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

  private final DocumentAnalysisClient documentAnalysisClient;
  private final TransactionServiceClient transactionServiceClient;
  private final OpenAIService openAIService;
  private final CategoryClient categoryClient;

  // private final CategoryService categoryService;

  /** Process a receipt from a file */
  @CircuitBreaker(name = "processReceipt", fallbackMethod = "processReceiptFallback")
  public OcrResponse processReceiptFromFile(OcrRequest request, String authToken) {
    try {
      log.info("Processing receipt from file: {}", request.getImagePath());

      // Verify the file exists
      File file = new File(request.getImagePath());
      if (!file.exists()) {
        throw new OcrProcessingException("Receipt file not found");
      }

      // Analyze receipt using Azure Form Recognizer
      SyncPoller<OperationResult, AnalyzeResult> analyzeReceiptPoller =
          documentAnalysisClient.beginAnalyzeDocument(
              "prebuilt-receipt", BinaryData.fromFile(Paths.get(request.getImagePath())));

      AnalyzeResult receiptResults = analyzeReceiptPoller.getFinalResult();

      return processAnalyzeResult(receiptResults, request, authToken);

    } catch (Exception e) {
      log.error("Error processing receipt from file: {}", e.getMessage(), e);
      throw new OcrProcessingException("Failed to process receipt: " + e.getMessage(), e);
    }
  }

  /** Process a receipt from a URL */
  @CircuitBreaker(name = "processReceipt", fallbackMethod = "processReceiptFallback")
  public OcrResponse processReceipt(OcrRequest request, String authToken) {
    try {
      log.info("Processing receipt from URL: {}", request.getImageUrl());

      SyncPoller<OperationResult, AnalyzeResult> analyzeReceiptPoller =
          documentAnalysisClient.beginAnalyzeDocumentFromUrl(
              "prebuilt-receipt", request.getImageUrl());

      AnalyzeResult receiptResults = analyzeReceiptPoller.getFinalResult();

      return processAnalyzeResult(receiptResults, request, authToken);

    } catch (Exception e) {
      log.error("Error processing receipt from URL: {}", e.getMessage(), e);
      throw new OcrProcessingException("Failed to process receipt: " + e.getMessage(), e);
    }
  }

  /** Common method to process the analyze result from Form Recognizer */
  private OcrResponse processAnalyzeResult(
      AnalyzeResult receiptResults, OcrRequest request, String authToken) {
    // Extract raw text from receipt
    String rawText = extractRawText(receiptResults);

    if (rawText.trim().isEmpty()) {
      throw new OcrProcessingException("No text could be extracted from the image");
    }

    // Get all categories for the hybrid categorization approach
    List<CategoryDto> categories = categoryClient.getCategories(authToken);

    // Process receipt text with hybrid approach (OpenAI + keyword matching)
    TransactionCreationRequest transactionRequest =
        openAIService.processReceiptText(rawText, request.getCategoryId(), categories);

    log.info("Transaction request created: {}", transactionRequest);

    // Create transaction record via Transaction Service
    TransactionResponse transactionResponse =
        transactionServiceClient.createTransactionFromOcr(authToken, transactionRequest);

    // For response, extract more details to return to client
    BigDecimal extractedAmount =
        transactionRequest.getAmount() != null
            ? transactionRequest.getAmount()
            : extractAmount(receiptResults);
    LocalDateTime extractedDate = extractDate(receiptResults);
    String merchantName = extractMerchantName(receiptResults);
    Map<String, Object> extractedFields = extractAdditionalFields(receiptResults);

    log.info(
        "Receipt processed successfully. Extracted amount: {}, date: {}, merchant: {}, description: {}, category: {}",
        extractedAmount,
        extractedDate,
        merchantName,
        transactionRequest.getDescription(),
        transactionRequest.getCategoryId());

    return OcrResponse.builder()
        .transactionId(transactionResponse.getId())
        .rawText(rawText)
        .extractedAmount(extractedAmount)
        .extractedDate(extractedDate)
        .merchantName(merchantName)
        .categoryId(transactionRequest.getCategoryId())
        .extractedFields(extractedFields)
        .build();
  }

  public OcrResponse processReceiptFallback(OcrRequest request, String authToken, Throwable e) {
    log.error("Circuit breaker triggered when processing receipt: {}", e.getMessage());
    return OcrResponse.builder().rawText("OCR processing failed. Please try again later.").build();
  }

  private String extractRawText(AnalyzeResult receiptResults) {
    StringBuilder rawText = new StringBuilder();
    for (DocumentPage page : receiptResults.getPages()) {
      for (DocumentLine line : page.getLines()) {
        rawText.append(line.getContent()).append("\n");
      }
    }
    return rawText.toString();
  }

  private BigDecimal extractAmount(AnalyzeResult receiptResults) {
    // Implement logic to extract amount from receipt results
    return BigDecimal.ZERO; // Placeholder
  }

  private LocalDateTime extractDate(AnalyzeResult receiptResults) {
    // Implement logic to extract date from receipt results
    return LocalDateTime.now(); // Placeholder
  }

  private String extractMerchantName(AnalyzeResult receiptResults) {
    // Implement logic to extract merchant name from receipt results
    return ""; // Placeholder
  }

  private Map<String, Object> extractAdditionalFields(AnalyzeResult receiptResults) {
    Map<String, Object> fields = new HashMap<>();
    // Implement logic to extract additional fields from receipt results
    return fields;
  }
}
