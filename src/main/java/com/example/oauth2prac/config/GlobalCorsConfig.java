package com.example.oauth2prac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");      // 모든 도메인 허용
        config.addAllowedHeader("*");      // 모든 헤더 허용
        config.addAllowedMethod("*");      // GET, POST, PUT, DELETE 등 모두 허용
        config.setAllowCredentials(true);  // 쿠키 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 모든 API에 적용
        return new CorsFilter(source);
    }
}
