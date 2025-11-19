package com.example.oauth2prac.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청에서 토큰을 꺼냅니다. (헤더 or 쿠키)
        String token = resolveToken(request);

        // 2. 토큰이 존재하고, 유효한지 검사합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            log.debug("JWT 토큰이 유효합니다. 인증 정보를 SecurityContext에 저장합니다.");
            // 3. 토큰이 유효하면, 토큰에서 인증 정보(Authentication)를 가져옵니다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            // 4. SecurityContextHolder에 인증 정보를 저장합니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.debug("JWT 토큰이 없거나 유효하지 않습니다. URI: {}", request.getRequestURI());
        }

        // 5. 다음 필터로 요청을 전달합니다.
        filterChain.doFilter(request, response);
    }


    private String resolveToken(HttpServletRequest request) {

        // 1순위: (Swagger/Postman 테스트용) 'Authorization' 헤더에서 'Bearer' 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            log.debug("Authorization 헤더에서 Bearer 토큰을 찾았습니다.");
            return bearerToken.substring(7); // "Bearer " 다음 문자열 반환
        }

        // 2순위: (실제 React 앱용) 'accessToken' 쿠키에서 토큰 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    log.debug("accessToken 쿠키에서 토큰을 찾았습니다.");
                    return cookie.getValue();
                }
            }
        }

        log.trace("요청에서 JWT 토큰을 찾지 못했습니다.");
        return null;
    }
}
