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

const EMPTY_FEEDBACK = {
  tone: 'info',
  message: '',
};

function createDefaultAuthForm() {
  const unique = Date.now();
  return {
    email: `demo-${unique}@mockstock.live`,
    password: 'password123',
    nickname: `demo-${String(unique).slice(-4)}`,
  };
}

function createFeedback(tone, message) {
  return { tone, message };
}

function App() {
  const [authMode, setAuthMode] = useState('signup');
  const [authForm, setAuthForm] = useState(createDefaultAuthForm);
  const [currentUser, setCurrentUser] = useState(null);
  const [authFeedback, setAuthFeedback] = useState(EMPTY_FEEDBACK);
  const [authLoading, setAuthLoading] = useState(false);
  const [globalError, setGlobalError] = useState('');

  const [stocks, setStocks] = useState([]);
  const [selectedStockId, setSelectedStockId] = useState(null);
  const [selectedStock, setSelectedStock] = useState(null);
  const [stocksLoading, setStocksLoading] = useState(true);
  const [stockDetailLoading, setStockDetailLoading] = useState(false);

  const [quoteStatus, setQuoteStatus] = useState('connecting');
  const [quotePublishedAt, setQuotePublishedAt] = useState(null);

  const [tradeQuantity, setTradeQuantity] = useState(1);
  const [tradeFeedback, setTradeFeedback] = useState(EMPTY_FEEDBACK);
  const [tradeLoading, setTradeLoading] = useState(false);
  const [holdings, setHoldings] = useState([]);
  const [portfolioLoading, setPortfolioLoading] = useState(false);
  const [tradeHistory, setTradeHistory] = useState(INITIAL_TRADE_HISTORY);
  const [tradeHistoryPage, setTradeHistoryPage] = useState(0);

  const [rooms, setRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [chatDraft, setChatDraft] = useState('');
  const [chatStatus, setChatStatus] = useState('disconnected');
  const [chatFeedback, setChatFeedback] = useState(EMPTY_FEEDBACK);

  const stompClientRef = useRef(null);
  const roomSubscriptionRef = useRef(null);

  const selectedRoom = rooms.find((room) => room.roomId === selectedRoomId) || null;
  const selectedHolding = holdings.find((holding) => holding.stockId === selectedStockId) || null;
  const parsedTradeQuantity = Math.max(Number(tradeQuantity) || 0, 0);
  const estimatedTradeAmount =
    selectedStock && parsedTradeQuantity > 0
      ? Number(selectedStock.currentPrice) * parsedTradeQuantity
      : null;

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

  useEffect(() => () => disconnectChat(), []);

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
    setStockDetailLoading(true);
    try {
      const detail = await api.getStock(stockId);
      setSelectedStock(detail);
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    } finally {
      setStockDetailLoading(false);
    }
  }

  async function loadPortfolio(userId, page) {
    setPortfolioLoading(true);
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
    } finally {
      setPortfolioLoading(false);
    }
  }

  async function loadRooms(userId) {
    setRoomsLoading(true);
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
    } finally {
      setRoomsLoading(false);
    }
  }

  async function loadRoomMessages(roomId) {
    setMessagesLoading(true);
    try {
      const pageResponse = await api.getMessages(roomId, 0, 30);
      setMessages([...pageResponse.messages].reverse());
      setGlobalError('');
    } catch (error) {
      setGlobalError(error.message);
    } finally {
      setMessagesLoading(false);
    }
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setAuthLoading(true);
    setAuthFeedback(createFeedback('info', 'Submitting auth request...'));

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
        cashBalance: response.cashBalance ?? null,
      });
      setTradeHistoryPage(0);
      setAuthFeedback(
        createFeedback(
          'success',
          authMode === 'signup'
            ? `Signed up and selected user ${response.userId}.`
            : `Logged in as user ${response.userId}.`,
        ),
      );
      setGlobalError('');
    } catch (error) {
      setAuthFeedback(createFeedback('error', error.message));
    } finally {
      setAuthLoading(false);
    }
  }

  async function handleTrade(type) {
    if (!currentUser || !selectedStock) {
      return;
    }

    const quantity = Math.max(Number(tradeQuantity) || 0, 0);
    if (!quantity) {
      setTradeFeedback(createFeedback('error', 'Quantity must be at least 1.'));
      return;
    }

    try {
      setTradeLoading(true);
      setTradeFeedback(createFeedback('info', `${type.toUpperCase()} request in progress...`));
      const body = {
        userId: currentUser.userId,
        stockId: selectedStock.stockId,
        quantity,
      };
      const response = type === 'buy' ? await api.buy(body) : await api.sell(body);

      setCurrentUser((user) =>
        user
          ? {
              ...user,
              cashBalance: response.remainingCashBalance,
            }
          : user,
      );
      setTradeFeedback(
        createFeedback(
          'success',
          `${type.toUpperCase()} completed: ${response.stockSymbol} x ${response.quantity} at ${formatMoney(
            response.executedPrice,
          )}. Remaining cash ${formatMoney(response.remainingCashBalance)}.`,
        ),
      );
      setTradeHistoryPage(0);
      await Promise.all([loadPortfolio(currentUser.userId, 0), loadRooms(currentUser.userId)]);
      setGlobalError('');
    } catch (error) {
      setTradeFeedback(createFeedback('error', error.message));
    } finally {
      setTradeLoading(false);
    }
  }

  async function handleJoinRoom() {
    if (!currentUser || !selectedRoom) {
      return;
    }

    try {
      setChatFeedback(createFeedback('info', `Joining room ${selectedRoom.roomId}...`));
      const response = await api.joinRoom(selectedRoom.roomId, currentUser.userId);
      setChatFeedback(
        createFeedback(
          'success',
          response.alreadyJoined
            ? `User ${response.userId} was already in room ${response.roomId}.`
            : `Joined room ${response.roomId}.`,
        ),
      );
      await loadRooms(currentUser.userId);
      setGlobalError('');
    } catch (error) {
      setChatFeedback(createFeedback('error', error.message));
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
        setChatFeedback(createFeedback('success', `Connected to ${getWebSocketUrl()}.`));
        if (selectedRoomId) {
          subscribeToRoom(selectedRoomId);
        }
      },
      onStompError: (frame) => {
        setChatStatus('error');
        setChatFeedback(createFeedback('error', frame.headers.message || 'STOMP error'));
        unsubscribeRoom();
        stompClientRef.current = null;
      },
      onWebSocketError: () => {
        setChatStatus('error');
        setChatFeedback(createFeedback('error', 'WebSocket connection failed.'));
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
    setChatFeedback(createFeedback('info', `Connecting to ${getWebSocketUrl()}...`));
    client.activate();
  }

  function disconnectChat() {
    unsubscribeRoom();
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }
    setChatStatus('disconnected');
    setChatFeedback(createFeedback('info', 'Chat connection closed.'));
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
      setChatFeedback(createFeedback('success', `Message sent to room ${selectedRoomId}.`));
    } catch (error) {
      setChatFeedback(createFeedback('error', error.message));
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
          <p className="muted intro-copy">
            Lightweight reviewer UI for trading, holdings, SSE quotes, and stock-room chat.
          </p>
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

            <button type="submit" disabled={authLoading}>
              {authLoading ? 'Submitting...' : authMode === 'signup' ? 'Create demo user' : 'Use existing user'}
            </button>
          </form>

          <FeedbackNotice
            feedback={authFeedback}
            fallback="Create or select a user to enable trading and chat join/send."
          />

          <div className="card subtle account-card">
            <div className="card-row">
              <span>Active user</span>
              <strong>{currentUser ? `${currentUser.nickname} (#${currentUser.userId})` : 'None'}</strong>
            </div>
            <div className="card-row balance-row">
              <span>Available cash</span>
              <strong>{currentUser ? formatMoney(currentUser.cashBalance) : '-'}</strong>
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
          <p className="muted section-copy">
            Pick a stock to inspect the live detail panel, trading actions, and the matching chat room.
          </p>
          <div className="stock-list">
            {stocksLoading ? <p className="muted">Loading stocks...</p> : null}
            {stocks.map((stock) => (
              <button
                key={stock.stockId}
                type="button"
                className={`stock-item ${stock.stockId === selectedStockId ? 'selected' : ''}`}
                onClick={() => handleSelectStock(stock.stockId)}
              >
                <div className="stock-main">
                  <div>
                    <strong>{stock.symbol}</strong>
                    <span>{stock.name}</span>
                  </div>
                  {stock.stockId === selectedStockId ? <span className="pill accent">Selected</span> : null}
                </div>
                <div className="price-block">
                  <strong>{formatMoney(stock.currentPrice)}</strong>
                  <span className={getTrendClass(stock.priceChangeRate)}>
                    {formatSignedPercent(stock.priceChangeRate)}
                  </span>
                </div>
              </button>
            ))}
          </div>
        </section>

        <section className="panel panel-tall">
          <div className="panel-header">
            <h2>Stock Detail And Trading</h2>
            {stockDetailLoading ? <span className="pill neutral">Refreshing detail</span> : null}
          </div>

          {selectedStock ? (
            <>
              <div className="detail-hero">
                <div>
                  <p className="eyebrow">Selected stock</p>
                  <h3>{selectedStock.symbol}</h3>
                  <p className="muted">{selectedStock.name}</p>
                </div>
                <div className="detail-price">
                  <strong>{formatMoney(selectedStock.currentPrice)}</strong>
                  <span className={getTrendClass(selectedStock.priceChangeRate)}>
                    {formatSignedPercent(selectedStock.priceChangeRate)}
                  </span>
                </div>
              </div>

              <div className="detail-grid">
                <Metric label="Market" value={selectedStock.marketType} />
                <Metric label="Updated" value={formatDateTime(selectedStock.updatedAt)} />
                <Metric label="Active user cash" value={currentUser ? formatMoney(currentUser.cashBalance) : '-'} />
                <Metric label="Holding quantity" value={selectedHolding?.quantity ?? 0} />
              </div>

              <div className="trade-card">
                <div className="trade-card-header">
                  <div>
                    <h3>Buy / Sell</h3>
                    <p className="muted">Trades use the selected user and the existing backend buy/sell APIs.</p>
                  </div>
                  <span className={`pill ${currentUser ? 'success' : 'neutral'}`}>
                    {currentUser ? 'User ready' : 'Select user first'}
                  </span>
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
                  <div className="trade-summary">
                    <span>Estimated total</span>
                    <strong>{estimatedTradeAmount ? formatMoney(estimatedTradeAmount) : '-'}</strong>
                  </div>
                  <div className="button-row trade-actions">
                    <button type="button" disabled={!currentUser || tradeLoading} onClick={() => handleTrade('buy')}>
                      {tradeLoading ? 'Processing...' : 'Buy'}
                    </button>
                    <button type="button" disabled={!currentUser || tradeLoading} onClick={() => handleTrade('sell')}>
                      {tradeLoading ? 'Processing...' : 'Sell'}
                    </button>
                  </div>
                </form>
              </div>

              <FeedbackNotice
                feedback={tradeFeedback}
                fallback="Execute a trade to see immediate balance, holdings, and history updates."
              />
            </>
          ) : (
            <p className="muted">Select a stock to load detail data.</p>
          )}

          <div className="split-section">
            <div className="subpanel">
              <div className="panel-header small">
                <h3>Holdings</h3>
                {portfolioLoading ? <span className="pill neutral">Refreshing</span> : null}
              </div>
              {currentUser ? (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Stock</th>
                      <th>Qty</th>
                      <th>Avg buy</th>
                      <th>Current</th>
                      <th>Eval</th>
                      <th>P/L</th>
                      <th>Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {holdings.map((holding) => (
                      <tr
                        key={holding.holdingId}
                        className={holding.stockId === selectedStockId ? 'highlight-row' : ''}
                      >
                        <td>
                          <div className="cell-stack">
                            <strong>{holding.stockSymbol}</strong>
                            <span>{holding.stockName}</span>
                          </div>
                        </td>
                        <td>{holding.quantity}</td>
                        <td>{formatMoney(holding.avgBuyPrice)}</td>
                        <td>{formatMoney(holding.currentPrice)}</td>
                        <td>{formatMoney(holding.evaluatedAmount)}</td>
                        <td className={getTrendClass(holding.profitLoss)}>{formatSignedMoney(holding.profitLoss)}</td>
                        <td className={getTrendClass(holding.profitRate)}>{formatSignedPercent(holding.profitRate)}</td>
                      </tr>
                    ))}
                    {holdings.length === 0 ? (
                      <tr>
                        <td colSpan="7" className="muted empty-row">
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

            <div className="subpanel">
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
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Type</th>
                      <th>Stock</th>
                      <th>Qty</th>
                      <th>Price</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tradeHistory.trades.map((trade) => (
                      <tr key={trade.tradeOrderId}>
                        <td>{formatDateTime(trade.createdAt)}</td>
                        <td>
                          <span className={`pill ${trade.tradeType === 'BUY' ? 'success' : 'danger'}`}>
                            {trade.tradeType}
                          </span>
                        </td>
                        <td>
                          <div className="cell-stack">
                            <strong>{trade.stockSymbol}</strong>
                            <span>{trade.stockName}</span>
                          </div>
                        </td>
                        <td>{trade.quantity}</td>
                        <td>{formatMoney(trade.price)}</td>
                        <td>{formatMoney(trade.totalAmount)}</td>
                      </tr>
                    ))}
                    {tradeHistory.trades.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="muted empty-row">
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

          <div className="chat-summary">
            <span className={`pill ${selectedRoom?.joined ? 'success' : 'neutral'}`}>
              {selectedRoom ? `${selectedRoom.stockSymbol} room` : 'Select a room'}
            </span>
            <span className="muted">
              {selectedRoom?.joined ? 'Joined and ready for send.' : 'Join the selected room before sending.'}
            </span>
          </div>

          <div className="split-section">
            <div className="subpanel">
              <div className="panel-header small">
                <h3>Rooms</h3>
                {roomsLoading ? <span className="pill neutral">Refreshing</span> : null}
              </div>
              <div className="room-list">
                {rooms.map((room) => (
                  <button
                    key={room.roomId}
                    type="button"
                    className={`room-item ${room.roomId === selectedRoomId ? 'selected' : ''}`}
                    onClick={() => setSelectedRoomId(room.roomId)}
                  >
                    <div className="room-item-top">
                      <strong>{room.stockSymbol}</strong>
                      <span className={`pill ${room.joined ? 'success' : 'neutral'}`}>
                        {room.joined ? 'joined' : 'not joined'}
                      </span>
                    </div>
                    <span>{room.roomName}</span>
                    <small>{room.lastMessagePreview || 'No recent messages yet.'}</small>
                  </button>
                ))}
              </div>
            </div>

            <div className="subpanel">
              <div className="panel-header small">
                <h3>Messages</h3>
                <button type="button" disabled={!currentUser || !selectedRoom} onClick={handleJoinRoom}>
                  Join selected room
                </button>
              </div>

              <div className="messages">
                {messagesLoading ? <p className="muted">Loading messages...</p> : null}
                {messages.map((message) => (
                  <div
                    key={message.messageId}
                    className={`message ${message.senderNickname === currentUser?.nickname ? 'own' : ''}`}
                  >
                    <div className="message-meta">
                      <strong>{message.senderNickname}</strong>
                      <span>{formatDateTime(message.createdAt)}</span>
                    </div>
                    <p>{message.content}</p>
                  </div>
                ))}
                {!messagesLoading && messages.length === 0 ? (
                  <p className="muted">Select a room to load messages.</p>
                ) : null}
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

              <FeedbackNotice
                feedback={chatFeedback}
                fallback="Connect WebSocket, join the selected room, then send through the existing STOMP destination."
              />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

function StatusBadge({ label, value }) {
  return (
    <div className={`status-badge ${getStatusBadgeTone(value)}`}>
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

function FeedbackNotice({ feedback, fallback }) {
  if (!feedback?.message) {
    return <p className="feedback muted">{fallback}</p>;
  }

  return <div className={`feedback-banner ${feedback.tone}`}>{feedback.message}</div>;
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  return `KRW ${new Intl.NumberFormat('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value))}`;
}

function formatSignedMoney(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const numeric = Number(value);
  const prefix = numeric > 0 ? '+' : '';
  return `${prefix}${formatMoney(numeric)}`;
}

function formatPercent(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  return `${Number(value).toFixed(2)}%`;
}

function formatSignedPercent(value) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const numeric = Number(value);
  const prefix = numeric > 0 ? '+' : '';
  return `${prefix}${formatPercent(numeric)}`;
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString();
}

function getTrendClass(value) {
  return Number(value) >= 0 ? 'up' : 'down';
}

function getStatusBadgeTone(value) {
  const normalized = String(value || '').toLowerCase();

  if (normalized.includes('connected') || normalized.includes('user ')) {
    return 'success';
  }

  if (normalized.includes('error') || normalized.includes('reconnecting')) {
    return 'warning';
  }

  return 'neutral';
}

export default App;
