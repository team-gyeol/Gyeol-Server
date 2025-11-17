package com.example.oauth2prac.controller;

import com.example.oauth2prac.dto.LoginResponseDto;
import com.example.oauth2prac.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final OAuthService oAuthService;

    @Operation(summary = "카카오 로그인 콜백", description = "카카오로부터 인가 코드를 받아 로그인을 처리합니다.")
    @GetMapping("/kakao/callback")
    public Mono<ResponseEntity<LoginResponseDto>> kakaoCallback(@RequestParam("code") String code) {
        return oAuthService.loginWithKakao(code)
                .map(ResponseEntity::ok);
    }
}