package com.example.oauth2prac.dto;

import com.example.oauth2prac.entity.SegmentedImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SegmentationImageResponseDto {
    private Long id;
    private String originalImageUrl;
    private String segmentedImageUrl;
    private String analysisResult;
    private LocalDateTime createdAt;

    public static SegmentationImageResponseDto from(SegmentedImage image) {
        return SegmentationImageResponseDto.builder()
                .id(image.getId())
                .originalImageUrl(image.getOriginalImage().getImageUrl())
                .segmentedImageUrl(image.getImageUrl())
                .analysisResult(image.getAnalysisResult())
                .createdAt(image.getCreatedAt())
                .build();
    }
}