package ru.netology.filestorage.dto;

import java.time.LocalDateTime;

public class FileResponse {
    private String filename;
    private Long size;
    private LocalDateTime uploadedAt;

    public FileResponse() {}

    public FileResponse(String filename, Long size, LocalDateTime uploadedAt) {
        this.filename = filename;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "FileResponse{" +
               "filename='" + filename + '\'' +
               ", size=" + size +
               ", uploadedAt=" + uploadedAt +
               '}';
    }
}