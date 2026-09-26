package com.expensetracker.service;

import com.expensetracker.dto.AuthDtos.*;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiExceptions;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    @Test
    void register_createsUser_whenEmailNotTaken() {
        var request = new RegisterRequest("Matrixx", "matrixx@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(1L, "matrixx@example.com")).thenReturn("token123");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("token123");
        assertThat(response.userId()).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsDuplicate_whenEmailAlreadyExists() {
        var request = new RegisterRequest("Matrixx", "matrixx@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ApiExceptions.DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeeds_whenCredentialsMatch() {
        var request = new LoginRequest("matrixx@example.com", "password123");
        User user = User.builder().id(1L).email("matrixx@example.com").password("hashed").fullName("Matrixx").build();
        when(userRepository.findByEmail("matrixx@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "matrixx@example.com")).thenReturn("token123");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token123");
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordWrong() {
        var request = new LoginRequest("matrixx@example.com", "wrongpass");
        User user = User.builder().id(1L).email("matrixx@example.com").password("hashed").build();
        when(userRepository.findByEmail("matrixx@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiExceptions.InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        var request = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiExceptions.InvalidCredentialsException.class);
    }
}
