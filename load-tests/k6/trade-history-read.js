import { check } from 'k6';
import http from 'k6/http';

import { buy, findCursorForPage, getStocks, sell, signupUser } from './lib/mockstock.js';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const paginationMode = __ENV.PAGINATION_MODE || 'offset';
const page = Number(__ENV.TRADE_PAGE || 15);
const size = Number(__ENV.TRADE_SIZE || 20);
const tradePairs = Number(__ENV.TRADE_PAIRS || 300);

export const options = {
  vus: 12,
  duration: '20s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<400'],
  },
};

export function setup() {
  const signup = signupUser(baseUrl, `phase8-trades-${paginationMode}`);
  const stocks = getStocks(baseUrl);
  const stock = stocks[0];

  for (let index = 0; index < tradePairs; index++) {
    buy(baseUrl, signup.userId, stock.stockId, 1);
    sell(baseUrl, signup.userId, stock.stockId, 1);
  }

  return {
    userId: signup.userId,
    beforeTradeId: findCursorForPage(baseUrl, signup.userId, page, size),
  };
}

export default function (data) {
  const url = paginationMode === 'cursor'
    ? `${baseUrl}/api/v1/trades/history/cursor?userId=${data.userId}&beforeTradeId=${data.beforeTradeId}&size=${size}`
    : `${baseUrl}/api/v1/trades/history?userId=${data.userId}&page=${page}&size=${size}`;

  const response = http.get(url, {
    tags: { phase8_scenario: `trade_history_${paginationMode}` },
  });

  check(response, {
    'trade history status is 200': (res) => res.status === 200,
    'trade history success body': (res) => res.json('success') === true,
  });
}
