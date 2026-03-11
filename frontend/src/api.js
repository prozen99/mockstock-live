const DEFAULT_API_BASE_URL = 'http://localhost:8080';

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL).replace(/\/$/, '');

export function getWebSocketUrl() {
  const configured = import.meta.env.VITE_WS_URL;
  if (configured) {
    return configured;
  }

  const url = new URL(apiBaseUrl);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = '/ws';
  url.search = '';
  url.hash = '';
  return url.toString();
}

export function createQuoteStream(symbols) {
  const url = new URL('/api/v1/quotes/stream', apiBaseUrl);
  if (symbols && symbols.length > 0) {
    url.searchParams.set('symbols', symbols.join(','));
  }
  return new EventSource(url.toString());
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(payload?.error?.message || `Request failed with status ${response.status}`);
  }

  if (payload && payload.success === false) {
    throw new Error(payload.error?.message || 'Request failed');
  }

  return payload?.data ?? payload;
}

export const api = {
  getStocks() {
    return request('/api/v1/stocks');
  },

  getStock(stockId) {
    return request(`/api/v1/stocks/${stockId}`);
  },

  signup(body) {
    return request('/api/v1/auth/signup', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  login(body) {
    return request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  buy(body) {
    return request('/api/v1/trades/buy', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  sell(body) {
    return request('/api/v1/trades/sell', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  getHoldings(userId) {
    return request(`/api/v1/portfolio/holdings?userId=${userId}`);
  },

  getTradeHistory(userId, page = 0, size = 20) {
    return request(`/api/v1/trades/history?userId=${userId}&page=${page}&size=${size}`);
  },

  getRooms(userId) {
    const query = userId ? `?userId=${userId}` : '';
    return request(`/api/v1/chat/rooms${query}`);
  },

  getMessages(roomId, page = 0, size = 30) {
    return request(`/api/v1/chat/rooms/${roomId}/messages?page=${page}&size=${size}`);
  },

  joinRoom(roomId, userId) {
    return request(`/api/v1/chat/rooms/${roomId}/join`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    });
  },
};
