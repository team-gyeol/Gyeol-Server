#!/usr/bin/env bash
# WireMock ML 스텁의 전역 응답 지연을 설정한다.
# ML 추론 시간을 바꿔가며 업스트림 지연 민감도를 실험할 때 사용.
#
#   ./load-test/set-mock-delay.sh 800     # 기본값(실측 추론 시간에 근사)
#   ./load-test/set-mock-delay.sh 200     # 빠른 ML
#   ./load-test/set-mock-delay.sh 3000    # 느린 ML
set -euo pipefail

DELAY_MS="${1:-800}"
WIREMOCK_URL="${WIREMOCK_URL:-http://localhost:8081}"

curl -sf -X POST "${WIREMOCK_URL}/__admin/settings" \
  -H 'Content-Type: application/json' \
  -d "{\"fixedDelay\": ${DELAY_MS}}" >/dev/null

echo "WireMock 전역 응답 지연을 ${DELAY_MS}ms 로 설정했습니다."
