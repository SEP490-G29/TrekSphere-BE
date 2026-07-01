package com.sep.treksphere.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    String uploadFile(MultipartFile file, String folder);

    List<String> uploadFiles(List<MultipartFile> files, String folder);

    void deleteFile(String publicId);
}
