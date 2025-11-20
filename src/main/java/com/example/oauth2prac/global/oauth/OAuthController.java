package com.example.oauth2prac.global.oauth;

import com.example.oauth2prac.global.jwt.LoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final OAuthService oAuthService;
    @Value("${frontend.url}")
    private String frontendUrl;

    @Operation(summary = "카카오 로그인 콜백", description = "카카오로부터 인가 코드를 받아 로그인을 처리합니다.")
    @GetMapping("/kakao/callback")
    public Mono<ResponseEntity<LoginResponseDto>> kakaoCallback(@RequestParam("code") String code) {
//        return oAuthService.loginWithKakao(code)
//                .map(tokens -> {
//                    String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
//                            .queryParam("accessToken", tokens.getAccessToken())
//                            .queryParam("refreshToken", tokens.getRefreshToken())
//                            .build()
//                            .encode(StandardCharsets.UTF_8)
//                            .toUriString();
//
//                    HttpHeaders headers = new HttpHeaders();
//                    headers.add(HttpHeaders.LOCATION, targetUrl);
//
//                    return ResponseEntity
//                            .status(HttpStatus.FOUND)
//                            .headers(headers)
//                            .build();
//                });
        return oAuthService.loginWithKakao(code)
                .map(ResponseEntity::ok);
    }
}