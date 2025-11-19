package com.example.oauth2prac.domain.user;

import com.example.oauth2prac.domain.segmentedImage.SegmentationImageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "사용자 기본 정보 가져오기")
    @GetMapping
    public ResponseEntity<MyPageResponseDto> getMyPageInfo() {
        MyPageResponseDto responseDto = myPageService.getMyPageInfo();
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "분석한 이미지'들'보기")
    @GetMapping("/images")
    public ResponseEntity<Page<SegmentationImageResponseDto>> getMySegmentationImages(@RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<SegmentationImageResponseDto> responseDto = myPageService.getMySegmentationImages(pageable);
        return ResponseEntity.ok(responseDto);

    }

    @Operation(summary = "분석한 이미지 한 장 보기")
    @GetMapping("/image")
    public ResponseEntity<SegmentationImageResponseDto> getMySegmentationImage(@RequestParam Long imageId) {
        SegmentationImageResponseDto responseDto = myPageService.getMySegmentationImage(imageId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "분석 이미지 삭제(원본)")
    @DeleteMapping("/image/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        myPageService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }
}