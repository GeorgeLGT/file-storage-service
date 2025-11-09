package ru.netology.filestorage.controller;

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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException | InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Login failed", 500));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        try {
            String cleanToken = token != null && token.startsWith("Bearer ") ?
                    token.substring(7) : token;
            authService.logout(cleanToken);
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Logout failed", 500));
        }
    }
}
