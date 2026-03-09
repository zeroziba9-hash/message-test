# ADR-0001: Prefer session-based authentication for current web client

## Status
Accepted

## Context
Current client is server-hosted HTML page + WebSocket(STOMP) and does not require cross-platform token distribution yet.

## Decision
Use session-based login state for HTTP + WebSocket access checks.

## Consequences
### Positive
- Simpler login/logout lifecycle for browser-first app
- Lower accidental token leakage risk in beginner deployments
- Easier revocation by invalidating session

### Negative
- Horizontal scale requires sticky session or external session store
- Less convenient for 3rd-party/mobile clients

## Follow-up
If mobile/public API scope grows, introduce JWT for API gateway edge only and keep internal permission checks centralized.
