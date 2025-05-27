package id.co.bankbsi.coinsight.transaction.repository;

import id.co.bankbsi.coinsight.transaction.model.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
  Page<Transaction> findByUserId(UUID userId, Pageable pageable);

  Page<Transaction> findByUserIdAndTransactionDateBetween(
      UUID userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

  List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
      UUID userId, LocalDateTime startDate, LocalDateTime endDate);

  // Admin-only method for auditing (optional)
  @Query("SELECT t FROM Transaction t WHERE t.deletedAt IS NOT NULL")
  Page<Transaction> findAllDeletedForAudit(Pageable pageable);
}
