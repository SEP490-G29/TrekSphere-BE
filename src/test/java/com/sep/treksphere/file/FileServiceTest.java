package com.sep.treksphere.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Bao phủ 5 method của `FileService`: uploadFile, upload, uploadFiles, deleteFile, deleteFileBestEffort. */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    @InjectMocks
    private FileService fileService;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02, 0x03, 0x04
    };

    private MockMultipartFile pngFile(String filename) {
        return new MockMultipartFile("file", filename, "image/png", PNG_SIGNATURE);
    }

    @Test
    @DisplayName("uploadFile: file hợp lệ -> trả về URL từ Cloudinary")
    void uploadFile_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(
                Map.of("secure_url", "https://cdn.example.com/x.png", "public_id", "trekker/x"));

        String url = fileService.uploadFile(pngFile("avatar.png"), "avatars");

        assertThat(url).isEqualTo("https://cdn.example.com/x.png");
    }

    @Test
    @DisplayName("upload: file hợp lệ -> trả về StoredFile đầy đủ, tên file được làm sạch")
    void upload_Success_ReturnsStoredFile() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(
                Map.of("secure_url", "https://cdn.example.com/x.png", "public_id", "trekker/x"));

        StoredFile stored = fileService.upload(pngFile("Ảnh Bìa Tour.png"), "tours", UploadPolicy.IMAGE);

        assertThat(stored.storageId()).isEqualTo("trekker/x");
        assertThat(stored.url()).isEqualTo("https://cdn.example.com/x.png");
        assertThat(stored.mimeType()).isEqualTo("image/png");
        assertThat(stored.sizeBytes()).isEqualTo(PNG_SIGNATURE.length);
        assertThat(stored.originalName()).endsWith(".png");
    }

    @Test
    @DisplayName("upload: tên file chứa path/ký tự lạ -> làm sạch, chỉ giữ lại phần tên hợp lệ")
    void upload_SanitizesUnsafeOriginalFilename() throws IOException {
        MockMultipartFile unsafe = new MockMultipartFile(
                "file", "../folder/evil<>name?.png", "image/png", PNG_SIGNATURE);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(
                Map.of("secure_url", "https://cdn.example.com/x.png", "public_id", "trekker/x"));

        StoredFile stored = fileService.upload(unsafe, "tours", UploadPolicy.IMAGE);

        assertThat(stored.originalName()).doesNotContain("/").doesNotContain("<").doesNotContain(">");
    }

    @Test
    @DisplayName("upload: file rỗng -> ném AppException INVALID_FILE_FORMAT, không gọi Cloudinary")
    void upload_EmptyFile_ThrowsInvalidFileFormat() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(empty, "tours", UploadPolicy.IMAGE))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("upload: vượt quá dung lượng tối đa (10MB) -> ném AppException FILE_TOO_LARGE")
    void upload_ExceedsMaxSize_ThrowsFileTooLarge() {
        byte[] tooBig = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile big = new MockMultipartFile("file", "a.png", "image/png", tooBig);

        assertThatThrownBy(() -> fileService.upload(big, "tours", UploadPolicy.IMAGE))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("upload: đuôi file không khớp policy (VD .pdf khi policy chỉ nhận ảnh) -> ném AppException INVALID_FILE_FORMAT")
    void upload_ExtensionNotAllowed_ThrowsInvalidFileFormat() {
        MockMultipartFile wrongExt = new MockMultipartFile("file", "a.pdf", "image/png", PNG_SIGNATURE);

        assertThatThrownBy(() -> fileService.upload(wrongExt, "tours", UploadPolicy.IMAGE))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
    }

    @Test
    @DisplayName("upload: nội dung file không khớp magic-byte của content-type khai báo -> ném AppException INVALID_FILE_FORMAT")
    void upload_SignatureMismatch_ThrowsInvalidFileFormat() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "a.png", "image/png", "not a real png".getBytes());

        assertThatThrownBy(() -> fileService.upload(fakeImage, "tours", UploadPolicy.IMAGE))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("upload: Cloudinary ném IOException -> bọc thành AppException UPLOAD_FAILED")
    void upload_CloudinaryIOException_ThrowsUploadFailed() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("network"));

        assertThatThrownBy(() -> fileService.upload(pngFile("a.png"), "tours", UploadPolicy.IMAGE))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_FAILED));
    }

    @Test
    @DisplayName("uploadFiles: nhiều file hợp lệ -> trả về đúng danh sách URL theo thứ tự")
    void uploadFiles_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://cdn.example.com/1.png", "public_id", "p1"))
                .thenReturn(Map.of("secure_url", "https://cdn.example.com/2.png", "public_id", "p2"));

        List<String> urls = fileService.uploadFiles(List.of(pngFile("a.png"), pngFile("b.png")), "tours");

        assertThat(urls).containsExactly("https://cdn.example.com/1.png", "https://cdn.example.com/2.png");
    }

    @Test
    @DisplayName("uploadFiles: danh sách rỗng -> ném AppException INVALID_FILE_FORMAT")
    void uploadFiles_EmptyList_ThrowsInvalidFileFormat() {
        assertThatThrownBy(() -> fileService.uploadFiles(List.of(), "tours"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_FORMAT));
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("deleteFile: xoá thành công trên Cloudinary -> không ném lỗi")
    void deleteFile_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("trekker/x"), anyMap())).thenReturn(Map.of("result", "ok"));

        assertThatCode(() -> fileService.deleteFile("trekker/x")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFile: Cloudinary ném IOException -> bọc thành AppException UPLOAD_FAILED")
    void deleteFile_IOException_ThrowsUploadFailed() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network"));

        assertThatThrownBy(() -> fileService.deleteFile("trekker/x"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_FAILED));
    }

    @Test
    @DisplayName("deleteFileBestEffort: publicId rỗng -> bỏ qua, không gọi Cloudinary")
    void deleteFileBestEffort_BlankPublicId_NoOp() {
        fileService.deleteFileBestEffort("  ", "image/png");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("deleteFileBestEffort: xoá thành công -> không ném lỗi")
    void deleteFileBestEffort_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("trekker/x"), anyMap())).thenReturn(Map.of("result", "ok"));

        assertThatCode(() -> fileService.deleteFileBestEffort("trekker/x", "image/png"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFileBestEffort: Cloudinary lỗi -> nuốt lỗi, không ném ra ngoài (best-effort)")
    void deleteFileBestEffort_ExceptionThrown_Swallowed() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network"));

        assertThatCode(() -> fileService.deleteFileBestEffort("trekker/x", "image/png"))
                .doesNotThrowAnyException();
    }
}
