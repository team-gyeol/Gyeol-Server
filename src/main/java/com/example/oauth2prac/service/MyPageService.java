package com.example.oauth2prac.service;

import com.example.oauth2prac.config.SecurityUtils;
import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.dto.SegmentationImageResponseDto;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final SegmentedImageRepository segmentedImageRepository;

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
}