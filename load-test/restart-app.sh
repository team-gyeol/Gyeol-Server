#!/usr/bin/env bash
# 부하 테스트용 애플리케이션 재기동.
# bootRun 은 Gradle 데몬을 끼고 있어 반복 재시작이 불안정하다. jar 로 직접 띄운다.
#
#   ./load-test/restart-app.sh              # 다시 빌드하고 재기동
#   SKIP_BUILD=1 ./load-test/restart-app.sh # 설정만 바꿨을 때
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG="${APP_LOG:-${ROOT}/build/app.log}"
JAR="${ROOT}/build/libs/Oauth2Prac-1.0.0.jar"

echo "▶ 기존 프로세스 종료"
pkill -f "Oauth2Prac-1.0.0.jar" 2>/dev/null || true
while curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do sleep 1; done

if [ -z "${SKIP_BUILD:-}" ]; then
  echo "▶ 빌드"
  (cd "${ROOT}" && ./gradlew bootJar -q)
fi

echo "▶ 기동"
cd "${ROOT}"
nohup java -XX:+UseG1GC ${JAVA_OPTS:-} \
  -jar "${JAR}" --spring.profiles.active="${SPRING_PROFILE:-local}" \
  > "${LOG}" 2>&1 &

until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
  sleep 2
  if ! pgrep -f "Oauth2Prac-1.0.0.jar" >/dev/null; then
    echo "기동 실패. 로그:"; tail -30 "${LOG}"; exit 1
  fi
done

grep -m1 "커넥션 풀" "${LOG}" || true
echo "✔ 기동 완료 (로그: ${LOG})"
