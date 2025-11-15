package ru.netology.filestorage.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.UserAlreadyExistsException;
import ru.netology.filestorage.exception.UserNotFoundException;
import ru.netology.filestorage.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initDefaultUsers() {
        createUserIfNotExists("user@example.com", "password");
        createUserIfNotExists("admin@example.com", "admin");
        createUserIfNotExists("test@example.com", "test");
    }

    private void createUserIfNotExists(String username, String password) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, passwordEncoder.encode(password));
            userRepository.save(user);
        }
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public User createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Пользователь уже существует: " + username);
        }
        User user = new User(username, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
    }
}