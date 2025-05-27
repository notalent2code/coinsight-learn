package id.co.bankbsi.coinsight.budget.controller;

import id.co.bankbsi.coinsight.budget.dto.BudgetCreateRequest;
import id.co.bankbsi.coinsight.budget.dto.BudgetResponse;
import id.co.bankbsi.coinsight.budget.dto.BudgetUpdateRequest;
import id.co.bankbsi.coinsight.budget.service.BudgetManagementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

  private final BudgetManagementService budgetManagementService;

  @PostMapping
  public ResponseEntity<BudgetResponse> createBudget(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BudgetCreateRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    BudgetResponse budget = budgetManagementService.createBudget(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(budget);
  }

  @GetMapping
  public ResponseEntity<Page<BudgetResponse>> getUserBudgets(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = UUID.fromString(jwt.getSubject());
    Page<BudgetResponse> budgets = budgetManagementService.getUserBudgets(userId, pageable);
    return ResponseEntity.ok(budgets);
  }

  @GetMapping("/{budgetId}")
  public ResponseEntity<BudgetResponse> getBudget(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID budgetId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    BudgetResponse budget = budgetManagementService.getBudget(userId, budgetId);
    return ResponseEntity.ok(budget);
  }

  @PutMapping("/{budgetId}")
  public ResponseEntity<BudgetResponse> updateBudget(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID budgetId,
      @Valid @RequestBody BudgetUpdateRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    BudgetResponse budget = budgetManagementService.updateBudget(userId, budgetId, request);
    return ResponseEntity.ok(budget);
  }

  @DeleteMapping("/{budgetId}")
  public ResponseEntity<Void> deleteBudget(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID budgetId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    budgetManagementService.deleteBudget(userId, budgetId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/active")
  public ResponseEntity<Page<BudgetResponse>> getActiveBudgets(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = UUID.fromString(jwt.getSubject());
    Page<BudgetResponse> budgets = budgetManagementService.getActiveBudgets(userId, pageable);
    return ResponseEntity.ok(budgets);
  }
}
