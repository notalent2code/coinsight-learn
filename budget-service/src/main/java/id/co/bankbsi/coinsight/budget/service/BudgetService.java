package id.co.bankbsi.coinsight.budget.service;

import id.co.bankbsi.coinsight.budget.event.BudgetAlertEvent;
import id.co.bankbsi.coinsight.budget.event.TransactionCreatedEvent;
import id.co.bankbsi.coinsight.budget.event.TransactionDeletedEvent;
import id.co.bankbsi.coinsight.budget.model.Budget;
import id.co.bankbsi.coinsight.budget.repository.BudgetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

  private final BudgetRepository budgetRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(topics = "transactions", groupId = "budget-service")
  @Transactional
  public void handleTransactionCreated(TransactionCreatedEvent event) {
    log.info("Processing transaction for budget check: {}", event.getId());

    // Only process expense transactions
    if (!"expense".equals(event.getCategoryType())) {
      return;
    }

    UUID userId = UUID.fromString(event.getUserId());

    // Find active budgets for this user and category
    List<Budget> budgets =
        budgetRepository.findActiveBudgetsByUserIdAndCategoryId(userId, event.getCategoryId());

    for (Budget budget : budgets) {
      updateBudgetSpending(budget, event);
    }
  }

  @KafkaListener(topics = "transaction-deletions", groupId = "budget-service")
  @Transactional
  public void handleTransactionDeleted(TransactionDeletedEvent event) {
    log.info("Processing transaction deletion for budget reversal: {}", event.getTransactionId());

    // Only process expense transactions
    if (!"expense".equals(event.getCategoryType())) {
      return;
    }

    UUID userId = UUID.fromString(event.getUserId());

    // Find active budgets for this user and category
    List<Budget> budgets =
        budgetRepository.findActiveBudgetsByUserIdAndCategoryId(userId, event.getCategoryId());

    for (Budget budget : budgets) {
      reverseBudgetSpending(budget, event);
    }
  }

  private void reverseBudgetSpending(Budget budget, TransactionDeletedEvent event) {
    // Subtract the transaction amount from current spending
    BigDecimal newSpentAmount = budget.getCurrentSpent().subtract(event.getAmount());

    // Ensure we don't go below zero
    if (newSpentAmount.compareTo(BigDecimal.ZERO) < 0) {
      newSpentAmount = BigDecimal.ZERO;
      log.warn(
          "Budget reversal would result in negative spending, setting to zero for budget: {}",
          budget.getId());
    }

    budget.setCurrentSpent(newSpentAmount);
    budgetRepository.save(budget);

    log.info(
        "Reversed budget spending for transaction deletion - Budget: {}, Amount reversed: {}, New total: {}",
        budget.getId(),
        event.getAmount(),
        newSpentAmount);
  }

  private void updateBudgetSpending(Budget budget, TransactionCreatedEvent event) {
    BigDecimal oldSpent = budget.getCurrentSpent();
    BigDecimal newSpent = oldSpent.add(event.getAmount());
    budget.setCurrentSpent(newSpent);

    Budget savedBudget = budgetRepository.save(budget);
    log.info("Updated budget {} spending from {} to {}", budget.getId(), oldSpent, newSpent);

    // Check if any thresholds are exceeded
    checkBudgetThresholds(savedBudget, event);
  }

  private void checkBudgetThresholds(Budget budget, TransactionCreatedEvent event) {
    BigDecimal spentPercentage =
        budget
            .getCurrentSpent()
            .divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

    String alertType = null;
    Integer threshold = null;

    if (spentPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
      alertType = "EXCEEDED";
      threshold = 100;
    } else if (spentPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
      alertType = "WARNING";
      threshold = 80;
    } else if (spentPercentage.compareTo(BigDecimal.valueOf(50)) >= 0) {
      alertType = "INFO";
      threshold = 50;
    }

    if (alertType != null) {
      publishBudgetAlert(budget, event, alertType, threshold);
    }
  }

  private void publishBudgetAlert(
      Budget budget, TransactionCreatedEvent event, String alertType, Integer threshold) {
    try {
      BudgetAlertEvent alertEvent =
          BudgetAlertEvent.builder()
              .budgetId(budget.getId().toString())
              .userId(event.getUserId())
              .budgetName(budget.getName())
              .categoryName(event.getCategoryName())
              .budgetLimit(budget.getAmount())
              .currentSpent(budget.getCurrentSpent())
              .transactionAmount(event.getAmount())
              .thresholdPercentage(threshold)
              .alertType(alertType)
              .userEmail(event.getUserEmail()) // Assuming email is part of the event
              .alertTime(LocalDateTime.now())
              .build();

      kafkaTemplate.send("budget-alerts", alertEvent);
      log.info("Published budget alert: {} for budget {}", alertType, budget.getId());

    } catch (Exception e) {
      log.error("Failed to publish budget alert for budget {}: {}", budget.getId(), e.getMessage());
    }
  }
}
