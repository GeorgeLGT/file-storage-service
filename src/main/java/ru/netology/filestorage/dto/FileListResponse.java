package ru.netology.filestorage.dto;

public class FileListResponse {
    private String filename;
    private Long size;

    public FileListResponse() {
    }

    public FileListResponse(String filename, Long size) {
        this.filename = filename;
        this.size = size;
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
}