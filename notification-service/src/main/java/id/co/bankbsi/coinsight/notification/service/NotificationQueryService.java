package id.co.bankbsi.coinsight.notification.service;

import id.co.bankbsi.coinsight.notification.model.Notification;
import id.co.bankbsi.coinsight.notification.model.NotificationStatus;
import id.co.bankbsi.coinsight.notification.repository.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

  private final NotificationRepository notificationRepository;

  public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Transactional
  public void markAsRead(UUID userId, UUID notificationId) {
    notificationRepository
        .findById(notificationId)
        .filter(notification -> notification.getUserId().equals(userId))
        .ifPresent(
            notification -> {
              notification.setStatus(NotificationStatus.DELIVERED);
              notificationRepository.save(notification);
              log.info("Marked notification {} as read for user {}", notificationId, userId);
            });
  }

  public long getUnreadCount(UUID userId) {
    return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
  }
}
