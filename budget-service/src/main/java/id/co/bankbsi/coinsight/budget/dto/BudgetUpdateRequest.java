package id.co.bankbsi.coinsight.budget.dto;

import id.co.bankbsi.coinsight.budget.model.BudgetPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BudgetUpdateRequest {
  @Size(min = 1, max = 100, message = "Budget name must be between 1 and 100 characters")
  private String name;

  @DecimalMin(value = "10000.00", message = "Amount must be at least 10,000.00")
  private BigDecimal amount;

  private BudgetPeriod period;
}