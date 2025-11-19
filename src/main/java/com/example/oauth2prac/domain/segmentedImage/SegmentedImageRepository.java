package com.example.oauth2prac.domain.segmentedImage;

import com.example.oauth2prac.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SegmentedImageRepository extends JpaRepository<SegmentedImage, Long> {
    SegmentedImage findByOriginalImageId(Long originalImageId);
    SegmentedImage findByImageUrl(String imageUrl);
    SegmentedImage findByAnalysisResult(String analysisResult);
    List<SegmentedImage> findAllByUser(User user);

    @Query(value = "SELECT si FROM SegmentedImage si " +
            "JOIN FETCH si.user u " +
            "JOIN FETCH si.originalImage oi " +
            "WHERE u.id = :userId",
            countQuery = "SELECT count(si) FROM SegmentedImage si WHERE si.user.id = :userId")
    Page<SegmentedImage> findAllByUserIdWithDetails(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

}
