package id.co.bankbsi.coinsight.ocr.service;

import id.co.bankbsi.coinsight.ocr.dto.CategoryDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

  /**
   * Determine the category based on the receipt text using keyword matching
   *
   * @return a map with categoryId and confidence score (0-100)
   */
  public Map<String, Object> determineCategory(String receiptText, List<CategoryDto> categories) {
    // Map of category names to relevant keywords (with weights)
    Map<String, Map<String, Integer>> categoryKeywords = new HashMap<>();

    // Transfer category keywords and weights
    Map<String, Integer> transferKeywords = new HashMap<>();
    transferKeywords.put("transfer", 10);
    transferKeywords.put("kirim uang", 10);
    transferKeywords.put("send money", 10);
    transferKeywords.put("bank", 5);
    transferKeywords.put("atm", 5);
    transferKeywords.put("mobile banking", 8);
    categoryKeywords.put("transfer", transferKeywords);

    // Topup category keywords and weights
    Map<String, Integer> topupKeywords = new HashMap<>();
    topupKeywords.put("topup", 10);
    topupKeywords.put("top up", 10);
    topupKeywords.put("top-up", 10);
    topupKeywords.put("reload", 8);
    topupKeywords.put("pulsa", 9);
    topupKeywords.put("data", 6);
    topupKeywords.put("e-wallet", 9);
    topupKeywords.put("gopay", 9);
    topupKeywords.put("ovo", 9);
    topupKeywords.put("dana", 9);
    topupKeywords.put("linkaja", 9);
    categoryKeywords.put("topup", topupKeywords);

    // Bills category keywords and weights
    Map<String, Integer> billsKeywords = new HashMap<>();
    billsKeywords.put("bill", 10);
    billsKeywords.put("tagihan", 10);
    billsKeywords.put("listrik", 9);
    billsKeywords.put("air", 7);
    billsKeywords.put("pln", 9);
    billsKeywords.put("pdam", 9);
    billsKeywords.put("internet", 8);
    billsKeywords.put("wifi", 8);
    billsKeywords.put("gas", 8);
    billsKeywords.put("telepon", 8);
    billsKeywords.put("phone", 7);
    categoryKeywords.put("bills", billsKeywords);

    // Needs category keywords and weights
    Map<String, Integer> needsKeywords = new HashMap<>();
    needsKeywords.put("grocery", 10);
    needsKeywords.put("groceries", 10);
    needsKeywords.put("market", 8);
    needsKeywords.put("supermarket", 9);
    needsKeywords.put("food", 8);
    needsKeywords.put("makan", 9);
    needsKeywords.put("makanan", 9);
    needsKeywords.put("restaurant", 8);
    needsKeywords.put("restoran", 8);
    needsKeywords.put("warung", 8);
    needsKeywords.put("cafe", 7);
    categoryKeywords.put("needs", needsKeywords);

    // Transport category keywords and weights
    Map<String, Integer> transportKeywords = new HashMap<>();
    transportKeywords.put("transport", 10);
    transportKeywords.put("transportasi", 10);
    transportKeywords.put("gojek", 9);
    transportKeywords.put("grab", 9);
    transportKeywords.put("taxi", 9);
    transportKeywords.put("taksi", 9);
    transportKeywords.put("uber", 8);
    transportKeywords.put("bensin", 9);
    transportKeywords.put("gas", 6);
    transportKeywords.put("fuel", 8);
    transportKeywords.put("toll", 8);
    transportKeywords.put("tol", 8);
    transportKeywords.put("parkir", 8);
    transportKeywords.put("parking", 8);
    categoryKeywords.put("transport", transportKeywords);

    // Shopping category keywords and weights
    Map<String, Integer> shoppingKeywords = new HashMap<>();
    shoppingKeywords.put("shopping", 10);
    shoppingKeywords.put("belanja", 10);
    shoppingKeywords.put("mall", 9);
    shoppingKeywords.put("online", 7);
    shoppingKeywords.put("tokopedia", 9);
    shoppingKeywords.put("shopee", 9);
    shoppingKeywords.put("lazada", 9);
    shoppingKeywords.put("baju", 8);
    shoppingKeywords.put("clothing", 8);
    shoppingKeywords.put("shoes", 8);
    shoppingKeywords.put("sepatu", 8);
    categoryKeywords.put("shopping", shoppingKeywords);

    String lowerCaseText = receiptText.toLowerCase();
    Map<String, Integer> categoryScores = new HashMap<>();

    // Calculate scores for each category
    for (CategoryDto category : categories) {
      if (categoryKeywords.containsKey(category.getName())) {
        Map<String, Integer> keywords = categoryKeywords.get(category.getName());
        int score = 0;
        int maxPossibleScore = 0;

        // Count matched keywords and their weights
        for (Map.Entry<String, Integer> entry : keywords.entrySet()) {
          String keyword = entry.getKey();
          Integer weight = entry.getValue();
          maxPossibleScore += weight;

          if (lowerCaseText.contains(keyword.toLowerCase())) {
            score += weight;
            log.debug(
                "Found keyword '{}' in text, adding {} points to {} category",
                keyword,
                weight,
                category.getName());
          }
        }

        // Calculate confidence as percentage of max possible score
        if (maxPossibleScore > 0) {
          int confidence = (score * 100) / maxPossibleScore;
          categoryScores.put(category.getName(), confidence);
        }
      }
    }

    // Find the highest scoring category
    String bestCategory = null;
    int highestConfidence = 0;
    int categoryId = 7; // Default to "others"

    for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
      if (entry.getValue() > highestConfidence) {
        highestConfidence = entry.getValue();
        bestCategory = entry.getKey();

        // Find the category ID
        for (CategoryDto category : categories) {
          if (category.getName().equals(bestCategory)) {
            categoryId = category.getId();
            break;
          }
        }
      }
    }

    // If confidence is too low (below 30%), default to "others"
    if (highestConfidence < 30) {
      categoryId = 7; // "others"
      highestConfidence = 0;
    }

    log.info(
        "Keyword matching determined category ID {} with confidence {}%",
        categoryId, highestConfidence);

    Map<String, Object> result = new HashMap<>();
    result.put("categoryId", categoryId);
    result.put("confidence", highestConfidence);

    return result;
  }
}
