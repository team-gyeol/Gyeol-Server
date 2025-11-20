package com.example.oauth2prac.domain.segmentedImage;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MultipleImageAnalysisResponseDTO {
    private int totalCount;
    private List<SegmentationResponseDTO> results;
}