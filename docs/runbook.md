# Operational Runbook (message-test)

## SLO (starter)
- API success rate: >= 99.5%
- p95 latency (GET /api/channels): <= 300ms
- WebSocket reconnect success within 30s

## Incident 1: DB unavailable
### Symptoms
- Login or message history endpoint returns 5xx
- sudden spike in exception logs

### Immediate actions
1. Check DB process / connectivity
2. Switch app to maintenance banner (if available)
3. Restart app only after DB healthy

### Verification
- /health returns UP
- login + channel list smoke test passes

## Incident 2: Authorization regression
### Symptoms
- non-admin can mutate admin-only endpoints

### Immediate actions
1. Disable affected endpoint route (temporary guard)
2. Roll back latest deploy
3. Inspect authorization checks in Service layer

### Verification
- 403 test cases pass for MEMBER/GUEST roles

## Incident 3: WebSocket reconnect storm
### Symptoms
- CPU spike + many reconnect attempts per second

### Immediate actions
1. Enable client reconnect backoff
2. Rate-limit connect attempts by IP/session
3. Monitor active sessions stabilization

### Verification
- reconnect attempts normalize
- message delivery delay returns to baseline
