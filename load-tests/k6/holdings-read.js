import { check } from 'k6';
import http from 'k6/http';

import { buy, getStocks, signupUser } from './lib/mockstock.js';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 15,
  duration: '20s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<250'],
  },
};

export function setup() {
  const signup = signupUser(baseUrl, 'phase8-holdings');
  const stocks = getStocks(baseUrl);

  stocks.forEach((stock) => buy(baseUrl, signup.userId, stock.stockId, 1));

  return {
    userId: signup.userId,
  };
}

export default function (data) {
  const response = http.get(`${baseUrl}/api/v1/portfolio/holdings?userId=${data.userId}`, {
    tags: { phase8_scenario: 'holdings_read' },
  });

  check(response, {
    'holdings status is 200': (res) => res.status === 200,
    'holdings success body': (res) => res.json('success') === true,
  });
}
