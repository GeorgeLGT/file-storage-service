package ru.netology.filestorage.dto;

public class RenameFileRequest {
    private String name;

    public RenameFileRequest() {
    }

    public RenameFileRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}