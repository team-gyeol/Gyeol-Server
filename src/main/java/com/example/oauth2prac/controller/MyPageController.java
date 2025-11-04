package com.example.oauth2prac.controller;

import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    public ResponseEntity<MyPageResponseDto> getMyPageInfo() {
        MyPageResponseDto responseDto = myPageService.getMyPageInfo();
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