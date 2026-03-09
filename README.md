# message-test

실시간 채팅(WebSocket STOMP) + 인증/권한 관리 + 관리자 기능을 포함한 Spring Boot 3 기반 샘플 프로젝트입니다.

> Current Version: **v0.5.0**

## 1) 프로젝트 개요

`message-test`는 **실시간 메시징 + 권한 제어 + 운영 관리**를 함께 다루는 채팅 서비스 프로젝트입니다.

## 2) 기술 스택
- Java 17
- Spring Boot 3.3.2
- Spring Web / WebSocket (STOMP)
- Spring JDBC
- Bean Validation
- Spring Boot Actuator
- Micrometer Prometheus Registry
- Gradle (multi-module root + `app`)

## 3) 주요 기능
### Auth
- 회원가입 / 로그인
- 입력값 검증 + 중복 아이디 방지 + 표준 오류 응답

### Chat
- 채널 목록 조회
- 채널별 메시지 조회/전송
- 메시지 수정/삭제
- 삭제 이벤트 실시간 반영

### Access/Role
- 역할 기반 채널 읽기/쓰기 권한 제어
- 사용자 역할 변경(관리자)
- 채널 생성/순서 변경/권한 정책 설정

## 4) API 요약
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

---

## 5) Authentication Response Model

이 프로젝트의 인증 API는 **성공/실패 응답 포맷을 분리**해서 운영 시 파싱 안정성을 높였습니다.

- 성공: `ApiResponse<T>`
- 실패: `ApiErrorResponse`
- 예외 라우팅: `GlobalExceptionHandler`

### 5.1 성공 응답 예시 (회원가입 / 로그인)
```json
{
  "timestamp": "2026-03-09T10:12:34.123Z",
  "success": true,
  "data": {
    "success": true,
    "message": "로그인 성공",
    "username": "user01",
    "nickname": "제로"
  }
}
```

### 5.2 실패 응답 예시 (유효성 오류 / 인증 실패)
```json
{
  "timestamp": "2026-03-09T10:12:34.123Z",
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
  "details": []
}
```

### 5.3 Auth API Request/Response Contract

#### POST `/api/auth/signup`
| Field | Type | Required | Validation |
|---|---|---|---|
| username | string | Yes | `^[a-z0-9]{4,20}$` |
| password | string | Yes | length `4~20` |
| passwordConfirm | string | Yes | must match `password` |
| nickname | string | Yes | length `2~20`, `관리자` reserved |

Success `data` schema:
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "username": "user01",
  "nickname": "제로"
}
```

#### POST `/api/auth/login`
| Field | Type | Required | Validation |
|---|---|---|---|
| username | string | Yes | not blank |
| password | string | Yes | not blank |

Success `data` schema:
```json
{
  "success": true,
  "message": "로그인 성공",
  "username": "user01",
  "nickname": "제로"
}
```

Common error codes (auth path):
- `BAD_REQUEST`: invalid input / format mismatch
- `CONFLICT`: duplicate username on signup
- `UNAUTHORIZED`: invalid username/password on login

> 참고: 현재 인증 API는 `signup/login` 중심입니다. (로그아웃 엔드포인트는 추후 세션/토큰 정책과 함께 확장 가능)

---

## 6) Core Implementation Snippets

### 6.1 AuthController: 엔드포인트 + 표준 성공 응답 래핑
`app/src/main/java/message/auth/AuthController.java`
```java
@PostMapping("/signup")
public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ApiResponse.ok(authService.signup(request));
}

@PostMapping("/login")
public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
}
```

### 6.2 AuthService: 중복 체크 + JDBC 저장 + 인증 검증
`app/src/main/java/message/auth/AuthService.java`
```java
if (existsByUsername(username)) {
    throw new ApiException(ErrorCode.CONFLICT, "이미 사용 중인 아이디입니다.");
}

jdbcTemplate.update(
    "INSERT INTO users(username, password, nickname, created_at) VALUES(?, ?, ?, ?)",
    username, password, nickname, Timestamp.from(Instant.now()));
```

```java
UserAccount account = findByUsername(username);
if (account == null || !account.password().equals(password)) {
    throw new ApiException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
}
```

### 6.3 공통 성공 응답 모델
`app/src/main/java/message/common/api/ApiResponse.java`
```java
public record ApiResponse<T>(
        Instant timestamp,
        boolean success,
        T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(Instant.now(), true, data);
    }
}
```

### 6.4 공통 오류 응답 + 전역 예외 처리
- `app/src/main/java/message/common/api/ApiErrorResponse.java`
- `app/src/main/java/message/common/api/GlobalExceptionHandler.java`

```java
@ExceptionHandler(ApiException.class)
public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
    var errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.of(
            errorCode.getStatus().value(),
            errorCode.getCode(),
            e.getMessage(),
            e.getDetails()));
}
```

---

## 7) 실행
```bash
gradlew.bat :app:bootRun
```

기본 접속:
- App: `http://localhost:8080`
- Login: `http://localhost:8080/login.html`
- WS Test: `http://localhost:8080/ws-test.html`
- Health: `http://localhost:8080/health`

테스트:
```bash
gradlew.bat :app:test
```

## 8) Observability / Ops
- Health: `/actuator/health`
- Readiness/Liveness probes: enabled
- Prometheus metrics: `/actuator/prometheus`
- 운영 대응 가이드: `docs/runbook.md`
- ADR: `docs/adr/0001-session-auth-over-token.md`

## 9) Performance Smoke (k6)
```bash
k6 run perf/k6-smoke.js
```

## 10) Local Infra (MySQL + Redis)
```bash
docker compose -f docker-compose.infra.yml up -d
```

## 11) 실행 스크린샷

### 로그인 화면
![login-page](docs/screenshots/login-page.png)

### 회원가입 화면
![signup-page](docs/screenshots/signup-page.png)

### 채팅 테스트 화면
![ws-test-page](docs/screenshots/ws-test-page.png)

### 채팅 메인 화면
![chat-main](docs/screenshots/chat-main.png)

## 12) Engineering Standards
- CI: `.github/workflows/ci.yml`
- PR Template: `.github/pull_request_template.md`
- Code style baseline: `.editorconfig`
