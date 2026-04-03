package com.financedashboard.zorvyn.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.financedashboard.zorvyn.dto.*;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.*;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl service;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@mail.com")
                .role(RolesEnum.ANALYST)
                .status(UserStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ================= GET ALL =================

    @Test
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = service.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void getAllUsers_exception() {
        when(userRepository.findAll()).thenThrow(RuntimeException.class);

        assertThrows(UserException.class, () -> service.getAllUsers());
    }

    // ================= GET BY ID =================

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertNotNull(service.getUserById(1L));
    }

    @Test
    void getUserById_invalidInput() {
        assertThrows(UserException.class, () -> service.getUserById(0L));
    }

    @Test
    void getUserById_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> service.getUserById(1L));
    }

    @Test
    void getUserById_wrapException() {
        when(userRepository.findById(1L)).thenThrow(RuntimeException.class);

        assertThrows(UserException.class, () -> service.getUserById(1L));
    }

    // ================= GET BY EMAIL =================

    @Test
    void getUserByEmail_success() {
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertNotNull(service.getUserByEmail("test@mail.com"));
    }

    @Test
    void getUserByEmail_invalidInput() {
        assertThrows(UserException.class, () -> service.getUserByEmail(""));
    }

    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> service.getUserByEmail("x"));
    }

    @Test
    void getUserByEmail_wrapException() {
        when(userRepository.findByEmail(anyString())).thenThrow(RuntimeException.class);

        assertThrows(UserException.class, () -> service.getUserByEmail("x"));
    }

    // ================= CREATE =================

    @Test
    void createUser_success() {
        UserRequest request = UserRequest.builder()
                .name("New")
                .email("new@mail.com")
                .password("123")
                .role(RolesEnum.ANALYST)
                .status(UserStatusEnum.ACTIVE)
                .build();

        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);

        assertNotNull(service.createUser(request));
    }

    @Test
    void createUser_emailExists() {
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        UserRequest request = UserRequest.builder().email("test@mail.com").build();

        assertThrows(UserException.class, () -> service.createUser(request));
    }

    @Test
    void createUser_wrapException() {
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(UserException.class, () -> service.createUser(new UserRequest()));
    }

    // ================= UPDATE =================

    @Test
    void updateUser_success_partial() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setName("Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertNotNull(service.updateUser(1L, req));
    }

    @Test
    void updateUser_emailChange_success() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);

        assertNotNull(service.updateUser(1L, req));
    }

    @Test
    void updateUser_emailAlreadyExists() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("dup@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("dup@mail.com")).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> service.updateUser(1L, req));
    }

    @Test
    void updateUser_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> service.updateUser(1L, new UserUpdateRequest()));
    }

    @Test
    void updateUser_wrapException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(UserException.class, () -> service.updateUser(1L, new UserUpdateRequest()));
    }

    // ================= UPDATE STATUS =================

    @Test
    void updateUserStatus_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertNotNull(service.updateUserStatus(1L, UserStatusEnum.INACTIVE));
    }

    @Test
    void updateUserStatus_nullStatus() {
        assertThrows(UserException.class,
                () -> service.updateUserStatus(1L, null));
    }

    @Test
    void updateUserStatus_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserException.class,
                () -> service.updateUserStatus(1L, UserStatusEnum.ACTIVE));
    }

    @Test
    void updateUserStatus_wrapException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenThrow(RuntimeException.class);

        assertThrows(UserException.class,
                () -> service.updateUserStatus(1L, UserStatusEnum.ACTIVE));
    }

    // ================= DELETE =================

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        service.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(UserException.class, () -> service.deleteUser(1L));
    }

    @Test
    void deleteUser_wrapException() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doThrow(RuntimeException.class).when(userRepository).deleteById(1L);

        assertThrows(UserException.class, () -> service.deleteUser(1L));
    }

    // ================= EMAIL EXISTS =================

    @Test
    void emailExists_true() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.of(user));

        assertTrue(service.emailExists("x"));
    }

    @Test
    void emailExists_false() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.empty());

        assertFalse(service.emailExists("x"));
    }

    @Test
    void emailExists_blank() {
        assertFalse(service.emailExists(""));
    }

    // ================= IS USER ACTIVE =================

    @Test
    void isUserActive_true() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertTrue(service.isUserActive(1L));
    }

    @Test
    void isUserActive_false_whenInactive() {
        user.setStatus(UserStatusEnum.INACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertFalse(service.isUserActive(1L));
    }

    @Test
    void isUserActive_false_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertFalse(service.isUserActive(1L));
    }
}