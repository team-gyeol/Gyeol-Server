package com.example.oauth2prac.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
@Component

public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_VALIDITY = 1000L*60*60;
    private static final long REFRESH_TOKEN_VALIDITY = 1000*60*60*24*14;

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String createAccessToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(key,io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role").toString();
    }

    public String reissue(String refreshToken) {
        if(!validateToken(refreshToken)) {
            throw new JwtException("Invalid refresh token");
        }

        String userId = getUserId(refreshToken);
        String role = getRole(refreshToken);

        return createAccessToken(userId, role);
    }

    public Authentication getAuthentication(String token) {

        // 1. 토큰에서 사용자 ID (principal)를 가져옵니다.
        String userId = getUserId(token);

        // 2. 토큰에서 "role" 클레임 (authorities)을 가져옵니다.
        String role = getRole(token);

        // 3. "role" 문자열을 Spring Security가 인식하는 GrantedAuthority 객체로 변환합니다.
        //    여기서는 단일 역할을 가정하므로 Collections.singletonList를 사용합니다.
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        // 4. principal, credentials(null), authorities를 사용하여
        //    UsernamePasswordAuthenticationToken (Authentication의 구현체)을 생성합니다.
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}