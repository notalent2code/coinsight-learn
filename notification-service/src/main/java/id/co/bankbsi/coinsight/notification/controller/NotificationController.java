package id.co.bankbsi.coinsight.notification.controller;

import id.co.bankbsi.coinsight.notification.model.Notification;
import id.co.bankbsi.coinsight.notification.service.NotificationQueryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationQueryService notificationQueryService;

  @GetMapping
  public ResponseEntity<Page<Notification>> getUserNotifications(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = UUID.fromString(jwt.getSubject());
    Page<Notification> notifications =
        notificationQueryService.getUserNotifications(userId, pageable);
    return ResponseEntity.ok(notifications);
  }

  @PutMapping("/{notificationId}/mark-read")
  public ResponseEntity<Void> markAsRead(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    notificationQueryService.markAsRead(userId, notificationId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/unread-count")
  public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    long count = notificationQueryService.getUnreadCount(userId);
    return ResponseEntity.ok(count);
  }
}
