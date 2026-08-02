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
                .doOnSuccess(r -> recordMl(mlSample, "success"))
                .doOnError(e -> recordMl(mlSample, "failure"))
                .flatMap(responseDto -> {
                    responseDto.parseAnalysisResult();

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
