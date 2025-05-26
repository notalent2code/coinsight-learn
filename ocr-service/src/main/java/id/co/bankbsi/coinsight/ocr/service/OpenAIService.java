package id.co.bankbsi.coinsight.ocr.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import id.co.bankbsi.coinsight.ocr.dto.CategoryDto;
import id.co.bankbsi.coinsight.ocr.dto.TransactionCreationRequest;
import id.co.bankbsi.coinsight.ocr.exception.OcrProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    private final OpenAIClient openAIClient;
    private final String deploymentId;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionCreationRequest processReceiptText(String receiptText, Integer suggestedCategoryId, List<CategoryDto> categories) {
        try {
            log.info("Processing receipt text with OpenAI using deployment: {}", deploymentId);

            // Create category options string for the AI
            StringBuilder categoryOptions = new StringBuilder("Available categories:\n");
            for (CategoryDto category : categories) {
                categoryOptions.append("- ").append(category.getId()).append(": ")
                              .append(category.getName()).append(" (").append(category.getType()).append(")\n");
            }

            // Create the messages list with proper types
            List<ChatRequestMessage> chatMessages = Arrays.asList(
                new ChatRequestSystemMessage(
                    "You are a financial assistant that extracts transaction information from receipt text. " +
                    "Analyze the text and extract the following information:\n" +
                    "1. amount: The numerical amount of the transaction (without currency symbols)\n" +
                    "2. description: A brief description of what the transaction was for\n" +
                    "3. categoryId: The most appropriate category ID from the list below\n" +
                    "4. confidence: How confident you are in your categorization (0-100)\n\n" +
                    categoryOptions.toString() + "\n" +
                    "If the input doesn't look like a receipt or transaction evidence (like a shopping receipt, payment confirmation, or transfer receipt), " +
                    "respond with 'INVALID_RECEIPT'.\n\n" +
                    "Format your response as a JSON object with these keys:\n" +
                    "{\n" +
                    "  \"amount\": [number],\n" +
                    "  \"description\": [string],\n" +
                    "  \"categoryId\": [number],\n" +
                    "  \"confidence\": [number]\n" +
                    "}"
                ),
                new ChatRequestUserMessage(receiptText)
            );

            // Create the chat completion options
            ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
            options.setMaxTokens(8000);
            
            // Log the request details for debugging
            // log.debug("OpenAI endpoint: {}", openAIClient.getEndpoint());
            log.debug("Using deployment ID: {}", deploymentId);
            log.debug("Message count: {}", chatMessages.size());
            
            // Get completion from OpenAI
            ChatCompletions chatCompletions = openAIClient.getChatCompletions(deploymentId, options);
            
            if (chatCompletions == null || chatCompletions.getChoices() == null || chatCompletions.getChoices().isEmpty()) {
                throw new OcrProcessingException("No response received from OpenAI");
            }
            
            String responseContent = chatCompletions.getChoices().get(0).getMessage().getContent();
            log.info("OpenAI response: {}", responseContent);

            // Check if the receipt is invalid
            if (responseContent.contains("INVALID_RECEIPT")) {
                throw new OcrProcessingException("The provided image does not appear to be a valid receipt or transaction evidence");
            }

            // Parse JSON response
            // The response might be wrapped in ```json and ``` markers, so we'll clean that up
            responseContent = responseContent.replaceAll("```json", "").replaceAll("```", "").trim();
            
            // Extract transaction data using rule-based methods as backup
            BigDecimal amount = extractAmountFromText(receiptText);
            String description = extractDescriptionFromText(receiptText);
            
            // Try to parse JSON response for structured data
            try {
                // Parse the JSON to get values including confidence
                JsonNode jsonNode = objectMapper.readTree(responseContent);
                
                // Override with AI extracted data if available
                if (jsonNode.has("amount") && !jsonNode.get("amount").isNull()) {
                    amount = new BigDecimal(jsonNode.get("amount").asText());
                }
                
                if (jsonNode.has("description") && !jsonNode.get("description").isNull()) {
                    description = jsonNode.get("description").asText();
                }
                
                // Get AI's confidence in its categorization
                int aiConfidence = 0;
                if (jsonNode.has("confidence") && !jsonNode.get("confidence").isNull()) {
                    aiConfidence = jsonNode.get("confidence").asInt();
                }
                
                // Get AI's category suggestion
                Integer aiCategoryId = null;
                if (jsonNode.has("categoryId") && !jsonNode.get("categoryId").isNull()) {
                    aiCategoryId = jsonNode.get("categoryId").asInt();
                }
                
                // Get keyword-based category and confidence
                Map<String, Object> keywordCategory = categoryService.determineCategory(receiptText, categories);
                Integer keywordCategoryId = (Integer) keywordCategory.get("categoryId");
                int keywordConfidence = (Integer) keywordCategory.get("confidence");
                
                // Hybrid decision logic: compare confidences and use the most confident result
                Integer finalCategoryId;
                
                if (aiCategoryId != null && aiConfidence >= 50 && aiConfidence >= keywordConfidence) {
                    // Use AI categorization if it's confident enough and more confident than keywords
                    finalCategoryId = aiCategoryId;
                    log.info("Using AI categorization (ID: {}, confidence: {}%)", finalCategoryId, aiConfidence);
                } else if (keywordConfidence >= 30) {
                    // Use keyword categorization if it has reasonable confidence
                    finalCategoryId = keywordCategoryId;
                    log.info("Using keyword categorization (ID: {}, confidence: {}%)", finalCategoryId, keywordConfidence);
                } else if (suggestedCategoryId != null) {
                    // Fall back to suggested category if provided
                    finalCategoryId = suggestedCategoryId;
                    log.info("Using suggested category ID: {}", finalCategoryId);
                } else {
                    // Default to "transfer" (1) for a bank transfer receipt
                    finalCategoryId = 1;
                    log.info("Defaulting to 'transfer' category (ID: 1)");
                }
                
                // Create and populate the request
                TransactionCreationRequest request = new TransactionCreationRequest();
                request.setAmount(amount);
                request.setDescription(description != null ? description : "Transaction from receipt");
                request.setCategoryId(finalCategoryId);
                request.setReceiptText(receiptText);
                
                return request;
                
            } catch (Exception jsonEx) {
                log.warn("Failed to parse OpenAI JSON response: {}", jsonEx.getMessage());
                
                // Fallback to rule-based processing
                Map<String, Object> keywordCategory = categoryService.determineCategory(receiptText, categories);
                Integer categoryId = (Integer) keywordCategory.get("categoryId");
                
                // Create a basic transaction request
                TransactionCreationRequest request = new TransactionCreationRequest();
                request.setAmount(amount);
                request.setDescription(description != null ? description : "Transaction from receipt");
                request.setCategoryId(categoryId != null ? categoryId : 
                                     (suggestedCategoryId != null ? suggestedCategoryId : 1));
                request.setReceiptText(receiptText);
                
                return request;
            }
            
        } catch (Exception e) {
            log.error("Error processing receipt text with OpenAI: {}", e.getMessage(), e);
            
            // If OpenAI failed, fallback to rule-based processing
            try {
                log.info("Falling back to rule-based processing");
                
                // Extract amount from text using simple rules
                BigDecimal amount = extractAmountFromText(receiptText);
                
                // Extract description or use default
                String description = extractDescriptionFromText(receiptText);
                
                // Determine category using keyword matching
                Map<String, Object> keywordCategory = categoryService.determineCategory(receiptText, categories);
                Integer categoryId = (Integer) keywordCategory.get("categoryId");
                
                // Create a basic transaction request
                TransactionCreationRequest request = new TransactionCreationRequest();
                request.setAmount(amount);
                request.setDescription(description != null ? description : "Transaction from receipt");
                request.setCategoryId(categoryId != null ? categoryId : 
                                     (suggestedCategoryId != null ? suggestedCategoryId : 1));
                request.setReceiptText(receiptText);
                
                return request;
            } catch (Exception fallbackEx) {
                // If fallback also fails, throw the original exception
                throw new OcrProcessingException("Failed to process receipt text: " + e.getMessage(), e);
            }
        }
    }
    
    // Smart amount extraction for Indonesian receipts
    private BigDecimal extractAmountFromText(String text) {
        try {
            // This implementation recognizes Indonesian currency formats
            String[] lines = text.split("\n");
            
            // First look for transfer amount or total
            for (String line : lines) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("nominal") || lowerLine.contains("total") || 
                    lowerLine.contains("jumlah") || lowerLine.contains("amount")) {
                    
                    // Extract amount with Rp or other patterns
                    if (lowerLine.contains("rp")) {
                        // Extract digits
                        String amountStr = line.replaceAll("[^0-9]", "");
                        if (!amountStr.isEmpty()) {
                            return new BigDecimal(amountStr);
                        }
                    }
                }
            }
            
            // If not found, look for any line with Rp and numbers
            for (String line : lines) {
                if (line.toLowerCase().contains("rp")) {
                    String amountStr = line.replaceAll("[^0-9]", "");
                    if (!amountStr.isEmpty()) {
                        return new BigDecimal(amountStr);
                    }
                }
            }
            
            // If still not found, look for any numbers after "transfer" keyword
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].toLowerCase().contains("transfer") && i < lines.length - 1) {
                    // Check the next few lines for numbers
                    for (int j = i + 1; j < Math.min(i + 4, lines.length); j++) {
                        String amountStr = lines[j].replaceAll("[^0-9]", "");
                        if (!amountStr.isEmpty()) {
                            return new BigDecimal(amountStr);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract amount: {}", e.getMessage());
        }
        
        // Return a default value if extraction fails
        return BigDecimal.ZERO;
    }

    // Extract description from receipt
    private String extractDescriptionFromText(String text) {
        try {
            // Look for common description indicators in Indonesian receipts
            String[] lines = text.split("\n");
            for (String line : lines) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("catatan") || lowerLine.contains("note") ||
                    lowerLine.contains("deskripsi") || lowerLine.contains("description")) {
                    // Return the content after the indicator
                    String description = line;
                    int index = line.toLowerCase().indexOf(":");
                    if (index != -1 && index < line.length() - 1) {
                        description = line.substring(index + 1).trim();
                    }
                    
                    // If next line is not empty and doesn't contain a keyword, include it too
                    // (sometimes description continues to next line)
                    int lineIndex = Arrays.asList(lines).indexOf(line);
                    if (lineIndex < lines.length - 1) {
                        String nextLine = lines[lineIndex + 1];
                        if (!nextLine.trim().isEmpty() && 
                            !containsTransactionKeywords(nextLine.toLowerCase())) {
                            description += " " + nextLine.trim();
                        }
                    }
                    
                    return description.trim();
                }
            }
            
            // For BSI transfers, look for "Nama Penerima" as a fallback
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].toLowerCase().contains("nama penerima") && i < lines.length - 1) {
                    StringBuilder recipient = new StringBuilder();
                    // Get next 1-3 lines as recipient name
                    for (int j = i + 1; j < Math.min(i + 3, lines.length); j++) {
                        String line = lines[j].trim();
                        // Stop if we hit another field
                        if (containsTransactionKeywords(line.toLowerCase())) {
                            break;
                        }
                        recipient.append(line).append(" ");
                    }
                    return "Transfer to " + recipient.toString().trim();
                }
            }
            
        } catch (Exception e) {
            log.warn("Failed to extract description: {}", e.getMessage());
        }
        
        return null;
    }
    
    private boolean containsTransactionKeywords(String text) {
        String[] keywords = {"nominal", "rekening", "total", "biaya", "admin", "bank", "bsi", "struk", "tanggal"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}