package ru.netology.filestorage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import ru.netology.filestorage.dto.AuthRequest;
import ru.netology.filestorage.dto.AuthResponse;
import ru.netology.filestorage.entity.Token;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.InvalidCredentialsException;
import ru.netology.filestorage.exception.UserNotFoundException;
import ru.netology.filestorage.repository.TokenRepository;
import ru.netology.filestorage.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_Successful() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        User user = new User("test@example.com", "encodedPassword");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(userService.checkPassword("password", "encodedPassword")).thenReturn(true);
        when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getAuthToken());
        verify(userRepository).findByUsername("test@example.com");
        verify(userService).checkPassword("password", "encodedPassword");
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void login_UserNotFound() {
        AuthRequest request = new AuthRequest("nonexistent@example.com", "password");
        when(userRepository.findByUsername("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(request));
        verify(userRepository).findByUsername("nonexistent@example.com");
        verify(userService, never()).checkPassword(any(), any());
    }

    @Test
    void login_InvalidPassword() {
        AuthRequest request = new AuthRequest("test@example.com", "wrongpassword");
        User user = new User("test@example.com", "encodedPassword");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user));
        when(userService.checkPassword("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(userRepository).findByUsername("test@example.com");
        verify(userService).checkPassword("wrongpassword", "encodedPassword");
    }

    @Test
    void validateToken_ValidToken() {
        String token = "valid-token";
        User user = new User("test@example.com", "password");
        Token tokenEntity = new Token(token, user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.of(tokenEntity));

        boolean isValid = authService.validateToken(token);

        assertTrue(isValid);
        verify(tokenRepository).findByTokenAndActiveTrue(token);
    }

    @Test
    void validateToken_ExpiredToken() {
        String token = "expired-token";
        User user = new User("test@example.com", "password");
        Token tokenEntity = new Token(token, user, LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.of(tokenEntity));

        boolean isValid = authService.validateToken(token);

        assertFalse(isValid);
        verify(tokenRepository).findByTokenAndActiveTrue(token);
    }

    @Test
    void validateToken_InvalidToken() {
        String token = "invalid-token";
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.empty());

        boolean isValid = authService.validateToken(token);

        assertFalse(isValid);
        verify(tokenRepository).findByTokenAndActiveTrue(token);
    }

    @Test
    void validateToken_NullToken() {
        boolean isValid = authService.validateToken(null);

        assertFalse(isValid);
        verify(tokenRepository, never()).findByTokenAndActiveTrue(any());
    }

    @Test
    void getAuthentication_ValidToken() {
        String token = "valid-token";
        User user = new User("test@example.com", "password");
        Token tokenEntity = new Token(token, user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.of(tokenEntity));

        Authentication authentication = authService.getAuthentication(token);

        assertNotNull(authentication);
        assertEquals("test@example.com", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("USER")));
    }

    @Test
    void getAuthentication_InvalidToken() {
        String token = "invalid-token";
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.empty());

        Authentication authentication = authService.getAuthentication(token);

        assertNull(authentication);
    }

    @Test
    void logout_Successful() {
        String token = "valid-token";
        User user = new User("test@example.com", "password");
        Token tokenEntity = new Token(token, user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.of(tokenEntity));

        authService.logout(token);

        assertFalse(tokenEntity.isActive());
        verify(tokenRepository).save(tokenEntity);
    }

    @Test
    void logout_TokenNotFound() {
        String token = "nonexistent-token";
        when(tokenRepository.findByTokenAndActiveTrue(token)).thenReturn(Optional.empty());

        authService.logout(token);

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void logout_NullToken() {
        authService.logout(null);

        verify(tokenRepository, never()).findByTokenAndActiveTrue(any());
        verify(tokenRepository, never()).save(any());
    }
}