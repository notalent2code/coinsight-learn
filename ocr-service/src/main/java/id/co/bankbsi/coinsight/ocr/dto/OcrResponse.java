package id.co.bankbsi.coinsight.ocr.dto;

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
  private String rawText;
  private BigDecimal extractedAmount;
  private LocalDateTime extractedDate;
  private String merchantName;
  private Integer categoryId;
  private Map<String, Object> extractedFields;
}
