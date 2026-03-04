# 💬 message-test

Spring Boot 3 기반 실시간 채팅(WebSocket STOMP) + 권한 관리 프로젝트입니다.

---

## 📌 프로젝트 목적
- REST API + WebSocket 이벤트를 함께 설계/구현
- 역할 기반 권한(ADMIN/MODERATOR/MEMBER/GUEST) 제어 연습
- 채팅 서비스 운영에 필요한 기본 관리 흐름 실습

## 🧱 기술 스택
- Java 17
- Spring Boot 3.x
- Spring WebSocket (STOMP, SockJS)
- Spring Security
- JDBC + MySQL/H2
- Gradle

## ✨ 핵심 기능
- 실시간 메시지 송수신
- 권한별 채널 접근 제어
- 메시지 수정/삭제 이벤트 반영
- 관리자 관리 기능

## ⚡ 실행 방법
```bash
./gradlew bootRun
```
(Windows: `gradlew.bat bootRun`)

## 📁 디렉토리 구조
- `app/` : 메인 애플리케이션 코드
- `docs/` : 문서
- `gradle/` : 빌드 설정

## ✅ 상태
- 기본 채팅/권한 흐름 구현 완료
