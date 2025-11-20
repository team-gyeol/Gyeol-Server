package com.example.oauth2prac.domain.segmentedImage;

import com.example.oauth2prac.domain.originalImage.OriginalImage;
import com.example.oauth2prac.domain.user.User;
import com.example.oauth2prac.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class SegmentedImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_image_id")
    private OriginalImage originalImage;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false, length = 500) // 길이를 넉넉하게 설정
    private String analysisResult;

    private Integer multicopterBodyCount;
    private Integer propellerCount;
    private Integer cameraCount;
    private Integer legCount;

}