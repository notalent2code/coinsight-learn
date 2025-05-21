package id.co.bankbsi.coinsight.ocr.controller;

import id.co.bankbsi.coinsight.ocr.dto.OcrRequest;
import id.co.bankbsi.coinsight.ocr.dto.OcrResponse;
import id.co.bankbsi.coinsight.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/process")
    public ResponseEntity<OcrResponse> processReceipt(
            @RequestBody OcrRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        OcrResponse response = ocrService.processReceipt(request, authorizationHeader);
        return ResponseEntity.ok(response);
    }
}