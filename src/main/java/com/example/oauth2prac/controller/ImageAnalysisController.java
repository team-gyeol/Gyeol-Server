package com.example.oauth2prac.controller;

import com.example.oauth2prac.config.SecurityUtils;
import com.example.oauth2prac.dto.MultipleImageAnalysisResponseDTO;
import com.example.oauth2prac.dto.SegmentationResponseDTO;
import com.example.oauth2prac.entity.OriginalImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.OriginalImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import com.example.oauth2prac.service.GCSUploadService;
import com.example.oauth2prac.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageAnalysisController {

    private final GCSUploadService gcsUploadService;
    private final ImageProcessingService imageProcessingService;
    private final UserRepository userRepository;
    private final OriginalImageRepository originalImageRepository;

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeImage(
            @RequestPart(value = "image") MultipartFile image) {

        try {
            // 1. 사용자 정보 조회
            Long userId = SecurityUtils.currentUserIdOrThrow();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 2. 이미지를 S3에 업로드하고 URL을 받음
            log.info("Uploading image to S3: {}", image.getOriginalFilename());
            String imageUrl = gcsUploadService.upload(image);

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
                    .block();

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

    @PostMapping(value = "/analyze-multiple", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeMultipleImages(
            @RequestPart(value = "images") List<MultipartFile> images) {

        try {
            Long userId = SecurityUtils.currentUserIdOrThrow();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 이미지 개수 제한
            if (images.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("한 번에 최대 10개의 이미지만 처리할 수 있습니다.");
            }

            log.info("Processing {} images for user {}", images.size(), userId);

            List<OriginalImage> originalImages = new ArrayList<>();

            for (MultipartFile image : images) {
                log.info("Uploading image to S3: {}", image.getOriginalFilename());
                String imageUrl = gcsUploadService.upload(image);

                OriginalImage originalImage = OriginalImage.builder()
                        .user(user)
                        .imageUrl(imageUrl)
                        .fileName(image.getOriginalFilename())
                        .build();
                originalImageRepository.save(originalImage);
                originalImages.add(originalImage);
                log.info("Original image saved with ID: {}", originalImage.getId());
            }

            // Reactor Flux 비동기 병렬 처리
            List<SegmentationResponseDTO> results = Flux.fromIterable(originalImages)
                    .flatMap(originalImage -> imageProcessingService.requestSegmentation(originalImage))
                    .collectList()
                    .block();

            MultipleImageAnalysisResponseDTO response = MultipleImageAnalysisResponseDTO.builder()
                    .totalCount(results.size())
                    .results(results)
                    .build();

            return ResponseEntity.ok(response);

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
