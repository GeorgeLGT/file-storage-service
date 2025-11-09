package ru.netology.filestorage.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import ru.netology.filestorage.dto.AuthRequest;
import ru.netology.filestorage.dto.AuthResponse;
import ru.netology.filestorage.entity.Token;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.InvalidCredentialsException;
import ru.netology.filestorage.exception.UserNotFoundException;
import ru.netology.filestorage.repository.TokenRepository;
import ru.netology.filestorage.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final UserService userService;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository, UserService userService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getLogin())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!userService.checkPassword(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = generateToken();
        Token tokenEntity = new Token(token, user, LocalDateTime.now().plusDays(1));
        tokenRepository.save(tokenEntity);

        return new AuthResponse(token);
    }

    public void logout(String token) {
        tokenRepository.findByTokenAndActiveTrue(token).ifPresent(tokenEntity -> {
            tokenEntity.setActive(false);
            tokenRepository.save(tokenEntity);
        });
    }

    public boolean validateToken(String token) {
        return tokenRepository.findByTokenAndActiveTrue(token)
                .map(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public Authentication getAuthentication(String token) {
        return tokenRepository.findByTokenAndActiveTrue(token)
                .map(t -> new UsernamePasswordAuthenticationToken(
                        t.getUser().getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("USER"))
                ))
                .orElse(null);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}