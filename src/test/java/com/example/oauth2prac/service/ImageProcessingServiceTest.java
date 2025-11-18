package com.example.oauth2prac.service;

import com.example.oauth2prac.dto.SegmentationResponseDTO;
import com.example.oauth2prac.entity.OriginalImage;
import com.example.oauth2prac.entity.SegmentedImage;
import com.example.oauth2prac.entity.User;
import com.example.oauth2prac.repository.SegmentedImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageProcessingService 테스트")
class ImageProcessingServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private SegmentedImageRepository segmentedImageRepository;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ImageProcessingService imageProcessingService;

    private User testUser;
    private OriginalImage testOriginalImage;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testOriginalImage = OriginalImage.builder()
                .id(1L)
                .user(testUser)
                .imageUrl("https://storage.example.com/test-image.jpg")
                .fileName("test-image.jpg")
                .build();
    }

    @Test
    @DisplayName("세그멘테이션 요청 성공 - 모든 객체가 탐지됨")
    void requestSegmentation_Success_AllObjectsDetected() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented-image.jpg");
        mockResponse.setAnalysisResult("multicopter_body:2개, propeller:4개, camera:1개, leg:4개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getSegmentedImageUrl()).isEqualTo("https://storage.example.com/segmented-image.jpg");
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(2);
                    assertThat(response.getPropellerCount()).isEqualTo(4);
                    assertThat(response.getCameraCount()).isEqualTo(1);
                    assertThat(response.getLegCount()).isEqualTo(4);
                })
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/analyze-url");
        verify(segmentedImageRepository).save(any(SegmentedImage.class));
    }

    @Test
    @DisplayName("세그멘테이션 요청 성공 - 객체가 탐지되지 않음")
    void requestSegmentation_Success_NoObjectsDetected() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented-image.jpg");
        mockResponse.setAnalysisResult("탐지된 객체가 없습니다.");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(0);
                    assertThat(response.getPropellerCount()).isEqualTo(0);
                    assertThat(response.getCameraCount()).isEqualTo(0);
                    assertThat(response.getLegCount()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("세그멘테이션 요청 성공 - 일부 객체만 탐지됨")
    void requestSegmentation_Success_PartialObjectsDetected() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented-image.jpg");
        mockResponse.setAnalysisResult("multicopter_body:1개, propeller:2개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(1);
                    assertThat(response.getPropellerCount()).isEqualTo(2);
                    assertThat(response.getCameraCount()).isEqualTo(0);
                    assertThat(response.getLegCount()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("SegmentedImage가 올바른 데이터로 저장되는지 확인")
    void requestSegmentation_VerifySegmentedImageSaved() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("multicopter_body:3개, propeller:6개, camera:2개, leg:4개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result).expectNextCount(1).verifyComplete();

        ArgumentCaptor<SegmentedImage> captor = ArgumentCaptor.forClass(SegmentedImage.class);
        verify(segmentedImageRepository).save(captor.capture());

        SegmentedImage savedImage = captor.getValue();
        assertThat(savedImage.getUser()).isEqualTo(testUser);
        assertThat(savedImage.getOriginalImage()).isEqualTo(testOriginalImage);
        assertThat(savedImage.getImageUrl()).isEqualTo("https://storage.example.com/segmented.jpg");
        assertThat(savedImage.getMulticopterBodyCount()).isEqualTo(3);
        assertThat(savedImage.getPropellerCount()).isEqualTo(6);
        assertThat(savedImage.getCameraCount()).isEqualTo(2);
        assertThat(savedImage.getLegCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("WebClient 호출 시 올바른 URI 사용 확인")
    void requestSegmentation_VerifyCorrectURI() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("탐지된 객체가 없습니다.");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
        verify(requestBodyUriSpec).uri("/analyze-url");
    }

    @Test
    @DisplayName("WebClientResponseException 발생 시 에러 전파")
    void requestSegmentation_WebClientResponseException() {
        // given
        WebClientResponseException exception = WebClientResponseException.create(
                500,
                "Internal Server Error",
                null,
                "FastAPI error".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.error(exception));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();

        verify(segmentedImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("네트워크 타임아웃 발생 시 에러 전파")
    void requestSegmentation_NetworkTimeout() {
        // given
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("분석 결과가 null인 경우 처리")
    void requestSegmentation_NullAnalysisResult() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult(null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(0);
                    assertThat(response.getPropellerCount()).isEqualTo(0);
                    assertThat(response.getCameraCount()).isEqualTo(0);
                    assertThat(response.getLegCount()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("분석 결과가 빈 문자열인 경우 처리")
    void requestSegmentation_EmptyAnalysisResult() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(0);
                    assertThat(response.getPropellerCount()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("큰 객체 개수가 포함된 분석 결과 처리")
    void requestSegmentation_LargeObjectCounts() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("multicopter_body:10개, propeller:40개, camera:5개, leg:20개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(10);
                    assertThat(response.getPropellerCount()).isEqualTo(40);
                    assertThat(response.getCameraCount()).isEqualTo(5);
                    assertThat(response.getLegCount()).isEqualTo(20);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("repository save 실패 시 에러 전파")
    void requestSegmentation_RepositorySaveFailure() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("multicopter_body:1개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("여러 이미지에 대한 순차 처리")
    void requestSegmentation_MultipleImages() {
        // given
        OriginalImage image1 = OriginalImage.builder()
                .id(1L)
                .user(testUser)
                .imageUrl("https://storage.example.com/image1.jpg")
                .fileName("image1.jpg")
                .build();

        OriginalImage image2 = OriginalImage.builder()
                .id(2L)
                .user(testUser)
                .imageUrl("https://storage.example.com/image2.jpg")
                .fileName("image2.jpg")
                .build();

        SegmentationResponseDTO response1 = new SegmentationResponseDTO();
        response1.setSegmentedImageUrl("https://storage.example.com/segmented1.jpg");
        response1.setAnalysisResult("multicopter_body:1개");

        SegmentationResponseDTO response2 = new SegmentationResponseDTO();
        response2.setSegmentedImageUrl("https://storage.example.com/segmented2.jpg");
        response2.setAnalysisResult("multicopter_body:2개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class))
                .thenReturn(Mono.just(response1))
                .thenReturn(Mono.just(response2));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when & then
        StepVerifier.create(imageProcessingService.requestSegmentation(image1))
                .assertNext(response -> assertThat(response.getMulticopterBodyCount()).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(imageProcessingService.requestSegmentation(image2))
                .assertNext(response -> assertThat(response.getMulticopterBodyCount()).isEqualTo(2))
                .verifyComplete();

        verify(segmentedImageRepository, times(2)).save(any(SegmentedImage.class));
    }

    @Test
    @DisplayName("0개의 객체가 탐지된 경우")
    void requestSegmentation_ZeroObjectsDetected() {
        // given
        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("multicopter_body:0개, propeller:0개, camera:0개, leg:0개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getMulticopterBodyCount()).isEqualTo(0);
                    assertThat(response.getPropellerCount()).isEqualTo(0);
                    assertThat(response.getCameraCount()).isEqualTo(0);
                    assertThat(response.getLegCount()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("WebClient 400 Bad Request 응답 처리")
    void requestSegmentation_BadRequest() {
        // given
        WebClientResponseException exception = WebClientResponseException.create(
                400,
                "Bad Request",
                null,
                "Invalid image URL".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.error(exception));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(testOriginalImage);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof WebClientResponseException &&
                        ((WebClientResponseException) throwable).getStatusCode().value() == 400
                )
                .verify();
    }

    @Test
    @DisplayName("이미지 URL이 매우 긴 경우")
    void requestSegmentation_VeryLongImageUrl() {
        // given
        String longUrl = "https://storage.example.com/" + "a".repeat(500) + ".jpg";
        OriginalImage longUrlImage = OriginalImage.builder()
                .id(1L)
                .user(testUser)
                .imageUrl(longUrl)
                .fileName("long-name-image.jpg")
                .build();

        SegmentationResponseDTO mockResponse = new SegmentationResponseDTO();
        mockResponse.setSegmentedImageUrl("https://storage.example.com/segmented.jpg");
        mockResponse.setAnalysisResult("multicopter_body:1개");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SegmentationResponseDTO.class)).thenReturn(Mono.just(mockResponse));
        when(segmentedImageRepository.save(any(SegmentedImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Mono<SegmentationResponseDTO> result = imageProcessingService.requestSegmentation(longUrlImage);

        // then
        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getMulticopterBodyCount()).isEqualTo(1))
                .verifyComplete();
    }
}