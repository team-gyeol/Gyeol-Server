// 핵심 시나리오 — POST /api/images/analyze 지속 부하.
// before/after 비교의 기준이 되는 시나리오이므로 조건을 절대 바꾸지 말 것.
import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { BASE_URL, mintTokens, authHeaders, checkAnalyzeResponse, warmup } from './lib/config.js';

const IMAGE_SIZE = __ENV.IMAGE_SIZE || 'small';
const IMAGE = open(`./data/drone-${IMAGE_SIZE}.jpg`, 'b');

const analyzeDuration = new Trend('analyze_duration', true);

export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 }, // 램프업
        { duration: '5m', target: 50 }, // 정상 상태 — 이 구간의 수치를 리포트에 쓴다
        { duration: '30s', target: 0 }, // 램프다운
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // 개선 목표를 코드에 박아둔다. baseline 에서는 실패해도 정상이다.
    'http_req_duration{name:analyze}': ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
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
    { image: http.file(IMAGE, `drone-${IMAGE_SIZE}.jpg`, 'image/jpeg') },
    { headers: authHeaders(data.tokens), timeout: '120s', tags: { name: 'analyze' } }
  );
  checkAnalyzeResponse(res);
  analyzeDuration.add(res.timings.duration);
}
