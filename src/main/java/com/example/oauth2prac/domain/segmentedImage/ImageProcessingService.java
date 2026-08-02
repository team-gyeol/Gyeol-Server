package com.example.oauth2prac.domain.segmentedImage;

import com.example.oauth2prac.domain.originalImage.OriginalImage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final WebClient webClient;
    private final SegmentedImageWriter segmentedImageWriter;
    private final MeterRegistry meterRegistry;

    /**
     * 이 메서드에는 {@code @Transactional} 을 붙이지 않는다.
     * <p>
     * 붙여봤자 메서드가 {@link Mono} 를 "조립해서 즉시 반환"하는 시점에 트랜잭션이
     * 열렸다 곧바로 커밋된다. 정작 실제 DB 저장은 나중에 구독 시점에 다른 스레드에서
     * 일어나므로 그 트랜잭션 밖이다. 즉 커넥션만 한 번 잡았다 놓을 뿐 아무 효과가 없었다.
     * 저장의 트랜잭션 경계는 {@link SegmentedImageWriter} 가 책임진다.
     */
    public Mono<SegmentationResponseDTO> requestSegmentation(OriginalImage originalImage) {

        SegmentationRequestDto requestDto = new SegmentationRequestDto(originalImage.getImageUrl());

        log.debug("Sending segmentation request to FastAPI for image: {}", originalImage.getImageUrl());

        Timer.Sample mlSample = Timer.start(meterRegistry);

        return webClient.post()
                .uri("/analyze-url")
                .bodyValue(requestDto)
                .retrieve()
                // JSON -> DTO
                .bodyToMono(SegmentationResponseDTO.class)
                // ML 호출 구간만 따로 계측 (전체 응답시간 중 기여분 분해용)
                .doOnSuccess(r -> recordMl(mlSample, "success"))
                .doOnError(e -> recordMl(mlSample, "failure"))
                // 응답 받은 이후
                .flatMap(responseDto -> {
                    responseDto.parseAnalysisResult();

                    // JPA 저장은 블로킹 호출이다. 여기는 Netty 이벤트 루프 스레드이므로
                    // 그대로 호출하면 이벤트 루프가 DB I/O 동안 멈춰 다른 요청의 응답
                    // 처리까지 막는다. 반드시 별도 스케줄러로 밀어낸다.
                    return Mono.fromCallable(() -> segmentedImageWriter.save(originalImage, responseDto))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(saved -> log.debug(
                                    "Segmentation result saved successfully for image ID: {}",
                                    originalImage.getId()))
                            .thenReturn(responseDto);
                })
                .doOnError(error -> log.warn(
                        "Segmentation failed for original image ID {}: {}",
                        originalImage.getId(),
                        error.getMessage()
                ));
    }

    private void recordMl(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("ml.segmentation")
                .description("FastAPI 세그멘테이션 호출 소요시간")
                .tag("outcome", outcome)
                .register(meterRegistry));
    }
}
