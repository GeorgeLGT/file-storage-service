package ru.netology.filestorage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.netology.filestorage.dto.AuthRequest;
import ru.netology.filestorage.dto.AuthResponse;
import ru.netology.filestorage.dto.ErrorResponse;
import ru.netology.filestorage.exception.InvalidCredentialsException;
import ru.netology.filestorage.exception.UserNotFoundException;
import ru.netology.filestorage.service.AuthService;

import java.util.Collections;

@RestController
@RequestMapping("/cloud")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        log.info("Попытка входа пользователя: {}", request.getLogin());
        try {
            AuthResponse response = authService.login(request);
            log.info("Пользователь {} успешно вошел в систему", request.getLogin());
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            log.warn("Пользователь не найден: {}", request.getLogin());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        } catch (InvalidCredentialsException e) {
            log.warn("Неверные учетные данные пользователя: {}", request.getLogin());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Не удалось войти пользователю: {}", request.getLogin(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка входа", 500));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        log.debug("Запрос на выход получен");
        try {
            String cleanToken = token != null && token.startsWith("Bearer ") ?
                    token.substring(7) : token;
            authService.logout(cleanToken);
            log.info("Пользователь успешно вышел из системы");
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Успешно вышел из системы"));
        } catch (Exception e) {
            log.error("Не удалось выйти из системы", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Не удалось выйти из системы", 500));
        }
    }
}