package ru.netology.filestorage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.filestorage.dto.FileListResponse;
import ru.netology.filestorage.entity.File;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.FileNotFoundException;
import ru.netology.filestorage.exception.UserNotFoundException;
import ru.netology.filestorage.repository.FileRepository;
import ru.netology.filestorage.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

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
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public List<FileListResponse> getUserFiles(Integer limit) {
        User user = getCurrentUser();
        List<File> files = fileRepository.findByUserOrderByUploadedAtDesc(user);

        if (limit != null && limit > 0) {
            files = files.stream().limit(limit).collect(Collectors.toList());
        }

        return files.stream()
                .map(file -> new FileListResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

    public void uploadFile(String filename, MultipartFile file) throws IOException {
        User user = getCurrentUser();

        if (fileRepository.existsByUserAndFilename(user, filename)) {
            throw new RuntimeException("File already exists");
        }

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        Path filePath = getFilePath(user, filename);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());

        File fileEntity = new File(filename, file.getSize(), file.getContentType(), user);
        fileRepository.save(fileEntity);
    }

    public Resource downloadFile(String filename) throws IOException {
        User user = getCurrentUser();
        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + filename));

        Path filePath = getFilePath(user, filename);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("Could not read file: " + filename);
        }
    }

    public void deleteFile(String filename) throws IOException {
        User user = getCurrentUser();
        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + filename));

        Path filePath = getFilePath(user, filename);
        Files.deleteIfExists(filePath);
        fileRepository.delete(file);
    }

    public void renameFile(String filename, String newFilename) {
        User user = getCurrentUser();
        File file = fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + filename));

        if (fileRepository.existsByUserAndFilename(user, newFilename)) {
            throw new RuntimeException("File with new name already exists");
        }

        try {
            Path oldPath = getFilePath(user, filename);
            Path newPath = getFilePath(user, newFilename);

            if (Files.exists(oldPath)) {
                Files.move(oldPath, newPath);
            }

            file.setFilename(newFilename);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not rename file: " + e.getMessage(), e);
        }
    }

    private Path getFilePath(User user, String filename) {
        return Paths.get(storagePath, user.getId().toString(), filename);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}