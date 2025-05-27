package id.co.bankbsi.coinsight.notification.service;

import id.co.bankbsi.coinsight.notification.repository.ProcessedMessageRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCleanupService {

  private final ProcessedMessageRepository processedMessageRepository;

  @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
  @Transactional
  public void cleanupOldProcessedMessages() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // Keep 7 days

    try {
      // You'll need to add this method to the repository
      int deletedCount = processedMessageRepository.deleteByProcessedAtBefore(cutoff);
      log.info("Cleaned up {} old processed messages", deletedCount);
    } catch (Exception e) {
      log.error("Failed to cleanup old processed messages", e);
    }
  }
}
