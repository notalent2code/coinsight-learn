package id.co.bankbsi.coinsight.transaction.service;

import id.co.bankbsi.coinsight.transaction.dto.*;
import id.co.bankbsi.coinsight.transaction.event.TransactionCreatedEvent;
import id.co.bankbsi.coinsight.transaction.exception.CategoryNotFoundException;
import id.co.bankbsi.coinsight.transaction.exception.TransactionNotFoundException;
import id.co.bankbsi.coinsight.transaction.model.Transaction;
import id.co.bankbsi.coinsight.transaction.model.TransactionCategory;
import id.co.bankbsi.coinsight.transaction.repository.TransactionCategoryRepository;
import id.co.bankbsi.coinsight.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UUID getCurrentUserId() {
        JwtAuthenticationToken authentication = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        UUID userId = getCurrentUserId();
        TransactionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));
        
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(request.getAmount())
                .category(category)
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now())
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Publish event to Kafka
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTransaction.getId().toString(),
                userId.toString(),
                savedTransaction.getAmount(),
                category.getName(),
                category.getType(),
                savedTransaction.getDescription(),
                savedTransaction.getTransactionDate()
        );
        kafkaTemplate.send("transactions", event);
        
        log.info("Transaction created: {}", savedTransaction.getId());
        return mapToTransactionResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse createTransactionFromOCR(UUID userId, OCRTransactionRequest request) {
        TransactionCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));
        
        // In a real application, you would parse the receipt text to extract amount, date, etc.
        // For simplicity, we'll just create a transaction with dummy data here
        BigDecimal amount = new BigDecimal("0.00");
        String description = "OCR Transaction";
        LocalDateTime transactionDate = LocalDateTime.now();
        
        // Simple regex to extract amount (this is a very basic example)
        String receiptText = request.getReceiptText();
        if (receiptText != null) {
            String[] lines = receiptText.split("\\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("total")) {
                    // Try to extract a number
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        part = part.replaceAll("[^0-9.]", "");
                        try {
                            amount = new BigDecimal(part);
                            break;
                        } catch (NumberFormatException ignored) {
                            // Not a valid number, continue
                        }
                    }
                }
            }
            description = "OCR Transaction from receipt";
        }
        
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amount(amount)
                .category(category)
                .description(description)
                .receiptText(receiptText)
                .transactionDate(transactionDate)
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Publish event to Kafka
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                savedTransaction.getId().toString(),
                userId.toString(),
                savedTransaction.getAmount(),
                category.getName(),
                category.getType(),
                savedTransaction.getDescription(),
                savedTransaction.getTransactionDate()
        );
        kafkaTemplate.send("transactions", event);
        
        log.info("Transaction created from OCR: {}", savedTransaction.getId());
        return mapToTransactionResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        UUID userId = getCurrentUserId();
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
        
        // Security check - ensure user only accesses their own transactions
        if (!transaction.getUserId().equals(userId)) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }
        
        return mapToTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(int page, int size) {
        UUID userId = getCurrentUserId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);
        
        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
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

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        UUID userId = getCurrentUserId();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, start, end, pageable);
        
        List<TransactionResponse> transactionResponses = transactions.getContent().stream()
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
    @Transactional(readOnly = true)
    public TransactionSummaryResponse getTransactionSummary(LocalDate startDate, LocalDate endDate) {
        UUID userId = getCurrentUserId();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, start, end);
        
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
        
        List<TransactionResponse> recentTransactions = transactions.stream()
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

    @Transactional(readOnly = true)
    public List<TransactionCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTransaction(UUID id) {
        UUID userId = getCurrentUserId();
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + id));
        
        // Security check - ensure user only deletes their own transactions
        if (!transaction.getUserId().equals(userId)) {
            throw new TransactionNotFoundException("Transaction not found with id: " + id);
        }
        
        transactionRepository.delete(transaction);
        log.info("Transaction deleted: {}", id);
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
}