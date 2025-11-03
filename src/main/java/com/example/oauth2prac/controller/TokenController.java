package com.example.oauth2prac.controller;

import com.example.oauth2prac.dto.TokenRefreshRequestDto;
import com.example.oauth2prac.dto.TokenRefreshResponseDto;
import com.example.oauth2prac.entity.JwtTokenProvider;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 개발용 토큰 발급
    // @Deprecated
    @GetMapping("/{userId}")
    public String generateToken(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        String accessToken=jwtTokenProvider.createToken(user.getId().toString(),user.getRoleKey());
        return accessToken;
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public TokenRefreshResponseDto refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {

        User user = userRepository.findByRefreshToken(requestDto.getRefreshToken()).orElseThrow();
        String newAccessToken = jwtTokenProvider.reissue(requestDto.getRefreshToken());
        return new TokenRefreshResponseDto(newAccessToken);
    }
}
