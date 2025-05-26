import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1, // Single user
  iterations: 50, // 50 requests total
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Test public endpoint with IP-based rate limiting (5 req/sec limit)
  const payload = {
    email: 'test@example.com',
    password: 'test',
  };

  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  console.log(`Request ${__ITER + 1}: Status ${response.status}`);

  check(response, {
    'request completed': (r) => r.status !== 0,
    'rate limited (429) or normal response': (r) =>
      [200, 400, 401, 429, 500].includes(r.status),
  });

  if (response.status === 429) {
    console.log(`🚫 Rate limited! Response: ${response.body}`);
  }

  // Send requests quickly to trigger rate limiting
  sleep(0.1); // 10 requests per second (exceeds 5 req/sec limit)
}
