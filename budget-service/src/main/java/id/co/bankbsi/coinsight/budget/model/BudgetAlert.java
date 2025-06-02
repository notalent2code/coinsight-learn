// package id.co.bankbsi.coinsight.budget.model;

// import jakarta.persistence.*;
// import java.time.LocalDateTime;
// import java.util.UUID;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import org.hibernate.annotations.CreationTimestamp;

// @Entity
// @Table(name = "budget_alerts")
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class BudgetAlert {
//   @Id
//   @Column(name = "id")
//   private UUID id;

//   @ManyToOne(fetch = FetchType.LAZY)
//   @JoinColumn(name = "budget_id", nullable = false)
//   private Budget budget;

//   @Column(name = "threshold_percentage", nullable = false)
//   private Integer thresholdPercentage;

//   @Enumerated(EnumType.STRING)
//   @Column(name = "alert_type", nullable = false)
//   private AlertType alertType;

//   @Column(name = "is_enabled")
//   @Builder.Default
//   private Boolean isEnabled = true;

//   @CreationTimestamp
//   @Column(name = "created_at")
//   private LocalDateTime createdAt;
// }
