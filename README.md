# message-test

실시간 채팅(WebSocket STOMP) + 인증/권한 관리 + 관리자 기능을 포함한 Spring Boot 3 샘플 프로젝트입니다.

> Current Version: **v0.3.0**

## 목차
- [Tech Stack](#tech-stack)
- [Run (Windows)](#run-windows)
- [Architecture](#architecture)
- [핵심 동작 방식](#핵심-동작-방식)
- [API Summary](#api-summary)
- [Response Format](#response-format)
- [Refactoring Log (2026-02)](#refactoring-log-2026-02)
- [Screenshots](#screenshots)
- [Test](#test)

## Tech Stack
- Java 17
- Spring Boot 3.3.2
- Spring Web / WebSocket(STOMP + SockJS)
- Spring JDBC
- MySQL (prod) / H2 (local default)
- Bean Validation
- Gradle

## Run (Windows)
```bash
gradlew.bat :app:bootRun
```

기본은 H2 인메모리 DB로 실행됩니다(개발 편의용).

MySQL 사용 시 환경변수 예시:
```bash
set DB_URL=jdbc:mysql://localhost:3306/message_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
set DB_USER=root
set DB_PASSWORD=your_password
set DB_DRIVER=com.mysql.cj.jdbc.Driver
```

기본 접속:
- App: `http://localhost:8080`
- Login: `http://localhost:8080/login.html`
- WebSocket Test: `http://localhost:8080/ws-test.html`
- Health: `http://localhost:8080/health`

## Architecture
```text
app/src/main/java/message
├─ auth/
│  ├─ AuthController.java
│  └─ AuthService.java
├─ chat/
│  ├─ ChatController.java
│  ├─ ChatService.java
│  ├─ ChatRepository.java        # (3차 리팩토링) 저장소 분리
│  ├─ AccessControlService.java
│  └─ WebSocketConfig.java
└─ common/
   ├─ api/
   │  ├─ ApiResponse.java        # (2차/3차) 성공 응답 표준화
   │  ├─ ApiErrorResponse.java
   │  └─ GlobalExceptionHandler.java
   └─ error/
      ├─ ErrorCode.java          # (2차) 에러 코드 체계화
      └─ ApiException.java
```

## 핵심 동작 방식
1. 클라이언트가 `/ws` STOMP 연결
2. 메시지 구독 `/sub/channels/{channelId}`
3. 메시지 전송 `/pub/channels/{channelId}`
4. 서버 권한 체크 후 브로드캐스트
5. 클라이언트 실시간 렌더링
   - URL 링크화
   - YouTube 썸네일 + 제목(oEmbed)
   - 이미지(`[img]data:image/...`) 렌더링
6. 메시지 삭제 이벤트
   - 서버가 `[deleted]` 이벤트 전송
   - 클라이언트가 대상 message id를 즉시 제거
   - 중앙 회색 시스템 안내 출력

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

## Response Format
### Success
```json
{
  "timestamp": "2026-02-24T02:20:00Z",
  "success": true,
  "data": { }
}
```

### Error
```json
{
  "timestamp": "2026-02-24T02:20:00Z",
  "status": 403,
  "code": "FORBIDDEN",
  "message": "관리자만 수행할 수 있습니다",
  "details": []
}
```

## Refactoring Log (2026-02)
### 1차
- WebSocket/메시지 수정·삭제 동작 안정화
- 링크 프리뷰(YouTube 썸네일/제목) 적용
- 삭제 이벤트 실시간 반영

### 2차
- `ErrorCode`, `ApiException` 도입
- `GlobalExceptionHandler` 확장
- 인증(Auth) 에러를 HTTP 의미에 맞게 정리 (`CONFLICT`, `UNAUTHORIZED` 등)

### 3차
- `ChatRepository` 도입으로 Service/저장소 책임 분리
- Auth/Chat REST 응답을 `ApiResponse<T>`로 점진 통일
- 프론트(`login.html`, `ws-test.html`)에서 래핑 응답/기존 응답 동시 호환 처리
- 실패 테스트 수정 (`$.username` → `$.data.username`)
- 불필요한 문서 산출물 정리

### 4차 (DB 영구저장)
- `spring-boot-starter-jdbc` + MySQL/H2 드라이버 추가
- `schema.sql` 기반 테이블 자동 초기화
- 인증(`AuthService`)을 DB 기반으로 전환 (users)
- 채널/메시지(`ChatRepository`, `ChatService`)를 DB 기반으로 전환
- 역할/채널 권한(`AccessControlService`)을 DB 기반으로 전환
- 기본 관리자 계정/역할/기본 채널 시드 로직 추가

### 대표 코드 (실시간 삭제)
```js
function handleRealtimeEvent(msg) {
  const deletedActor = extractDeletedEvent(msg);
  if (!deletedActor) return false;

  const targetId = Number(msg?.id || 0);
  if (targetId > 0) {
    const targetEl = messagesEl.querySelector(`[data-message-id="${targetId}"]`);
    if (targetEl) targetEl.remove();
  }

  renderMessage({
    sender: "시스템",
    sentAt: msg?.sentAt || new Date(),
    content: `메시지가 ${deletedActor}에 의해 삭제되었습니다.`
  }, false);
  return true;
}
```

## Screenshots
### 1) 로그인 화면
![login-page](docs/screenshots/login-page.png)

### 2) 회원가입 화면
![signup-page](docs/screenshots/signup-page.png)

### 3) 채팅 테스트 화면
![ws-test-page](docs/screenshots/ws-test-page.png)

### 4) 하이퍼링크(YouTube 프리뷰) 테스트 화면
- 재현 URL: `http://localhost:8080/ws-test.html?demo=link`
![hyperlink-test](docs/screenshots/hyperlink-test.png)

### 5) 실시간 삭제 처리 코드 캡처
![code-realtime-delete](docs/screenshots/code-realtime-delete.png)

## Test
```bash
gradlew.bat :app:test
```
