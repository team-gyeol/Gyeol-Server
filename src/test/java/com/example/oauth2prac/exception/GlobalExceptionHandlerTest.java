package com.example.oauth2prac.exception;

import com.example.oauth2prac.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 400 Bad Request")
    void handleWebClientResponseException_BadRequest() {
        // given
        String errorBody = "{\"error\": \"Invalid request\"}";
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                null,
                errorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).contains("FastAPI server error:");
        assertThat(response.getBody().getMessage()).contains(errorBody);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 500 Internal Server Error")
    void handleWebClientResponseException_InternalServerError() {
        // given
        String errorBody = "{\"detail\": \"Internal server error occurred\"}";
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                null,
                errorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).startsWith("FastAPI server error:");
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 404 Not Found")
    void handleWebClientResponseException_NotFound() {
        // given
        String errorBody = "Resource not found";
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                null,
                errorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains(errorBody);
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 빈 응답 본문")
    void handleWebClientResponseException_EmptyBody() {
        // given
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway",
                null,
                new byte[0],
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(502);
        assertThat(response.getBody().getMessage()).startsWith("FastAPI server error:");
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 한글 에러 메시지")
    void handleWebClientResponseException_KoreanErrorMessage() {
        // given
        String errorBody = "{\"error\": \"이미지 처리 중 오류가 발생했습니다.\"}";
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                null,
                errorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(errorBody);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - 기본 케이스")
    void handleIllegalArgumentException_BasicCase() {
        // given
        String errorMessage = "Invalid argument provided";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - null 메시지")
    void handleIllegalArgumentException_NullMessage() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException((String) null);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNull();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - 빈 메시지")
    void handleIllegalArgumentException_EmptyMessage() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEmpty();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - 상세한 에러 메시지")
    void handleIllegalArgumentException_DetailedMessage() {
        // given
        String errorMessage = "숫자 파싱 실패: For input string: 'abc'";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Exception 처리 - 일반적인 예외")
    void handleException_GenericException() {
        // given
        String errorMessage = "Something went wrong";
        Exception exception = new Exception(errorMessage);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("An unexpected error occurred:");
        assertThat(response.getBody().getMessage()).contains(errorMessage);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Exception 처리 - RuntimeException")
    void handleException_RuntimeException() {
        // given
        String errorMessage = "Runtime error occurred";
        RuntimeException exception = new RuntimeException(errorMessage);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(errorMessage);
    }

    @Test
    @DisplayName("Exception 처리 - NullPointerException")
    void handleException_NullPointerException() {
        // given
        NullPointerException exception = new NullPointerException("Null value encountered");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).startsWith("An unexpected error occurred:");
    }

    @Test
    @DisplayName("Exception 처리 - null 메시지가 있는 예외")
    void handleException_WithNullMessage() {
        // given
        Exception exception = new Exception((String) null);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("An unexpected error occurred:");
    }

    @Test
    @DisplayName("Exception 처리 - 긴 에러 메시지")
    void handleException_LongMessage() {
        // given
        String longMessage = "A".repeat(1000);
        Exception exception = new Exception(longMessage);

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(longMessage);
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 다양한 상태 코드")
    void handleWebClientResponseException_VariousStatusCodes() {
        // given & when & then
        int[] statusCodes = {400, 401, 403, 404, 500, 502, 503};

        for (int statusCode : statusCodes) {
            WebClientResponseException exception = WebClientResponseException.create(
                    statusCode,
                    HttpStatus.valueOf(statusCode).getReasonPhrase(),
                    null,
                    "Error".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

            assertThat(response.getStatusCode().value()).isEqualTo(statusCode);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(statusCode);
        }
    }

    @Test
    @DisplayName("WebClientResponseException 처리 - 특수 문자가 포함된 응답 본문")
    void handleWebClientResponseException_SpecialCharacters() {
        // given
        String errorBody = "{\"error\": \"<script>alert('xss')</script>\", \"code\": \"ERR_001\"}";
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                null,
                errorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWebClientResponseException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains(errorBody);
    }

    @Test
    @DisplayName("예외 핸들러가 올바른 ErrorResponse 구조를 반환하는지 확인")
    void verifyErrorResponseStructure() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Test error");

        // when
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = response.getBody();
        assertThat(body.getStatus()).isNotZero();
        assertThat(body.getError()).isNotNull().isNotEmpty();
        assertThat(body.getMessage()).isNotNull();
        assertThat(body.getTimestamp()).isNotNull();
    }
}