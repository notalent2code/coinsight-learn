import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import http from 'k6/http';

export const options = {
  scenarios: {
    monitor: {
      executor: 'constant-vus',
      vus: 1,
      duration: '60s',
    },
  },
};

export default function () {
  // This would require a Redis monitoring endpoint
  // For now, just monitor the gateway health
  const response = http.get('http://localhost:8080/actuator/health');

  if (response.status === 200) {
    console.log(`Gateway health: ${response.body}`);
  }

  sleep(5); // Check every 5 seconds
}
