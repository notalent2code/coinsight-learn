package id.co.bankbsi.coinsight.budget.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDeletedEvent {
  private String transactionId;
  private String userId;
  private String userEmail;
  private BigDecimal amount;
  private Integer categoryId;
  private String categoryName;
  private String categoryType;
  private String description;
  private LocalDateTime originalTransactionDate;
  private LocalDateTime deletedAt;
}
