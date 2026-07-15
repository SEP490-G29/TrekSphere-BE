package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        log.info("REST request to upload file [{}] to folder: {}", file.getOriginalFilename(), folder);
        String fileUrl = fileService.uploadFile(file, folder);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, fileUrl));
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        log.info("REST request to upload {} files to folder: {}", files.size(), folder);
        List<String> fileUrls = fileService.uploadFiles(files, folder);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, fileUrls));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @RequestParam("publicId") String publicId
    ) {
        log.info("REST request to delete file with publicId: {}", publicId);
        fileService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.FILE_DELETED_SUCCESSFULLY));
    }
}
