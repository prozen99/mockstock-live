import http from 'k6/http';
import { fail } from 'k6';

const JSON_HEADERS = {
  headers: {
    'Content-Type': 'application/json',
  },
};

export function apiGetJson(url, params = {}) {
  const response = http.get(url, params);
  return unwrapSuccess(response, url);
}

export function apiPostJson(url, payload, params = {}) {
  const response = http.post(url, JSON.stringify(payload), {
    ...JSON_HEADERS,
    ...params,
  });
  return unwrapSuccess(response, url);
}

export function signupUser(baseUrl, prefix) {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  const payload = {
    email: `${prefix}-${suffix}@mockstock.live`,
    password: 'phase8pass1',
    nickname: `${prefix}${Math.floor(Math.random() * 1000000)}`,
  };

  return apiPostJson(`${baseUrl}/api/v1/auth/signup`, payload).data;
}

export function getStocks(baseUrl) {
  return apiGetJson(`${baseUrl}/api/v1/stocks`).data;
}

export function buy(baseUrl, userId, stockId, quantity = 1) {
  return apiPostJson(`${baseUrl}/api/v1/trades/buy`, {
    userId,
    stockId,
    quantity,
  }).data;
}

export function sell(baseUrl, userId, stockId, quantity = 1) {
  return apiPostJson(`${baseUrl}/api/v1/trades/sell`, {
    userId,
    stockId,
    quantity,
  }).data;
}

export function findCursorForPage(baseUrl, userId, page, size) {
  let beforeTradeId = null;

  for (let currentPage = 0; currentPage < page; currentPage++) {
    const separator = beforeTradeId === null ? '' : `&beforeTradeId=${beforeTradeId}`;
    const data = apiGetJson(
      `${baseUrl}/api/v1/trades/history/cursor?userId=${userId}&size=${size}${separator}`
    ).data;

    if (!data.hasNext || data.nextBeforeTradeId === null) {
      fail(`Unable to build cursor for page ${page}. Current page=${currentPage}`);
    }

    beforeTradeId = data.nextBeforeTradeId;
  }

  return beforeTradeId;
}

function unwrapSuccess(response, url) {
  if (response.status !== 200) {
    fail(`Unexpected status ${response.status} for ${url}`);
  }

  let body;
  try {
    body = response.json();
  } catch (error) {
    fail(`Invalid JSON from ${url}: ${error}`);
  }

  if (!body.success) {
    fail(`API failure from ${url}: ${JSON.stringify(body.error)}`);
  }

  return body;
}
