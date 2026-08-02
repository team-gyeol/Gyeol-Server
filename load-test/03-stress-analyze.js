// 스트레스 — 계단식으로 VU 를 올려 한계점(에러율이 꺾이는 지점)을 찾는다.
// thresholds 를 abortOnFail 로 두지 않는다: 무너지는 지점 자체가 관측 대상이다.
import http from 'k6/http';
import { BASE_URL, mintTokens, authHeaders, checkAnalyzeResponse, warmup } from './lib/config.js';

const IMAGE = open('./data/drone-small.jpg', 'b');

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 25 },
        { duration: '2m', target: 25 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // 참고용. 실패해도 테스트는 끝까지 돌린다.
    'http_req_duration{name:analyze}': ['p(95)<5000'],
    http_req_failed: ['rate<0.05'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  const tokens = mintTokens();
  warmup(tokens, IMAGE);
  return { tokens };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/api/images/analyze`,
    { image: http.file(IMAGE, 'drone.jpg', 'image/jpeg') },
    { headers: authHeaders(data.tokens), timeout: '120s', tags: { name: 'analyze' } }
  );
  checkAnalyzeResponse(res);
}
