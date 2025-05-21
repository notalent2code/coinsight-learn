// filepath: transaction-service/src/main/java/id/co/bankbsi/coinsight/transaction/model/TransactionCategory.java
package id.co.bankbsi.coinsight.transaction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "name", unique = true)
    private String name;
    
    @Column(name = "type")
    private String type;
}