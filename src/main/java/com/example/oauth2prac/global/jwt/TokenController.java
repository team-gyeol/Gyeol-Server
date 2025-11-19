package com.example.oauth2prac.global.jwt;

import com.example.oauth2prac.global.security.SecurityUtils;
import com.example.oauth2prac.domain.user.User;
import com.example.oauth2prac.domain.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 개발용 토큰 발급
    // @Deprecated
    @Operation(summary = "개발용 토큰 발급(access)")
    @GetMapping("/{userId}")
    public String generateToken(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        String accessToken=jwtTokenProvider.createToken(user.getId().toString(),user.getRoleKey());
        return accessToken;
    }

    // 토큰 재발급
    @Operation(summary = "토큰 재발급(access)")
    @PostMapping("/refresh")
    public TokenRefreshResponseDto refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {

        User user = userRepository.findByRefreshToken(requestDto.getRefreshToken()).orElseThrow();
        String newAccessToken = jwtTokenProvider.reissue(requestDto.getRefreshToken());
        return new TokenRefreshResponseDto(newAccessToken);
    }

    // 로그아웃
    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Long userId = SecurityUtils.currentUserIdOrThrow();
            
            // refreshToken 삭제
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            user.updateRefreshToken(null);
            userRepository.save(user);
            
            // accessToken 쿠키 삭제
            Cookie cookie = new Cookie("accessToken", null);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            
            // SecurityContext 클리어 -> 인증정보 제거
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // 이미 로그아웃된 상태 -> 쿠키만 삭제
            Cookie cookie = new Cookie("accessToken", null);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            
            return ResponseEntity.ok().build();
        }
    }
}
