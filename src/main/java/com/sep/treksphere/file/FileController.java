package com.sep.treksphere.file;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("REST request to upload file [{}]", file.getOriginalFilename());
        String fileUrl = fileService.uploadFile(file, "general");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, fileUrl));
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files
    ) {
        log.info("REST request to upload {} files", files.size());
        List<String> fileUrls = fileService.uploadFiles(files, "general");
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, fileUrls));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @RequestParam("publicId") String publicId
    ) {
        log.info("REST request to delete file with publicId: {}", publicId);
        fileService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.FILE_DELETED_SUCCESSFULLY));
    }
}
