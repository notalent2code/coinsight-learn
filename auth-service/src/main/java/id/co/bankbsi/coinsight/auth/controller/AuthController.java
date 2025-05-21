package id.co.bankbsi.coinsight.auth.controller;

import id.co.bankbsi.coinsight.auth.dto.AuthResponse;
import id.co.bankbsi.coinsight.auth.dto.LoginRequest;
import id.co.bankbsi.coinsight.auth.dto.UserRegistrationRequest;
import id.co.bankbsi.coinsight.auth.dto.UserResponse;
import id.co.bankbsi.coinsight.auth.service.KeycloakService;
import id.co.bankbsi.coinsight.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        String keycloakId = this.keycloakService.createKeycloakUser(request);
        UserResponse userResponse = this.userService.registerUser(request, keycloakId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = this.keycloakService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
}