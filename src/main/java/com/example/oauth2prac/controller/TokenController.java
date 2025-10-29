package com.example.oauth2prac.controller;

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

    @GetMapping("/{userId}")
    public String generateToken(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElseThrow();

        String accessToken=jwtTokenProvider.createToken(user.getId().toString(),user.getRoleKey());

        return accessToken;
    }

    @PostMapping("/{userId}")
    public String refreshToken(@PathVariable Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        String refreshToken = jwtTokenProvider.reissue(user.getRefreshToken());
        user.updateRefreshToken(refreshToken);

        return refreshToken;
    }
}
