package com.example.oauth2prac.domain.segmentedImage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SegmentationRequestDto {
    private String imageUrl;
}