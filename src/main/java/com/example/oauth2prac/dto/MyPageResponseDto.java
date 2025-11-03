package com.example.oauth2prac.dto;

import com.example.oauth2prac.entity.SegmentedImage;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MyPageResponseDto {

    private String userName;
    private String userEmail;
    private List<AnalysisHistoryDto> history;

    public MyPageResponseDto(String userName, String userEmail, List<SegmentedImage> segmentedImages) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.history = segmentedImages.stream()
                .map(AnalysisHistoryDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class AnalysisHistoryDto {
        private String originalImageUrl;
        private String segmentedImageUrl;
        private String analysisResult;

        public AnalysisHistoryDto(SegmentedImage image) {
            this.originalImageUrl = image.getOriginalImage().getImageUrl();
            this.segmentedImageUrl = image.getImageUrl();
            this.analysisResult = image.getAnalysisResult();
        }
    }
}