package com.example.oauth2prac.global.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Slf4j
public class SecurityUtils {

    // private 생성자로 객체 생성을 막습니다.
    private SecurityUtils() { }

    public static String getCurrentUserIdString() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            log.debug("Security Context에 인증 정보(Authentication)가 없습니다.");
            return null;
        }

        // JwtTokenProvider에서 principal을 String (userId)으로 저장했습니다.
        if (authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }

        // "anonymousUser" 등 예외 처리
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("Principal이 anonymousUser입니다.");
            return null;
        }

        log.warn("Security principal의 타입이 예상한 String이 아닙니다: {}", authentication.getPrincipal().getClass());
        return null;
    }

    public static Long currentUserIdOrThrow() {
        String userIdString = getCurrentUserIdString();

        if (userIdString == null) {
            throw new IllegalStateException("Security Context에서 사용자 ID를 찾을 수 없습니다. (인증되지 않은 요청)");
        }

        try {
            // JwtTokenProvider에 저장된 String 타입의 ID를 Long으로 변환합니다.
            return Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            log.error("Security principal을 Long으로 파싱하는 데 실패했습니다: {}", userIdString, e);
            throw new IllegalStateException("저장된 사용자 ID가 숫자 형식이 아닙니다: " + userIdString);
        }
    }
}