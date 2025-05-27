package id.co.bankbsi.coinsight.auth.repository;

import id.co.bankbsi.coinsight.auth.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
