# message-test

실시간 채팅(WebSocket STOMP) + 인증/권한 관리 + 관리자 기능을 포함한 Spring Boot 3 기반 샘플 프로젝트입니다.

> Current Version: **v0.5.0**

---

## 1) 프로젝트 개요

`message-test`는 **실시간 메시징 + 권한 제어 + 운영 관리**를 함께 다루는 채팅 서비스 프로젝트입니다.  
단순 채팅 CRUD를 넘어서, 실제 운영 시 필요한 인증·인가 흐름과 관리 기능까지 통합 구현하는 것을 목표로 했습니다.

### 프로젝트 목적
- WebSocket(STOMP) 실시간 통신과 REST API를 함께 설계/구현하는 경험 확보
- 역할 기반 권한 제어(ADMIN/MODERATOR/MEMBER/GUEST) 적용
- 표준화된 API 성공/실패 응답 체계 구축
- 인메모리 기반 구조를 JDBC + MySQL/H2 영구저장 구조로 전환

### 해결하려는 문제
단순히 메시지를 주고받는 기능에서 끝나지 않고,
- 사용자별 채널 접근 권한을 통제하고
- 메시지 수정/삭제 이벤트를 실시간 반영하며
- 관리자 관점에서 사용자 역할/채널 권한을 제어할 수 있는
**운영형 채팅 시스템의 핵심 플로우**를 구현했습니다.

### 프로젝트 유형
- **개인 프로젝트 (1인 개발)**

---

## 2) 기술 스택 (선택 이유 포함)

- **Java 17**  
  LTS 버전 기반 안정성과 생태계 호환성을 고려해 선택했습니다.

- **Spring Boot 3.3.2**  
  REST + WebSocket 구성을 표준 방식으로 빠르게 구성할 수 있어 선택했습니다.

- **Spring WebSocket (STOMP + SockJS)**  
  채널 단위 publish/subscribe 패턴으로 실시간 채팅 구현에 적합해 사용했습니다.

- **Spring JDBC**  
  SQL 흐름과 DB 접근 책임을 명확히 다루기 위해 JDBC 기반으로 구현했습니다.

- **MySQL / H2**  
  운영 환경(MySQL)과 로컬 개발(H2) 모두 대응하기 위해 이원화했습니다.

- **Bean Validation**  
  입력 검증을 일관되게 처리하고 표준 에러 응답과 연계하기 위해 사용했습니다.

- **Gradle (Wrapper 8.10.2)**  
  빌드 자동화 및 실행/테스트 태스크 관리를 위해 사용했습니다.

---

## 3) 프로젝트 구조

### 패키지 구조

```text
app/src/main/java/message
├─ auth/
│  ├─ AuthController.java
│  ├─ AuthService.java
│  └─ ...
├─ chat/
│  ├─ ChatController.java
│  ├─ ChatService.java
│  ├─ ChatRepository.java
│  ├─ AccessControlService.java
│  └─ WebSocketConfig.java
└─ common/
   ├─ api/
   │  ├─ ApiResponse.java
   │  ├─ ApiErrorResponse.java
   │  └─ GlobalExceptionHandler.java
   └─ error/
      ├─ ErrorCode.java
      └─ ApiException.java
```

### 계층 분리 원칙

- **Controller**: HTTP/WS 입출력, 요청 파라미터 처리
- **Service**: 비즈니스 규칙 및 권한 검증
- **Repository**: SQL 실행 및 데이터 영속성 처리

핵심 흐름:
- `Controller → Service → Repository`

---

## 4) 주요 기능

### [Auth]
- 회원가입
- 로그인
- 사용자 정보 기반 접근 제어 연계

### [Chat]
- 채널 목록 조회
- 채널별 메시지 조회/전송
- 메시지 수정/삭제
- 삭제 이벤트 실시간 반영

### [Access/Role]
- 역할 기반 채널 읽기/쓰기 권한 제어
- 사용자 역할 변경(관리자 기능)
- 채널 생성/순서 변경/권한 정책 설정

### [UI]
- 로그인/회원가입/채팅 테스트 화면 제공
- URL 자동 링크화
- YouTube 링크 프리뷰(oEmbed)
- Base64 이미지(`[img]data:image/...`) 렌더링

---

## 5) 핵심 동작 방식

1. 클라이언트가 `/ws`로 STOMP 연결
2. 채널 구독: `/sub/channels/{channelId}`
3. 메시지 발행: `/pub/channels/{channelId}`
4. 서버에서 권한 확인 후 브로드캐스트
5. 클라이언트가 실시간 렌더링
6. 삭제 이벤트 발생 시 대상 메시지 즉시 제거 + 시스템 안내 출력

---

## 6) API 요약

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

## 7) 응답 포맷

### Success
```json
{
  "timestamp": "2026-02-24T02:20:00Z",
  "success": true,
  "data": {}
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

---

## 8) 실행 방법

### 사전 요구사항
- Java 17
- (선택) MySQL 실행 환경

### 로컬 실행 (H2 기본)
```bash
gradlew.bat :app:bootRun
```

### 테스트
```bash
gradlew.bat clean test
```

### 빌드
```bash
gradlew.bat clean :app:bootJar
```

### 접속 URL
- App: `http://localhost:8080`
- Login: `http://localhost:8080/login.html`
- WS Test: `http://localhost:8080/ws-test.html`
- Health: `http://localhost:8080/health`

### MySQL 사용 시 환경변수 예시
```bash
set DB_URL=jdbc:mysql://localhost:3306/message_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
set DB_USER=root
set DB_PASSWORD=your_password
set DB_DRIVER=com.mysql.cj.jdbc.Driver
```

---

## 9) 테스트 범위

- 헬스체크 API 정상 동작 검증
- 회원가입 유효성 실패 케이스 검증
- 회원가입 → 로그인 성공 플로우 검증

> 테스트 클래스: `app/src/test/java/message/ApplicationApiTest.java`

---

## 10) 리팩토링 로그 (2026-02)

### 1차
- WebSocket/메시지 수정·삭제 동작 안정화
- YouTube 링크 프리뷰 적용
- 삭제 이벤트 실시간 반영

### 2차
- `ErrorCode`, `ApiException` 도입
- `GlobalExceptionHandler` 확장
- 인증 에러 상태코드 정리 (`CONFLICT`, `UNAUTHORIZED` 등)

### 3차
- `ChatRepository` 도입으로 책임 분리
- REST 응답을 `ApiResponse<T>`로 점진 통일
- 프론트에서 래핑/기존 응답 동시 호환 처리

### 4차
- JDBC + MySQL/H2 드라이버 추가
- `schema.sql` 기반 초기화
- 인증/채널/권한 DB 기반 전환
- 기본 관리자/채널 시드 로직 추가
- 초기 템플릿 잔재(`massege` 패키지, bin 추적 파일) 제거
- Gradle wrapper 안정화(8.10.2)

---

## 11) 트러블슈팅

### `:app:bootJar` 실패 이슈
- 증상: 특정 Gradle 버전에서 `CopyProcessingSpec.getDirMode()` 관련 실패
- 조치: Gradle Wrapper를 8.10.2로 조정
- 결과: `clean test :app:bootJar` 정상 통과

### 산출물 추적 이슈
- 증상: `app/bin` 클래스 파일이 Git에 추적
- 조치: 추적 해제 + `.gitignore` 보강(`app/bin`, `**/bin`)
- 결과: 소스 중심 이력 관리로 정리

---

## 12) 이 프로젝트에서 보여준 역량

- 실시간 통신(WebSocket)과 역할 기반 권한 제어를 함께 구현
- 예외/응답 포맷 표준화로 API 일관성 확보
- DB 전환 및 빌드 이슈 해결 과정을 통해 문제 해결 경험 축적

---

## 13) 스크린샷

### 로그인 화면
![login-page](docs/screenshots/login-page.png)

### 회원가입 화면
![signup-page](docs/screenshots/signup-page.png)

### 채팅 테스트 화면
![ws-test-page](docs/screenshots/ws-test-page.png)

### YouTube 링크 프리뷰 화면
![hyperlink-test](docs/screenshots/hyperlink-test.png)

### 실시간 삭제 처리 코드 캡처
![code-realtime-delete](docs/screenshots/code-realtime-delete.png)

---

## 14) 다음 개선 계획

- [ ] 외부 배포 URL 공개
- [ ] GitHub Actions CI(test) 구성
- [ ] 통합 테스트 확장(권한/채널/관리자 플로우)
- [ ] 성능/부하 테스트 지표 추가
