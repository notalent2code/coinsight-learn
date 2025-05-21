package id.co.bankbsi.coinsight.ocr.service;

import com.azure.ai.formrecognizer.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.models.*;
import id.co.bankbsi.coinsight.ocr.client.TransactionServiceClient;
import id.co.bankbsi.coinsight.ocr.dto.OcrRequest;
import id.co.bankbsi.coinsight.ocr.dto.OcrResponse;
import id.co.bankbsi.coinsight.ocr.dto.TransactionCreationRequest;
import id.co.bankbsi.coinsight.ocr.dto.TransactionResponse;
import id.co.bankbsi.coinsight.ocr.exception.OcrProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final DocumentAnalysisClient documentAnalysisClient;
    private final TransactionServiceClient transactionServiceClient;

    @CircuitBreaker(name = "processReceipt", fallbackMethod = "processReceiptFallback")
    public OcrResponse processReceipt(OcrRequest request, String authToken) {
        try {
            log.info("Processing receipt from URL: {}", request.getImageUrl());
            
            // Analyze receipt using Azure Form Recognizer
            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions()
                    .setPages("1");
            
            SyncPoller<OperationResult, AnalyzeResult> analyzeReceiptPoller =
                    documentAnalysisClient.beginAnalyzeDocumentFromUrl("prebuilt-receipt", request.getImageUrl(), options);
            
            AnalyzeResult receiptResults = analyzeReceiptPoller.getFinalResult();
            
            // Extract data from receipt
            String rawText = extractRawText(receiptResults);
            BigDecimal extractedAmount = extractAmount(receiptResults);
            LocalDateTime extractedDate = extractDate(receiptResults);
            String merchantName = extractMerchantName(receiptResults);
            Map<String, Object> extractedFields = extractAdditionalFields(receiptResults);
            
            log.info("Receipt processed successfully. Extracted amount: {}, date: {}, merchant: {}",
                    extractedAmount, extractedDate, merchantName);
            
            // Create transaction record via Transaction Service
            TransactionCreationRequest transactionRequest = TransactionCreationRequest.builder()
                    .receiptText(rawText)
                    .categoryId(request.getCategoryId())
                    .build();
            
            TransactionResponse transactionResponse = transactionServiceClient.createTransactionFromOcr(authToken, transactionRequest);
            
            return OcrResponse.builder()
                    .transactionId(transactionResponse.getId())
                    .rawText(rawText)
                    .extractedAmount(extractedAmount)
                    .extractedDate(extractedDate)
                    .merchantName(merchantName)
                    .extractedFields(extractedFields)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing receipt: {}", e.getMessage(), e);
            throw new OcrProcessingException("Failed to process receipt: " + e.getMessage());
        }
    }
    
    public OcrResponse processReceiptFallback(OcrRequest request, String authToken, Throwable e) {
        log.error("Circuit breaker triggered when processing receipt: {}", e.getMessage());
        return OcrResponse.builder()
                .rawText("OCR processing failed. Please try again later.")
                .build();
    }
    
    private String extractRawText(AnalyzeResult result) {
        StringBuilder text = new StringBuilder();
        for (DocumentPage page : result.getPages()) {
            for (DocumentLine line : page.getLines()) {
                text.append(line.getContent()).append("\n");
            }
        }
        return text.toString();
    }
    
    private BigDecimal extractAmount(AnalyzeResult result) {
        // This is a simplified version. In a real-world application,
        // you would look for specific fields like "total" in the receipt
        try {
            for (DocumentKeyValuePair kvp : result.getKeyValuePairs()) {
                if (kvp.getKey() != null && 
                        kvp.getKey().getContent().toLowerCase().contains("total")) {
                    String valueText = kvp.getValue().getContent();
                    // Clean up the string to extract just the number
                    String numericValue = valueText.replaceAll("[^0-9.]", "");
                    return new BigDecimal(numericValue);
                }
            }
            
            // If we couldn't find it in key-value pairs, try to find it in the raw text
            for (DocumentPage page : result.getPages()) {
                for (DocumentLine line : page.getLines()) {
                    String lineText = line.getContent().toLowerCase();
                    if (lineText.contains("total")) {
                        // Extract numbers from this line
                        String[] parts = lineText.split("\\s+");
                        for (String part : parts) {
                            // Try to parse any numeric-looking parts
                            String numericPart = part.replaceAll("[^0-9.]", "");
                            if (!numericPart.isEmpty()) {
                                try {
                                    return new BigDecimal(numericPart);
                                } catch (NumberFormatException ignored) {
                                    // Continue if this part isn't a valid number
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract amount: {}", e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    private LocalDateTime extractDate(AnalyzeResult result) {
        try {
            // Try to find date in structured data
            for (DocumentKeyValuePair kvp : result.getKeyValuePairs()) {
                if (kvp.getKey() != null && 
                        (kvp.getKey().getContent().toLowerCase().contains("date") ||
                         kvp.getKey().getContent().toLowerCase().contains("time"))) {
                    
                    // This is simplified - in a real application you would apply
                    // more sophisticated date parsing logic here
                    return LocalDateTime.now();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract date: {}", e.getMessage());
        }
        
        return LocalDateTime.now();
    }
    
    private String extractMerchantName(AnalyzeResult result) {
        try {
            for (DocumentKeyValuePair kvp : result.getKeyValuePairs()) {
                if (kvp.getKey() != null && 
                        (kvp.getKey().getContent().toLowerCase().contains("merchant") ||
                         kvp.getKey().getContent().toLowerCase().contains("store") ||
                         kvp.getKey().getContent().toLowerCase().contains("vendor"))) {
                    return kvp.getValue().getContent();
                }
            }
            
            // If not found in key-value pairs, try the first few lines for a merchant name
            if (!result.getPages().isEmpty()) {
                DocumentPage firstPage = result.getPages().get(0);
                if (!firstPage.getLines().isEmpty()) {
                    // First line is often the merchant name in receipts
                    return firstPage.getLines().get(0).getContent();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract merchant name: {}", e.getMessage());
        }
        
        return "Unknown Merchant";
    }
    
    private Map<String, Object> extractAdditionalFields(AnalyzeResult result) {
        Map<String, Object> fields = new HashMap<>();
        
        try {
            for (DocumentKeyValuePair kvp : result.getKeyValuePairs()) {
                if (kvp.getKey() != null && kvp.getValue() != null) {
                    fields.put(kvp.getKey().getContent(), kvp.getValue().getContent());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract additional fields: {}", e.getMessage());
        }
        
        return fields;
    }
}