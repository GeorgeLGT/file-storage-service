package ru.netology.filestorage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.filestorage.dto.ErrorResponse;
import ru.netology.filestorage.dto.FileListResponse;
import ru.netology.filestorage.dto.RenameFileRequest;
import ru.netology.filestorage.entity.File;
import ru.netology.filestorage.exception.*;
import ru.netology.filestorage.service.FileService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cloud")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam(value = "limit", required = false) Integer limit) {
        log.debug("Запрос списка файлов с ограничением: {}", limit);
        try {
            List<File> files = fileService.getUserFiles(limit);
            List<FileListResponse> response = files.stream()
                    .map(file -> new FileListResponse(file.getFilename(), file.getSize()))
                    .collect(Collectors.toList());
            log.info("Извлеченные {} файлы", files.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при получении списка файлов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка при получении списка файлов", 500));
        }
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {
        log.info("Запрос на загрузку файла: {} (size: {} bytes)", filename, file.getSize());
        try {
            fileService.uploadFile(filename, file);
            log.info("Файл {} успешно загружен", filename);
            return ResponseEntity.ok().build();
        } catch (FileAlreadyExistsException | EmptyFileException e) {
            log.warn("Загрузка файла отклонена: {} - {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            log.error("Не удалось загрузить файл: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Не удалось загрузить файл", 500));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        log.info("Запрос на загрузку файла: {}", filename);
        try {
            Resource resource = fileService.downloadFile(filename);
            log.info("Файл {} успешно подготовлен для загрузки", filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            log.error("Не удалось загрузить файл: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Не удалось загрузить файл", 500));
        } catch (FileNotFoundException e) {
            log.warn("Файл не найден для скачивания: {}", filename);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        log.info("Запрос на удаление файла: {}", filename);
        try {
            fileService.deleteFile(filename);
            log.info("Файл {} успешно удален", filename);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Не удалось удалить файл: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Не удалось удалить файл", 500));
        } catch (FileNotFoundException e) {
            log.warn("Не найден файл для удаления: {}", filename);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestBody RenameFileRequest renameRequest) {
        log.info("Запрос на переименование файла : {} -> {}", filename, renameRequest.getName());
        try {
            fileService.renameFile(filename, renameRequest.getName());
            log.info("Файл {} успешно переименован на {}", filename, renameRequest.getName());
            return ResponseEntity.ok().build();
        } catch (FileNotFoundException | FileAlreadyExistsException e) {
            log.warn("Ошибка переименования файла: {} - {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("Ошибка переименования файла: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка переименования файла", 500));
        }
    }
}