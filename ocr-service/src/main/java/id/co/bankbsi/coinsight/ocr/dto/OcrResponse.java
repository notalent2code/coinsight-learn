package id.co.bankbsi.coinsight.ocr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
  private UUID transactionId;
  private UUID userId;
  private BigDecimal amount;
  private TransactionCategoryDto category;
  private String description;
  private String receiptText;
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime transactionDate;
  
  // Additional OCR-specific fields
  // private String rawText;
  // private BigDecimal extractedAmount;
  
  // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  // private LocalDateTime extractedDate;
  
  // private String merchantName;
  // private Map<String, Object> extractedFields;
  
  // Processing metadata
  // private String processingStatus;
  // private String confidence;
}
