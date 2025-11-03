package com.example.oauth2prac.repository;

import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentedImageRepository extends JpaRepository<SegmentedImage, Long> {
    SegmentedImage findByOriginalImageId(Long originalImageId);
    SegmentedImage findByImageUrl(String imageUrl);
    SegmentedImage findByAnalysisResult(String analysisResult);
    List<SegmentedImage> findAllByUser(User user);
}
