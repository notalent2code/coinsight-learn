package id.co.bankbsi.coinsight.notification.repository;

import id.co.bankbsi.coinsight.notification.model.ProcessedMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
  boolean existsByMessageId(String messageId);

  @Modifying
  @Query("DELETE FROM ProcessedMessage p WHERE p.processedAt < :cutoff")
  int deleteByProcessedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
