package com.example.viajesgvr.controllers;

import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {

    private static final String INACTIVE_USER_MESSAGE = "User account is inactive";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaimAsString("email");
            String fullName = buildFullName(jwt);

            String phone = jwt.getClaimAsString("phone");
            String nationality = jwt.getClaimAsString("nationality");
            String documentNumber = jwt.getClaimAsString("documentNumber");

            String role = UserEntity.ROLE_CLIENT;

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                if (roles.contains(UserEntity.ROLE_ADMIN)) {
                    role = UserEntity.ROLE_ADMIN;
                } else if (roles.contains(UserEntity.ROLE_CLIENT)) {
                    role = UserEntity.ROLE_CLIENT;
                }
            }

            UserEntity user = userService.getOrCreateKeycloakUser(
                    email,
                    fullName,
                    role,
                    phone,
                    nationality,
                    documentNumber
            );

            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return handleUserException(e);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UserEntity user
    ) {
        try {
            String email = jwt.getClaimAsString("email");

            UserEntity updatedUser = userService.updateCurrentUserProfile(email, user);

            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return handleUserException(e);
        }
    }

    private ResponseEntity<?> handleUserException(RuntimeException e) {
        if (e.getMessage().equals(INACTIVE_USER_MESSAGE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }

        if (e.getMessage().equals("User not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }

        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private String buildFullName(Jwt jwt) {
        String fullName = jwt.getClaimAsString("name");

        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }

        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        String generatedFullName = (
                (firstName != null ? firstName : "") + " " +
                        (lastName != null ? lastName : "")
        ).trim();

        if (!generatedFullName.isBlank()) {
            return generatedFullName;
        }

        return jwt.getClaimAsString("preferred_username");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserEntity> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserEntity user) {
        try {
            UserEntity updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return handleUserException(e);
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserEntity> deactivateUser(@PathVariable Long id) {
        try {
            UserEntity deactivatedUser = userService.deactivateUser(id);
            return ResponseEntity.ok(deactivatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserEntity> activateUser(@PathVariable Long id) {
        try {
            UserEntity activatedUser = userService.activateUser(id);
            return ResponseEntity.ok(activatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}