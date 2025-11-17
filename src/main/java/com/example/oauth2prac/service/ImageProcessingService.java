package com.example.oauth2prac.service;

import com.example.oauth2prac.dto.SegmentationRequestDto;
import com.example.oauth2prac.dto.SegmentationResponseDTO;
import com.example.oauth2prac.entity.OriginalImage;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    private final WebClient webClient;
    private final SegmentedImageRepository segmentedImageRepository;

    @Transactional
    public Mono<SegmentationResponseDTO> requestSegmentation(OriginalImage originalImage) {

        SegmentationRequestDto requestDto = new SegmentationRequestDto(originalImage.getImageUrl());

        log.info("Sending segmentation request to FastAPI for image: {}", originalImage.getImageUrl());

        return webClient.post()
                .uri("/analyze-url")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(SegmentationResponseDTO.class)
                .doOnError(WebClientResponseException.class, error -> {
                    log.error("FastAPI 요청 실패: {} - {}", error.getStatusCode(), error.getResponseBodyAsString());
                })
                .doOnError(Exception.class, error -> {
                    log.error("예상치 못한 오류 발생: {}", error.getMessage(), error);
                })
                .flatMap(responseDto -> {
                    responseDto.parseAnalysisResult();
                    
                    log.info("Analysis result parsed - Body: {}, Propeller: {}, Camera: {}, Leg: {}", 
                            responseDto.getMulticopterBodyCount(),
                            responseDto.getPropellerCount(),
                            responseDto.getCameraCount(),
                            responseDto.getLegCount());

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

                    segmentedImageRepository.save(segmentedImage);
                    log.info("Segmentation result saved successfully for image ID: {}", originalImage.getId());

                    return Mono.just(responseDto);
                });
    }
}