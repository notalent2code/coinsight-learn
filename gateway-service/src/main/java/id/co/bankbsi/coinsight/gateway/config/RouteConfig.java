// package id.co.bankbsi.coinsight.gateway.config;

// import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
// import io.github.resilience4j.timelimiter.TimeLimiterConfig;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
// import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
// import org.springframework.cloud.gateway.route.RouteLocator;
// import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.env.Environment;

// import java.time.Duration;

// @Configuration
// public class RouteConfig {
//   private final Environment env;

//   @Autowired
//   private RedisRateLimiter publicAuthRateLimiter;
//   @Autowired
//   private RedisRateLimiter authRateLimiter;
//   @Autowired
//   private RedisRateLimiter transactionRateLimiter;
//   @Autowired
//   private RedisRateLimiter ocrRateLimiter;

//   @Autowired
//   private KeyResolver userKeyResolver;
//   @Autowired
//   private KeyResolver ipKeyResolver;

//   public RouteConfig(Environment env) {
//     this.env = env;
//   }

//   @Bean
//   public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//     return builder.routes()
//         .route("public-auth-routes", r -> r.path("/api/auth/login", "/api/auth/register")
//             .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(publicAuthRateLimiter)
//                 .setKeyResolver(ipKeyResolver)))
//             .uri(env.getProperty("services.auth-service.url", "http://localhost:8081")))
//         .route("auth-service", r -> r.path("/api/auth/**")
//             .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(authRateLimiter)
//                 .setKeyResolver(userKeyResolver)))
//             .uri(env.getProperty("services.auth-service.url", "http://localhost:8081")))
//         .route("transaction-service", r -> r.path("/api/transactions/**")
//             .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(transactionRateLimiter)
//                 .setKeyResolver(ipKeyResolver)))
//             .uri(env.getProperty("services.transaction-service.url", "http://localhost:8082")))
//         .route("ocr-service", r -> r.path("/api/ocr/**")
//             .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(ocrRateLimiter)
//                 .setKeyResolver(userKeyResolver)))
//             .uri(env.getProperty("services.ocr-service.url", "http://localhost:8083")))
//         .build();
//   }

//   @Bean
//   public CircuitBreakerConfig circuitBreakerConfig() {
//     return CircuitBreakerConfig.custom()
//         .failureRateThreshold(50)
//         .waitDurationInOpenState(Duration.ofMillis(30000))
//         .slidingWindowSize(10)
//         .permittedNumberOfCallsInHalfOpenState(5)
//         .build();
//   }

//   @Bean
//   public TimeLimiterConfig timeLimiterConfig() {
//     return TimeLimiterConfig.custom()
//         .timeoutDuration(Duration.ofSeconds(4))
//         .build();
//   }
// }
