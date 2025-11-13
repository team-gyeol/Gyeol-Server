# Gyeol (결): 드론 부품 자동 인식 세그멘테이션 시스템

## 프로젝트 개요

**Gyeol** 프로젝트는 드론 대여/정비 사업 현장에서 발생하는 수작업 검사의 비효율성과 인적 오류를 해결하기 위해 개발되었습니다.

AI 기반 **Mask R-CNN** 세그멘테이션 기술을 활용하여 드론 이미지 내 주요 부품 (**프로폴레, 본체, 카메라, 다리**) 를 자동으로 인식하고 시각화합니다.

백엔드는 **Spring Boot** 와 **FastAPI**의 마이크로서비스 아키토체로 분리되어, 인증 및 데이터 관리의 안정성과 AI 모델 추론의 효율성을 동시에 확보했습니다.

---

## 문제 해결 및 목표

| 문제 정의                                                           | 해결책                                                           | 핵심 목표                                                  |
| :-------------------------------------------------------------- | :------------------------------------------------------------ | :----------------------------------------------------- |
| 드론 부품 손상 여부를 사람이 육안으로 판독하여 시간 소요가 크고, 검사자의 숙련도에 따라 결과의 일관성이 떨어짐 | AI 기반 이미지 세그멘테이션을 통해 부품별 영역을 정무하게 분할하고, 손상 여부를 자동 식별하는 시스템 구축 | Mask R-CNN 모델 최적화 및 FastAPI-Spring 기반의 실시간 웹 데모 서비스 구현 |

---

## 시스템 아키텍처

본 프로젝트는 **Spring Boot (메인 서버)** 와 **FastAPI (AI 서버)**로 분리된 마이크로서비스 아키텍처를 채택하며, 모든 이미지 데이터는 **Google Cloud Storage (GCS)**를 중심으로 후복됩니다.

### **Spring Server**

* 사용자 인증, 이미지 GCS 업로드 및 URL 획득
* FastAPI 비동기 요청 전송
* 최종 분석 결과 저장 및 마이페이지 제공

### **FastAPI Server**

* Spring으로부터 이미지 URL을 받아 GCS에서 다운로드
* Mask R-CNN 모델을 통해 분석 수행
* 결과 이미지를 다시 GCS에 업로드 후 URL을 Spring에 반환

---

## 기술 스택

| 프로젝트                     | Gyeol-Server (Spring Boot)                                 | Gyeol-AI (FastAPI)                         |
| :----------------------- | :--------------------------------------------------------- | :----------------------------------------- |
| **언어/플랫폼**         | Java 17, Spring Boot 3.0, Google Cloud Run, GitHub Actions | Python, FastAPI, PyTorch, Google Cloud Run |
| **인증/보안**            | Spring Security, OAuth2 (Google/Kakao), JWT                | -                                          |
| **데이터**              | MySQL (Cloud SQL), Spring Data JPA                         | NumPy, Pillow                              |
| **소프트웨어/데이터 저장** | Google Cloud Storage (GCS)                                 | Google Cloud Storage (GCS)                 |
| **통신**               | WebFlux (WebClient)                                        | requests                                   |
| **AI 모델**                | -                                                          | Mask R-CNN (Object/Instance Segmentation)  |

---

## 주요 기능 상세

### 인증 및 사용자 관리 (Gyeol-Server)

* **소셜 로그인**: Google 및 Kakao OAuth 2.0을 통해 간편 로그인 지원
* **JWT & Refresh Token**: Access Token은 HTTP Only Cookie로, Refresh Token은 DB에 저장하여 안전하게 토큰 재발급 관리
* **로그아웃**: `/api/token/logout`에서 Cookie 및 DB Refresh Token을 삭제하여 세션을 안전하게 종료

### 이미지 처리 및 AI 연동 (Gyeol-Server & Gyeol-AI)

* **GCS 통합**: 모든 원본 및 분석 이미지는 GCS에 저장, DB에는 URL만 관리
* **비동기 분석 요청**: Spring WebClient(WebFlux)를 사용하여 FastAPI 서버에 분석 요청을 비동기로 처리
* **멀티 이미지 분석**: 다중 이미지 업로드를 지원, Reactor Flux를 이용해 병렬 처리 효율성 극복

### 마이페이지 및 기록 조회

* **사용자 정보**: `/api/mypage` 에서 로그인된 사용자 정보 조회
* **분석 기록**: `/api/mypage/images` 에서 분석된 이미지 목록 페이지네이션 기능 제공

---

## 주요 API 엔드포인트

| HTTP Method | 경로                           | 설명                                    | 인증 |
| :---------- | :--------------------------- | :------------------------------------ | :- |
| GET         | /                            | 소셜 로그인 페이지 (HTML)                     | ❌  |
| POST        | /api/token/refresh           | Access Token 재발급 (Refresh Token 필요)   | ❌  |
| POST        | /api/token/logout            | 로그아웃 처리                               | ⭕  |
| POST        | /api/images/analyze          | 단일 이미지 업로드 및 분석 요청                    | ⭕  |
| POST        | /api/images/analyze-multiple | 다중 이미지 업로드 및 병렬 분석 요청                 | ⭕  |
| GET         | /api/mypage                  | 사용자 기본 정보 조회                          | ⭕  |
| GET         | /api/mypage/images           | 분석 기록 목록 조회 (Pagination)              | ⭕  |
| POST        | /analyze-url                 | (FastAPI) 이미지 URL을 받아 분석 후 GCS URL 반환 | ❌  |

---

## 배포 (CI/CD)

프로젝트는 **GitHub Actions** 과 **Jib** 을 사용하여 컨테이너화되어 **Google Cloud Run** 에 배포됩니다.

* **Jib 플러그인**: Dockerfile 없이 Spring Boot 애플리케이션을 OCI 이미지로 빌드 및 GCR(Google Container Registry)에 푸시
* **GitHub Actions (`dev_deploy.yml`)**: main 브랜치에 Push 발생 시, GCP 인증 후 Jib을 실행, Cloud Run에 배포 시 환경 변수 및 Secret Manager에 저장된 민감 정보를 주입 (Cloud SQL 연동 포함)

---
