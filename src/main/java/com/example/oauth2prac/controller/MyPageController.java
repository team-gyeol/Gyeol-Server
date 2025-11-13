package com.example.oauth2prac.controller;

import com.example.oauth2prac.dto.MyPageResponseDto;
import com.example.oauth2prac.dto.SegmentationImageResponseDto;
import com.example.oauth2prac.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping
    public ResponseEntity<MyPageResponseDto> getMyPageInfo() {
        MyPageResponseDto responseDto = myPageService.getMyPageInfo();
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/images")
    public ResponseEntity<Page<SegmentationImageResponseDto>> getMySegmentationImages(@RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<SegmentationImageResponseDto> responseDto = myPageService.getMySegmentationImages(pageable);
        return ResponseEntity.ok(responseDto);

    }

    @GetMapping("/image")
    public ResponseEntity<SegmentationImageResponseDto> getMySegmentationImage(@RequestParam Long imageId) {
        SegmentationImageResponseDto responseDto = myPageService.getMySegmentationImage(imageId);
        return ResponseEntity.ok(responseDto);
    }
    
    @DeleteMapping("/image/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        myPageService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }
}