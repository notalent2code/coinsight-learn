package id.co.bankbsi.coinsight.transaction.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
  private String id;
  private String userId; // Keep as String (Keycloak ID)
  private String userEmail; // Include email in the event
  private BigDecimal amount;
  private Integer categoryId; // Include categoryId for budget matching
  private String categoryName;
  private String categoryType;
  private String description;
  private LocalDateTime transactionDate;
}
