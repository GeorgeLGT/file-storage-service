package ru.netology.filestorage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.filestorage.entity.File;
import ru.netology.filestorage.entity.User;
import ru.netology.filestorage.exception.*;
import ru.netology.filestorage.repository.FileRepository;
import ru.netology.filestorage.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @TempDir
    Path tempDir;

    private FileService fileService;
    private User testUser;

    @BeforeEach
    void setUp() {
        String storagePath = tempDir.toString();
        fileService = new FileService(fileRepository, userRepository, storagePath);

        testUser = new User("test@example.com", "password");
        testUser.setId(1L);

        setupSecurityContext();
    }

    private void setupSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getUserFiles_WithLimit() {
        File file1 = new File("file1.txt", 100L, "text/plain", testUser);
        File file2 = new File("file2.txt", 200L, "text/plain", testUser);
        File file3 = new File("file3.txt", 300L, "text/plain", testUser);
        List<File> files = Arrays.asList(file1, file2, file3);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserOrderByUploadedAtDesc(testUser)).thenReturn(files);

        List<File> result = fileService.getUserFiles(2);

        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getFilename());
        assertEquals("file2.txt", result.get(1).getFilename());
        verify(userRepository).findByUsername("test@example.com");
        verify(fileRepository).findByUserOrderByUploadedAtDesc(testUser);
    }

    @Test
    void getUserFiles_WithoutLimit() {
        File file1 = new File("file1.txt", 100L, "text/plain", testUser);
        File file2 = new File("file2.txt", 200L, "text/plain", testUser);
        List<File> files = Arrays.asList(file1, file2);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserOrderByUploadedAtDesc(testUser)).thenReturn(files);

        List<File> result = fileService.getUserFiles(null);

        assertEquals(2, result.size());
        verify(userRepository).findByUsername("test@example.com");
        verify(fileRepository).findByUserOrderByUploadedAtDesc(testUser);
    }

    @Test
    void getUserFiles_UserNotFound() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> fileService.getUserFiles(10));
        verify(userRepository).findByUsername("test@example.com");
    }

    @Test
    void uploadFile_Successful() throws IOException {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.existsByUserAndFilename(testUser, "test.txt")).thenReturn(false);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("content".getBytes());
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        fileService.uploadFile("test.txt", multipartFile);

        verify(fileRepository).existsByUserAndFilename(testUser, "test.txt");
        verify(fileRepository).save(any(File.class));
    }

    @Test
    void uploadFile_FileAlreadyExists() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.existsByUserAndFilename(testUser, "existing.txt")).thenReturn(true);

        assertThrows(FileAlreadyExistsException.class, () -> fileService.uploadFile("existing.txt", multipartFile));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void uploadFile_EmptyFile() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.existsByUserAndFilename(testUser, "empty.txt")).thenReturn(false);
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(EmptyFileException.class, () -> fileService.uploadFile("empty.txt", multipartFile));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void uploadFile_UserNotFound() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> fileService.uploadFile("test.txt", multipartFile));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void downloadFile_FileNotFoundInDatabase() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.downloadFile("nonexistent.txt"));
    }

    @Test
    void downloadFile_FileNotFoundOnDisk() throws IOException {
        File file = new File("missing.txt", 100L, "text/plain", testUser);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "missing.txt")).thenReturn(Optional.of(file));

        assertThrows(StorageException.class, () -> fileService.downloadFile("missing.txt"));
    }

    @Test
    void downloadFile_Successful() throws IOException {
        File file = new File("test.txt", 100L, "text/plain", testUser);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.of(file));

        Path userDir = tempDir.resolve("1");
        Files.createDirectories(userDir);
        Path testFile = userDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());

        Resource resource = fileService.downloadFile("test.txt");

        assertNotNull(resource);
        assertTrue(resource.exists());
        verify(fileRepository).findByUserAndFilename(testUser, "test.txt");
    }

    @Test
    void deleteFile_Successful() throws IOException {
        File file = new File("test.txt", 100L, "text/plain", testUser);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.of(file));

        Path userDir = tempDir.resolve("1");
        Files.createDirectories(userDir);
        Path testFile = userDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());

        fileService.deleteFile("test.txt");

        verify(fileRepository).findByUserAndFilename(testUser, "test.txt");
        verify(fileRepository).delete(file);
        assertFalse(Files.exists(testFile));
    }

    @Test
    void deleteFile_FileNotFound() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteFile("nonexistent.txt"));
        verify(fileRepository, never()).delete(any());
    }

    @Test
    void renameFile_Successful() throws IOException {
        File file = new File("old.txt", 100L, "text/plain", testUser);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(file));
        when(fileRepository.existsByUserAndFilename(testUser, "new.txt")).thenReturn(false);

        Path userDir = tempDir.resolve("1");
        Files.createDirectories(userDir);
        Path oldFile = userDir.resolve("old.txt");
        Files.write(oldFile, "content".getBytes());

        fileService.renameFile("old.txt", "new.txt");

        assertEquals("new.txt", file.getFilename());
        verify(fileRepository).save(file);
        assertFalse(Files.exists(oldFile)); // Old file should not exist
        assertTrue(Files.exists(userDir.resolve("new.txt"))); // New file should exist
    }

    @Test
    void renameFile_FileNotFound() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "nonexistent.txt")).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.renameFile("nonexistent.txt", "new.txt"));
        verify(fileRepository, never()).save(any());
    }

    @Test
    void renameFile_NewNameAlreadyExists() {
        File file = new File("old.txt", 100L, "text/plain", testUser);

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(file));
        when(fileRepository.existsByUserAndFilename(testUser, "new.txt")).thenReturn(true);

        assertThrows(FileAlreadyExistsException.class, () -> fileService.renameFile("old.txt", "new.txt"));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void currentUserContextIsCorrect() {
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserOrderByUploadedAtDesc(testUser)).thenReturn(Arrays.asList());

        List<File> result = fileService.getUserFiles(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername("test@example.com");
    }

    @Test
    void currentUserIsUsedInFileOperations() {
        File file = new File("test.txt", 100L, "text/plain", testUser);
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
        when(fileRepository.findByUserOrderByUploadedAtDesc(testUser)).thenReturn(Arrays.asList(file));

        List<File> result = fileService.getUserFiles(null);

        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getFilename());
    }

    @Test
    void differentUsernameInSecurityContext() {
        when(authentication.getName()).thenReturn("different@example.com");
        User differentUser = new User("different@example.com", "password");
        differentUser.setId(2L);

        when(userRepository.findByUsername("different@example.com")).thenReturn(Optional.of(differentUser));
        when(fileRepository.findByUserOrderByUploadedAtDesc(differentUser)).thenReturn(Arrays.asList());

        List<File> result = fileService.getUserFiles(null);

        assertNotNull(result);
        verify(userRepository).findByUsername("different@example.com");
        verify(fileRepository).findByUserOrderByUploadedAtDesc(differentUser);
    }
}