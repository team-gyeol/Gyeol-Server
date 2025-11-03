package com.example.oauth2prac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SegmentationResponseDTO {
    @JsonProperty("analysisResult")
    private String analysisResult;

    @JsonProperty("segmentedImageUrl")
    private String segmentedImageUrl;
}
