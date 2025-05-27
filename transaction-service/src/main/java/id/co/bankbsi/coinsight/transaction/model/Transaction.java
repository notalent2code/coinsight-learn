package id.co.bankbsi.coinsight.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "transactions")
@SQLDelete(sql = "UPDATE transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal amount;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private TransactionCategory category;

  @Column(name = "description")
  private String description;

  @Column(name = "receipt_text", columnDefinition = "TEXT")
  private String receiptText;

  @Column(name = "transaction_date")
  private LocalDateTime transactionDate;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Simple soft delete field - no restore logic needed
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  // Simple helper method
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
