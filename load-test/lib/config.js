// 모든 시나리오가 공유하는 설정과 헬퍼.
import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 시딩된 테스트 사용자 수 (load-test/seed-users.sh 와 맞출 것)
export const USER_COUNT = Number(__ENV.USER_COUNT || 50);

// 결과 파일에 함께 기록할 실행 라벨 (baseline / after-b4 / after-all ...)
export const RUN_LABEL = __ENV.RUN_LABEL || 'unlabeled';

/**
 * setup() 에서 호출. 테스트 사용자 전원의 JWT 를 미리 발급받아 둔다.
 * 부하 구간에서 토큰 발급 요청이 측정에 섞이지 않도록 분리하는 것이 핵심.
 */
export function mintTokens() {
  const tokens = [];
  for (let userId = 1; userId <= USER_COUNT; userId++) {
    const res = http.get(`${BASE_URL}/api/token/${userId}`);
    if (res.status === 200 && res.body && res.body.length > 20) {
      tokens.push(res.body);
    }
  }
  if (tokens.length === 0) {
    throw new Error(
      `테스트 토큰을 하나도 발급받지 못했습니다. ` +
      `./load-test/seed-users.sh ${USER_COUNT} 를 먼저 실행했는지 확인하세요.`
    );
  }
  console.log(`토큰 ${tokens.length}개 발급 완료`);
  return tokens;
}

/** VU 마다 서로 다른 사용자로 요청하도록 토큰을 분산시킨다. */
export function tokenFor(tokens) {
  return tokens[(__VU - 1) % tokens.length];
}

export function authHeaders(tokens) {
  return { Authorization: `Bearer ${tokenFor(tokens)}` };
}

/** 모든 시나리오 공통 합격 기준. */
export const COMMON_THRESHOLDS = {
  http_req_failed: ['rate<0.01'],
  checks: ['rate>0.99'],
};

export function checkAnalyzeResponse(res) {
  return check(res, {
    'status 200': (r) => r.status === 200,
    '분석 결과 포함': (r) => r.status === 200 && String(r.body).includes('analysisResult'),
  });
}

/** 부하 구간 진입 전 JIT 워밍업. 첫 요청의 클래스 로딩/커넥션 수립 비용을 제거한다. */
export function warmup(tokens, imageFile, rounds = 5) {
  for (let i = 0; i < rounds; i++) {
    http.post(
      `${BASE_URL}/api/images/analyze`,
      { image: http.file(imageFile, `warmup-${i}.jpg`, 'image/jpeg') },
      { headers: { Authorization: `Bearer ${tokens[0]}` }, timeout: '120s' }
    );
  }
  console.log(`워밍업 ${rounds}회 완료`);
}
