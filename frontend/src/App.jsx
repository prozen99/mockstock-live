import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { api, apiBaseUrl, createQuoteStream, getWebSocketUrl } from './api';

const INITIAL_TRADE_HISTORY = {
  trades: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

function createDefaultAuthForm() {
  const unique = Date.now();
  return {
    email: `demo-${unique}@mockstock.live`,
    password: 'password123',
    nickname: `demo-${String(unique).slice(-4)}`,
  };
}

function App() {
  const [authMode, setAuthMode] = useState('signup');
  const [authForm, setAuthForm] = useState(createDefaultAuthForm);
  const [currentUser, setCurrentUser] = useState(null);
  const [authFeedback, setAuthFeedback] = useState('');
  const [globalError, setGlobalError] = useState('');

  const [stocks, setStocks] = useState([]);
  const [selectedStockId, setSelectedStockId] = useState(null);
  const [selectedStock, setSelectedStock] = useState(null);
  const [stocksLoading, setStocksLoading] = useState(true);

  const [quoteStatus, setQuoteStatus] = useState('connecting');
  const [quotePublishedAt, setQuotePublishedAt] = useState(null);

  const [tradeQuantity, setTradeQuantity] = useState(1);
  const [tradeFeedback, setTradeFeedback] = useState('');
  const [holdings, setHoldings] = useState([]);
  const [tradeHistory, setTradeHistory] = useState(INITIAL_TRADE_HISTORY);
  const [tradeHistoryPage, setTradeHistoryPage] = useState(0);

  const [rooms, setRooms] = useState([]);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [chatDraft, setChatDraft] = useState('');
  const [chatStatus, setChatStatus] = useState('disconnected');
  const [chatFeedback, setChatFeedback] = useState('');

  const stompClientRef = useRef(null);
  const roomSubscriptionRef = useRef(null);

  const selectedRoom = rooms.find((room) => room.roomId === selectedRoomId) || null;

  useEffect(() => {
    loadStocks();
  }, []);

  useEffect(() => {
    const stream = createQuoteStream();

    const handleQuotes = (event) => {
      try {
        const payload = JSON.parse(event.data);
        applyQuotePayload(payload);
        setQuoteStatus('connected');
      } catch (error) {
        setQuoteStatus('error');
        setGlobalError(`Failed to parse quote stream payload: ${error.message}`);
      }
    };

    stream.onopen = () => setQuoteStatus('connected');
    stream.onerror = () => setQuoteStatus('reconnecting');
    stream.addEventListener('quote-snapshot', handleQuotes);
    stream.addEventListener('quote', handleQuotes);

    return () => {
      stream.removeEventListener('quote-snapshot', handleQuotes);
      stream.removeEventListener('quote', handleQuotes);
      stream.close();
    };
  }, []);

  useEffect(() => {
    if (!selectedStockId && stocks.length > 0) {
      setSelectedStockId(stocks[0].stockId);
    }
  }, [stocks, selectedStockId]);

  useEffect(() => {
    if (!selectedStockId) {
      setSelectedStock(null);
      return;
    }

    loadStockDetail(selectedStockId);
  }, [selectedStockId]);

  useEffect(() => {
    if (!currentUser) {
      setHoldings([]);
      setTradeHistory(INITIAL_TRADE_HISTORY);
      loadRooms(null);
      return;
    }

    loadPortfolio(currentUser.userId, tradeHistoryPage);
  }, [currentUser, tradeHistoryPage]);

  useEffect(() => {
    loadRooms(currentUser?.userId || null);
  }, [currentUser]);

  useEffect(() => {
    if (!selectedRoomId) {
      setMessages([]);
      unsubscribeRoom();
      return;
    }

    loadRoomMessages(selectedRoomId);

    if (chatStatus === 'connected') {
      subscribeToRoom(selectedRoomId);
    }
  }, [selectedRoomId, chatStatus]);

  useEffect(() => {
    return () => disconnectChat();
  }, []);

  function applyQuotePayload(payload) {
    if (!payload?.quotes) {
      return;
    }

    setQuotePublishedAt(payload.publishedAt || null);
    const quoteByStockId = new Map(payload.quotes.map((quote) => [quote.stockId, quote]));

    setStocks((currentStocks) =>
      currentStocks.map((stock) =>
        quoteByStockId.has(stock.stockId)
          ? { ...stock, ...quoteByStockId.get(stock.stockId) }
          : stock,
      ),
    );

    setSelectedStock((currentStock) => {
      if (!currentStock || !quoteByStockId.has(currentStock.stockId)) {
        return currentStock;
      }

      return { ...currentStock, ...quoteByStockId.get(currentStock.stockId) };
    });
  }

  async function loadStocks() {
    setStocksLoading(true);
    try {
      const nextStocks = await api.getStocks();
      setStocks(nextStocks);
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    } finally {
      setStocksLoading(false);
    }
  }

  async function loadStockDetail(stockId) {
    try {
      const detail = await api.getStock(stockId);
      setSelectedStock(detail);
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    }
  }

  async function loadPortfolio(userId, page) {
    try {
      const [nextHoldings, nextTradeHistory] = await Promise.all([
        api.getHoldings(userId),
        api.getTradeHistory(userId, page, 20),
      ]);

      setHoldings(nextHoldings);
      setTradeHistory(nextTradeHistory);
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    }
  }

  async function loadRooms(userId) {
    try {
      const nextRooms = await api.getRooms(userId);
      setRooms(nextRooms);
      setSelectedRoomId((currentRoomId) => {
        if (currentRoomId && nextRooms.some((room) => room.roomId === currentRoomId)) {
          return currentRoomId;
        }

        const matchingStockRoom = selectedStockId
          ? nextRooms.find((room) => room.stockId === selectedStockId)
          : null;

        return matchingStockRoom?.roomId || nextRooms[0]?.roomId || null;
      });
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    }
  }

  async function loadRoomMessages(roomId) {
    try {
      const pageResponse = await api.getMessages(roomId, 0, 30);
      setMessages([...pageResponse.messages].reverse());
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    }
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();

    try {
      const response =
        authMode === 'signup'
          ? await api.signup(authForm)
          : await api.login({
              email: authForm.email,
              password: authForm.password,
            });

      setCurrentUser({
        userId: response.userId,
        email: response.email,
        nickname: response.nickname,
      });
      setTradeHistoryPage(0);
      setAuthFeedback(
        authMode === 'signup'
          ? `Signed up and selected user ${response.userId}.`
          : `Logged in as user ${response.userId}.`,
      );
      setGlobalError('');
    } catch (error) {
      setAuthFeedback(error.message);
    }
  }

  async function handleTrade(type) {
    if (!currentUser || !selectedStock) {
      return;
    }

    try {
      const quantity = Number(tradeQuantity);
      const body = {
        userId: currentUser.userId,
        stockId: selectedStock.stockId,
        quantity,
      };

      const response = type === 'buy' ? await api.buy(body) : await api.sell(body);

      setTradeFeedback(
        `${type.toUpperCase()} completed: ${response.stockSymbol} x ${response.quantity} at ${formatMoney(
          response.executedPrice,
        )}`,
      );
      setTradeHistoryPage(0);
      await Promise.all([loadPortfolio(currentUser.userId, 0), loadRooms(currentUser.userId)]);
      setGlobalError('');
    } catch (error) {
      setTradeFeedback(error.message);
    }
  }

  async function handleJoinRoom() {
    if (!currentUser || !selectedRoom) {
      return;
    }

    try {
      const response = await api.joinRoom(selectedRoom.roomId, currentUser.userId);
      setChatFeedback(
        response.alreadyJoined
          ? `User ${response.userId} was already in room ${response.roomId}.`
          : `Joined room ${response.roomId}.`,
      );
      await loadRooms(currentUser.userId);
      setGlobalError('');
    } catch (error) {
      setChatFeedback(error.message);
    }
  }

  function connectChat() {
    if (stompClientRef.current) {
      return;
    }

    const client = new Client({
      brokerURL: getWebSocketUrl(),
      reconnectDelay: 0,
      debug: () => {},
      onConnect: () => {
        setChatStatus('connected');
        setChatFeedback(`Connected to ${getWebSocketUrl()}.`);
        if (selectedRoomId) {
          subscribeToRoom(selectedRoomId);
        }
      },
      onStompError: (frame) => {
        setChatStatus('error');
        setChatFeedback(frame.headers.message || 'STOMP error');
        unsubscribeRoom();
        stompClientRef.current = null;
      },
      onWebSocketError: () => {
        setChatStatus('error');
        setChatFeedback('WebSocket connection failed.');
        unsubscribeRoom();
        stompClientRef.current = null;
      },
      onWebSocketClose: () => {
        setChatStatus('disconnected');
        unsubscribeRoom();
        stompClientRef.current = null;
      },
    });

    stompClientRef.current = client;
    setChatStatus('connecting');
    client.activate();
  }

  function disconnectChat() {
    unsubscribeRoom();
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }
    setChatStatus('disconnected');
  }

  function unsubscribeRoom() {
    if (roomSubscriptionRef.current) {
      roomSubscriptionRef.current.unsubscribe();
      roomSubscriptionRef.current = null;
    }
  }

  function subscribeToRoom(roomId) {
    const client = stompClientRef.current;
    if (!client?.connected) {
      return;
    }

    unsubscribeRoom();
    roomSubscriptionRef.current = client.subscribe(`/sub/chat/rooms/${roomId}`, (frame) => {
      const message = JSON.parse(frame.body);
      setMessages((currentMessages) => {
        if (currentMessages.some((currentMessage) => currentMessage.messageId === message.messageId)) {
          return currentMessages;
        }

        return [...currentMessages, message];
      });
      setRooms((currentRooms) =>
        currentRooms.map((room) =>
          room.roomId === roomId
            ? {
                ...room,
                lastMessageId: message.messageId,
                lastMessagePreview: message.content,
                lastMessageAt: message.createdAt,
              }
            : room,
        ),
      );
    });
  }

  function handleSendMessage(event) {
    event.preventDefault();

    if (!stompClientRef.current?.connected || !currentUser || !selectedRoomId || !chatDraft.trim()) {
      return;
    }

    try {
      stompClientRef.current.publish({
        destination: `/pub/chat/rooms/${selectedRoomId}`,
        body: JSON.stringify({
          userId: currentUser.userId,
          content: chatDraft.trim(),
        }),
      });
      setChatDraft('');
      setChatFeedback(`Message sent to room ${selectedRoomId}.`);
    } catch (error) {
      setChatFeedback(error.message);
    }
  }

  function handleSelectStock(stockId) {
    setSelectedStockId(stockId);
    const matchingRoom = rooms.find((room) => room.stockId === stockId);
    if (matchingRoom) {
      setSelectedRoomId(matchingRoom.roomId);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Phase 11 local demo</p>
          <h1>MockStock Live Frontend Demo</h1>
          <p className="muted">
            Backend base URL: <code>{apiBaseUrl}</code>
          </p>
        </div>
        <div className="status-strip">
          <StatusBadge label="Quotes" value={quoteStatus} />
          <StatusBadge label="Chat" value={chatStatus} />
          <StatusBadge label="User" value={currentUser ? `user ${currentUser.userId}` : 'not selected'} />
        </div>
      </header>

      {globalError ? <div className="banner error">{globalError}</div> : null}

      <main className="layout">
        <section className="panel">
          <div className="panel-header">
            <h2>Demo User</h2>
          </div>
          <form className="stack" onSubmit={handleAuthSubmit}>
            <div className="toggle-row">
              <button
                type="button"
                className={authMode === 'signup' ? 'active' : ''}
                onClick={() => setAuthMode('signup')}
              >
                Sign up
              </button>
              <button
                type="button"
                className={authMode === 'login' ? 'active' : ''}
                onClick={() => setAuthMode('login')}
              >
                Login
              </button>
            </div>

            <label>
              Email
              <input
                value={authForm.email}
                onChange={(event) =>
                  setAuthForm((current) => ({ ...current, email: event.target.value }))
                }
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) =>
                  setAuthForm((current) => ({ ...current, password: event.target.value }))
                }
              />
            </label>

            {authMode === 'signup' ? (
              <label>
                Nickname
                <input
                  value={authForm.nickname}
                  onChange={(event) =>
                    setAuthForm((current) => ({ ...current, nickname: event.target.value }))
                  }
                />
              </label>
            ) : null}

            <button type="submit">{authMode === 'signup' ? 'Create demo user' : 'Use existing user'}</button>
          </form>

          <p className="feedback">{authFeedback || 'Create or select a user to enable trading and chat join/send.'}</p>

          <div className="card subtle">
            <div className="card-row">
              <span>Active user</span>
              <strong>{currentUser ? `${currentUser.nickname} (#${currentUser.userId})` : 'None'}</strong>
            </div>
            <div className="card-row">
              <span>Quote stream</span>
              <strong>{quotePublishedAt ? formatDateTime(quotePublishedAt) : 'Waiting for updates'}</strong>
            </div>
          </div>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Stocks</h2>
            <button type="button" onClick={loadStocks}>
              Refresh
            </button>
          </div>

          <div className="stock-list">
            {stocksLoading ? <p className="muted">Loading stocks...</p> : null}
            {stocks.map((stock) => (
              <button
                key={stock.stockId}
                type="button"
                className={`stock-item ${stock.stockId === selectedStockId ? 'selected' : ''}`}
                onClick={() => handleSelectStock(stock.stockId)}
              >
                <div>
                  <strong>{stock.symbol}</strong>
                  <span>{stock.name}</span>
                </div>
                <div className="price-block">
                  <strong>{formatMoney(stock.currentPrice)}</strong>
                  <span className={Number(stock.priceChangeRate) >= 0 ? 'up' : 'down'}>
                    {formatPercent(stock.priceChangeRate)}
                  </span>
                </div>
              </button>
            ))}
          </div>
        </section>

        <section className="panel panel-tall">
          <div className="panel-header">
            <h2>Stock Detail And Trading</h2>
          </div>

          {selectedStock ? (
            <>
              <div className="detail-grid">
                <Metric label="Symbol" value={selectedStock.symbol} />
                <Metric label="Name" value={selectedStock.name} />
                <Metric label="Market" value={selectedStock.marketType} />
                <Metric label="Updated" value={formatDateTime(selectedStock.updatedAt)} />
                <Metric label="Current price" value={formatMoney(selectedStock.currentPrice)} />
                <Metric
                  label="Change rate"
                  value={
                    <span className={Number(selectedStock.priceChangeRate) >= 0 ? 'up' : 'down'}>
                      {formatPercent(selectedStock.priceChangeRate)}
                    </span>
                  }
                />
              </div>

              <form
                className="trade-form"
                onSubmit={(event) => {
                  event.preventDefault();
                }}
              >
                <label>
                  Quantity
                  <input
                    type="number"
                    min="1"
                    value={tradeQuantity}
                    onChange={(event) => setTradeQuantity(event.target.value)}
                  />
                </label>
                <button type="button" disabled={!currentUser} onClick={() => handleTrade('buy')}>
                  Buy
                </button>
                <button type="button" disabled={!currentUser} onClick={() => handleTrade('sell')}>
                  Sell
                </button>
              </form>

              <p className="feedback">
                {tradeFeedback || 'Trades use the selected active user and existing backend buy/sell APIs.'}
              </p>
            </>
          ) : (
            <p className="muted">Select a stock to load detail data.</p>
          )}

          <div className="split-section">
            <div>
              <div className="panel-header small">
                <h3>Holdings</h3>
              </div>
              {currentUser ? (
                <table>
                  <thead>
                    <tr>
                      <th>Stock</th>
                      <th>Qty</th>
                      <th>Current</th>
                      <th>P/L</th>
                    </tr>
                  </thead>
                  <tbody>
                    {holdings.map((holding) => (
                      <tr key={holding.holdingId}>
                        <td>{holding.stockSymbol}</td>
                        <td>{holding.quantity}</td>
                        <td>{formatMoney(holding.currentPrice)}</td>
                        <td className={Number(holding.profitLoss) >= 0 ? 'up' : 'down'}>
                          {formatMoney(holding.profitLoss)}
                        </td>
                      </tr>
                    ))}
                    {holdings.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="muted">
                          No holdings yet.
                        </td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              ) : (
                <p className="muted">Select a user to load holdings.</p>
              )}
            </div>

            <div>
              <div className="panel-header small">
                <h3>Trade History</h3>
                <div className="pager">
                  <button
                    type="button"
                    disabled={!currentUser || tradeHistory.page <= 0}
                    onClick={() => setTradeHistoryPage((page) => Math.max(page - 1, 0))}
                  >
                    Prev
                  </button>
                  <span>
                    Page {tradeHistory.page + 1} / {Math.max(tradeHistory.totalPages, 1)}
                  </span>
                  <button
                    type="button"
                    disabled={
                      !currentUser ||
                      tradeHistory.totalPages === 0 ||
                      tradeHistory.page + 1 >= tradeHistory.totalPages
                    }
                    onClick={() => setTradeHistoryPage((page) => page + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>

              {currentUser ? (
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Type</th>
                      <th>Stock</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tradeHistory.trades.map((trade) => (
                      <tr key={trade.tradeOrderId}>
                        <td>{formatDateTime(trade.createdAt)}</td>
                        <td>{trade.tradeType}</td>
                        <td>{trade.stockSymbol}</td>
                        <td>{formatMoney(trade.totalAmount)}</td>
                      </tr>
                    ))}
                    {tradeHistory.trades.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="muted">
                          No trades yet.
                        </td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              ) : (
                <p className="muted">Select a user to load trade history.</p>
              )}
            </div>
          </div>
        </section>

        <section className="panel panel-tall">
          <div className="panel-header">
            <h2>Chat Demo</h2>
            <div className="button-row">
              <button
                type="button"
                onClick={connectChat}
                disabled={chatStatus === 'connected' || chatStatus === 'connecting'}
              >
                Connect
              </button>
              <button type="button" onClick={disconnectChat} disabled={!stompClientRef.current}>
                Disconnect
              </button>
              <button type="button" onClick={() => loadRooms(currentUser?.userId || null)}>
                Refresh rooms
              </button>
            </div>
          </div>

          <div className="split-section">
            <div>
              <div className="panel-header small">
                <h3>Rooms</h3>
              </div>
              <div className="room-list">
                {rooms.map((room) => (
                  <button
                    key={room.roomId}
                    type="button"
                    className={`room-item ${room.roomId === selectedRoomId ? 'selected' : ''}`}
                    onClick={() => setSelectedRoomId(room.roomId)}
                  >
                    <strong>{room.stockSymbol}</strong>
                    <span>{room.roomName}</span>
                    <small>
                      {room.joined ? 'joined' : 'not joined'}
                      {room.lastMessagePreview ? ` | ${room.lastMessagePreview}` : ''}
                    </small>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="panel-header small">
                <h3>Messages</h3>
                <button type="button" disabled={!currentUser || !selectedRoom} onClick={handleJoinRoom}>
                  Join selected room
                </button>
              </div>

              <div className="messages">
                {messages.map((message) => (
                  <div key={message.messageId} className="message">
                    <div className="message-meta">
                      <strong>{message.senderNickname}</strong>
                      <span>{formatDateTime(message.createdAt)}</span>
                    </div>
                    <p>{message.content}</p>
                  </div>
                ))}
                {messages.length === 0 ? <p className="muted">Select a room to load messages.</p> : null}
              </div>

              <form className="chat-form" onSubmit={handleSendMessage}>
                <textarea
                  rows="3"
                  placeholder="Send a message through /pub/chat/rooms/{roomId}"
                  value={chatDraft}
                  onChange={(event) => setChatDraft(event.target.value)}
                />
                <button
                  type="submit"
                  disabled={!currentUser || !selectedRoom?.joined || chatStatus !== 'connected' || !chatDraft.trim()}
                >
                  Send message
                </button>
              </form>

              <p className="feedback">
                {chatFeedback ||
                  'Connect WebSocket, join the selected room, then send through the existing STOMP destination.'}
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

function StatusBadge({ label, value }) {
  return (
    <div className="status-badge">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  return new Intl.NumberFormat('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value));
}

function formatPercent(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  return `${Number(value).toFixed(2)}%`;
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString();
}

export default App;
