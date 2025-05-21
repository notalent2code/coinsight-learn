package id.co.bankbsi.coinsight.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
    private Map<String, Object> extractedFields;
}