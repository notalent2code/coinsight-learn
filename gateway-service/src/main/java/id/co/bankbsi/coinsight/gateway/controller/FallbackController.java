package id.co.bankbsi.coinsight.gateway.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @GetMapping("/auth")
  public ResponseEntity<Map<String, String>> authServiceFallback() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "Auth Service is currently unavailable. Please try again later.");
    response.put("status", "error");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/transactions")
  public ResponseEntity<Map<String, String>> transactionServiceFallback() {
    Map<String, String> response = new HashMap<>();
    response.put(
        "message", "Transaction Service is currently unavailable. Please try again later.");
    response.put("status", "error");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @RequestMapping(value = "/ocr", method = { RequestMethod.GET, RequestMethod.POST })
  public ResponseEntity<Map<String, String>> ocrProcessFallback() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "OCR processing is currently unavailable. Please try again later.");
    response.put("status", "error");
    response.put("timestamp", LocalDateTime.now().toString());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/budget")
  public ResponseEntity<Map<String, String>> budgetServiceFallback() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "Budget Service is currently unavailable. Please try again later.");
    response.put("status", "error");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/notification")
  public ResponseEntity<Map<String, String>> notificationServiceFallback() {
    Map<String, String> response = new HashMap<>();
    response.put(
        "message", "Notification Service is currently unavailable. Please try again later.");
    response.put("status", "error");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }
}
