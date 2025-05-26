package id.co.bankbsi.coinsight.ocr.controller;

import id.co.bankbsi.coinsight.ocr.dto.OcrRequest;
import id.co.bankbsi.coinsight.ocr.dto.OcrResponse;
import id.co.bankbsi.coinsight.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Slf4j
public class OcrController {

    private final OcrService ocrService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OcrResponse processReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestHeader("Authorization") String authToken) {
        
        try {
            log.info("Received OCR request with file {} and category {}", file.getOriginalFilename(), categoryId);
            
            // Save file temporarily
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path tempFile = Files.createTempFile("receipt_", filename);
            file.transferTo(tempFile.toFile());
            
            // Process the OCR request with the local file path
            OcrRequest request = new OcrRequest();
            request.setImagePath(tempFile.toString());
            request.setCategoryId(categoryId);
            
            OcrResponse response = ocrService.processReceiptFromFile(request, authToken);
            
            // Delete the temp file
            Files.deleteIfExists(tempFile);
            
            return response;
        } catch (Exception e) {
            log.error("Error processing OCR request", e);
            throw new RuntimeException("Failed to process OCR request: " + e.getMessage());
        }
    }
}