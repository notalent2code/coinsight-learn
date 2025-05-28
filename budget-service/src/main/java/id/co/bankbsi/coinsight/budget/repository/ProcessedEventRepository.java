package id.co.bankbsi.coinsight.budget.repository;

import id.co.bankbsi.coinsight.budget.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    
    boolean existsByEventId(String eventId);
    
    void deleteByEventId(String eventId);
}