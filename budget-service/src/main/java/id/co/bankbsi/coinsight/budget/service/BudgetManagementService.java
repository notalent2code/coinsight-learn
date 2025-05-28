package id.co.bankbsi.coinsight.budget.service;

import id.co.bankbsi.coinsight.budget.dto.BudgetCreateRequest;
import id.co.bankbsi.coinsight.budget.dto.BudgetResponse;
import id.co.bankbsi.coinsight.budget.dto.BudgetUpdateRequest;
import id.co.bankbsi.coinsight.budget.exception.BudgetNotFoundException;
import id.co.bankbsi.coinsight.budget.exception.DuplicateBudgetException;
import id.co.bankbsi.coinsight.budget.model.Budget;
import id.co.bankbsi.coinsight.budget.model.BudgetPeriod;
import id.co.bankbsi.coinsight.budget.repository.BudgetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetManagementService {

  private final BudgetRepository budgetRepository;

  @Transactional
  public BudgetResponse createBudget(UUID userId, BudgetCreateRequest request) {
    // Validate: Only one active budget per category per user
    if (budgetRepository.existsByUserIdAndCategoryIdAndIsActiveTrue(userId, request.getCategoryId())) {
      throw new DuplicateBudgetException(
          String.format("Active budget already exists for category %d. Please deactivate existing budget first.",
              request.getCategoryId()));
    }

    // Calculate start and end dates based on period
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = calculateEndDate(startDate, request.getPeriod());

    Budget budget = Budget.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .categoryId(request.getCategoryId())
        .name(request.getName())
        .amount(request.getAmount())
        .period(request.getPeriod())
        .startDate(startDate)
        .endDate(endDate)
        .currentSpent(BigDecimal.ZERO)
        .isActive(true)
        .build();

    Budget savedBudget = this.budgetRepository.save(budget);
    log.info("Created budget {} for user {}", savedBudget.getId(), userId);

    return mapToBudgetResponse(savedBudget);
  }

  @Transactional(readOnly = true)
  public Page<BudgetResponse> getUserBudgets(UUID userId, Pageable pageable) {
    Page<Budget> budgets = this.budgetRepository.findByUserId(userId, pageable);
    List<BudgetResponse> responses = budgets.getContent().stream().map(this::mapToBudgetResponse)
        .collect(Collectors.toList());

    return new PageImpl<>(responses, pageable, budgets.getTotalElements());
  }

  @Transactional(readOnly = true)
  public BudgetResponse getBudget(UUID userId, UUID budgetId) {
    Budget budget = budgetRepository
        .findByIdAndUserId(budgetId, userId)
        .orElseThrow(() -> new BudgetNotFoundException("Budget not found with id: " + budgetId));

    return mapToBudgetResponse(budget);
  }

  @Transactional
  public BudgetResponse updateBudget(UUID userId, UUID budgetId, BudgetUpdateRequest request) {
    Budget budget = budgetRepository
        .findByIdAndUserId(budgetId, userId)
        .orElseThrow(() -> new BudgetNotFoundException("Budget not found with id: " + budgetId));

    // Check what's being changed (only update non-null fields)
    boolean amountChanged = request.getAmount() != null && !budget.getAmount().equals(request.getAmount());
    boolean periodChanged = request.getPeriod() != null && !budget.getPeriod().equals(request.getPeriod());
    boolean nameChanged = request.getName() != null && !budget.getName().equals(request.getName());

    // If only name is changed - safe to update directly
    if (nameChanged && !amountChanged && !periodChanged) {
        budget.setName(request.getName());
        Budget savedBudget = this.budgetRepository.save(budget);
        log.info("Updated budget name {} for user {}", budgetId, userId);
        return mapToBudgetResponse(savedBudget);
    }

    // If amount or period changed - update existing budget instead of creating new version
    if (amountChanged || periodChanged) {
        return updateExistingBudget(budget, request);
    }

    // If no changes were made, return current budget
    return mapToBudgetResponse(budget);
}

  private BudgetResponse updateExistingBudget(Budget budget, BudgetUpdateRequest request) {
    // Update the fields that changed
    if (request.getName() != null) {
        budget.setName(request.getName());
    }
    
    if (request.getAmount() != null) {
        budget.setAmount(request.getAmount());
    }
    
    if (request.getPeriod() != null) {
        budget.setPeriod(request.getPeriod());
        // Recalculate end date based on new period
        LocalDate newEndDate = calculateEndDate(budget.getStartDate(), request.getPeriod());
        budget.setEndDate(newEndDate);
        
        // Reset spending when period changes (optional - you can keep it)
        // budget.setCurrentSpent(BigDecimal.ZERO);
    }

    Budget savedBudget = budgetRepository.save(budget);
    log.info("Updated budget {} for user {}", budget.getId(), budget.getUserId());
    
    return mapToBudgetResponse(savedBudget);
  }

  @Transactional
  public void deleteBudget(UUID userId, UUID budgetId) {
    Budget budget = budgetRepository
        .findByIdAndUserId(budgetId, userId)
        .orElseThrow(() -> new BudgetNotFoundException("Budget not found with id: " + budgetId));

    budget.setIsActive(false);
    this.budgetRepository.save(budget);
    log.info("Deleted budget {} for user {}", budgetId, userId);
  }

  @Transactional(readOnly = true)
  public Page<BudgetResponse> getActiveBudgets(UUID userId, Pageable pageable) {
    LocalDate currentDate = LocalDate.now();
    Page<Budget> budgets = this.budgetRepository.findActiveBudgetsByUserId(userId, currentDate, pageable);

    List<BudgetResponse> responses = budgets.getContent().stream().map(this::mapToBudgetResponse)
        .collect(Collectors.toList());

    return new PageImpl<>(responses, pageable, budgets.getTotalElements());
  }

  private BudgetResponse mapToBudgetResponse(Budget budget) {
    BigDecimal remaining = budget.getAmount().subtract(budget.getCurrentSpent());
    BigDecimal percentageUsed = budget
        .getCurrentSpent()
        .divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));

    return BudgetResponse.builder()
        .id(budget.getId())
        .name(budget.getName())
        .categoryId(budget.getCategoryId())
        .categoryName(getCategoryName(budget.getCategoryId())) // You might want to implement this
        .amount(budget.getAmount())
        .currentSpent(budget.getCurrentSpent())
        .remaining(remaining)
        .percentageUsed(percentageUsed)
        .period(budget.getPeriod())
        .startDate(budget.getStartDate())
        .endDate(budget.getEndDate())
        .isActive(budget.getIsActive())
        .isExceeded(budget.getCurrentSpent().compareTo(budget.getAmount()) > 0)
        .createdAt(budget.getCreatedAt())
        .updatedAt(budget.getUpdatedAt())
        .build();
  }

  private LocalDate calculateEndDate(LocalDate startDate, BudgetPeriod period) {
    return switch (period) {
      case WEEKLY -> startDate.plusWeeks(1);
      case MONTHLY -> startDate.plusMonths(1);
      case YEARLY -> startDate.plusYears(1);
    };
  }

  private String getCategoryName(Integer categoryId) {
    // TODO: Implement category lookup or cache
    // For now, return a placeholder
    return "Category " + categoryId;
  }
}
