import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 }, // Ramp up to 10 users over 30 seconds
    { duration: '1m', target: 10 },  // Stay at 10 users for 1 minute
    { duration: '30s', target: 0 },  // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
    http_req_failed: ['rate<0.05'],   // Less than 5% of requests can fail
  },
};

export default function () {
  // Test the API Gateway welcome endpoint
  const response = http.get('http://localhost:8080/');
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response body contains welcome message': (r) => r.body.includes('Coinsight API Gateway'),
  });
  
  sleep(1);
}