package id.co.bankbsi.coinsight.ocr.client;

import id.co.bankbsi.coinsight.ocr.dto.CategoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "category-client", url = "${services.transaction-service.url}")
public interface CategoryClient {

    @GetMapping("/api/transactions/categories")
    List<CategoryDto> getCategories(@RequestHeader("Authorization") String authToken);
}