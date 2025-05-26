// filepath: ocr-service/src/main/java/id/co/bankbsi/coinsight/ocr/client/FeignClientConfig.java
package id.co.bankbsi.coinsight.ocr.client;

import feign.Logger;
// import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}