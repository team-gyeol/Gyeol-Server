package com.example.oauth2prac.global.gcs;

import com.google.cloud.storage.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class GCSUploadService {

    private final Storage storage;
    private static final String folderName = "originalImage";

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    // 파일을 Google Cloud Storage에 업로드하고 공개 URL 반환
    public String upload(MultipartFile multipartFile) throws IOException {
        // 고유한 파일 이름 생성
        String originalFilename = multipartFile.getOriginalFilename();
        String fileName = UUID.randomUUID() + "-" + originalFilename;

        String gcsPath = folderName + "/" + fileName;

        // BlobId 생성 (버킷명 + 파일명)
        BlobId blobId = BlobId.of(bucketName, gcsPath);

        // BlobInfo 생성 (메타데이터 설정)
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(multipartFile.getContentType())
                .build();

        // 파일 업로드
        Blob blob = storage.create(
                blobInfo,
                multipartFile.getBytes()
        );

        log.info("File uploaded to GCS: {}", fileName);

        // 공개 URL 반환
        return String.format(
                "https://storage.googleapis.com/%s/%s",
                bucketName,
                gcsPath
        );
    }

    // signed url
    public String generateSignedUrl(String fileName, int validityMinutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName).build();

        return storage.signUrl(
                blobInfo,
                validityMinutes,
                java.util.concurrent.TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        ).toString();
    }

    // 파일 삭제
    public void delete(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        boolean deleted = storage.delete(blobId);

        if (deleted) {
            log.info("File deleted from GCS: {}", fileName);
        } else {
            log.warn("File not found in GCS: {}", fileName);
        }
    }

    // 존재 확인
    public boolean exists(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);
        return blob != null && blob.exists();
    }


    public String extractFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        return imageUrl;
    }

    public void deleteByUrl(String imageUrl) {
        String fileName = extractFileNameFromUrl(imageUrl);
        if (fileName != null) {
            delete(fileName);
        }
    }
}