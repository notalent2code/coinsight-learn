package id.co.bankbsi.coinsight.auth.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relay-test")
public class TestController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> tokenRelayTest(
      @AuthenticationPrincipal Jwt jwt, @RequestHeader Map<String, String> headers) {
    Map<String, Object> response = new HashMap<>();
    response.put("receivedToken", jwt != null);
    response.put("username", jwt != null ? jwt.getClaim("preferred_username") : null);
    response.put("receivedHeaders", headers);
    return ResponseEntity.ok(response);
  }
}
