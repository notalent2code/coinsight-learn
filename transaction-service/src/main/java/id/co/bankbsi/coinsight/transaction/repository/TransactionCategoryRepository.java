package id.co.bankbsi.coinsight.transaction.repository;

import id.co.bankbsi.coinsight.transaction.model.TransactionCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Integer> {
  Optional<TransactionCategory> findByName(String name);
}
