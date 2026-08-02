// 스모크 — 부하가 아니라 "경로가 살아 있는지" 확인용.
// 본 시나리오를 돌리기 전에 항상 먼저 실행한다.
import http from 'k6/http';
import { sleep, check } from 'k6';
import { BASE_URL, mintTokens, authHeaders, checkAnalyzeResponse } from './lib/config.js';

const IMAGE = open('./data/drone-small.jpg', 'b');

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return { tokens: mintTokens() };
}

export default function (data) {
  const headers = authHeaders(data.tokens);

  const analyze = http.post(
    `${BASE_URL}/api/images/analyze`,
    { image: http.file(IMAGE, 'drone.jpg', 'image/jpeg') },
    { headers, timeout: '120s', tags: { name: 'analyze' } }
  );
  checkAnalyzeResponse(analyze);

  const mypage = http.get(`${BASE_URL}/api/mypage`, { headers, tags: { name: 'mypage' } });
  check(mypage, { 'mypage 200': (r) => r.status === 200 });

  const images = http.get(`${BASE_URL}/api/mypage/images?page=1&size=10`, {
    headers,
    tags: { name: 'mypage-images' },
  });
  check(images, { 'mypage/images 200': (r) => r.status === 200 });

  sleep(1);
}
