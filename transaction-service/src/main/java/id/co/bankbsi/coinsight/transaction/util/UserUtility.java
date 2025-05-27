package id.co.bankbsi.coinsight.transaction.util;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserUtility {

  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
      JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
      return UUID.fromString(jwtAuth.getName());
    }
    throw new IllegalStateException("User not authenticated or invalid authentication type");
  }
}
