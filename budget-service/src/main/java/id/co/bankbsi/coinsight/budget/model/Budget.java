// filepath:
// /home/miosha/code/bsi/ojt/coinsight/budget-service/src/main/java/id/co/bankbsi/coinsight/budget/model/Budget.java
package id.co.bankbsi.coinsight.budget.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
// import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "budgets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "category_id", nullable = false)
  private Integer categoryId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "period", nullable = false)
  private BudgetPeriod period;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "current_spent", precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal currentSpent = BigDecimal.ZERO;

  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  // private List<BudgetAlert> alerts;
}
