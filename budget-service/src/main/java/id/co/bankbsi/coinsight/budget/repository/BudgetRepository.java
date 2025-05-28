package id.co.bankbsi.coinsight.budget.repository;

import id.co.bankbsi.coinsight.budget.model.Budget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Page<Budget> findByUserId(UUID userId, Pageable pageable);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findByUserIdAndIsActiveTrue(UUID userId);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true "
            + "AND b.startDate <= :currentDate AND b.endDate >= :currentDate")
    Page<Budget> findActiveBudgetsByUserId(
            @Param("userId") UUID userId, @Param("currentDate") LocalDate currentDate, Pageable pageable);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true "
            + "AND b.startDate <= :currentDate AND b.endDate >= :currentDate")
    List<Budget> findActiveBudgetsByUserId(
            @Param("userId") UUID userId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.isActive = true "
            + "AND b.startDate <= CURRENT_DATE AND b.endDate >= CURRENT_DATE "
            + "AND b.categoryId = :categoryId")
    List<Budget> findActiveBudgetsByUserIdAndCategoryId(
            @Param("userId") UUID userId, @Param("categoryId") Integer categoryId);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.categoryId = :categoryId AND b.isActive = true")
    Optional<Budget> findActiveByUserIdAndCategoryId(
            @Param("userId") UUID userId,
            @Param("categoryId") Integer categoryId);

    boolean existsByUserIdAndCategoryIdAndIsActiveTrue(UUID userId, Integer categoryId);
}
