package com.example.oauth2prac.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse DTO 테스트")
class ErrorResponseTest {

    @Test
    @DisplayName("HttpStatus.BAD_REQUEST로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithBadRequest() {
        // given
        String message = "Invalid input parameter";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("HttpStatus.INTERNAL_SERVER_ERROR로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithInternalServerError() {
        // given
        String message = "An unexpected error occurred";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("HttpStatus.NOT_FOUND로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithNotFound() {
        // given
        String message = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Not Found");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("HttpStatus.UNAUTHORIZED로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithUnauthorized() {
        // given
        String message = "Authentication required";
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getError()).isEqualTo("Unauthorized");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("HttpStatus.FORBIDDEN로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithForbidden() {
        // given
        String message = "Access denied";
        HttpStatus status = HttpStatus.FORBIDDEN;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(403);
        assertThat(errorResponse.getError()).isEqualTo("Forbidden");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("빈 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithEmptyMessage() {
        // given
        String message = "";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isEmpty();
        assertThat(errorResponse.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("null 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithNullMessage() {
        // given
        String message = null;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isNull();
        assertThat(errorResponse.getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("긴 에러 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithLongMessage() {
        // given
        String message = "This is a very long error message that contains detailed information about the error. " +
                "It includes stack traces, request details, and other debugging information that might be useful " +
                "for troubleshooting the issue. The message can be arbitrarily long.";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getMessage().length()).isGreaterThan(100);
    }

    @Test
    @DisplayName("특수 문자가 포함된 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithSpecialCharacters() {
        // given
        String message = "Error: Invalid JSON format - {\"field\": \"value\"} <script>alert('xss')</script>";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getMessage()).contains("{", "}", "<", ">", "'");
    }

    @Test
    @DisplayName("다양한 HTTP 상태 코드로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithVariousStatusCodes() {
        // given & when & then
        HttpStatus[] statuses = {
            HttpStatus.BAD_REQUEST,
            HttpStatus.UNAUTHORIZED,
            HttpStatus.FORBIDDEN,
            HttpStatus.NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED,
            HttpStatus.CONFLICT,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE
        };

        for (HttpStatus status : statuses) {
            ErrorResponse errorResponse = new ErrorResponse(status, "Test message");
            assertThat(errorResponse.getStatus()).isEqualTo(status.value());
            assertThat(errorResponse.getError()).isEqualTo(status.getReasonPhrase());
        }
    }

    @Test
    @DisplayName("timestamp가 생성 시점과 일치하는지 확인")
    void verifyTimestampIsCreatedAtConstructionTime() {
        // given
        LocalDateTime beforeCreation = LocalDateTime.now();

        // when
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, "Test");
        LocalDateTime afterCreation = LocalDateTime.now();

        // then
        assertThat(errorResponse.getTimestamp()).isAfterOrEqualTo(beforeCreation);
        assertThat(errorResponse.getTimestamp()).isBeforeOrEqualTo(afterCreation);
    }

    @Test
    @DisplayName("두 개의 ErrorResponse 인스턴스가 독립적인 timestamp를 가지는지 확인")
    void verifyIndependentTimestamps() throws InterruptedException {
        // given & when
        ErrorResponse first = new ErrorResponse(HttpStatus.BAD_REQUEST, "First");
        Thread.sleep(10); // 작은 지연을 추가하여 timestamp 차이 보장
        ErrorResponse second = new ErrorResponse(HttpStatus.BAD_REQUEST, "Second");

        // then
        assertThat(first.getTimestamp()).isNotEqualTo(second.getTimestamp());
        assertThat(first.getTimestamp()).isBefore(second.getTimestamp());
    }

    @Test
    @DisplayName("HttpStatus.OK로 ErrorResponse 생성 - 비정상적이지만 가능")
    void createErrorResponseWithOkStatus() {
        // given
        String message = "This is actually not an error";
        HttpStatus status = HttpStatus.OK;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(200);
        assertThat(errorResponse.getError()).isEqualTo("OK");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("한글 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithKoreanMessage() {
        // given
        String message = "잘못된 요청입니다. 입력값을 확인해주세요.";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("여러 줄 메시지로 ErrorResponse 생성 - 성공")
    void createErrorResponseWithMultiLineMessage() {
        // given
        String message = "Error occurred:\n" +
                "Line 1: Invalid parameter\n" +
                "Line 2: Missing required field\n" +
                "Line 3: Contact support";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // when
        ErrorResponse errorResponse = new ErrorResponse(status, message);

        // then
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getMessage()).contains("\n");
    }
}