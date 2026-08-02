package com.example.oauth2prac.domain.segmentedImage;

import com.example.oauth2prac.domain.originalImage.OriginalImage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세그멘테이션 결과 저장만 담당한다.
 * <p>
 * 별도 빈으로 분리한 이유: {@code @Transactional} 은 프록시 기반이라
 * 같은 클래스 안에서 호출하면(self-invocation) 적용되지 않는다.
 * {@link ImageProcessingService} 의 리액티브 체인에서 이 빈을 호출해야
 * 저장이 실제 트랜잭션 안에서 일어난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SegmentedImageWriter {

    private final SegmentedImageRepository segmentedImageRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public SegmentedImage save(OriginalImage originalImage, SegmentationResponseDTO responseDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SegmentedImage segmentedImage = SegmentedImage.builder()
                    .user(originalImage.getUser())
                    .originalImage(originalImage)
                    .imageUrl(responseDto.getSegmentedImageUrl())
                    .analysisResult(responseDto.getAnalysisResult())
                    .multicopterBodyCount(responseDto.getMulticopterBodyCount())
                    .propellerCount(responseDto.getPropellerCount())
                    .cameraCount(responseDto.getCameraCount())
                    .legCount(responseDto.getLegCount())
                    .build();

            return segmentedImageRepository.save(segmentedImage);
        } finally {
            sample.stop(meterRegistry.timer("segmentation.persist"));
        }
    }
}
