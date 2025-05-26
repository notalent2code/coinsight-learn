package id.co.bankbsi.coinsight.transaction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
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
}