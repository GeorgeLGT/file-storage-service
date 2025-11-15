package ru.netology.filestorage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final UserService userService;

    public AuthService(UserRepository userRepository, TokenRepository tokenRepository, UserService userService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
    }

    public AuthResponse login(AuthRequest request) {
        log.debug("Попытка входа пользователя: {}", request.getLogin());
        User user = userRepository.findByUsername(request.getLogin())
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", request.getLogin());
                    return new UserNotFoundException("Пользователь не найден");
                });

        if (!userService.checkPassword(request.getPassword(), user.getPassword())) {
            log.warn("Неверный пароль для пользователя: {}", request.getLogin());
            throw new InvalidCredentialsException("Неверный пароль");
        }

        String token = generateToken();
        Token tokenEntity = new Token(token, user, LocalDateTime.now().plusDays(1));
        tokenRepository.save(tokenEntity);

        log.info("Пользователь {} успешно аутентифицирован. Токен сгенерирован.", request.getLogin());
        return new AuthResponse(token);
    }

    public void logout(String token) {
        log.debug("Запрос на выход из системы для токена");
        if (token == null) {
            log.debug("Выход из системы вызван с нулевым токеном, игнорируя");
            return;
        }

        tokenRepository.findByTokenAndActiveTrue(token).ifPresent(tokenEntity -> {
            tokenEntity.setActive(false);
            tokenRepository.save(tokenEntity);
            log.info("Токен недействителен для пользователя: {}", tokenEntity.getUser().getUsername());
        });
    }

    public boolean validateToken(String token) {
        if (token == null) {
            log.debug("Токен нулевой, проверка не пройдена");
            return false;
        }

        boolean isValid = tokenRepository.findByTokenAndActiveTrue(token)
                .map(t -> {
                    boolean notExpired = t.getExpiresAt().isAfter(LocalDateTime.now());
                    if (!notExpired) {
                        log.debug("Срок действия токена истек: {}", token);
                    }
                    return notExpired;
                })
                .orElse(false);

        log.debug("Результат проверки токена: {} для токена: {}", isValid, token);
        return isValid;
    }

    public Authentication getAuthentication(String token) {
        if (token == null) {
            log.debug("Токен нулевой, невозможно получить аутентификацию");
            return null;
        }

        return tokenRepository.findByTokenAndActiveTrue(token)
                .map(t -> {
                    log.debug("Создание аутентификации для пользователя: {}", t.getUser().getUsername());
                    return new UsernamePasswordAuthenticationToken(
                            t.getUser().getUsername(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("USER"))
                    );
                })
                .orElse(null);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}