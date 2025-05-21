package id.co.bankbsi.coinsight.transaction.repository;

import id.co.bankbsi.coinsight.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    Page<Transaction> findByUserIdAndTransactionDateBetween(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}