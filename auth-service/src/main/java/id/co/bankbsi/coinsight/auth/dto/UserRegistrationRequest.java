// filepath:
// auth-service/src/main/java/id/co/bankbsi/coinsight/auth/dto/UserRegistrationRequest.java
package id.co.bankbsi.coinsight.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
  private String email;
  private String password;
  private String fullName;
}
