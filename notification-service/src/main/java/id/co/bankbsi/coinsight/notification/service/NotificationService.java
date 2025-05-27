package id.co.bankbsi.coinsight.notification.service;

import id.co.bankbsi.coinsight.notification.event.BudgetAlertEvent;
import id.co.bankbsi.coinsight.notification.model.Notification;
import id.co.bankbsi.coinsight.notification.model.NotificationStatus;
import id.co.bankbsi.coinsight.notification.model.NotificationType;
import id.co.bankbsi.coinsight.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Remove this import: import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final JavaMailSender mailSender;
  private final NotificationTemplateService templateService;

  // Remove the @KafkaListener annotation - this method should only be called by
  // NotificationConsumerService
  // @KafkaListener(topics = "budget-alerts", groupId = "notification-service") // REMOVE THIS
  @Transactional
  public void processBudgetAlert(
      BudgetAlertEvent event) { // Rename from handleBudgetAlert to processBudgetAlert
    log.info("Processing budget alert notification for user: {}", event.getUserId());

    try {
      // Generate notification content from template
      String subject = templateService.generateSubject("BUDGET_" + event.getAlertType(), event);
      String content = templateService.generateContent("BUDGET_" + event.getAlertType(), event);

      // Save notification record
      Notification notification =
          Notification.builder()
              .id(UUID.randomUUID())
              .userId(UUID.fromString(event.getUserId()))
              .notificationType(NotificationType.EMAIL)
              .recipient(event.getUserEmail())
              .subject(subject)
              .content(content)
              .metadata(createMetadata(event))
              .build();

      Notification savedNotification = notificationRepository.save(notification);

      // Send email
      sendEmail(savedNotification);

    } catch (Exception e) {
      log.error("Failed to process budget alert notification: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void sendEmail(Notification notification) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(notification.getRecipient());
      message.setSubject(notification.getSubject());
      message.setText(notification.getContent());
      message.setFrom("noreply@coinsight.com");

      mailSender.send(message);

      notification.setStatus(NotificationStatus.SENT);
      notification.setSentAt(LocalDateTime.now());
      notificationRepository.save(notification);

      log.info("Email sent successfully to: {}", notification.getRecipient());

    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", notification.getRecipient(), e.getMessage());

      notification.setStatus(NotificationStatus.FAILED);
      notificationRepository.save(notification);
    }
  }

  private Map<String, Object> createMetadata(BudgetAlertEvent event) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("budgetId", event.getBudgetId());
    metadata.put("budgetName", event.getBudgetName());
    metadata.put("categoryName", event.getCategoryName());
    metadata.put("thresholdPercentage", event.getThresholdPercentage());
    metadata.put("alertType", event.getAlertType());
    metadata.put("budgetLimit", event.getBudgetLimit());
    metadata.put("currentSpent", event.getCurrentSpent());
    metadata.put("transactionAmount", event.getTransactionAmount());
    metadata.put("alertTime", event.getAlertTime());

    return metadata;
  }
}
