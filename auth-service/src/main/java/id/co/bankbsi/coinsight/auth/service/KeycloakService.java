// filepath: auth-service/src/main/java/id/co/bankbsi/coinsight/auth/service/KeycloakService.java
package id.co.bankbsi.coinsight.auth.service;

import id.co.bankbsi.coinsight.auth.dto.AuthResponse;
import id.co.bankbsi.coinsight.auth.dto.UserRegistrationRequest;
import id.co.bankbsi.coinsight.auth.dto.UserResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

  private final Keycloak keycloakAdmin;
  private final UserService userService;

  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.resource}")
  private String clientId;

  @Value("${keycloak.credentials.secret}")
  private String clientSecret;

  @Value("${keycloak.auth-server-url}")
  private String authServerUrl;

  public String createKeycloakUser(UserRegistrationRequest request) {
    System.out.printf(
        "realm: %s, clientId: %s, clientSecret: %s, authServerUrl: %s%n",
        realm, clientId, clientSecret, authServerUrl);

    UserRepresentation user = new UserRepresentation();
    user.setEnabled(true);
    user.setUsername(request.getEmail());
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFullName().split(" ")[0]);
    user.setLastName(
        request.getFullName().contains(" ")
            ? request.getFullName().substring(request.getFullName().indexOf(" ") + 1)
            : "");
    user.setEmailVerified(true);

    CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
    credentialRepresentation.setTemporary(false);
    credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
    credentialRepresentation.setValue(request.getPassword());

    user.setCredentials(Collections.singletonList(credentialRepresentation));

    RealmResource realmResource = keycloakAdmin.realm(realm);
    UsersResource usersResource = realmResource.users();

    try (Response response = usersResource.create(user)) {
      if (response.getStatus() == 201) {
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        log.info("Created keycloak user with id: {}", userId);
        return userId;
      } else {
        log.error("Failed to create keycloak user. Status: {}", response.getStatus());
        throw new BadRequestException("Failed to create user in Keycloak");
      }
    }
  }

  public AuthResponse authenticate(String username, String password) {
    try {
      // Set up credentials for the Keycloak client
      Map<String, Object> clientCredentials = new HashMap<>();
      clientCredentials.put("secret", clientSecret);

      // Create Keycloak configuration
      Configuration configuration =
          new Configuration(authServerUrl, realm, clientId, clientCredentials, null);

      // Create AuthzClient for token exchange
      AuthzClient authzClient = AuthzClient.create(configuration);

      // Try to obtain access token with username/password
      log.info("Attempting authentication for user: {}", username);
      AccessTokenResponse response = authzClient.obtainAccessToken(username, password);

      if (response == null) {
        log.error("Failed to obtain access token for user: {}", username);
        throw new RuntimeException("Authentication failed");
      }

      log.info("Authentication successful for user: {}", username);

      // Get user details from your database
      UserResponse user = userService.getUserByEmail(username);

      // Build and return authentication response
      return AuthResponse.builder()
          .accessToken(response.getToken())
          .refreshToken(response.getRefreshToken())
          .expiresIn(response.getExpiresIn())
          .tokenType("Bearer")
          .user(user)
          .build();
    } catch (Exception e) {
      log.error("Authentication failed: {}", e.getMessage());
      throw new RuntimeException("Authentication failed: " + e.getMessage());
    }
  }
}
