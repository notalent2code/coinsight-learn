package id.co.bankbsi.coinsight.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "keycloak_id", unique = true)
  private String keycloakId;

  @Column(name = "email", unique = true)
  private String email;

  @Column(name = "full_name")
  private String fullName;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
