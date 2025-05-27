package id.co.bankbsi.coinsight.budget.dto;

import id.co.bankbsi.coinsight.budget.model.BudgetPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BudgetResponse {
  private UUID id;
  private String name;
  private Integer categoryId;
  private String categoryName;
  private BigDecimal amount;
  private BigDecimal currentSpent;
  private BigDecimal remaining;
  private BigDecimal percentageUsed;
  private BudgetPeriod period;
  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean isActive;
  private Boolean isExceeded;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
