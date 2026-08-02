// 스파이크 — 트래픽 급증 시 회복 능력.
// Cloud Run 대상으로 돌리면 콜드스타트/오토스케일 지연이 그대로 드러난다.
import http from 'k6/http';
import { BASE_URL, mintTokens, authHeaders, checkAnalyzeResponse } from './lib/config.js';

const IMAGE = open('./data/drone-small.jpg', 'b');

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },   // 평시
        { duration: '10s', target: 100 }, // 급증
        { duration: '1m', target: 100 },  // 유지
        { duration: '10s', target: 5 },   // 급감
        { duration: '1m', target: 5 },    // 회복 관찰
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return { tokens: mintTokens() };
}

export default function (data) {
  const res = http.post(
    `${BASE_URL}/api/images/analyze`,
    { image: http.file(IMAGE, 'drone.jpg', 'image/jpeg') },
    { headers: authHeaders(data.tokens), timeout: '120s', tags: { name: 'analyze' } }
  );
  checkAnalyzeResponse(res);
}
