package id.co.bankbsi.coinsight.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
        response.put("message", "Transaction Service is currently unavailable. Please try again later.");
        response.put("status", "error");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/ocr")
    public ResponseEntity<Map<String, String>> ocrServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "OCR Service is currently unavailable. Please try again later.");
        response.put("status", "error");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}