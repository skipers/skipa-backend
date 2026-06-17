# API Response Time Load Test

SKIPA backend API 응답 시간 p95를 측정하는 k6 부하 테스트입니다.

## 사전 조건

- 백엔드 서버가 실행 중이어야 합니다.
- 기본값은 local profile 샘플 계정 기준입니다.
  - Legal: `legal01` / `1234`
  - Business: `biz01` / `1234`
- local profile은 샘플 특허/리뷰/보고서를 자동 생성하므로 조회 API 측정에 바로 사용할 수 있습니다.

## 실행

```bash
k6 run load-tests/api-response-time.js
```

로컬 서버 주소가 다르면 `BASE_URL`을 지정합니다.

```bash
BASE_URL=http://localhost:8080 k6 run load-tests/api-response-time.js
```

부하 조건과 목표 p95를 바꿀 수 있습니다.

```bash
VUS=50 DURATION=5m TARGET_P95_MS=300 k6 run load-tests/api-response-time.js
```

## 주요 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | 백엔드 서버 URL |
| `API_PREFIX` | `/api/v1` | API prefix |
| `LEGAL_LOGIN_ID` | `legal01` | Legal 사용자 로그인 ID |
| `LEGAL_PASSWORD` | `1234` | Legal 사용자 비밀번호 |
| `BUSINESS_LOGIN_ID` | `biz01` | Business 사용자 로그인 ID |
| `BUSINESS_PASSWORD` | `1234` | Business 사용자 비밀번호 |
| `VUS` | `20` | 동시 가상 사용자 수 |
| `RAMP_UP` | `30s` | ramp-up 시간 |
| `DURATION` | `2m` | 최대 부하 유지 시간 |
| `RAMP_DOWN` | `30s` | ramp-down 시간 |
| `TARGET_P95_MS` | `300` | API 응답 시간 p95 기준 |
| `THINK_TIME_SECONDS` | `0.2` | VU 반복 사이 대기 시간 |

## 확인할 지표

k6 결과에서 아래 지표를 보면 됩니다.

```text
http_req_duration{type:api}
```

threshold는 `http_req_duration{type:api}: p(95)<300`으로 설정되어 있습니다. 로그인과 테스트 데이터 탐색은 `type:setup`으로 분리해서 API p95 기준에서 제외했습니다.
