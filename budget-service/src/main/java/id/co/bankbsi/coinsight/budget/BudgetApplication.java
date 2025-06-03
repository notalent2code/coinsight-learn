package id.co.bankbsi.coinsight.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
// @RefreshScope
public class BudgetApplication {
  public static void main(String[] args) {
    SpringApplication.run(BudgetApplication.class, args);
  }
}
