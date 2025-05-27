package id.co.bankbsi.coinsight.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(unique = true, nullable = false)
  private String messageId;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false)
  private LocalDateTime processedAt;

  @PrePersist
  protected void onCreate() {
    processedAt = LocalDateTime.now();
  }
}
