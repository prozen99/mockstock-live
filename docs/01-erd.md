# ERD

## 1. 문서 목적
이 문서는 MockStock Live의 핵심 데이터 구조를 정의한다.

이 프로젝트는 단순 CRUD가 아니라,
실시간 시세 스트림, 모의 투자 정합성, 채팅, unread 관리,
그리고 조회 성능 최적화를 보여주는 것을 목표로 한다.

따라서 ERD도 단순한 정규화 설명보다
아래 관점을 함께 고려한다.

- 어떤 조회 패턴이 자주 발생하는가
- 어떤 테이블이 성능 병목 지점이 될 가능성이 높은가
- 어떤 컬럼을 미리 저장해 조회 비용을 줄일 수 있는가
- 어떤 인덱스를 통해 keyset pagination, 정렬, unread 계산을 빠르게 할 것인가

---

## 2. 설계 원칙

### 2-1. 거래 정합성 우선
매수/매도는 단순 로그가 아니라 자산 상태를 바꾸는 행위이므로,
trade_orders와 holdings, users.cash_balance는 같은 트랜잭션 안에서 반영한다.

### 2-2. 읽기 성능을 고려한 보조 컬럼 허용
채팅방 목록 조회는 마지막 메시지, unread count, 마지막 활동 시간까지 보여줘야 한다.
이를 위해 `chat_rooms.last_message_id`, `chat_rooms.last_message_at`,
`chat_room_members.last_read_message_id` 같은 컬럼을 둔다.

### 2-3. 무한 스크롤은 keyset pagination 전제
거래 내역, 메시지 목록, 시세 이력은 모두 시간이 지나면 데이터가 커진다.
따라서 초기부터 offset보다 keyset pagination을 염두에 두고 설계한다.

### 2-4. 현재 상태와 이력은 분리
현재 상태를 빠르게 보여주는 테이블과,
기록용 이력 테이블을 분리한다.

예:
- 현재 보유 상태: `holdings`
- 거래 이력: `trade_orders`
- 현재 시세: `stocks.current_price`
- 시세 이력: `stock_price_ticks`

---

## 3. 엔티티 관계 요약

### 핵심 관계
- users 1 : N trade_orders
- users 1 : N holdings
- users 1 : N notifications
- users N : N chat_rooms (through chat_room_members)
- stocks 1 : N stock_price_ticks
- stocks 1 : 1 chat_rooms
- chat_rooms 1 : N chat_messages

---

## 4. Mermaid ERD

```mermaid
erDiagram
    USERS ||--o{ HOLDINGS : owns
    USERS ||--o{ TRADE_ORDERS : places
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ CHAT_ROOM_MEMBERS : joins
    USERS ||--o{ CHAT_MESSAGES : sends
    USERS ||--o{ PORTFOLIO_DAILY_SNAPSHOTS : records

    STOCKS ||--o{ STOCK_PRICE_TICKS : has
    STOCKS ||--|| CHAT_ROOMS : has

    CHAT_ROOMS ||--o{ CHAT_ROOM_MEMBERS : contains
    CHAT_ROOMS ||--o{ CHAT_MESSAGES : contains

    USERS {
        bigint id PK
        varchar email
        varchar password_hash
        varchar nickname
        decimal cash_balance
        datetime created_at
        datetime updated_at
    }

    STOCKS {
        bigint id PK
        varchar symbol
        varchar name
        varchar market_type
        decimal current_price
        decimal price_change_rate
        datetime updated_at
    }

    STOCK_PRICE_TICKS {
        bigint id PK
        bigint stock_id FK
        decimal price
        bigint volume
        datetime tick_time
    }

    HOLDINGS {
        bigint id PK
        bigint user_id FK
        bigint stock_id FK
        bigint quantity
        decimal avg_buy_price
        datetime evaluated_at
    }

    TRADE_ORDERS {
        bigint id PK
        bigint user_id FK
        bigint stock_id FK
        varchar trade_type
        decimal price
        bigint quantity
        decimal total_amount
        datetime created_at
    }

    CHAT_ROOMS {
        bigint id PK
        bigint stock_id FK
        varchar room_name
        bigint last_message_id
        datetime last_message_at
        datetime created_at
    }

    CHAT_ROOM_MEMBERS {
        bigint id PK
        bigint room_id FK
        bigint user_id FK
        bigint last_read_message_id
        datetime joined_at
    }

    CHAT_MESSAGES {
        bigint id PK
        bigint room_id FK
        bigint sender_id FK
        varchar content
        datetime created_at
        boolean deleted
    }

    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar notification_type
        bigint target_id
        text payload_json
        boolean is_read
        datetime created_at
    }

    PORTFOLIO_DAILY_SNAPSHOTS {
        bigint id PK
        bigint user_id FK
        date snapshot_date
        decimal total_asset
        decimal profit_loss
        decimal profit_rate
        datetime created_at
    }