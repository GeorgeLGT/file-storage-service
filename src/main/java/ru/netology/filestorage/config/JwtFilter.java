package ru.netology.filestorage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.filestorage.service.AuthService;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public JwtFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("auth-token");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null) {
            token = authHeader;
        }

        if (token != null && authService.validateToken(token)) {
            SecurityContextHolder.getContext().setAuthentication(
                    authService.getAuthentication(token)
            );
        }

        filterChain.doFilter(request, response);
    }
}