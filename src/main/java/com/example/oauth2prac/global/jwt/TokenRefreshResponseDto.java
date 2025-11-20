// /Users/ryong/Desktop/Ryong/Oauth2Prac/src/main/java/com/example/oauth2prac/dto/TokenRefreshResponseDto.java (새 파일)
package com.example.oauth2prac.global.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResponseDto {
    private String accessToken;
}