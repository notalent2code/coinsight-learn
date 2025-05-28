package id.co.bankbsi.coinsight.transaction.service;

import id.co.bankbsi.coinsight.transaction.dto.*;
import id.co.bankbsi.coinsight.transaction.event.TransactionCreatedEvent;
import id.co.bankbsi.coinsight.transaction.event.TransactionDeletedEvent;
import id.co.bankbsi.coinsight.transaction.exception.CategoryNotFoundException;
import id.co.bankbsi.coinsight.transaction.exception.TransactionNotFoundException;
import id.co.bankbsi.coinsight.transaction.model.Transaction;
import id.co.bankbsi.coinsight.transaction.model.TransactionCategory;
import id.co.bankbsi.coinsight.transaction.repository.TransactionCategoryRepository;
import id.co.bankbsi.coinsight.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final TransactionCategoryRepository categoryRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public UUID getCurrentUserId() {
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString(authentication.getName());
  }

  public String getCurrentUserEmail() {
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    return authentication.getTokenAttributes().get("email").toString();
  }

  @CacheEvict(
      cacheNames = {"transactionSummary", "transactionsList", "transactionsByDateRange"},
      allEntries = true)
  @Transactional
  public TransactionResponse createTransaction(TransactionRequest request) {
    UUID userId = getCurrentUserId();
    String userEmail = getCurrentUserEmail();

    log.info("Creating transaction for user {} ({}) with request: {}", userId, userEmail, request);

    TransactionCategory category =
        categoryRepository
            .findById(request.getCategoryId())
            .orElseThrow(
                () ->
                    new CategoryNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

    Transaction transaction =
        Transaction.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .amount(request.getAmount())
            .category(category)
            .description(request.getDescription())
            .receiptText(request.getReceiptText())
            .transactionDate(
                request.getTransactionDate() != null
                    ? request.getTransactionDate()
                    : LocalDateTime.now())
            .build();

    Transaction savedTransaction = transactionRepository.save(transaction);

    // Publish event to Kafka
    TransactionCreatedEvent event =
        new TransactionCreatedEvent(
            savedTransaction.getId().toString(),
            userId.toString(),
            userEmail,
            savedTransaction.getAmount(),
            category.getId(),
            category.getName(),
            category.getType(),
            savedTransaction.getDescription(),
            savedTransaction.getTransactionDate());
    kafkaTemplate.send("transactions", event);

    log.info("Transaction created: {}", savedTransaction.getId());
    return mapToTransactionResponse(savedTransaction);
  }

  @Cacheable(value = "transaction", key = "#id")
  @Transactional(readOnly = true)
  public TransactionResponse getTransactionById(UUID id) {
    UUID userId = getCurrentUserId();

    Transaction transaction =
        transactionRepository
            .findById(id)
            .orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id: " + id));

    // Security check - ensure user only accesses their own transactions
    if (!transaction.getUserId().equals(userId)) {
      throw new TransactionNotFoundException("Transaction not found with id: " + id);
    }

    return mapToTransactionResponse(transaction);
  }

  @Cacheable(
      value = "transactionsList",
      key = "'user:' + @userUtility.getCurrentUserId() + ':page:' + #page + ':size:' + #size")
  @Transactional(readOnly = true)
  public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
    UUID userId = getCurrentUserId();
    log.info("Fetching transactions for user {}, page {}, size {}", userId, page, size);

    Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
    Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);

    List<TransactionResponse> transactionResponses =
        transactions.getContent().stream()
            .map(this::mapToTransactionResponse)
            .collect(Collectors.toList());

    return PageResponse.<TransactionResponse>builder()
        .content(transactionResponses)
        .pageNumber(transactions.getNumber())
        .pageSize(transactions.getSize())
        .totalElements(transactions.getTotalElements())
        .totalPages(transactions.getTotalPages())
        .last(transactions.isLast())
        .build();
  }

  @Cacheable(
      value = "transactionsByDateRange",
      key =
          "'user:' + @userUtility.getCurrentUserId() + ':start:' + #startDate + ':end:' + #endDate + ':page:' + #page + ':size:' + #size")
  @Transactional(readOnly = true)
  public PageResponse<TransactionResponse> getTransactionsByDateRange(
      LocalDate startDate, LocalDate endDate, int page, int size) {
    UUID userId = getCurrentUserId();
    log.info(
        "Fetching transactions for user {} from {} to {}, page {}, size {}",
        userId,
        startDate,
        endDate,
        page,
        size);

    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);

    Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
    Page<Transaction> transactions =
        transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end, pageable);

    List<TransactionResponse> transactionResponses =
        transactions.getContent().stream()
            .map(this::mapToTransactionResponse)
            .collect(Collectors.toList());

    return PageResponse.<TransactionResponse>builder()
        .content(transactionResponses)
        .pageNumber(transactions.getNumber())
        .pageSize(transactions.getSize())
        .totalElements(transactions.getTotalElements())
        .totalPages(transactions.getTotalPages())
        .last(transactions.isLast())
        .build();
  }

  @CircuitBreaker(name = "transactionSummary", fallbackMethod = "getTransactionSummaryFallback")
  @Cacheable(
      value = "transactionSummary",
      key = "#startDate + '-' + #endDate + '-' + @userUtility.getCurrentUserId()")
  @Transactional(readOnly = true)
  public TransactionSummaryResponse getTransactionSummary(LocalDate startDate, LocalDate endDate) {
    UUID userId = getCurrentUserId();
    log.info(
        "Calculating transaction summary for user {} from {} to {}", userId, startDate, endDate);

    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);

    List<Transaction> transactions =
        transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            userId, start, end);

    BigDecimal totalIncome = BigDecimal.ZERO;
    BigDecimal totalExpense = BigDecimal.ZERO;
    Map<String, BigDecimal> expenseByCategory = new HashMap<>();
    Map<String, BigDecimal> incomeByCategory = new HashMap<>();

    for (Transaction transaction : transactions) {
      String categoryName = transaction.getCategory().getName();
      String categoryType = transaction.getCategory().getType();
      BigDecimal amount = transaction.getAmount();

      if ("income".equals(categoryType)) {
        totalIncome = totalIncome.add(amount);
        incomeByCategory.merge(categoryName, amount, BigDecimal::add);
      } else if ("expense".equals(categoryType)) {
        totalExpense = totalExpense.add(amount);
        expenseByCategory.merge(categoryName, amount, BigDecimal::add);
      }
    }

    BigDecimal balance = totalIncome.subtract(totalExpense);

    List<TransactionResponse> recentTransactions =
        transactions.stream()
            .limit(5)
            .map(this::mapToTransactionResponse)
            .collect(Collectors.toList());

    return TransactionSummaryResponse.builder()
        .totalIncome(totalIncome)
        .totalExpense(totalExpense)
        .balance(balance)
        .expenseByCategory(expenseByCategory)
        .incomeByCategory(incomeByCategory)
        .recentTransactions(recentTransactions)
        .build();
  }

  public TransactionSummaryResponse getTransactionSummaryFallback(
      LocalDate startDate, LocalDate endDate, Throwable e) {
    log.error("Circuit breaker triggered for transaction summary: {}", e.getMessage());

    return TransactionSummaryResponse.builder()
        .totalIncome(BigDecimal.ZERO)
        .totalExpense(BigDecimal.ZERO)
        .balance(BigDecimal.ZERO)
        .expenseByCategory(Collections.emptyMap())
        .incomeByCategory(Collections.emptyMap())
        .recentTransactions(Collections.emptyList())
        .build();
  }

  @Cacheable(value = "transactionCategories")
  @Transactional(readOnly = true)
  public List<TransactionCategoryDto> getAllCategories() {
    log.debug("Fetching all transaction categories");
    return categoryRepository.findAll().stream()
        .map(this::mapToCategoryDto)
        .collect(Collectors.toList());
  }

  @CacheEvict(
      cacheNames = {
        "transactionSummary",
        "transactionsList",
        "transactionsByDateRange",
        "transaction"
      },
      key = "#id",
      allEntries = true)
  @Transactional
  public void deleteTransaction(UUID id) {
    UUID userId = getCurrentUserId();
    String userEmail = getCurrentUserEmail();

    Transaction transaction =
        transactionRepository
            .findByIdIncludingDeleted(id)
            .orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id: " + id));

    // Security check
    if (!transaction.getUserId().equals(userId)) {
      throw new TransactionNotFoundException("Transaction not found with id: " + id);
    }

    // Check if already deleted
    if (transaction.isDeleted()) {
      throw new TransactionNotFoundException("Transaction already deleted with id: " + id);
    }

    // Soft delete - Hibernate will automatically set deleted_at due to @SQLDelete
    transactionRepository.delete(transaction);

    // Publish deletion event for budget reversal
    TransactionDeletedEvent event =
        TransactionDeletedEvent.builder()
            .transactionId(transaction.getId().toString())
            .userId(userId.toString())
            .userEmail(userEmail)
            .amount(transaction.getAmount())
            .categoryId(transaction.getCategory().getId())
            .categoryName(transaction.getCategory().getName())
            .categoryType(transaction.getCategory().getType())
            .description(transaction.getDescription())
            .originalTransactionDate(transaction.getTransactionDate())
            .deletedAt(LocalDateTime.now())
            .build();

    kafkaTemplate.send("transaction-deletions", event);

    log.info("Transaction soft deleted: {}", id);
  }

  private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    return TransactionResponse.builder()
        .id(transaction.getId())
        .userId(transaction.getUserId())
        .amount(transaction.getAmount())
        .category(mapToCategoryDto(transaction.getCategory()))
        .description(transaction.getDescription())
        .receiptText(transaction.getReceiptText())
        .transactionDate(transaction.getTransactionDate())
        .createdAt(transaction.getCreatedAt())
        .updatedAt(transaction.getUpdatedAt())
        .build();
  }

  private TransactionCategoryDto mapToCategoryDto(TransactionCategory category) {
    return TransactionCategoryDto.builder()
        .id(category.getId())
        .name(category.getName())
        .type(category.getType())
        .build();
  }

  // Add a method to clear cache when transactions are created/updated
  @CacheEvict(value = "transactionSummary", allEntries = true)
  @Transactional
  public void clearTransactionSummaryCache() {
    log.debug("Clearing transaction summary cache");
  }
}
