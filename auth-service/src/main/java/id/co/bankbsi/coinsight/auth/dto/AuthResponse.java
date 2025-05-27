package id.co.bankbsi.coinsight.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private long expiresIn;
  private UserResponse user;
}
