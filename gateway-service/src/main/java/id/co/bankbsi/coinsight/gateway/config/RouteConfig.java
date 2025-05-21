// /src/main/java/id/co/bankbsi/coinsight/gateway/config/RouteConfig.java
package id.co.bankbsi.coinsight.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;

@Configuration
public class RouteConfig {
    private final Environment env;
    
    public RouteConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri(env.getProperty("services.auth-service.url", "http://localhost:8081"))
                )
                .route("transaction-service", r -> r.path("/api/transactions/**")
                        .uri(env.getProperty("services.transaction-service.url", "http://localhost:8082"))
                )
                .route("ocr-service", r -> r.path("/api/ocr/**")
                        .uri(env.getProperty("services.ocr-service.url", "http://localhost:8083"))
                )
                .build();
    }

//     @Bean
//     public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//         return builder.routes()
//                 .route("auth-service", r -> r.path("/api/auth/**")
//                         .filters(f -> f.circuitBreaker(c -> c.setName("authServiceCircuitBreaker")
//                                 .setFallbackUri("forward:/fallback/auth")))
//                         .uri("${services.auth-service.url:http://localhost:8081}")
//                 )
//                 .route("transaction-service", r -> r.path("/api/transactions/**")
//                         .filters(f -> f.circuitBreaker(c -> c.setName("transactionServiceCircuitBreaker")
//                                 .setFallbackUri("forward:/fallback/transactions")))
//                         .uri("${services.transaction-service.url:http://localhost:8082}")
//                 )
//                 .route("ocr-service", r -> r.path("/api/ocr/**")
//                         .filters(f -> f.circuitBreaker(c -> c.setName("ocrServiceCircuitBreaker")
//                                 .setFallbackUri("forward:/fallback/ocr")))
//                         .uri("${services.ocr-service.url:http://localhost:8083}")
//                 )
//                 .build();
//     }
    
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(30000))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .build();
    }
    
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(4))
                .build();
    }
}