package id.co.bankbsi.coinsight.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryDto {
  private Integer id;
  private String name;
  private String type;
}
