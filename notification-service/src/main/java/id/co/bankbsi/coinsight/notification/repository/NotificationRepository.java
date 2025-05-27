package id.co.bankbsi.coinsight.notification.repository;

import id.co.bankbsi.coinsight.notification.model.Notification;
import id.co.bankbsi.coinsight.notification.model.NotificationStatus;
import id.co.bankbsi.coinsight.notification.model.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  List<Notification> findByStatusAndCreatedAtBefore(
      NotificationStatus status, LocalDateTime dateTime);

  @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = :status")
  long countByUserIdAndStatus(
      @Param("userId") UUID userId, @Param("status") NotificationStatus status);

  List<Notification> findByUserIdAndNotificationTypeAndStatus(
      UUID userId, NotificationType notificationType, NotificationStatus status);
}
