package id.co.bankbsi.coinsight.notification.event;

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
public class BudgetAlertEvent {
  private String budgetId;
  private String userId;
  private String budgetName;
  private String categoryName;
  private BigDecimal budgetLimit;
  private BigDecimal currentSpent;
  private BigDecimal transactionAmount;
  private Integer thresholdPercentage;
  private String alertType; // WARNING, EXCEEDED, CRITICAL
  private String userEmail;
  private LocalDateTime alertTime;
}
