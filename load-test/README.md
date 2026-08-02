# 부하 테스트 / 성능 계측

Gyeol-Server 백엔드의 병목을 재현 가능하게 측정하기 위한 로컬 스택과 k6 시나리오.

## 왜 로컬인가

실제 Cloud Run + Cloud SQL + GCS 로 측정하면 네트워크 지연·오토스케일·과금이 섞여
"백엔드 코드를 고쳐서 얼마나 좋아졌는가"를 분리할 수 없다. 그래서 로컬에서는
외부 의존성을 **상수로 고정**하고 백엔드 자체 병목만 본다.

| 의존성 | 로컬 | 이유 |
|---|---|---|
| FastAPI ML 서버 | WireMock (고정 지연 800ms) | 추론 시간을 상수로 고정해야 백엔드 개선분이 보인다 |
| GCS | fake-gcs-server 에뮬레이터 | 인터넷 왕복 지연 제거 + 부하 테스트가 실버킷을 오염시키지 않음 |
| Cloud SQL | 로컬 MySQL 8 | 동일 |

실환경 e2e 수치는 별도로 Cloud Run 에 저부하로 한 번 측정한다(B축).

## 필요 도구

```bash
brew install k6
```

Docker Desktop, JDK 17 (Gradle 툴체인이 자동 선택).

## 실행 순서

### 1. 스택 기동

```bash
docker compose up -d
```

| 서비스 | 주소 |
|---|---|
| MySQL | `localhost:3308` (gyeol/gyeol) |
| WireMock (ML 스텁) | `localhost:8081` |
| fake-gcs-server | `localhost:4443` |
| Prometheus | `localhost:9090` |
| Grafana | `localhost:3001` (admin/admin) |

### 2. 애플리케이션 기동

```bash
./load-test/restart-app.sh
```

`bootRun` 은 Gradle 데몬을 끼고 있어 반복 재시작이 불안정하다. 이 스크립트는
jar 를 빌드해 직접 띄우고, 헬스체크가 통과할 때까지 기다린다.
설정만 바꿨을 때는 `SKIP_BUILD=1 ./load-test/restart-app.sh`.

개선 전 설정으로 되돌려 비교하려면 환경변수로 덮어쓴다:

```bash
ML_MAX_CONNECTIONS=16 DB_POOL_SIZE=10 SKIP_BUILD=1 ./load-test/restart-app.sh
```

### 3. 테스트 이미지 생성

이미지는 git 에 넣지 않는다. 시드가 고정돼 있어 언제든 동일하게 재생성된다.

```bash
python3 load-test/make-images.py
```

### 4. 테스트 사용자 시딩

스키마는 앱이 한 번 뜨면서 `ddl-auto: update` 로 생성된다. 그 다음:

```bash
./load-test/seed-users.sh 50
```

### 5. ML 스텁 지연 설정

```bash
./load-test/set-mock-delay.sh 800
```

`200` / `800` / `3000` 으로 바꿔가며 업스트림 지연 민감도를 실험할 수 있다.
**before/after 비교 시에는 반드시 같은 값을 쓸 것.**

### 6. 시나리오 실행

```bash
./load-test/run.sh 01-smoke        baseline   # 먼저 경로 확인
./load-test/run.sh 02-load-analyze baseline   # 핵심 시나리오
```

두 번째 인자는 결과 라벨이다 (`baseline`, `after-b4`, `after-all` …).
결과는 `load-test/results/<라벨>/` 에 저장된다:

| 파일 | 내용 |
|---|---|
| `*.txt` | k6 콘솔 출력 |
| `*.summary.json` | k6 요약 지표 |
| `*.raw.json` | 원본 시계열 (gitignore) |
| `*.metrics.tsv` | 부하 중 10초 간격 병목 메트릭 시계열 |
| `*.bottleneck.txt` | 정상 상태 구간의 병목 메트릭 요약 — **개선 근거의 핵심** |
| `*.env.txt` | 실행 조건 (커밋 해시, ML 지연, 하드웨어) — 비교 신뢰성의 근거 |

부하가 끝난 뒤 한 번 찍으면 VU 가 이미 0 이라 아무것도 안 보인다.
그래서 실행 중에 계속 샘플링한다.

## 시나리오

| 파일 | 목적 | 프로파일 |
|---|---|---|
| `01-smoke.js` | 경로 정상 동작 확인 | 1 VU × 30s |
| `02-load-analyze.js` | **핵심** — 이미지 분석 지속 부하 | 0→50 VU, 5분 유지 |
| `03-stress-analyze.js` | 한계점 탐색 | 25→50→100→200 VU 계단식 |
| `04-load-mypage.js` | 읽기 경로 (쿼리/인덱스) | 0→100 VU, 3분 |
| `05-spike.js` | 급증 시 회복 능력 | 5→100→5 VU |

이미지 크기 변경:

```bash
IMAGE_SIZE=medium ./load-test/run.sh 02-load-analyze baseline-medium
```

`small` 145KB / `medium` 1.0MB / `large` 4.5MB.

## 관측

부하를 거는 동안 Grafana → **Gyeol / Gyeol Backend — 성능/병목** 대시보드를 본다.
패널은 가설과 1:1로 대응한다:

| 패널 | 확인하려는 것 |
|---|---|
| Tomcat 스레드 (busy vs max) | 서블릿 스레드가 블로킹 I/O 로 포화됐는가 |
| Reactor Netty 커넥션 풀 | ML 호출이 커넥션을 못 얻고 대기하는가 |
| HikariCP 커넥션 풀 | DB 커넥션 대기가 발생하는가 |
| 구간별 소요시간 | 전체 지연 중 GCS / ML / DB 각각의 기여분 |
| Heap · GC pause | GC 압박이 지연에 기여하는가 |

## 실제 GCS / 실제 ML 서버로 측정하려면

```bash
# 실제 GCS (gcloud auth application-default login 필요)
GCS_EMULATOR_HOST= ./gradlew bootRun --args='--spring.profiles.active=local'

# 실제 ML 서버
FASTAPI_URL=https://gyeol-ml-....run.app ./gradlew bootRun --args='--spring.profiles.active=local'
```

실제 ML 서버는 Cloud Run 과금이 발생하므로 저부하·단시간으로만 돌릴 것.

## 측정 신뢰성 주의사항

- k6 와 애플리케이션이 같은 머신에서 돌면 CPU 를 경합한다. VU 를 무리하게 올리지 말고,
  리포트에 이 한계를 명시할 것.
- `show-sql` 은 `local` 프로파일에서 꺼져 있다. 로그 I/O 가 측정을 왜곡한다.
- 각 시나리오는 `setup()` 에서 워밍업을 돌려 JIT/커넥션 수립 비용을 제외한다.
- 비교용 수치는 **3회 실행의 중앙값**을 쓸 것.
