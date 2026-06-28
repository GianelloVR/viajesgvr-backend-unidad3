package com.example.viajesgvr.services;

import com.example.viajesgvr.entities.UserEntity;
import com.example.viajesgvr.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsersShouldReturnUsers() {
        UserEntity user = validUser();

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserEntity> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("cliente@test.com", result.get(0).getEmail());

        verify(userRepository).findAll();
    }

    @Test
    void getUserByIdShouldReturnUser() {
        UserEntity user = validUser();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<UserEntity> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByEmailShouldReturnUser() {
        UserEntity user = validUser();

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(user));

        Optional<UserEntity> result = userService.getUserByEmail("cliente@test.com");

        assertTrue(result.isPresent());
        assertEquals("cliente@test.com", result.get().getEmail());

        verify(userRepository).findByEmail("cliente@test.com");
    }

    @Test
    void updateUserShouldUpdateExistingUser() {
        UserEntity existingUser = validUser();
        existingUser.setId(1L);
        existingUser.setEmail("antiguo@test.com");

        UserEntity updatedUser = validUser();
        updatedUser.setEmail("nuevo@test.com");
        updatedUser.setPhone("999999999");
        updatedUser.setNationality("Chilena");
        updatedUser.setDocumentNumber("12345678-9");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.updateUser(1L, updatedUser);

        assertEquals("nuevo@test.com", result.getEmail());
        assertEquals("999999999", result.getPhone());
        assertEquals("Chilena", result.getNationality());
        assertEquals("12345678-9", result.getDocumentNumber());

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("nuevo@test.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserDoesNotExist() {
        UserEntity updatedUser = validUser();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(1L, updatedUser));

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateUserShouldThrowExceptionWhenNewEmailAlreadyExists() {
        UserEntity existingUser = validUser();
        existingUser.setId(1L);
        existingUser.setEmail("antiguo@test.com");

        UserEntity updatedUser = validUser();
        updatedUser.setEmail("nuevo@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(1L, updatedUser));

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("nuevo@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void deactivateUserShouldSetActiveFalse() {
        UserEntity user = validUser();
        user.setId(1L);
        user.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.deactivateUser(1L);

        assertFalse(result.getActive());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUserShouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deactivateUser(1L));

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void activateUserShouldSetActiveTrue() {
        UserEntity user = validUser();
        user.setId(1L);
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.activateUser(1L);

        assertTrue(result.getActive());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void activateUserShouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.activateUser(1L));

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getOrCreateKeycloakUserShouldCreateUserWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("keycloak@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.getOrCreateKeycloakUser(
                "keycloak@test.com",
                "Usuario Keycloak",
                UserEntity.ROLE_ADMIN,
                "912345678",
                "Chilena",
                "12345678-9"
        );

        assertEquals("keycloak@test.com", result.getEmail());
        assertEquals("Usuario Keycloak", result.getFullName());
        assertEquals(UserEntity.ROLE_ADMIN, result.getRole());
        assertEquals("912345678", result.getPhone());
        assertEquals("Chilena", result.getNationality());
        assertEquals("12345678-9", result.getDocumentNumber());
        assertEquals("KEYCLOAK_USER", result.getPassword());
        assertTrue(result.getActive());

        verify(userRepository).findByEmail("keycloak@test.com");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void getOrCreateKeycloakUserShouldCreateUserWithDefaultNameAndRoleWhenDataIsMissing() {
        when(userRepository.findByEmail("keycloak@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.getOrCreateKeycloakUser(
                "keycloak@test.com",
                " ",
                " ",
                null,
                " ",
                ""
        );

        assertEquals("keycloak@test.com", result.getEmail());
        assertEquals("keycloak@test.com", result.getFullName());
        assertEquals(UserEntity.ROLE_CLIENT, result.getRole());
        assertEquals("", result.getPhone());
        assertEquals("", result.getNationality());
        assertEquals("", result.getDocumentNumber());
        assertTrue(result.getActive());

        verify(userRepository).findByEmail("keycloak@test.com");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void getOrCreateKeycloakUserShouldKeepExistingProfileDataWhenUserAlreadyExists() {
        UserEntity existingUser = validUser();
        existingUser.setFullName("Nombre Antiguo");
        existingUser.setPhone("111111111");
        existingUser.setNationality("Antigua");
        existingUser.setDocumentNumber("11111111-1");
        existingUser.setRole(UserEntity.ROLE_CLIENT);
        existingUser.setActive(true);

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.getOrCreateKeycloakUser(
                "cliente@test.com",
                "Nombre Actualizado",
                UserEntity.ROLE_ADMIN,
                "912345678",
                "Chilena",
                "12345678-9"
        );

        assertEquals("Nombre Antiguo", result.getFullName());
        assertEquals("111111111", result.getPhone());
        assertEquals("Antigua", result.getNationality());
        assertEquals("11111111-1", result.getDocumentNumber());
        assertEquals(UserEntity.ROLE_ADMIN, result.getRole());
        assertTrue(result.getActive());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    void getOrCreateKeycloakUserShouldThrowExceptionWhenUserIsInactive() {
        UserEntity existingUser = validUser();
        existingUser.setActive(false);

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getOrCreateKeycloakUser(
                        "cliente@test.com",
                        "Cliente Prueba",
                        UserEntity.ROLE_CLIENT,
                        "912345678",
                        "Chilena",
                        "12345678-9"
                ));

        assertEquals("User account is inactive", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getOrCreateKeycloakUserShouldThrowExceptionWhenEmailIsMissing() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getOrCreateKeycloakUser(
                        " ",
                        "Usuario",
                        UserEntity.ROLE_CLIENT,
                        "912345678",
                        "Chilena",
                        "12345678-9"
                ));

        assertEquals("Email is required", exception.getMessage());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldUpdateTrimmedProfileData() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setFullName(" Cliente Actualizado ");
        updatedData.setPhone(" 987654321 ");
        updatedData.setNationality(" Chilena ");
        updatedData.setDocumentNumber(" 99999999-9 ");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.updateCurrentUserProfile("cliente@test.com", updatedData);

        assertEquals("Cliente Actualizado", result.getFullName());
        assertEquals("987654321", result.getPhone());
        assertEquals("Chilena", result.getNationality());
        assertEquals("99999999-9", result.getDocumentNumber());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenEmailIsMissing() {
        UserEntity updatedData = validUser();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile(" ", updatedData));

        assertEquals("Email is required", exception.getMessage());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenUserDoesNotExist() {
        UserEntity updatedData = validUser();

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("User not found", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenUserIsInactive() {
        UserEntity existingUser = validUser();
        existingUser.setActive(false);

        UserEntity updatedData = validUser();

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("User account is inactive", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenPhoneIsMissing() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setPhone(" ");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Phone is required", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenPhoneLengthIsInvalid() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setPhone("123");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Phone must be between 8 and 20 characters", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenNationalityIsMissing() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setNationality(" ");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Nationality is required", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenNationalityLengthIsInvalid() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setNationality("CL");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Nationality must be between 3 and 80 characters", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenDocumentNumberIsMissing() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setDocumentNumber(" ");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Document number is required", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateCurrentUserProfileShouldThrowExceptionWhenDocumentNumberLengthIsInvalid() {
        UserEntity existingUser = validUser();

        UserEntity updatedData = validUser();
        updatedData.setDocumentNumber("123");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateCurrentUserProfile("cliente@test.com", updatedData));

        assertEquals("Document number must be between 5 and 30 characters", exception.getMessage());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getOrCreateKeycloakUserShouldFillEmptyExistingProfileDataFromKeycloak() {
        UserEntity existingUser = validUser();
        existingUser.setFullName(" ");
        existingUser.setPhone(null);
        existingUser.setNationality("");
        existingUser.setDocumentNumber(null);
        existingUser.setActive(true);

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.getOrCreateKeycloakUser(
                "cliente@test.com",
                " Cliente Keycloak ",
                UserEntity.ROLE_CLIENT,
                " 912345678 ",
                " Chilena ",
                " 12345678-9 "
        );

        assertEquals("Cliente Keycloak", result.getFullName());
        assertEquals("912345678", result.getPhone());
        assertEquals("Chilena", result.getNationality());
        assertEquals("12345678-9", result.getDocumentNumber());

        verify(userRepository).findByEmail("cliente@test.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUserShouldThrowExceptionWhenEmailFormatIsInvalid() {
        UserEntity existingUser = validUser();
        existingUser.setId(1L);

        UserEntity updatedData = validUser();
        updatedData.setEmail("correo-invalido");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(1L, updatedData));

        assertEquals("Invalid email format", exception.getMessage());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    private UserEntity validUser() {
        UserEntity user = new UserEntity();

        user.setId(1L);
        user.setFullName("Cliente Prueba");
        user.setEmail("cliente@test.com");
        user.setPassword("123456");
        user.setPhone("999999999");
        user.setNationality("Chilena");
        user.setDocumentNumber("12345678-9");
        user.setRole(UserEntity.ROLE_CLIENT);
        user.setActive(true);

        return user;
    }
}