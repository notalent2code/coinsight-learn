// src/main/java/id/co/bankbsi/coinsight/ocr/dto/OcrRequest.java
package id.co.bankbsi.coinsight.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequest {
  private String imageUrl;
  private String imagePath;
  private Integer categoryId;
}
