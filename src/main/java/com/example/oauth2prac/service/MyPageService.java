package com.example.oauth2prac.service;

import com.example.oauth2prac.config.SecurityUtils;
import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.dto.SegmentationImageResponseDto;
import com.example.oauth2prac.entity.OriginalImage;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.OriginalImageRepository;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyPageService {

    private final UserRepository userRepository;
    private final SegmentedImageRepository segmentedImageRepository;
    private final OriginalImageRepository originalImageRepository;
    private final GCSUploadService gcsUploadService;

    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPageInfo() {
        Long userId = SecurityUtils.currentUserIdOrThrow();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return MyPageResponseDto.from(user);

    }

    @Transactional(readOnly = true) // 시간 저장
    public Page<SegmentationImageResponseDto> getMySegmentationImages(Pageable pageable){
        Long userId = SecurityUtils.currentUserIdOrThrow();

        Page<SegmentedImage> userHistory = segmentedImageRepository.findAllByUserIdWithDetails(userId, pageable);
        return userHistory.map(SegmentationImageResponseDto::from);

    }

    @Transactional(readOnly = true)
    public SegmentationImageResponseDto getMySegmentationImage(Long imageId){
        Long userId = SecurityUtils.currentUserIdOrThrow();

        SegmentedImage segmentedImage = segmentedImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        return SegmentationImageResponseDto.builder()
                .segmentedImageUrl(segmentedImage.getImageUrl())
                .id(segmentedImage.getId())
                .analysisResult(segmentedImage.getAnalysisResult())
                .createdAt(segmentedImage.getCreatedAt())
                .originalImageUrl(segmentedImage.getOriginalImage().getImageUrl())
                .build();
    }

    @Transactional
    public void deleteImage(Long imageId) {
        Long userId = SecurityUtils.currentUserIdOrThrow();

        SegmentedImage segmentedImage = segmentedImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        if (!segmentedImage.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 이미지만 삭제할 수 있습니다.");
        }

        OriginalImage originalImage = segmentedImage.getOriginalImage();
        if (originalImage == null) {
            throw new IllegalStateException("원본 이미지 정보를 찾을 수 없습니다.");
        }

        try {
            log.info("Deleting segmented image from GCS: {}", segmentedImage.getImageUrl());
            gcsUploadService.deleteByUrl(segmentedImage.getImageUrl());

            log.info("Deleting original image from GCS: {}", originalImage.getImageUrl());
            gcsUploadService.deleteByUrl(originalImage.getImageUrl());

            segmentedImageRepository.delete(segmentedImage);
            log.info("Segmented image deleted from DB: {}", imageId);

            originalImageRepository.delete(originalImage);
            log.info("Original image deleted from DB: {}", originalImage.getId());

        } catch (Exception e) {
            log.error("이미지 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이미지 삭제에 실패했습니다: " + e.getMessage());
        }
    }
}