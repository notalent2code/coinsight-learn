package id.co.bankbsi.coinsight.budget.dto;

import id.co.bankbsi.coinsight.budget.model.BudgetPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BudgetCreateRequest {
  @NotBlank(message = "Budget name is required")
  private String name;

  @NotNull(message = "Category ID is required") private Integer categoryId;

  @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
  private BigDecimal amount;

  @NotNull(message = "Period is required") private BudgetPeriod period;
}
