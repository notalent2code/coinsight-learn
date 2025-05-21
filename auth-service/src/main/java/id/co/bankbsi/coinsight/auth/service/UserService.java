package id.co.bankbsi.coinsight.auth.service;

import id.co.bankbsi.coinsight.auth.dto.UserRegistrationRequest;
import id.co.bankbsi.coinsight.auth.dto.UserResponse;
import id.co.bankbsi.coinsight.auth.exception.EmailAlreadyExistsException;
import id.co.bankbsi.coinsight.auth.exception.UserNotFoundException;
import id.co.bankbsi.coinsight.auth.model.User;
import id.co.bankbsi.coinsight.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request, String keycloakId) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }
        
        User user = User.builder()
                .id(UUID.randomUUID())
                .keycloakId(keycloakId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        
        return mapToUserResponse(savedUser);
    }
    
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user);
    }
    
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}