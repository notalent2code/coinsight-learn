package id.co.bankbsi.coinsight.notification.service;

import id.co.bankbsi.coinsight.notification.event.BudgetAlertEvent;
import id.co.bankbsi.coinsight.notification.model.ProcessedMessage;
import id.co.bankbsi.coinsight.notification.repository.ProcessedMessageRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumerService {

  private final NotificationService notificationService;
  private final ProcessedMessageRepository processedMessageRepository;

  @KafkaListener(topics = "budget-alerts", groupId = "notification-service")
  @Transactional
  public void handleBudgetAlert(
      @Payload BudgetAlertEvent event,
      @Header(KafkaHeaders.OFFSET) Long offset,
      @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition) {

    // Create a deterministic messageId based on event content + offset + partition
    String messageId =
        UUID.nameUUIDFromBytes(
                String.format(
                        "%s-%s-%d-%d", event.getUserId(), event.getBudgetId(), partition, offset)
                    .getBytes())
            .toString();

    log.info(
        "Received budget alert event for user: {}, messageId: {}", event.getUserId(), messageId);

    if (processedMessageRepository.existsByMessageId(messageId)) {
      log.info("Message already processed, skipping: {}", messageId);
      return;
    }

    try {
      notificationService.processBudgetAlert(event);

      ProcessedMessage processedMessage =
          ProcessedMessage.builder().messageId(messageId).eventType("BUDGET_ALERT").build();

      processedMessageRepository.save(processedMessage);

      log.info("Successfully processed budget alert for user: {}", event.getUserId());

    } catch (Exception e) {
      log.error(
          "Failed to process budget alert for user: {}, messageId: {}",
          event.getUserId(),
          messageId,
          e);
      throw e;
    }
  }
}
