// 읽기 경로 부하 — N+1 쿼리 / 인덱스 부재(B7) 검증용.
// analyze 와 달리 외부 의존성이 없으므로 순수 DB·JPA 성능만 드러난다.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, mintTokens, authHeaders } from './lib/config.js';

export const options = {
  scenarios: {
    read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '3m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:mypage-images}': ['p(95)<500'],
    'http_req_duration{name:mypage}': ['p(95)<300'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return { tokens: mintTokens() };
}

export default function (data) {
  const headers = authHeaders(data.tokens);

  const profile = http.get(`${BASE_URL}/api/mypage`, { headers, tags: { name: 'mypage' } });
  check(profile, { 'mypage 200': (r) => r.status === 200 });

  const page = 1 + (__ITER % 3);
  const list = http.get(`${BASE_URL}/api/mypage/images?page=${page}&size=10`, {
    headers,
    tags: { name: 'mypage-images' },
  });
  check(list, { 'mypage/images 200': (r) => r.status === 200 });

  const count = http.get(`${BASE_URL}/api/mypage/images/count`, {
    headers,
    tags: { name: 'mypage-count' },
  });
  check(count, { 'mypage/images/count 200': (r) => r.status === 200 });
}
