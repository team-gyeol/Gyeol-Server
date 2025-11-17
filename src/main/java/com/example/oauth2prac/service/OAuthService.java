package com.example.oauth2prac.service;

import com.example.oauth2prac.config.JwtTokenProvider;
import com.example.oauth2prac.dto.KakaoUserInfoResponseDto;
import com.example.oauth2prac.dto.LoginResponseDto;
import com.example.oauth2prac.entity.Role;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Transactional
    public Mono<LoginResponseDto> loginWithKakao(String code) {
        return getKakaoAccessToken(code)
                .flatMap(this::getKakaoUserInfo)
                .flatMap(this::saveOrUpdateUser)
                .map(this::generateTokens);
    }

    private Mono<String> getKakaoAccessToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoClientId);
        formData.add("client_secret", kakaoClientSecret); // client_secret 추가
        formData.add("redirect_uri", kakaoRedirectUri);
        formData.add("code", code);

        return webClient.post()
                .uri(kakaoTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"));
    }

    private Mono<KakaoUserInfoResponseDto> getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri(kakaoUserInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfoResponseDto.class);
    }

    private Mono<User> saveOrUpdateUser(KakaoUserInfoResponseDto userInfo) {
        String email = userInfo.getKakaoAccount().getEmail();
        String nickname = userInfo.getProperties().getNickname();
        String profileImage = userInfo.getProperties().getProfileImageUrl();

        return Mono.fromCallable(() -> userRepository.findByEmail(email)
                .map(user -> {
                    log.info("Existing user found: {}", user.getEmail());
                    // Optionally update user info here if needed
                    return user;
                })
                .orElseGet(() -> {
                    log.info("New user, creating and saving: {}", email);
                    User newUser = User.builder()
                            .email(email)
                            .name(nickname)
                            .picture(profileImage)
                            .role(Role.USER)
                            .build();
                    return userRepository.save(newUser);
                })).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private LoginResponseDto generateTokens(User user) {
        String accessToken = jwtTokenProvider.createToken(user.getId().toString(), user.getRoleKey());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId().toString());
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}