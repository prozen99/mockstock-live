import { check } from 'k6';
import http from 'k6/http';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 20,
  duration: '20s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/api/v1/stocks`, {
    tags: { phase8_scenario: 'stock_list_read' },
  });

  check(response, {
    'stock list status is 200': (res) => res.status === 200,
    'stock list success body': (res) => res.json('success') === true,
  });
}
