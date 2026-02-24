# message-test

실시간 채팅(WebSocket STOMP) + 인증/권한 관리 + 관리자 기능을 포함한 Spring Boot 3 샘플 프로젝트입니다.

## 목차
- [Tech Stack](#tech-stack)
- [실행 방법 (Windows)](#실행-방법-windows)
- [프로젝트 구조](#프로젝트-구조)
- [핵심 동작 방식](#핵심-동작-방식)
- [API Summary](#api-summary)
  - [Auth](#auth)
  - [Chat](#chat)
  - [Admin](#admin)
- [WebSocket Contract](#websocket-contract)
- [스크린샷 (코드/동작)](#스크린샷-코드동작)
- [Engineering Improvements (2026-02)](#engineering-improvements-2026-02)
- [Test](#test)

## Tech Stack
- Java 17
- Spring Boot 3.3.2
- Spring Web / WebSocket(STOMP + SockJS)
- Bean Validation
- Gradle

## 실행 방법 (Windows)
```bash
gradlew.bat :app:bootRun
```

기본 접속:
- App: `http://localhost:8080`
- Login: `http://localhost:8080/login.html`
- WebSocket Test: `http://localhost:8080/ws-test.html`
- Health: `http://localhost:8080/health`

## 프로젝트 구조
```text
app/src/main/java/message
├─ auth/      # 회원가입/로그인
├─ chat/      # 채팅, 권한, Presence, WebSocket 설정
└─ common/    # 공통 예외/응답 처리

app/src/main/resources/static
├─ ws-test.html  # 채팅 UI (실시간 메시지, 링크/이미지 프리뷰, 메뉴 액션)
└─ css/ws-test.css
```

## 핵심 동작 방식
1. 클라이언트가 `/ws`로 STOMP 연결
2. 채널 구독: `/sub/channels/{channelId}`
3. 메시지 전송: `/pub/channels/{channelId}`
4. 서버가 권한 검증 후 브로드캐스트
5. 클라이언트가 실시간 렌더링
   - 일반 텍스트/URL 링크화
   - YouTube 썸네일 + 제목(oEmbed)
   - `[img]data:image/...` 형식 이미지 렌더링
6. 메시지 삭제 시
   - 서버가 삭제 이벤트(`[deleted]...`) 브로드캐스트
   - 모든 클라이언트가 해당 메시지 DOM 즉시 제거
   - 시스템 안내 문구를 중앙 회색 작은 텍스트로 표시

## API Summary
### Auth
- `POST /api/auth/signup`
- `POST /api/auth/login`

### Chat
- `GET /api/channels`
- `GET /api/channels/{channelId}/messages?sender={name}&limit={n}`
- `GET /api/channels/{channelId}/online-users?sender={name}`
- `GET /api/access/{channelId}?sender={name}`
- `PUT /api/channels/{channelId}/messages/{messageId}?sender={name}`
- `DELETE /api/channels/{channelId}/messages/{messageId}?sender={name}`

### Admin
- `POST /api/admin/users/{sender}/role?actor={name}&role={ADMIN|MODERATOR|MEMBER|GUEST}`
- `POST /api/admin/channels?sender={name}&channelId={id}`
- `POST /api/admin/channels/reorder?sender={name}`
- `POST /api/admin/channels/{channelId}/permissions?role={...}&canRead={true|false}&canWrite={true|false}`

## WebSocket Contract
- Endpoint: `/ws`
- Publish prefix: `/pub`
- Subscribe prefix: `/sub`

예시:
- 채널 메시지 publish: `/pub/channels/{channelId}`
- 채널 메시지 subscribe: `/sub/channels/{channelId}`
- 채널 입장 publish: `/pub/channels/{channelId}/join`
- 접속자 subscribe: `/sub/channels/{channelId}/presence`

## 스크린샷 (코드/동작)
### 1) 채팅 동작 화면
![chat-main](docs/screenshots/chat-main.png)

### 2) 실시간 삭제 처리 코드 캡처
![code-realtime-delete](docs/screenshots/code-realtime-delete.png)

## Engineering Improvements (2026-02)
- 요청 DTO Bean Validation 적용
- 컨트롤러 파라미터 검증(범위/필수값) 강화
- 전역 예외 처리(`@RestControllerAdvice`) 도입
- 일관된 에러 응답 스키마(`ApiErrorResponse`) 제공
- 루트 프로젝트 네이밍 정리 (`massege-test` → `message-test`)
- API 레벨 통합 테스트(MockMvc) 추가
- 이미지 전송 payload 한도 확장(WebSocket transport/DTO)
- URL 자동 링크화 + YouTube 썸네일/제목(oEmbed) 프리뷰
- 메시지 수정/삭제 메뉴 동작 안정화
- 삭제 이벤트 실시간 반영(메시지 즉시 제거 + 시스템 안내)
- 시스템 안내 UI 정리(중앙/회색/작은 볼드)

## Test
```bash
gradlew.bat :app:test
```