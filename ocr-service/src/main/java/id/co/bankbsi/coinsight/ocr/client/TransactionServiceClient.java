package id.co.bankbsi.coinsight.ocr.client;

import id.co.bankbsi.coinsight.ocr.dto.TransactionCreationRequest;
import id.co.bankbsi.coinsight.ocr.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "transaction-service", url = "${services.transaction-service.url}", 
             configuration = FeignClientConfig.class)
public interface TransactionServiceClient {

    @PostMapping("/api/transactions/ocr")
    TransactionResponse createTransactionFromOcr(
            @RequestHeader("Authorization") String authToken, 
            @RequestBody TransactionCreationRequest request);
}