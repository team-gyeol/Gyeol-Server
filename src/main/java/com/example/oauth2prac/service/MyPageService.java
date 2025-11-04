package com.example.oauth2prac.service;

import com.example.oauth2prac.config.SecurityUtils;
import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

        List<SegmentedImage> userHistory = segmentedImageRepository.findAllByUser(user);

        // 트랜잭션 내에서 DTO 변환이 일어나므로 LazyInitializationException이 발생하지 않습니다.
        return new MyPageResponseDto(user.getName(), user.getEmail(), userHistory);
    }
}