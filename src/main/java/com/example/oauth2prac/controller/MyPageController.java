package com.example.oauth2prac.controller;

import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserRepository userRepository;
    private final SegmentedImageRepository segmentedImageRepository;

    @GetMapping
    public ResponseEntity<MyPageResponseDto> getMyPageInfo(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = Long.parseLong(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<SegmentedImage> userHistory = segmentedImageRepository.findAllByUser(user);

        MyPageResponseDto responseDto = new MyPageResponseDto(user.getName(), user.getEmail(), userHistory);
        return ResponseEntity.ok(responseDto);
    }

//    @DeleteMapping("/withdraw")
//    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
//        Long userId = Long.parseLong(principal.getUsername());
//        // 연관된 데이터(이미지 등)를 먼저 삭제한 후 사용자를 삭제해야 합니다. (Cascade 설정 또는 수동 삭제)
//        userRepository.deleteById(userId);
//        return ResponseEntity.ok().build();
//    }
}