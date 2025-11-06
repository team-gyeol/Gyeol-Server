package com.example.oauth2prac.dto;

import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

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