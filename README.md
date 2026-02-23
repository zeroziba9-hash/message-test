# message-test

실시간 채팅(WebSocket STOMP) + 간단 인증 API를 포함한 Spring Boot 3 기반 샘플 프로젝트입니다.

## Tech Stack
- Java 17
- Spring Boot 3.3.2
- Spring Web / WebSocket
- Bean Validation
- Gradle

## Run (Windows)
```bash
gradlew.bat :app:bootRun
```

기본 접속:
- App: `http://localhost:8080`
- Login: `http://localhost:8080/login.html`
- WebSocket Test: `http://localhost:8080/ws-test.html`
- Health: `http://localhost:8080/health`

## API Summary
### Auth
- `POST /api/auth/signup`
- `POST /api/auth/login`

### Chat
- `GET /api/channels`
- `GET /api/channels/{channelId}/messages?sender={name}&limit={n}`
- `GET /api/channels/{channelId}/online-users?sender={name}`
- `GET /api/access/{channelId}?sender={name}`

### Admin
- `POST /api/admin/users/{sender}/role?role={ADMIN|MODERATOR|MEMBER|GUEST}`
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

## Engineering Improvements (2026-02)
- 요청 DTO에 Bean Validation 적용
- 컨트롤러 파라미터 검증(범위/필수값) 강화
- 전역 예외 처리(`@RestControllerAdvice`) 도입
- 일관된 에러 응답 스키마(`ApiErrorResponse`) 제공
- 루트 프로젝트 네이밍 정리 (`massege-test` → `message-test`)
- API 레벨 통합 테스트 추가(MockMvc)

## Test
```bash
gradlew.bat :app:test
```
