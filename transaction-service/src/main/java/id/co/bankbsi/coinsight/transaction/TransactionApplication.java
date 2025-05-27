// filepath:
// transaction-service/src/main/java/id/co/bankbsi/coinsight/transaction/TransactionApplication.java
package id.co.bankbsi.coinsight.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TransactionApplication {
  public static void main(String[] args) {
    SpringApplication.run(TransactionApplication.class, args);
  }
}
