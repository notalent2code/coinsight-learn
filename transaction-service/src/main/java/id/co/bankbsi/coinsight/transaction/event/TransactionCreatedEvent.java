package id.co.bankbsi.coinsight.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    private String id;
    private String userId;
    private BigDecimal amount;
    private String categoryName;
    private String categoryType;
    private String description;
    private LocalDateTime transactionDate;
}