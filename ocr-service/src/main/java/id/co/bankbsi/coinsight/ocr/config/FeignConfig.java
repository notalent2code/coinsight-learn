package id.co.bankbsi.coinsight.ocr.config;

import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

  @Value("${http.client.timeout.connection:10000}")
  private int connectionTimeout;

  @Value("${http.client.timeout.read:30000}")
  private int readTimeout;

  @Bean
  public Request.Options feignOptions() {
    return new Request.Options(
        connectionTimeout, TimeUnit.MILLISECONDS,
        readTimeout, TimeUnit.MILLISECONDS,
        true  // followRedirects
    );
  }

  @Bean
  public ErrorDecoder errorDecoder() {
    return new FeignErrorDecoder();
  }

  public static class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, feign.Response response) {
      // Log the error
      return new RuntimeException("Error calling service: " + response.reason());
    }
  }
}
