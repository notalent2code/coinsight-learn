// filepath: ocr-service/src/main/java/id/co/bankbsi/coinsight/ocr/OcrApplication.java
package id.co.bankbsi.coinsight.ocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OcrApplication {

  static {
    // Suppress Netty version mismatch warnings
    System.setProperty("io.netty.tryReflectionSetAccessible", "true");
    System.setProperty("azure.sdk.suppress-netty-version-mismatch-warning", "true");
  }

  public static void main(String[] args) {
    SpringApplication.run(OcrApplication.class, args);
  }
}
