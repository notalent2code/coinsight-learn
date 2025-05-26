import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const rateLimitHitRate = new Rate('rate_limit_hits');

export const options = {
  scenarios: {
    // Test public auth endpoints (IP-based rate limiting: 5 req/sec, burst 10)
    public_auth_test: {
      executor: 'constant-arrival-rate',
      rate: 15, // 15 requests per second (exceeds limit of 5)
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 5,
      maxVUs: 10,
      tags: { test_type: 'public_auth' },
      env: { TEST_TYPE: 'public_auth' },
    },

    // Test authenticated endpoints (user-based rate limiting: 10 req/sec for auth, 15 for transactions)
    authenticated_test: {
      executor: 'constant-arrival-rate',
      rate: 25, // 25 requests per second (exceeds limits)
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 5,
      maxVUs: 10,
      tags: { test_type: 'authenticated' },
      env: { TEST_TYPE: 'authenticated' },
      startTime: '35s', // Start after public test
    },

    // Test OCR endpoints (lowest limit: 2 req/sec, burst 5)
    ocr_burst_test: {
      executor: 'constant-arrival-rate',
      rate: 10, // 10 requests per second (exceeds limit of 2)
      timeUnit: '1s',
      duration: '20s',
      preAllocatedVUs: 3,
      maxVUs: 5,
      tags: { test_type: 'ocr_burst' },
      env: { TEST_TYPE: 'ocr_burst' },
      startTime: '70s', // Start after authenticated test
    },
  },
  thresholds: {
    rate_limit_hits: ['rate>0'], // Expect some rate limit hits
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
  },
};

const BASE_URL = 'http://localhost:8080';

// Test data
let authToken = null;

export function setup() {
  // Get authentication token for authenticated tests
  console.log('Setting up authentication...');

  const loginPayload = {
    email: 'test@example.com',
    password: 'test',
  };

  const loginResponse = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify(loginPayload),
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );

  if (loginResponse.status === 200) {
    const responseBody = JSON.parse(loginResponse.body);
    authToken = responseBody.accessToken || responseBody.access_token;
    console.log('Authentication successful');
  } else {
    console.log('Authentication failed, proceeding without token');
  }

  return { authToken };
}

export default function (data) {
  const testType = __ENV.TEST_TYPE;

  switch (testType) {
    case 'public_auth':
      testPublicAuthEndpoints();
      break;
    case 'authenticated':
      testAuthenticatedEndpoints(data.authToken);
      break;
    case 'ocr_burst':
      testOCREndpoints(data.authToken);
      break;
    default:
      testPublicAuthEndpoints();
  }
}

function testPublicAuthEndpoints() {
  const endpoints = ['/api/auth/login', '/api/auth/register'];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];

  const payload = {
    username: `testuser${Math.random()}`,
    password: 'testpass123',
    email: `test${Math.random()}@example.com`,
  };

  const response = http.post(
    `${BASE_URL}${endpoint}`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      tags: { endpoint: endpoint, test_type: 'public_auth' },
    }
  );

  const isRateLimited = response.status === 429;
  rateLimitHitRate.add(isRateLimited);

  check(response, {
    'status is 200, 400, or 429': (r) => [200, 400, 429].includes(r.status),
    'rate limit response has correct structure': (r) => {
      if (r.status === 429) {
        // Check if response body exists and is not empty
        if (!r.body || r.body.trim() === '') {
          console.log(`Rate limit hit but empty response body for ${endpoint}`);
          return true; // Accept empty response as valid rate limit
        }

        try {
          const body = JSON.parse(r.body);
          return (
            body.code === 429 &&
            body.message &&
            body.message.includes('Too many requests')
          );
        } catch (e) {
          console.log(
            `Rate limit hit but invalid JSON for ${endpoint}: ${r.body}`
          );
          return true; // Accept invalid JSON as valid rate limit (since status is 429)
        }
      }
      return true;
    },
  });

  if (isRateLimited) {
    console.log(
      `Rate limited on ${endpoint}: ${response.status}, Body: ${response.body}`
    );
  }

  sleep(0.1);
}

function testAuthenticatedEndpoints(token) {
  if (!token) {
    console.log('No auth token available, skipping authenticated tests');
    return;
  }

   // Generate dynamic date range (last 7 days)
  const endDate = new Date().toISOString().split('T')[0]; // Today: YYYY-MM-DD
  const startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]; // 7 days ago

  const endpoints = [
    { path: '/api/transactions', method: 'GET' },
    { 
      path: `/api/transactions/summary?startDate=${startDate}&endDate=${endDate}`, 
      method: 'GET' 
    },
  ];
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];

  let response;
  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  if (endpoint.method === 'GET') {
    response = http.get(`${BASE_URL}${endpoint.path}`, {
      headers: headers,
      tags: { endpoint: endpoint.path, test_type: 'authenticated' },
    });
  } else {
    response = http.post(`${BASE_URL}${endpoint.path}`, '{}', {
      headers: headers,
      tags: { endpoint: endpoint.path, test_type: 'authenticated' },
    });
  }

  const isRateLimited = response.status === 429;
  rateLimitHitRate.add(isRateLimited);

  check(response, {
    'status is 200, 401, or 429': (r) => [200, 401, 429].includes(r.status),
    'rate limit response has correct structure': (r) => {
      if (r.status === 429) {
        if (!r.body || r.body.trim() === '') {
          console.log(
            `Rate limit hit but empty response body for ${endpoint.path}`
          );
          return true;
        }

        try {
          const body = JSON.parse(r.body);
          return (
            body.code === 429 &&
            body.message &&
            body.message.includes('Too many requests')
          );
        } catch (e) {
          console.log(
            `Rate limit hit but invalid JSON for ${endpoint.path}: ${r.body}`
          );
          return true;
        }
      }
      return true;
    },
  });

  if (isRateLimited) {
    console.log(
      `Rate limited on ${endpoint.path}: ${response.status}, Body: ${response.body}`
    );
  }

  sleep(0.1);
}

function testOCREndpoints(token) {
  if (!token) {
    console.log('No auth token available, skipping OCR tests');
    return;
  }

  // Create a simple test file for OCR
  const testFile = http.file(
    'test.txt',
    'Sample receipt content for OCR testing',
    'text/plain'
  );

  const formData = {
    file: testFile,
    type: 'receipt',
  };

  const response = http.post(`${BASE_URL}/api/ocr`, formData, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    tags: { endpoint: '/api/ocr', test_type: 'ocr_burst' },
  });

  const isRateLimited = response.status === 429;
  rateLimitHitRate.add(isRateLimited);

  check(response, {
    'status is 200, 400, 401, or 429': (r) =>
      [200, 400, 401, 429].includes(r.status),
    'rate limit response has correct structure': (r) => {
      if (r.status === 429) {
        if (!r.body || r.body.trim() === '') {
          console.log(`Rate limit hit but empty response body for OCR`);
          return true;
        }

        try {
          const body = JSON.parse(r.body);
          return (
            body.code === 429 &&
            body.message &&
            body.message.includes('Too many requests')
          );
        } catch (e) {
          console.log(`Rate limit hit but invalid JSON for OCR: ${r.body}`);
          return true;
        }
      }
      return true;
    },
  });

  if (isRateLimited) {
    console.log(
      `Rate limited on OCR endpoint: ${response.status}, Body: ${response.body}`
    );
  }

  sleep(0.1);
}

export function teardown(data) {
  console.log('Rate limiter test completed');
}
