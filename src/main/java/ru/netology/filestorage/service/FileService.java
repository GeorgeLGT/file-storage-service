package ru.netology.filestorage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.filestorage.entity.File;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.*;
import ru.netology.filestorage.repository.FileRepository;
import ru.netology.filestorage.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final String storagePath;

    public FileService(FileRepository fileRepository,
                       UserRepository userRepository,
                       @Value("${app.storage.path:./storage}") String storagePath) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storagePath = storagePath;

        try {
            Files.createDirectories(Paths.get(storagePath));
            log.info("Каталог хранения инициализирован: {}", storagePath);
        } catch (IOException e) {
            log.error("Не удалось создать каталог хранения: {}", storagePath, e);
            throw new RuntimeException("Не удалось создать каталог хранения", e);
        }
    }

    public List<File> getUserFiles(Integer limit) {
        User user = getCurrentUser();
        log.debug("Получение файлов для пользователя: {} с ограничением: {}", user.getUsername(), limit);

        List<File> files = fileRepository.findByUserOrderByUploadedAtDesc(user);

        if (limit != null && limit > 0) {
            files = files.stream().limit(limit).toList();
        }

        log.info("Получено {} файлов для пользователя: {}", files.size(), user.getUsername());
        return files;
    }

    public void uploadFile(String filename, MultipartFile file) throws IOException {
        User user = getCurrentUser();
        log.debug("Загрузка файла: {} для пользователя: {}", filename, user.getUsername());

        if (fileRepository.existsByUserAndFilename(user, filename)) {
            log.warn("Файл уже существует: {} для пользователя: {}", filename, user.getUsername());
            throw new FileAlreadyExistsException("Файл уже существует: " + filename);
        }

        if (file.isEmpty()) {
            log.warn("Попытка загрузить пустой файл: {}", filename);
            throw new EmptyFileException("Файл пуст: " + filename);
        }

        try {
            Path filePath = getFilePath(user, filename);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            File fileEntity = new File(filename, file.getSize(), file.getContentType(), user);
            fileRepository.save(fileEntity);

            log.info("Файл {} успешно загружен для пользователя: {} (size: {} bytes)",
                    filename, user.getUsername(), file.getSize());
        } catch (IOException e) {
            log.error("Ошибка хранения при загрузке файла: {} для пользователя: {}", filename, user.getUsername(), e);
            throw new StorageException("Не удалось загрузить файл: " + filename, e);
        }
    }

    public Resource downloadFile(String filename) throws IOException {
        User user = getCurrentUser();
        log.debug("Загрузка файла: {} пользователем: {}", filename, user.getUsername());

        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> {
                    log.warn("Файл не найден для загрузки: {} для пользователя: {}", filename, user.getUsername());
                    return new FileNotFoundException("Файл не найден: " + filename);
                });

        Path filePath = getFilePath(user, filename);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            log.info("Файл {} подготовлен для загрузки пользователем: {}", filename, user.getUsername());
            return resource;
        } else {
            log.error("Файл существует в базе данных, но не найден на диске: {} для пользователя: {}", filename, user.getUsername());
            throw new StorageException("Не удалось прочитать файл: " + filename);
        }
    }

    public void deleteFile(String filename) throws IOException {
        User user = getCurrentUser();
        log.debug("Удаление файла: {} для пользователя: {}", filename, user.getUsername());

        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> {
                    log.warn("Файл не найден для удаления: {} для пользователя: {}", filename, user.getUsername());
                    return new FileNotFoundException("Файл не найден: " + filename);
                });

        Path filePath = getFilePath(user, filename);
        Files.deleteIfExists(filePath);
        fileRepository.delete(file);

        log.info("Файл {} успешно удален для пользователя: {}", filename, user.getUsername());
    }

    public void renameFile(String filename, String newFilename) {
        User user = getCurrentUser();
        log.debug("Переименование файла: {} в {} для пользователя: {}", filename, newFilename, user.getUsername());

        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> {
                    log.warn("Файл не найден для переименования: {} для пользователя: {}", filename, user.getUsername());
                    return new FileNotFoundException("Файл не найден: " + filename);
                });

        if (fileRepository.existsByUserAndFilename(user, newFilename)) {
            log.warn("Файл с новым именем уже существует: {} для пользователя: {}", newFilename, user.getUsername());
            throw new FileAlreadyExistsException("Файл с новым именем уже существует: " + newFilename);
        }

        try {
            Path oldPath = getFilePath(user, filename);
            Path newPath = getFilePath(user, newFilename);

            if (Files.exists(oldPath)) {
                Files.move(oldPath, newPath);
            }

            file.setFilename(newFilename);
            fileRepository.save(file);
            log.info("Файл {} успешно переименован в {} для пользователя: {}", filename, newFilename, user.getUsername());
        } catch (IOException e) {
            log.error("Ошибка переименования файла: {} в {} для пользователя: {}", filename, newFilename, user.getUsername(), e);
            throw new StorageException("Не удалось переименовать файл: " + filename, e);
        }
    }

    private Path getFilePath(User user, String filename) {
        return Paths.get(storagePath, user.getId().toString(), filename);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Получение текущего пользователя: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден в контексте безопасности: {}", username);
                    return new UserNotFoundException("Пользователь не найден");
                });
    }
}