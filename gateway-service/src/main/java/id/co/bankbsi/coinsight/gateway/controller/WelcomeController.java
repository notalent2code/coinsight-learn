package id.co.bankbsi.coinsight.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {
  @GetMapping("/")
  public Mono<Map<String, Object>> welcome() {
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Welcome to Coinsight API Gateway");
    response.put("status", "UP");
    response.put("endpoints", new String[]{"/api/auth", "/api/transactions", "/api/ocr"});
    
    return Mono.just(response);
  }
}