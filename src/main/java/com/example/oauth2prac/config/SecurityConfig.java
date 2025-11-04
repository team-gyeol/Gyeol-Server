package com.example.oauth2prac.config;

import com.example.oauth2prac.config.oauth2.OAuthAttributes;
import com.example.oauth2prac.entity.*;
import com.example.oauth2prac.repository.UserRepository;
import com.example.oauth2prac.service.CustomOAuth2UserService;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**","/swagger-ui/**","/v3/api-docs/**", "/api/token/**").permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .oauth2Login(oauth -> oauth
                        .loginPage("/")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            String registrationId = ((OAuth2AuthenticationToken)  authentication).getAuthorizedClientRegistrationId();

                            OAuthAttributes attributes = OAuthAttributes.of(registrationId,"sub",oAuth2User.getAttributes());

                            User user = userRepository.findByEmail(attributes.getEmail()).orElseThrow(() -> new IllegalArgumentException("해당 이메일이 존재하지 않습니다."));

                            String token = jwtTokenProvider.createToken(user.getId().toString(), user.getRoleKey());
                            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId().toString());

                            String oauthId = attributes.getNameAttributeKey();

                            if(oauthId.equals("id")){
                                oauthId = "kakao";
                            }
                            else {
                                oauthId = "Google";
                            }

                            user.setOauthId(oauthId);
                            user.updateRefreshToken(refreshToken);
                            userRepository.save(user);

                            Cookie cookie = new Cookie("accessToken", token);
                            cookie.setHttpOnly(true);
                            cookie.setPath("/");
                            cookie.setMaxAge(60 * 60);
                            response.addCookie(cookie);

                            response.sendRedirect("/success");
                        })
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}