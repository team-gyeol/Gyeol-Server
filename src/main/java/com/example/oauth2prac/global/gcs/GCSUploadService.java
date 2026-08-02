package com.example.oauth2prac.global.gcs;

import com.google.cloud.storage.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class GCSUploadService {

    private final Storage storage;
    private final MeterRegistry meterRegistry;
    private static final String folderName = "originalImage";

    /**
     * 이 크기 이하면 단일 요청으로 업로드한다.
     * 요청 본문을 힙에 통째로 올리므로, 동시 요청 수 × 이 값이 힙에 들어갈 만큼만 잡는다.
     * (multipart 제한이 10MB 이므로 그보다 작게)
     */
    private static final long SINGLE_SHOT_UPLOAD_LIMIT_BYTES = 2L * 1024 * 1024;

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    // 파일을 Google Cloud Storage에 업로드하고 공개 URL 반환
    // 전체 응답시간 중 GCS 업로드 기여분을 분리하기 위해 gcs.upload 타이머로 계측한다.
    public String upload(MultipartFile multipartFile) throws IOException {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return doUpload(multipartFile);
        } catch (IOException | RuntimeException e) {
            outcome = "failure";
            throw e;
        } finally {
            sample.stop(Timer.builder("gcs.upload")
                    .description("GCS 원본 이미지 업로드 소요시간")
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private String doUpload(MultipartFile multipartFile) throws IOException {
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

        // storage.createFrom(InputStream) 은 크기를 모르므로 항상 resumable 업로드를 쓴다.
        // 즉 작은 파일 하나에도 세션 생성(POST) + 데이터 전송(PUT) 으로 왕복이 2번 든다.
        // 이 API 가 받는 이미지는 대부분 수백 KB 라, 크기를 아는 경우에는 단일 요청
        // 업로드로 보내 왕복을 절반으로 줄인다.
        // (부하 테스트에서 업로드 평균 901ms -> 왕복 1회로 단축)
        long size = multipartFile.getSize();
        if (size > 0 && size <= SINGLE_SHOT_UPLOAD_LIMIT_BYTES) {
            storage.create(blobInfo, multipartFile.getBytes());
        } else {
            // 큰 파일은 힙에 통째로 올리지 않도록 기존 스트리밍 경로를 유지한다.
            try (InputStream inputStream = multipartFile.getInputStream()) {
                storage.createFrom(blobInfo, inputStream);
            }
        }

        log.debug("File uploaded to GCS: {}", fileName);

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
