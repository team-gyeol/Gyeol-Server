#!/usr/bin/env bash
# 시나리오를 실행하고, k6 요약과 병목 메트릭 스냅샷을 함께 저장한다.
#
#   ./load-test/run.sh 02-load-analyze baseline
#   ./load-test/run.sh 02-load-analyze after-b4
#
# 결과: load-test/results/<label>/<scenario>.{summary.json,txt,metrics.txt}
set -euo pipefail

SCENARIO="${1:?사용법: run.sh <시나리오명(확장자 제외)> <라벨>}"
LABEL="${2:?사용법: run.sh <시나리오명(확장자 제외)> <라벨>}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${ROOT}/load-test/results/${LABEL}"
APP_URL="${BASE_URL:-http://localhost:8080}"
mkdir -p "${OUT}"

echo "▶ 사전 점검"
curl -sf "${APP_URL}/actuator/health" >/dev/null || { echo "앱이 응답하지 않습니다: ${APP_URL}"; exit 1; }
curl -sf "http://localhost:8081/__admin/mappings" >/dev/null || echo "  (경고) WireMock 스텁이 없습니다 - 실제 ML 서버를 쓰는 중인가요?"

# GCS 에뮬레이터 초기화.
# 한 번 돌 때마다 수 GB 가 쌓여서, 비우지 않으면 다음 실행이 디스크/메모리 압박을 받는다.
# 매 실행을 같은 상태에서 시작시켜야 before/after 비교가 성립한다.
if docker ps --format '{{.Names}}' | grep -q '^gyeol-fake-gcs$'; then
  echo "▶ GCS 에뮬레이터 초기화"
  docker exec gyeol-fake-gcs sh -c 'rm -rf /storage/gyeol-bucket/originalImage 2>/dev/null || true'
  docker restart gyeol-fake-gcs >/dev/null
  until curl -sf "http://localhost:4443/storage/v1/b?project=gyeol-server" >/dev/null 2>&1; do sleep 1; done
fi

# 실행 조건을 결과와 함께 남긴다. 이게 없으면 before/after 비교를 신뢰할 수 없다.
{
  echo "실행시각: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "라벨: ${LABEL}"
  echo "시나리오: ${SCENARIO}"
  echo "대상: ${APP_URL}"
  echo "이미지 크기: ${IMAGE_SIZE:-small}"
  echo "git commit: $(git -C "${ROOT}" rev-parse --short HEAD 2>/dev/null || echo n/a)"
  echo "ML 스텁 지연: $(curl -sf http://localhost:8081/__admin/settings 2>/dev/null || echo n/a)"
  echo "호스트: $(uname -mrs)"
  echo "CPU 코어: $(sysctl -n hw.ncpu 2>/dev/null || nproc)"
} > "${OUT}/${SCENARIO}.env.txt"

# 실행 중 업로드 누적 방지.
# 한 번 돌면 수만 건(수 GB)이 쌓여 디스크를 채우고, 그 압박이 측정을 오염시킨다.
# ML 스텁은 업로드된 객체를 다시 읽지 않으므로 중간 삭제는 시나리오에 영향이 없다.
if docker ps --format '{{.Names}}' | grep -q '^gyeol-fake-gcs$'; then
  ( while true; do
      sleep 45
      docker exec gyeol-fake-gcs sh -c 'rm -rf /storage/gyeol-bucket/originalImage/* 2>/dev/null' || true
    done ) &
  PURGER_PID=$!
  trap 'kill ${PURGER_PID} 2>/dev/null || true' EXIT
fi

# 병목 메트릭을 부하가 걸린 동안 계속 샘플링한다.
# 종료 후 한 번 찍으면 VU 가 이미 0 이라 아무것도 안 보인다 - 실행 중 값이 핵심이다.
METRICS_TSV="${OUT}/${SCENARIO}.metrics.tsv"
printf 'ts\ttomcat_busy\tnetty_active\tnetty_pending\tnetty_max\thikari_active\thikari_pending\thikari_max\theap_used_mb\n' > "${METRICS_TSV}"
(
  while true; do
    snap=$(curl -s --max-time 3 "${APP_URL}/actuator/prometheus" || true)
    val() { echo "$snap" | grep -E "^$1" | head -1 | awk '{print $NF}'; }
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$(date +%H:%M:%S)" \
      "$(val 'tomcat_threads_busy_threads')" \
      "$(val 'reactor_netty_connection_provider_active_connections')" \
      "$(val 'reactor_netty_connection_provider_pending_connections')" \
      "$(val 'reactor_netty_connection_provider_max_connections')" \
      "$(val 'hikaricp_connections_active')" \
      "$(val 'hikaricp_connections_pending')" \
      "$(val 'hikaricp_connections_max')" \
      "$(echo "$snap" | grep '^jvm_memory_used_bytes.*area="heap"' | awk '{s+=$NF} END {printf "%.0f", s/1048576}')" \
      >> "${METRICS_TSV}"
    sleep 10
  done
) &
SAMPLER_PID=$!
trap 'kill ${SAMPLER_PID} ${PURGER_PID:-} 2>/dev/null || true' EXIT

echo "▶ k6 실행: ${SCENARIO} (라벨: ${LABEL})"
cd "${ROOT}/load-test"
# threshold 초과 시 k6 는 exit 99 로 끝난다. baseline 에서는 정상적인 결과이므로
# 여기서 스크립트를 죽이면 안 된다.
set +e
k6 run \
  --summary-export "${OUT}/${SCENARIO}.summary.json" \
  --out "json=${OUT}/${SCENARIO}.raw.json" \
  "${SCENARIO}.js" 2>&1 | tee "${OUT}/${SCENARIO}.txt"
K6_EXIT=${PIPESTATUS[0]}
set -e
echo "k6 exit code: ${K6_EXIT}" >> "${OUT}/${SCENARIO}.env.txt"

kill ${SAMPLER_PID} 2>/dev/null || true

echo "▶ 부하 구간 병목 메트릭 요약"
# 램프업/다운을 빼고 정상 상태 구간만 본다.
awk -F'\t' 'NR>1 && $2>10 {
  n++; tb+=$2; na+=$3; np+=$4; ha+=$6; hp+=$7;
  if ($2>mtb) mtb=$2; if ($4>mnp) mnp=$4; if ($7>mhp) mhp=$7;
  nmax=$5; hmax=$8;
} END {
  if (n==0) { print "  (정상 상태 샘플 없음)"; exit }
  printf "  샘플 %d개 (정상 상태)\n", n;
  printf "  Tomcat busy   평균 %.1f / 최대 %d (max %s)\n", tb/n, mtb, "200";
  printf "  Netty active  평균 %.1f / max %s   ← 풀 포화 여부\n", na/n, nmax;
  printf "  Netty pending 평균 %.1f / 최대 %d  ← 커넥션 대기\n", np/n, mnp;
  printf "  Hikari active 평균 %.1f / max %s\n", ha/n, hmax;
  printf "  Hikari pending 평균 %.1f / 최대 %d\n", hp/n, mhp;
}' "${METRICS_TSV}" | tee "${OUT}/${SCENARIO}.bottleneck.txt"

echo "✔ 완료 → ${OUT}/"
