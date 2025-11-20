package com.example.oauth2prac.domain.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponseDto {

    private String userName;
    private String userEmail;
    private String userPicture;
    public static MyPageResponseDto from(User user){
        return MyPageResponseDto.builder()
                .userName(user.getName())
                .userEmail(user.getEmail())
                .userPicture(user.getPicture())
                .build();
    }
}