package com.example.oauth2prac.controller;

import com.example.oauth2prac.config.SecurityUtils;
import com.example.oauth2prac.dto.SegmentationResponseDTO;
import com.example.oauth2prac.entity.OriginalImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.OriginalImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import com.example.oauth2prac.service.ImageProcessingService;
import com.example.oauth2prac.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageAnalysisController {

    private final S3UploadService s3UploadService;
    private final ImageProcessingService imageProcessingService;
    private final UserRepository userRepository;
    private final OriginalImageRepository originalImageRepository;

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeImage(
            @RequestPart(value = "image") MultipartFile image) {

        try {
            // 1. 현재 로그인한 사용자 정보 조회
            Long userId = SecurityUtils.currentUserIdOrThrow();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 2. 이미지를 S3에 업로드하고 URL을 받음
            log.info("Uploading image to S3: {}", image.getOriginalFilename());
            String imageUrl = s3UploadService.upload(image);

            // 3. 원본 이미지 정보를 DB에 저장
            OriginalImage originalImage = OriginalImage.builder()
                    .user(user)
                    .imageUrl(imageUrl)
                    .fileName(image.getOriginalFilename())
                    .build();
            originalImageRepository.save(originalImage);
            log.info("Original image saved with ID: {}", originalImage.getId());

            // 4. FastAPI에 분석 요청 보내고 결과를 받음
            SegmentationResponseDTO analysisResult = imageProcessingService
                    .requestSegmentation(originalImage)
                    .block(); // 비동기 호출을 동기적으로 기다림

            // 5. 클라이언트에 최종 결과 반환
            return ResponseEntity.ok(analysisResult);

        } catch (IOException e) {
            log.error("이미지 업로드 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 업로드에 실패했습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("이미지 분석 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 분석에 실패했습니다: " + e.getMessage());
        }
    }
}
