package id.co.bankbsi.coinsight.gateway.controller;

// import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/public")
    public Mono<Map<String, Object>> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "success");
        return Mono.just(response);
    }

    @GetMapping("/authenticated")
    public Mono<Map<String, Object>> authenticatedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "You are authenticated!");
        response.put("name", jwt.getClaim("name"));
        response.put("email", jwt.getClaim("email"));
        response.put("subject", jwt.getSubject());
        response.put("tokenExpiresAt", jwt.getExpiresAt());
        return Mono.just(response);
    }

    @GetMapping("/token-info")
    public Mono<Map<String, Object>> tokenInfo(JwtAuthenticationToken authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("principal", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("tokenAttributes", authentication.getTokenAttributes());
        return Mono.just(response);
    }
    
    @GetMapping("/headers")
    public Mono<Map<String, Object>> headers(@RequestHeader Map<String, String> headers) {
        Map<String, Object> response = new HashMap<>();
        response.put("headers", headers);
        return Mono.just(response);
    }
    
    @GetMapping("/token-relay-test")
    public Mono<Map<String, Object>> tokenRelayTest() {
        // This endpoint should be called with TokenRelay filter
        // The token will be automatically forwarded to downstream services
        Map<String, Object> response = new HashMap<>();
        response.put("message", "If you see this, token relay is working");
        response.put("info", "Check the headers in your browser dev tools to verify");
        return Mono.just(response);
    }
}