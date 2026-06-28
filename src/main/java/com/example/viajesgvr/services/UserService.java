package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final String INACTIVE_USER_MESSAGE = "User account is inactive";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserEntity updateUser(Long id, UserEntity user) {
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateUserData(user);

        if (!existingUser.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setNationality(user.getNationality());
        existingUser.setDocumentNumber(user.getDocumentNumber());

        return userRepository.save(existingUser);
    }

    public UserEntity updateCurrentUserProfile(String email, UserEntity user) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateActiveUser(existingUser);
        validateProfileData(user);

        existingUser.setFullName(user.getFullName().trim());
        existingUser.setPhone(user.getPhone().trim());
        existingUser.setNationality(user.getNationality().trim());
        existingUser.setDocumentNumber(user.getDocumentNumber().trim());

        return userRepository.save(existingUser);
    }

    public UserEntity deactivateUser(Long id) {
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existingUser.setActive(false);

        return userRepository.save(existingUser);
    }

    public UserEntity activateUser(Long id) {
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existingUser.setActive(true);

        return userRepository.save(existingUser);
    }

    public UserEntity getOrCreateKeycloakUser(
            String email,
            String fullName,
            String role,
            String phone,
            String nationality,
            String documentNumber
    ) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    validateActiveUser(existingUser);

                    existingUser.setFullName(getProfileValue(existingUser.getFullName(), fullName));
                    existingUser.setPhone(getProfileValue(existingUser.getPhone(), phone));
                    existingUser.setNationality(getProfileValue(existingUser.getNationality(), nationality));
                    existingUser.setDocumentNumber(getProfileValue(existingUser.getDocumentNumber(), documentNumber));
                    existingUser.setRole(getValueOrDefault(role, UserEntity.ROLE_CLIENT));

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();

                    newUser.setEmail(email);
                    newUser.setFullName(getValueOrDefault(fullName, email));
                    newUser.setPassword("KEYCLOAK_USER");
                    newUser.setPhone(getValueOrDefault(phone, ""));
                    newUser.setNationality(getValueOrDefault(nationality, ""));
                    newUser.setDocumentNumber(getValueOrDefault(documentNumber, ""));
                    newUser.setRole(getValueOrDefault(role, UserEntity.ROLE_CLIENT));
                    newUser.setActive(true);

                    return userRepository.save(newUser);
                });
    }

    private void validateUserData(UserEntity user) {
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new RuntimeException("Full name is required");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (!isValidEmail(user.getEmail())) {
            throw new RuntimeException("Invalid email format");
        }
    }

    private void validateProfileData(UserEntity user) {
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new RuntimeException("Full name is required");
        }

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new RuntimeException("Phone is required");
        }

        if (user.getPhone().trim().length() < 8 || user.getPhone().trim().length() > 20) {
            throw new RuntimeException("Phone must be between 8 and 20 characters");
        }

        if (user.getNationality() == null || user.getNationality().isBlank()) {
            throw new RuntimeException("Nationality is required");
        }

        if (user.getNationality().trim().length() < 3 || user.getNationality().trim().length() > 80) {
            throw new RuntimeException("Nationality must be between 3 and 80 characters");
        }

        if (user.getDocumentNumber() == null || user.getDocumentNumber().isBlank()) {
            throw new RuntimeException("Document number is required");
        }

        if (user.getDocumentNumber().trim().length() < 5 || user.getDocumentNumber().trim().length() > 30) {
            throw new RuntimeException("Document number must be between 5 and 30 characters");
        }
    }

    private void validateActiveUser(UserEntity user) {
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new RuntimeException(INACTIVE_USER_MESSAGE);
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private String getValueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private String getProfileValue(String currentValue, String keycloakValue) {
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue.trim();
        }

        return getValueOrDefault(keycloakValue, "");
    }
}