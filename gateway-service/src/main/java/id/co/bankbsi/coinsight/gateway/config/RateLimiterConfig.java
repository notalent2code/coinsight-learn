package id.co.bankbsi.coinsight.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
// import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .cast(SecurityContext.class)
                .map(SecurityContext::getAuthentication)
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(Jwt.class)
                .map(jwt -> jwt.getClaimAsString("sub"))
                .onErrorReturn("anonymous")
                .switchIfEmpty(Mono.just("anonymous"));
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            String remoteAddress = exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return Mono.just(xForwardedFor.split(",")[0].trim());
            }
            return Mono.just(remoteAddress);
        };
    }

    // @Bean
    // public KeyResolver pathKeyResolver() {
    //     return exchange -> Mono.just(exchange.getRequest().getPath().value());
    // }

    // // Rate Limiter Beans
    // @Bean
    // @Primary
    // public RedisRateLimiter publicAuthRateLimiter() {
    //     return new RedisRateLimiter(2, 5, 1);
    // }

    // @Bean
    // public RedisRateLimiter authRateLimiter() {
    //     return new RedisRateLimiter(10, 20, 1);
    // }

    // @Bean
    // public RedisRateLimiter transactionRateLimiter() {
    //     return new RedisRateLimiter(15, 30, 1);
    // }

    // @Bean
    // public RedisRateLimiter ocrRateLimiter() {
    //     return new RedisRateLimiter(2, 5, 1);
    // }
}