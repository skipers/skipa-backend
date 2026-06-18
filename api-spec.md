# API 명세서

변경 날짜: 2026-06-18

## 기본 정보

| 항목 | 내용 |
| --- | --- |
| Base URL | `https://api.skipa.internal` |
| URL 버전 prefix | `/api/v1` |
| 인증 방식 | JWT Bearer Token (`Authorization: Bearer <token>`) |
| 내부 API 인증 방식 | Internal API Key (`X-Internal-Api-Key: <secret>`) |
| 토큰 발급 | `POST /api/v1/auth/login` |
| 토큰 만료 | access token 10분 / refresh token 7일 |

## 공통 응답 형식

모든 응답은 `application/json`이며 아래 구조를 따릅니다.

**성공**

```json
{
  "success": true,
  "data": {}
}
```

**실패**

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

페이지 조회 응답의 `data`에는 `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`가 포함됩니다.

## 공통 에러 코드

| HTTP Status | code | 설명 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 요청 파라미터 오류 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 또는 상태 충돌 |
| 502 | `EXTERNAL_SERVICE_ERROR` | 외부 시스템 연동 실패 |
| 500 | `AI_SERVER_ERROR` | AI 서버 연동 실패 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

## 역할

| Role | 설명 |
| --- | --- |
| `ADMIN` | 사용자 승인, 부서 관리, 전체 조회, 특허 기본 정보 관리 |
| `LEGAL` | 부서 관리, 특허 관리, 검토 요청, 검토 현황 조회, 보고서 관리 |
| `BUSINESS` | 본인 부서 담당 특허 조회, 요청받은 검토 확인 및 의견 제출 |

## Enum

| 구분 | 값 |
| --- | --- |
| 사용자 역할 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 사용자 상태 | `PENDING`, `ACTIVE` |
| 부서 상태 | `ACTIVE`, `INACTIVE` |
| 권리 상태 | `APPLIED`, `PUBLISHED`, `REGISTERED`, `REJECTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `WITHDRAWN` |
| 특허 승인 상태 | `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `WITHDRAWN` |
| 연차료 납부 상태 | `PAID`, `UNPAID`, `ABANDONED` |
| 특허 추출 작업 상태 | `UPLOAD_PENDING`, `ANALYZING`, `COMPLETED`, `FAILED` |
| 보고서 처리 상태 | `GENERATING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 사전 평가 처리 상태 | `PROCESSING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 사전 평가 채팅 역할 | `USER`, `ASSISTANT` |
| 검토 주기 유형 | `QUARTERLY`, `AD_HOC` |
| 검토 제출 상태 | `SCHEDULED`, `PENDING`, `OVERDUE`, `SUBMITTED` |
| 사업부 의견 | `MAINTAIN`, `ABANDON` |

보고서와 사전 평가는 백엔드 enum 값을 그대로 사용합니다. 최종 완료 상태는 `COMPLETED`가 아니라 `REPORT_COMPLETED` 또는 `EMBEDDING_COMPLETED`입니다.

---

## API 목록

### 1. 인증

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/api/v1/auth/login` | ID/PW 검증 후 access token, refresh token, 사용자 정보 반환 | 없음 |
| 회원가입 | `POST` | `/api/v1/auth/register` | `PENDING` 상태의 사용자 계정 생성 | 없음 |
| 로그아웃 | `POST` | `/api/v1/auth/logout` | 토큰 무효화 | 인증 사용자 |
| 내 정보 조회 | `GET` | `/api/v1/auth/me` | 현재 로그인 사용자 정보 반환 | 인증 사용자 |
| 토큰 갱신 | `POST` | `/api/v1/auth/refresh` | refresh token으로 access token 재발급 | 없음 |

---

#### `POST /api/v1/auth/login`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| loginId | string | * | 사용자 로그인 ID |
| password | string | * | 비밀번호 |

**요청 예시**

```json
{
  "loginId": "legal01",
  "password": "1234"
}
```

**응답**

| Name | Type | Description |
| --- | --- | --- |
| accessToken | string | 유효기간 10분. 만료 시 `/api/v1/auth/refresh` 호출 |
| refreshToken | string | 유효기간 7일 |
| user.loginId | string | 로그인 ID |
| user.role | string | `ADMIN` / `LEGAL` / `BUSINESS` |
| user.departmentId | long | 소속 부서 ID. `BUSINESS`만 해당, 나머지 `null` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "loginId": "legal01",
      "role": "LEGAL",
      "departmentId": null
    }
  }
}
```

**에러**

| HTTP | code | 설명 |
| --- | --- | --- |
| 401 | `INVALID_LOGIN_REQUEST` | 아이디 또는 비밀번호 불일치 |
| 403 | `PENDING_USER` | 미승인 계정 (`status = PENDING`) |

---

#### `GET /api/v1/auth/me`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| user.id | long | 사용자 DB ID |
| user.loginId | string | 로그인 ID |
| user.name | string | 이름 |
| user.email | string | 이메일 |
| user.role | string | `ADMIN` / `LEGAL` / `BUSINESS` |
| user.status | string | `ACTIVE` (정상) / `PENDING` (승인 대기) |
| user.departmentId | long | 소속 부서 ID. `BUSINESS`만 해당, 나머지 `null` |
| user.departmentName | string | 소속 부서명. `BUSINESS`만 해당, 나머지 `null` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "loginId": "legal01",
      "name": "홍길동",
      "email": "legal01@sk.com",
      "role": "LEGAL",
      "status": "ACTIVE",
      "departmentId": null,
      "departmentName": null
    }
  }
}
```

**에러**: `UNAUTHORIZED`(401)

---

#### `POST /api/v1/auth/refresh`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| refreshToken | string | * | 로그인 시 발급받은 리프레시 토큰 |

**응답**

| Name | Type | Description |
| --- | --- | --- |
| accessToken | string | 새로 발급된 액세스 토큰 |

**응답 예시**

```json
{
  "success": true,
  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

**에러**: `UNAUTHORIZED`(401)

---

#### `POST /api/v1/auth/logout`

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401)

---

### 2. 사용자 가입 승인

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사용자 가입 승인 | `PATCH` | `/api/v1/admin/users/{userId}/approve` | 사용자를 `ACTIVE`로 변경. `BUSINESS` 역할은 활성 부서 지정 필수 | `ADMIN` |

---

### 3. 부서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 부서 목록 조회 | `GET` | `/api/v1/departments` | 활성 부서 목록 조회. `page`, `size` 사용 가능 | `ADMIN`, `LEGAL` |
| 부서 단일 조회 | `GET` | `/api/v1/departments/{departmentId}` | 부서 정보 조회. 비활성 부서도 조회 가능 | `ADMIN`, `LEGAL` |
| 부서 생성 | `POST` | `/api/v1/departments` | 활성 부서 생성 | `ADMIN`, `LEGAL` |
| 부서 수정 | `PUT` | `/api/v1/departments/{departmentId}` | 부서명 수정 | `ADMIN`, `LEGAL` |
| 부서 비활성화 | `DELETE` | `/api/v1/departments/{departmentId}` | 삭제 대신 상태를 `INACTIVE`로 변경 | `ADMIN`, `LEGAL` |

비활성 부서는 기존 사용자, 특허, 검토 이력의 참조를 유지합니다.
비활성 부서는 신규 사용자 승인, 특허 담당 부서 변경, 신규 검토 요청에 사용할 수 없습니다.

---

#### `GET /api/v1/departments`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| keyword | string | N | 부서명 부분 검색 |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 부서 ID |
| name | string | 부서명 |
| status | string | 부서 상태. `ACTIVE` / `INACTIVE` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      { "id": 1, "name": "에너지솔루션 사업부", "status": "ACTIVE" },
      { "id": 2, "name": "반도체 사업부", "status": "ACTIVE" }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 5,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

### 4. 특허

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 특허 요약 조회 | `GET` | `/api/v1/patents/summary` | 최신 권리 상태 기준 유지중/비활성 특허 수 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 목록 조회 | `GET` | `/api/v1/patents` | 승인 완료 특허 목록 조회. 키워드·상태·국가·사업부·정렬 필터 포함 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 담당 특허 목록 조회 | `GET` | `/api/v1/patents/assigned` | 현재 담당 부서가 본인 소속 부서와 일치하는 특허 목록 조회 | `BUSINESS` |
| 소멸 예정 특허 요약 조회 | `GET` | `/api/v1/patents/expiring/summary` | 3개월, 6개월, 1년, 3년, 5년 기준 소멸 예정 특허 요약 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 소멸 예정 특허 목록 조회 | `GET` | `/api/v1/patents/expiring` | 선택 기간 내 소멸 예정 특허 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 소멸 예정 특허 캘린더 조회 | `GET` | `/api/v1/patents/expiring/calendar` | 선택 연도 월별 소멸 예정 특허 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 승인 대기 특허 목록 조회 | `GET` | `/api/v1/patents/pending-approval` | 사업부 등록 요청 중 승인 대기 특허 목록 조회 | `ADMIN`, `LEGAL` |
| 특허 등록 신청 목록 조회 | `GET` | `/api/v1/patents/applications` | 등록 신청 목록 조회. BUSINESS는 본인 부서 신청만 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 단일 조회 | `GET` | `/api/v1/patents/{patentId}` | 특허 상세 정보 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 등록 | `POST` | `/api/v1/patents` | `ADMIN`과 `LEGAL`이 특허를 즉시 등록 | `ADMIN`, `LEGAL` |
| 특허 수정 | `PUT` | `/api/v1/patents/{patentId}` | 특허 정보 수정 | `ADMIN`, `LEGAL` |
| 담당 부서 변경 | `PATCH` | `/api/v1/patents/{patentId}/department` | 현재 담당 부서를 활성 부서로 변경 | `ADMIN`, `LEGAL` |
| 특허 등록 신청 거절 | `PATCH` | `/api/v1/patents/{patentId}/reject` | 승인 대기 특허 등록 신청 거절 | `ADMIN`, `LEGAL` |
| 특허 등록 신청 철회 | `PATCH` | `/api/v1/patents/{patentId}/withdraw` | BUSINESS 사용자가 본인 부서의 승인 대기 신청 철회 | `BUSINESS` |
| 특허 삭제 | `DELETE` | `/api/v1/patents/{patentId}` | 특허와 권리 상태, 연차료, 검토, 보고서 삭제 | `ADMIN`, `LEGAL` |

`GET /api/v1/patents`는 `BUSINESS` 사용자도 승인 완료된 전체 특허를 조회할 수 있습니다.
담당 특허만 필요한 경우 `GET /api/v1/patents/assigned`를 사용합니다.

---

#### `GET /api/v1/patents`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| keyword | string | N | 특허명, 출원번호, 발명자, 출원인 검색 |
| status | string (복수) | N | 권리 상태 필터. `APPLIED` / `PUBLISHED` / `REGISTERED` / `REJECTED` / `ABANDONED` / `EXPIRED` / `INVALIDATED` / `WITHDRAWN`. 복수 지정 가능 (`?status=EXPIRED&status=ABANDONED`) |
| filingCountry | string | N | 출원국. `KR` / `US` / `EP` / `JP` / `CN` |
| departmentId | long | N | 현재 담당 사업부 ID. `-1` 지정 시 미배정 특허만 조회 |
| sort | string | N | 정렬 기준. `title`, `applicationNumber`, `expiryDate`, `applicationDate`, `citationCount`, `id`. `field,asc` 또는 `field,desc` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| registrationNumber | string | 등록번호 |
| applicationDate | string | 출원일자 (`yyyy-MM-dd`) |
| expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |
| ipcCodes | array | IPC 코드 목록 |
| cpcCodes | array | CPC 코드 목록 |
| applicant | string | 출원인명 |
| inventor | string | 발명자명 |
| latestLegalStatus | string | 최신 권리 상태 |
| techField | string | 관련기술 분야 |
| businessField | string | 관련사업 분야 |
| keywords | array | 키워드 |
| summary | string | 요약 |
| citationCount | integer | 피인용 수 |
| examinationClaimCount | integer | 심사청구항 수 |
| filingCountry | string | 출원국 |
| approvalStatus | string | 승인 상태 |
| rejectionReason | string | 거절 사유. 없으면 `null` |
| currentDepartmentId | long | 현재 담당 부서 ID. 미배정이면 `null` |
| currentDepartmentName | string | 현재 담당 부서명. 미배정이면 `null` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "고효율 배터리 셀 구조",
        "applicationNumber": "10-2023-0012345",
        "registrationNumber": "10-2500001",
        "applicationDate": "2023-03-15",
        "expiryDate": "2043-03-15",
        "ipcCodes": ["H01M 10/00"],
        "cpcCodes": [],
        "applicant": "SK이노베이션",
        "inventor": "홍길동",
        "latestLegalStatus": "REGISTERED",
        "techField": "배터리",
        "businessField": "에너지솔루션",
        "keywords": ["음극재", "에너지밀도"],
        "summary": "고용량 배터리 셀의 음극재 구조를 개선하여 에너지 밀도를 향상시킨 발명",
        "citationCount": 14,
        "examinationClaimCount": 12,
        "filingCountry": "KR",
        "approvalStatus": "APPROVED",
        "rejectionReason": null,
        "currentDepartmentId": 3,
        "currentDepartmentName": "에너지솔루션 사업부",
        "createdAt": "2026-06-08T01:00:00Z",
        "updatedAt": "2026-06-08T01:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalItems": 152,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/assigned`

**헤더**: `Authorization: Bearer {accessToken}`

**권한**: `BUSINESS`

현재 담당 부서가 로그인한 사업부 사용자의 소속 부서와 일치하는 승인 완료 특허만 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| keyword | string | N | 특허명, 출원번호, 발명자, 출원인 검색 |
| sort | string | N | 정렬 기준. `title`, `applicationNumber`, `expiryDate`, `applicationDate`, `citationCount`, `id`. `field,asc` 또는 `field,desc` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**: `GET /api/v1/patents`와 동일

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 사업부 사용자의 소속 부서 없음)

---

#### `GET /api/v1/patents/summary`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| active | long | 최신 권리 상태가 유지중인 특허 수 |
| inactive | long | 최신 권리 상태가 비활성인 특허 수 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "active": 142,
    "inactive": 38
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/expiring/summary`

**헤더**: `Authorization: Bearer {accessToken}`

3개월, 6개월, 12개월, 36개월, 60개월 기준으로 소멸 예정 특허를 기술 분야별로 집계합니다.
`BUSINESS` 사용자는 본인 소속 부서 특허만 집계합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| periods | array | 기간별 소멸 예정 특허 집계 |
| periods[].months | integer | 기준 기간(개월). `3`, `6`, `12`, `36`, `60` |
| periods[].byTechField | array | 기술 분야별 집계 |
| periods[].byTechField[].name | string | 기술 분야명. 값이 없으면 `미분류` |
| periods[].byTechField[].count | long | 해당 기간 내 소멸 예정 특허 수 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "periods": [
      {
        "months": 3,
        "byTechField": [
          { "name": "배터리", "count": 2 },
          { "name": "반도체", "count": 1 }
        ]
      },
      {
        "months": 6,
        "byTechField": [
          { "name": "배터리", "count": 5 }
        ]
      }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/expiring`

**헤더**: `Authorization: Bearer {accessToken}`

선택 기간 내 소멸 예정 특허를 `expiryDate` 오름차순으로 조회합니다.
`BUSINESS` 사용자는 본인 소속 부서 특허만 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| months | integer | N | 조회 기간(개월). 미지정 시 60개월 |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| techField | string | 관련기술 분야 |
| departmentId | long | 현재 담당 부서 ID. 미배정이면 `null` |
| departmentName | string | 현재 담당 부서명. 미배정이면 `null` |
| expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "고효율 배터리 셀 구조",
        "applicationNumber": "10-2023-0012345",
        "techField": "배터리",
        "departmentId": 3,
        "departmentName": "에너지솔루션 사업부",
        "expiryDate": "2026-09-30"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/expiring/calendar`

**헤더**: `Authorization: Bearer {accessToken}`

선택 연도의 월별 소멸 예정 특허 개수와 특허 목록을 조회합니다.
`BUSINESS` 사용자는 본인 소속 부서 특허만 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| year | integer | N | 조회 연도. 미지정 시 현재 연도 |

**응답**

| Name | Type | Description |
| --- | --- | --- |
| months | array | 월별 소멸 예정 특허 버킷 |
| months[].month | integer | 월 (`1`~`12`) |
| months[].count | long | 해당 월 소멸 예정 특허 수 |
| months[].patents | array | 해당 월 소멸 예정 특허 목록 |
| months[].patents[].id | long | 특허 ID |
| months[].patents[].title | string | 특허명 |
| months[].patents[].applicationNumber | string | 출원번호 |
| months[].patents[].expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |
| months[].patents[].techField | string | 관련기술 분야 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "months": [
      {
        "month": 9,
        "count": 1,
        "patents": [
          {
            "id": 1,
            "title": "고효율 배터리 셀 구조",
            "applicationNumber": "10-2023-0012345",
            "expiryDate": "2026-09-30",
            "techField": "배터리"
          }
        ]
      }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/pending-approval`

**헤더**: `Authorization: Bearer {accessToken}`

사업부가 등록 요청한 `PENDING_APPROVAL` 상태 특허만 조회합니다.

**권한**: `ADMIN`, `LEGAL`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| keyword | string | N | 특허명, 출원번호, 발명자, 출원인 검색 |
| sort | string | N | `title`, `applicationNumber`, `applicationDate`, `expiryDate`, `citationCount` 정렬. `field,asc` 또는 `field,desc` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**: `GET /api/v1/patents`와 동일

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 42,
        "title": "반도체 패키지 구조",
        "applicationNumber": "10-2026-0000000",
        "approvalStatus": "PENDING_APPROVAL",
        "rejectionReason": null,
        "currentDepartmentId": 2,
        "currentDepartmentName": "반도체 사업부"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/patents/applications`

**헤더**: `Authorization: Bearer {accessToken}`

특허 등록 신청 목록을 조회합니다. `ADMIN`, `LEGAL`은 전체 신청을 조회하고, `BUSINESS`는 본인 부서 신청만 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| approvalStatus | string | N | `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `WITHDRAWN`. `PENDING`은 `PENDING_APPROVAL`로 처리 |
| keyword | string | N | 특허명, 출원번호, 발명자, 출원인 검색 |
| sort | string | N | `title`, `applicationNumber`, `applicationDate`, `expiryDate`, `citationCount` 정렬. `field,asc` 또는 `field,desc` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**: `GET /api/v1/patents`와 동일

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 42,
        "title": "반도체 패키지 구조",
        "applicationNumber": "10-2026-0000000",
        "approvalStatus": "REJECTED",
        "rejectionReason": "필수 서지 정보가 부족합니다.",
        "currentDepartmentId": 2,
        "currentDepartmentName": "반도체 사업부"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `PATCH /api/v1/patents/{patentId}/reject`

**헤더**: `Authorization: Bearer {accessToken}`

**권한**: `ADMIN`, `LEGAL`

승인 대기 특허 등록 신청을 거절하고 거절 사유를 저장합니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| reason | string | * | 거절 사유. 빈 문자열 불가 |

**요청 예시**

```json
{ "reason": "필수 서지 정보가 부족합니다." }
```

**응답**: `GET /api/v1/patents/{patentId}`와 동일한 `PatentDetailResponse`

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "반도체 패키지 구조",
    "applicationNumber": "10-2026-0000000",
    "approvalStatus": "REJECTED",
    "rejectionReason": "필수 서지 정보가 부족합니다."
  }
}
```

**에러**: `INVALID_REQUEST`(400 — reason 누락 또는 승인 대기 상태가 아님), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `PATCH /api/v1/patents/{patentId}/withdraw`

**헤더**: `Authorization: Bearer {accessToken}`

**권한**: `BUSINESS`

사업부 사용자가 본인 부서의 승인 대기 특허 등록 신청을 철회합니다. 요청 Body 없음.

**응답**: `GET /api/v1/patents/{patentId}`와 동일한 `PatentDetailResponse`

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "반도체 패키지 구조",
    "applicationNumber": "10-2026-0000000",
    "approvalStatus": "WITHDRAWN",
    "rejectionReason": null
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 본인 부서 신청 아님), `NOT_FOUND`(404), `INVALID_REQUEST`(400 — 승인 대기 상태가 아님)

---

#### `GET /api/v1/patents/{patentId}`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| registrationNumber | string | 등록번호 |
| publicationNumber | string | 공개번호 |
| announcementNumber | string | 공고번호 |
| managementNumber | string | 관리번호 |
| applicationDate | string | 출원일자 (`yyyy-MM-dd`) |
| registrationDate | string | 등록일자 (`yyyy-MM-dd`) |
| publicationDate | string | 공개일자 (`yyyy-MM-dd`) |
| announcementDate | string | 공고일자 (`yyyy-MM-dd`) |
| expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |
| ipcCodes | array | IPC 코드 목록 |
| cpcCodes | array | CPC 코드 목록 |
| applicant | string | 출원인명 |
| inventor | string | 발명자명 |
| citationCount | integer | 피인용 수 |
| examinationClaimCount | integer | 심사청구항 수 |
| originalPdfKey | string | 특허 원문 MinIO object key |
| parsedJsonKey | string | 파싱 결과 JSON 파일 키 |
| approvalStatus | string | 승인 상태. `APPROVED`, `PENDING_APPROVAL`, `REJECTED` |
| rejectionReason | string | 거절 사유. 없으면 `null` |
| businessField | string | 관련사업 분야 |
| techField | string | 관련기술 분야 |
| relatedProducts | array | 관련 제품 목록 |
| filingCountry | string | 출원국 |
| isJointApplication | boolean | 공동출원 여부 |
| jointApplicant | string | 공동출원인명. 없으면 `null` |
| initialDepartment | string | 최초 담당 부서명 |
| currentDepartmentId | long | 현재 담당 부서 ID |
| currentDepartmentName | string | 현재 담당 부서명 |
| latestLegalStatus | string | 최신 권리 상태 |
| latestReportScore | number | 최신 AI 평가 총점. 없으면 `null` |
| keywords | array | 키워드 |
| summary | string | 요약 |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "고효율 배터리 셀 구조",
    "applicationNumber": "10-2023-0012345",
    "registrationNumber": "10-2500001",
    "publicationNumber": "10-2023-0111111",
    "announcementNumber": null,
    "managementNumber": "MNG-2023-001",
    "applicationDate": "2023-03-15",
    "registrationDate": "2024-01-10",
    "publicationDate": "2023-09-15",
    "announcementDate": null,
    "expiryDate": "2043-03-15",
    "ipcCodes": ["H01M 10/00"],
    "cpcCodes": [],
    "applicant": "SK이노베이션",
    "inventor": "홍길동",
    "citationCount": 14,
    "examinationClaimCount": 12,
    "originalPdfKey": "patents/1/original.pdf",
    "parsedJsonKey": "tmp/patent-extract-jobs/9001/parsed.json",
    "approvalStatus": "APPROVED",
    "rejectionReason": null,
    "businessField": "에너지솔루션",
    "techField": "배터리",
    "relatedProducts": ["배터리팩A", "셀모듈B"],
    "filingCountry": "KR",
    "isJointApplication": false,
    "jointApplicant": null,
    "initialDepartment": "에너지솔루션 사업부",
    "currentDepartmentId": 3,
    "currentDepartmentName": "에너지솔루션 사업부",
    "latestLegalStatus": "REGISTERED",
    "latestReportScore": 82.5,
    "keywords": ["음극재", "에너지밀도"],
    "summary": "고용량 배터리 셀의 음극재 구조를 개선하여 에너지 밀도를 향상시킨 발명",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:00:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /api/v1/patents`

**헤더**: `Authorization: Bearer {accessToken}`

`ADMIN`과 `LEGAL` 사용자가 생성한 특허는 `APPROVED`로 즉시 저장됩니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| title | string | * | 발명의 명칭(최종) |
| applicationNumber | string | * | 출원번호. 중복 불가 |
| registrationNumber | string | N | 등록번호 |
| applicationDate | string | N | 출원일 (`yyyy-MM-dd`) |
| registrationDate | string | N | 등록일 (`yyyy-MM-dd`) |
| publicationNumber | string | N | 공개번호 |
| announcementNumber | string | N | 공고번호 |
| ipcCodes | array | N | IPC 코드 목록 |
| cpcCodes | array | N | CPC 코드 목록 |
| applicant | string | N | 출원인명 |
| inventor | string | N | 발명자명 |
| expiryDate | string | N | 예상 소멸일 (`yyyy-MM-dd`) |
| citationCount | integer | N | 피인용 수 |
| examinationClaimCount | integer | N | 심사청구항 수 |
| managementNumber | string | N | 관리번호 |
| businessField | string | N | 관련사업 분야 |
| techField | string | N | 관련기술 분야 |
| relatedProducts | array | N | 관련 제품 목록 |
| filingCountry | string | N | 출원국 |
| isJointApplication | boolean | N | 공동출원 여부 |
| jointApplicant | string | N | 공동출원인명 |
| keywords | array | N | 키워드 |
| summary | string | N | 요약 |
| originalPdfKey | string | N | 기존 원문 PDF object key. `extractJobId`가 없을 때 그대로 저장 |
| extractJobId | long | N | 완료된 특허 추출 작업 ID. 값이 있으면 임시 PDF를 `patents/{patentId}/original.pdf`로 복사하고, AI 서버가 전달한 `parsedJsonKey`의 JSON을 `patents/{patentId}/parsed.json`로 복사 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "반도체 패키지 구조",
    "applicationNumber": "10-2026-0000000",
    "registrationNumber": "10-1234567",
    "publicationNumber": "10-2026-0000001",
    "announcementNumber": null,
    "applicationDate": "2026-05-26",
    "registrationDate": null,
    "publicationDate": null,
    "announcementDate": null,
    "ipcCodes": ["H01L 21/00"],
    "cpcCodes": [],
    "applicant": "SK하이닉스",
    "inventor": "홍길동",
    "expiryDate": "2046-05-26",
    "citationCount": 10,
    "examinationClaimCount": 12,
    "originalPdfKey": "patents/42/original.pdf",
    "parsedJsonKey": "patents/42/parsed.json",
    "approvalStatus": "APPROVED",
    "rejectionReason": null,
    "managementNumber": "MNG-2026-0001",
    "businessField": "반도체",
    "techField": "패키징",
    "relatedProducts": ["제품A", "제품B"],
    "filingCountry": "KR",
    "isJointApplication": false,
    "jointApplicant": null,
    "initialDepartment": "반도체",
    "currentDepartmentId": null,
    "currentDepartmentName": null,
    "latestLegalStatus": null,
    "latestReportScore": null,
    "keywords": ["패키지", "반도체"],
    "summary": "특허 요약",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:00:00Z"
  }
}
```

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404 — 추출 작업 없음), `CONFLICT`(409 — 동일 출원번호 중복 또는 추출 미완료), `EXTERNAL_SERVICE_ERROR`(502 — MinIO 복사 실패)

---

#### `PATCH /api/v1/patents/{patentId}/department`

**헤더**: `Authorization: Bearer {accessToken}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| departmentId | long | * | 변경할 사업부 ID. 활성 부서만 허용 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "currentDepartmentId": 2,
    "currentDepartmentName": "반도체 사업부"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `NOT_FOUND`(404), `CONFLICT`(409 — 비활성 부서)

---

### 4-1. 특허 원문 PDF 추출 작업

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| PDF 업로드 URL 발급 | `POST` | `/api/v1/patent-extract-jobs/upload-url` | 추출 작업 생성 후 MinIO PUT presigned URL 반환 | `ADMIN`, `LEGAL`, `BUSINESS` |
| PDF 업로드 완료 | `POST` | `/api/v1/patent-extract-jobs/{extractJobId}/upload-complete` | PDF 존재 확인 후 RabbitMQ 메시지 발행 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 추출 작업 상태 조회 | `GET` | `/api/v1/patent-extract-jobs/{extractJobId}/status` | 프론트 polling용 상태 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 추출 결과 조회 | `GET` | `/api/v1/patent-extract-jobs/{extractJobId}/result` | 완료된 추출 결과 JSON 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 추출 완료 콜백 | `PATCH` | `/api/v1/internal/patent-extract-jobs/{extractJobId}/complete` | AI Worker가 추출 결과 전달 | Internal API Key |
| 추출 실패 콜백 | `PATCH` | `/api/v1/internal/patent-extract-jobs/{extractJobId}/fail` | AI Worker가 추출 실패 전달 | Internal API Key |

---

#### `POST /api/v1/patent-extract-jobs/upload-url`

특허 신규 등록 전 원문 PDF를 업로드하기 위한 presigned URL을 발급합니다. 호출 즉시 `UPLOAD_PENDING` 상태의 `patent_extract_jobs` 레코드가 생성됩니다.

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| extractJobId | long | 추출 작업 ID |
| objectKey | string | 임시 PDF object key. `tmp/patent-extract-jobs/{extractJobId}/original.pdf` |
| uploadUrl | string | MinIO PUT presigned URL |
| expiresInSeconds | integer | URL 만료 시간(초) |
| status | string | `UPLOAD_PENDING` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "tmp/patent-extract-jobs/9001/original.pdf",
    "uploadUrl": "https://minio.skipa.internal/skipa/tmp/patent-extract-jobs/9001/original.pdf?...",
    "expiresInSeconds": 600,
    "status": "UPLOAD_PENDING",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:00:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `EXTERNAL_SERVICE_ERROR`(502 — presigned URL 발급 실패)

---

#### `POST /api/v1/patent-extract-jobs/{extractJobId}/upload-complete`

프론트가 presigned URL로 PDF 업로드를 완료한 뒤 호출합니다. 백엔드는 MinIO object 존재 여부를 확인하고, 성공 시 작업 상태를 `ANALYZING`으로 변경한 뒤 RabbitMQ에 추출 요청 메시지를 발행합니다.

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**RabbitMQ 메시지 예시**

```json
{
  "type": "PATENT_EXTRACT",
  "extractJobId": 9001,
  "objectKey": "tmp/patent-extract-jobs/9001/original.pdf"
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "tmp/patent-extract-jobs/9001/original.pdf",
    "parsedJsonKey": null,
    "status": "ANALYZING",
    "errorMessage": null,
    "uploadedAt": "2026-06-08T01:02:00Z",
    "completedAt": null,
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:02:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404 — 추출 작업 또는 PDF 없음), `CONFLICT`(409 — 이미 처리된 작업), `EXTERNAL_SERVICE_ERROR`(502 — RabbitMQ 발행 실패)

---

#### `GET /api/v1/patent-extract-jobs/{extractJobId}/status`

프론트 polling용 API입니다. `COMPLETED` 또는 `FAILED` 응답 시 polling을 종료합니다.

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| extractJobId | long | 추출 작업 ID |
| objectKey | string | 임시 PDF object key |
| parsedJsonKey | string | AI 서버가 저장한 추출 결과 JSON object key. 완료 전에는 `null` |
| status | string | `UPLOAD_PENDING` / `ANALYZING` / `COMPLETED` / `FAILED` |
| errorMessage | string | 실패 사유. 실패가 아니면 `null` |
| uploadedAt | datetime | 업로드 완료 처리 시각 |
| completedAt | datetime | 완료 또는 실패 처리 시각 |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /api/v1/patent-extract-jobs/{extractJobId}/result`

완료된 추출 작업의 결과 JSON을 조회합니다. 프론트는 `result`를 특허 등록 폼에 자동 입력하고, 사용자가 수정한 뒤 `POST /api/v1/patents`로 최종 생성합니다.

**헤더**: `Authorization: Bearer {accessToken}`

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "tmp/patent-extract-jobs/9001/original.pdf",
    "parsedJsonKey": "tmp/patent-extract-jobs/9001/parsed.json",
    "status": "COMPLETED",
    "result": {
      "title": "반도체 패키지 구조",
      "applicationNumber": "10-2026-0000000",
      "registrationNumber": "10-1234567",
      "applicationDate": "2020-05-26",
      "ipcCodes": ["H01L 21/00"],
      "cpcCodes": [],
      "applicant": "SK하이닉스",
      "inventor": "홍길동",
      "keywords": ["패키지", "반도체"],
      "summary": "특허 요약"
    },
    "uploadedAt": "2026-06-08T01:02:00Z",
    "completedAt": "2026-06-08T01:05:00Z",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:05:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 추출 미완료)

---

#### `PATCH /api/v1/internal/patent-extract-jobs/{extractJobId}/complete`

AI Worker가 PDF 분석을 완료한 뒤 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| parsedJsonKey | string | * | AI 서버가 MinIO에 업로드한 추출 결과 JSON object key |
| result | object | * | AI 추출 결과 JSON. `patent_extract_jobs.result_json`에 저장 |

**요청 예시**

```json
{
  "parsedJsonKey": "tmp/patent-extract-jobs/9001/parsed.json",
  "result": {
    "title": "반도체 패키지 구조",
    "applicationNumber": "10-2026-0000000",
    "registrationNumber": "10-1234567",
    "applicationDate": "2020-05-26",
    "ipcCodes": ["H01L 21/00"],
    "cpcCodes": [],
    "applicant": "SK하이닉스",
    "inventor": "홍길동",
    "keywords": ["패키지", "반도체"],
    "summary": "특허 요약"
  }
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "tmp/patent-extract-jobs/9001/original.pdf",
    "parsedJsonKey": "tmp/patent-extract-jobs/9001/parsed.json",
    "status": "COMPLETED",
    "errorMessage": null,
    "uploadedAt": "2026-06-08T01:02:00Z",
    "completedAt": "2026-06-08T01:05:00Z",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:05:00Z"
  }
}
```

**에러**: `INVALID_REQUEST`(400 — parsedJsonKey 또는 result 누락), `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 완료 가능한 상태가 아님)

---

#### `PATCH /api/v1/internal/patent-extract-jobs/{extractJobId}/fail`

AI Worker가 PDF 분석 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

요청 Body는 선택 사항입니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| errorMessage | string | N | 실패 사유 |

**요청 예시**

```json
{
  "errorMessage": "AI patent extraction failed"
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "tmp/patent-extract-jobs/9001/original.pdf",
    "parsedJsonKey": null,
    "status": "FAILED",
    "errorMessage": "AI patent extraction failed",
    "uploadedAt": "2026-06-08T01:02:00Z",
    "completedAt": "2026-06-08T01:05:00Z",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:05:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

### 4-2. 특허 담당 부서

특허 담당 부서는 별도 매핑 엔티티를 두지 않고, `patents.current_department_id`가 `departments.id`를 외래키로 참조하는 방식으로 관리합니다.
담당 부서 조회는 `GET /api/v1/patents/{patentId}` 응답 필드로 확인합니다.

---

### 5. 권리 상태 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 권리 상태 이력 조회 | `GET` | `/api/v1/patents/{patentId}/legal-status` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 권리 상태 이력 생성 | `POST` | `/api/v1/patents/{patentId}/legal-status` | 권리 상태 수동 추가 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다.

---

### 6. 연차료 납부 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 연차료 납부 이력 조회 | `GET` | `/api/v1/patents/{patentId}/annuities` | 납부 완료(`PAID`) 이력 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 연차료 납부 처리 | `POST` | `/api/v1/patents/{patentId}/annuities` | 최신 미납 연차료를 납부 처리하고 다음 미납 연차료를 생성 | `LEGAL` |
| 연차료 납부 이력 수정 | `PUT` | `/api/v1/patents/{patentId}/annuities/{annuityId}` | 납부 완료(`PAID`) 이력의 납부 연수, 금액, 납부일자 수정 | `LEGAL` |
| 연차료 납부 이력 삭제 | `DELETE` | `/api/v1/patents/{patentId}/annuities/{annuityId}` | 납부 완료(`PAID`) 이력 삭제 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다. 조회 결과에는 `PAID` 상태의 납부 완료 이력만 포함됩니다.
수정/삭제 대상도 `PAID` 상태의 납부 완료 이력으로 제한됩니다.

**추가 요청 예시**

```json
{
  "paymentYears": 2,
  "amount": 1000000
}
```

**수정 요청 예시**

```json
{
  "paymentYears": 2,
  "amount": 1000000,
  "paidDate": "2026-06-12"
}
```

---

### 7. 검토 주기

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 주기 생성 | `POST` | `/api/v1/review-cycles` | 검토 주기 등록 | `ADMIN` |
| 현재 검토 주기 조회 | `GET` | `/api/v1/review-cycles/current` | 오늘 날짜가 포함된 현재 활성 검토 주기 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 검토 주기 목록 조회 | `GET` | `/api/v1/review-cycles` | 최근 시작일 순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 검토 주기 단일 조회 | `GET` | `/api/v1/review-cycles/{reviewCycleId}` | 검토 주기 상세 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 검토 주기 수정 | `PUT` | `/api/v1/review-cycles/{reviewCycleId}` | 검토 주기 정보 수정 | `ADMIN` |
| 검토 주기 마감일자 설정 | `PATCH` | `/api/v1/review-cycles/{reviewCycleId}/deadline` | 검토 요청 발송에 사용할 마감일 설정 | `LEGAL` |
| 검토 주기 삭제 | `DELETE` | `/api/v1/review-cycles/{reviewCycleId}` | 미사용 검토 주기 삭제 | `ADMIN` |

검토 주기의 기간은 서로 겹칠 수 없습니다. 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.

---

### 8. 검토 요청 전송

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 요청 전송 | `POST` | `/api/v1/patents/{patentId}/reviews` | 현재 담당 부서와 현재 날짜가 포함된 검토 주기로 요청 생성. 최신 평가 보고서가 있으면 `reportId`로 함께 연결 | `LEGAL` |
| 검토 일괄 요청 전송 | `POST` | `/api/v1/reviews/bulk` | 여러 특허에 검토 요청 생성. 생성 불가 특허는 사유와 함께 건너뜀 | `LEGAL` |

검토 요청의 회신기한은 현재 활성 검토 주기의 `deadline`으로 서버에서 자동 설정합니다.
동일한 검토 주기, 특허, 부서 조합은 중복 요청할 수 없습니다.
일괄 요청은 최대 100건까지 처리하며, 특허별 결과를 `CREATED` 또는 `SKIPPED`로 반환합니다.

---

#### `POST /api/v1/reviews/bulk`

**헤더**: `Authorization: Bearer {accessToken}`

각 특허의 `current_department_id`와 현재 활성 `QUARTERLY` 주기를 서버에서 자동 적용합니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentIds | array | * | 검토 요청을 생성할 특허 ID 목록. 최대 100건 |

**요청 예시**

```json
{ "patentIds": [1, 2, 3] }
```

**응답**

| Name | Type | Description |
| --- | --- | --- |
| reviewCycleId | long | 적용된 검토 주기 ID |
| createdCount | integer | 생성 성공 건수 |
| skippedCount | integer | 건너뜀 건수 |
| items[].patentId | long | 특허 ID |
| items[].status | string | `CREATED` / `SKIPPED` |
| items[].reason | string | 건너뜀 사유. `CREATED`이면 `null`. `DUPLICATE_REVIEW_REQUEST` / `PATENT_DEPARTMENT_NOT_ASSIGNED` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reviewCycleId": 1,
    "createdCount": 2,
    "skippedCount": 1,
    "items": [
      { "patentId": 1, "status": "CREATED", "reason": null },
      { "patentId": 2, "status": "SKIPPED", "reason": "DUPLICATE_REVIEW_REQUEST" },
      { "patentId": 3, "status": "SKIPPED", "reason": "PATENT_DEPARTMENT_NOT_ASSIGNED" }
    ]
  }
}
```

**에러**: `INVALID_REQUEST`(400 — patentIds 누락 또는 100건 초과), `UNAUTHORIZED`(401), `NOT_FOUND`(404 — 활성 QUARTERLY 주기 없음)

---

### 9. 사업부 검토 - Legal 모니터링

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 대상 목록 조회 | `GET` | `/api/v1/review-targets` | 현재 활성 검토 주기 기준 검토 대상 목록 조회 | `ADMIN`, `LEGAL` |
| 검토 대상 단일 조회 | `GET` | `/api/v1/review-targets/{reviewId}` | 검토 대상 특허의 요청 상태와 사업부 회신 조회 | `ADMIN`, `LEGAL` |
| 회신 확인 처리 | `PATCH` | `/api/v1/reviews/{reviewId}/confirm` | Legal 팀 회신 확인 처리. `confirmed_at` 기록 | `LEGAL` |

---

#### `GET /api/v1/review-targets`

**헤더**: `Authorization: Bearer {accessToken}`

현재 날짜 기준 활성 검토 주기에 포함된 검토 대상 특허 목록을 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| status | string | N | `SCHEDULED` / `PENDING` / `OVERDUE` / `SUBMITTED` |
| departmentId | long | N | 담당 사업부 ID |
| patentId | long | N | 특허 ID |
| checked | boolean | N | 회신 확인 여부 |
| sort | string | N | `title,asc|desc`, `applicationNumber,asc|desc`, `applicationDate,asc|desc`, `expiryDate,asc|desc`, `citationCount,asc|desc`. 미지정 시 `applicationNumber ASC` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사업부 의견 검토 ID |
| patentId | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| techField | string | 관련기술 분야 |
| businessField | string | 관련사업 분야 |
| reportId | long | 참고 보고서 ID. 없으면 `null` |
| departmentId | long | 사업부 ID |
| departmentName | string | 담당 사업부명 |
| reviewCycleId | long | 검토 주기 ID |
| reviewCycleYear | integer | 검토 주기 연도 |
| reviewCycleQuarter | integer | 검토 주기 분기 |
| opinion | string | 사업부 의견. `MAINTAIN` / `ABANDON` / `null` |
| comment | string | 상세 의견 |
| status | string | 검토 상태. `SCHEDULED` / `PENDING` / `OVERDUE` / `SUBMITTED` |
| checked | boolean | Legal 팀 확인 여부 |
| submittedAt | datetime | 제출일시 |
| dueDate | string | 회신기한 (`yyyy-MM-dd`) |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 12,
        "patentId": 1,
        "title": "반도체 패키지 구조",
        "applicationNumber": "10-2026-0000000",
        "techField": "패키징",
        "businessField": "반도체",
        "reportId": 7,
        "departmentId": 2,
        "departmentName": "반도체 사업부",
        "reviewCycleId": 1,
        "reviewCycleYear": 2026,
        "reviewCycleQuarter": 2,
        "opinion": "MAINTAIN",
        "comment": "핵심 기술로 판단됩니다.",
        "status": "SUBMITTED",
        "checked": false,
        "submittedAt": "2026-06-07T10:30:00Z",
        "dueDate": "2026-06-30",
        "createdAt": "2026-06-01T09:00:00Z",
        "updatedAt": "2026-06-07T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `NOT_FOUND`(404 — 활성 QUARTERLY 주기 없음)

---

#### `GET /api/v1/review-targets/{reviewId}`

**헤더**: `Authorization: Bearer {accessToken}`

응답 필드는 `GET /api/v1/review-targets`의 items[] 항목과 동일합니다.

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `PATCH /api/v1/reviews/{reviewId}/confirm`

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음. `confirmed_at`에 현재 시각을 기록합니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 12,
    "checked": true
  }
}
```

**에러**: `UNAUTHORIZED`(401), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 확인 처리됨)

---

### 10. 사업부 검토 현황 - 사업부

각 특허와 부서의 가장 최근 검토 요청을 기준으로 반환합니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 현황 요약 조회 | `GET` | `/api/v1/business-reviews/summary` | 본인 소속 부서 기준 현재 검토 현황 KPI 조회 | `BUSINESS` |
| 검토 현황 목록 조회 | `GET` | `/api/v1/business-reviews` | 본인 부서에 요청된 최신 검토 현황 목록 조회 | `BUSINESS` |
| 과거 제출 이력 조회 | `GET` | `/api/v1/business-reviews/history` | 본인 부서 기준 과거 검토 제출 이력 조회 | `BUSINESS` |
| 검토 현황 단일 조회 | `GET` | `/api/v1/business-reviews/{patentId}` | 특허 상세 정보와 최신 검토 현황 조회 | `BUSINESS` |
| 사업부 검토 의견 제출 | `POST` | `/api/v1/business-reviews/{patentId}/opinions` | 현재 검토 주기 요청에 `MAINTAIN` 또는 `ABANDON` 제출 | `BUSINESS` |

회신기한이 지난 요청과 이미 제출한 요청에는 의견을 제출할 수 없습니다.

---

#### `GET /api/v1/business-reviews/summary`

**헤더**: `Authorization: Bearer {accessToken}`

사업부 사용자의 소속 부서 기준 현재 검토 주기 정보와 제출 KPI를 조회합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| reviewCycle | object | 현재 검토 주기 정보 |
| reviewCycle.id | long | 검토 주기 ID |
| reviewCycle.year | integer | 검토 주기 연도 |
| reviewCycle.quarter | integer | 검토 주기 분기 |
| reviewCycle.startDate | string | 시작일 (`yyyy-MM-dd`) |
| reviewCycle.endDate | string | 종료일 (`yyyy-MM-dd`) |
| reviewCycle.deadline | string | 회신기한 (`yyyy-MM-dd`) |
| kpi | object | 제출 현황 KPI |
| kpi.submitted | long | 제출 완료 건수 |
| kpi.notSubmitted | long | 미제출 건수 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reviewCycle": {
      "id": 1,
      "year": 2026,
      "quarter": 2,
      "startDate": "2026-04-01",
      "endDate": "2026-06-30",
      "deadline": "2026-06-30"
    },
    "kpi": {
      "submitted": 12,
      "notSubmitted": 3
    }
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 사업부 사용자 아님 또는 소속 부서 없음), `NOT_FOUND`(404 — 현재 활성 검토 주기 없음)

---

#### `GET /api/v1/business-reviews`

**헤더**: `Authorization: Bearer {accessToken}`

본인 부서에 현재 검토 주기로 요청된 특허 검토 현황 목록을 조회합니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| status | string | N | `PENDING`, `OVERDUE`, `SUBMITTED` |
| opinion | string | N | `MAINTAIN`, `ABANDON` |
| submittedFrom | string | N | 제출일 시작일 (`yyyy-MM-dd`) |
| submittedTo | string | N | 제출일 종료일 (`yyyy-MM-dd`) |
| sort | string | N | `title`, `applicationNumber`, `applicationDate`, `expiryDate`, `citationCount` 정렬. 미지정 시 출원번호 오름차순 |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사업부 의견 검토 ID |
| patentId | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| keywords | array | 키워드 |
| summary | string | 요약 |
| opinion | string | 사업부 의견. `MAINTAIN` / `ABANDON` / `null` |
| comment | string | 상세 의견 |
| status | string | 검토 상태. `PENDING` / `OVERDUE` / `SUBMITTED` |
| submittedAt | datetime | 제출일시 |
| createdAt | datetime | 특허 생성일시 |
| updatedAt | datetime | 특허 수정일시 |
| reviewRequestedAt | datetime | 검토 요청 생성일시 |
| dueDate | string | 회신기한 (`yyyy-MM-dd`) |
| totalScore | number | AI 평가 총점. 없으면 `null` |
| valueGrade | string | AI 평가 등급. 없으면 `null` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 10,
        "patentId": 1,
        "title": "고효율 배터리 셀 구조",
        "applicationNumber": "10-2023-0012345",
        "keywords": ["음극재", "에너지밀도"],
        "summary": "배터리 셀 구조 개선 특허",
        "opinion": null,
        "comment": null,
        "status": "PENDING",
        "submittedAt": null,
        "createdAt": "2026-06-01T09:00:00Z",
        "updatedAt": "2026-06-01T09:00:00Z",
        "reviewRequestedAt": "2026-06-07T01:00:00Z",
        "dueDate": "2026-06-30",
        "totalScore": 82.5,
        "valueGrade": "A"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 사업부 사용자 아님 또는 소속 부서 없음), `NOT_FOUND`(404 — 현재 활성 검토 주기 없음)

---

#### `GET /api/v1/business-reviews/history`

**헤더**: `Authorization: Bearer {accessToken}`

본인 부서 기준 과거 검토 주기의 제출 완료 이력을 조회합니다. 현재 진행 중인 검토 주기는 포함하지 않습니다.

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| year | integer | N | 검토 주기 연도 |
| quarter | integer | N | 검토 주기 분기. `year`와 함께 사용 |
| opinion | string | N | `MAINTAIN`, `ABANDON` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사업부 의견 검토 ID |
| patentId | long | 특허 ID |
| reviewCycle | object | 검토 주기 정보 |
| reviewCycle.id | long | 검토 주기 ID |
| reviewCycle.year | integer | 검토 주기 연도 |
| reviewCycle.quarter | integer | 검토 주기 분기 |
| reviewCycle.startDate | string | 시작일 (`yyyy-MM-dd`) |
| reviewCycle.endDate | string | 종료일 (`yyyy-MM-dd`) |
| reviewCycle.deadline | string | 회신기한 (`yyyy-MM-dd`) |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| opinion | string | 사업부 의견. `MAINTAIN` / `ABANDON` |
| comment | string | 상세 의견 |
| status | string | 검토 상태. `SUBMITTED` |
| submittedAt | datetime | 제출일시 |
| reviewRequestedAt | datetime | 검토 요청 생성일시 |
| dueDate | string | 회신기한 (`yyyy-MM-dd`) |
| totalScore | number | AI 평가 총점. 없으면 `null` |
| valueGrade | string | AI 평가 등급. 없으면 `null` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 9,
        "patentId": 1,
        "reviewCycle": {
          "id": 1,
          "year": 2026,
          "quarter": 1,
          "startDate": "2026-01-01",
          "endDate": "2026-03-31",
          "deadline": "2026-03-31"
        },
        "title": "고효율 배터리 셀 구조",
        "applicationNumber": "10-2023-0012345",
        "opinion": "MAINTAIN",
        "comment": "핵심 특허로 유지를 요청합니다.",
        "status": "SUBMITTED",
        "submittedAt": "2026-03-20T09:00:00Z",
        "reviewRequestedAt": "2026-03-01T09:00:00Z",
        "dueDate": "2026-03-31",
        "totalScore": 82.5,
        "valueGrade": "A"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 사업부 사용자 아님 또는 소속 부서 없음)

---

#### `GET /api/v1/business-reviews/{patentId}`

**헤더**: `Authorization: Bearer {accessToken}`

본인 부서에 현재 검토 주기로 요청된 특정 특허의 상세 정보와 의견 제출 상태를 조회합니다.

**Path Parameter**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentId | long | * | 특허 ID |

**응답**

| Name | Type | Description |
| --- | --- | --- |
| patent | object | `GET /api/v1/patents/{patentId}`와 동일한 특허 상세 정보 |
| opinion | string | `MAINTAIN` / `ABANDON` / `null` |
| comment | string | 상세 의견 |
| status | string | `PENDING` / `OVERDUE` / `SUBMITTED` |
| submittedAt | datetime | 제출일시 |
| reviewRequestedAt | datetime | 검토 요청 생성일시 |
| dueDate | string | 회신기한 (`yyyy-MM-dd`) |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "patent": {
      "id": 1,
      "title": "고효율 배터리 셀 구조",
      "applicationNumber": "10-2023-0012345",
      "approvalStatus": "APPROVED",
      "currentDepartmentId": 3,
      "currentDepartmentName": "에너지솔루션 사업부"
    },
    "opinion": null,
    "comment": null,
    "status": "PENDING",
    "submittedAt": null,
    "reviewRequestedAt": "2026-06-07T01:00:00Z",
    "dueDate": "2026-06-30"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 본인 부서 검토 대상 아님), `NOT_FOUND`(404)

---

#### `POST /api/v1/business-reviews/{patentId}/opinions`

**헤더**: `Authorization: Bearer {accessToken}`

`patentId`는 현재 검토 주기에서 본인 부서로 검토 요청된 특허 ID입니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| opinion | string | * | `MAINTAIN` (유지) 또는 `ABANDON` (포기) |
| comment | string | N | 상세 의견 텍스트 |

**요청 예시**

```json
{
  "opinion": "MAINTAIN",
  "comment": "핵심 기술로 유지를 권고합니다."
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "patentId": 1,
    "title": "고효율 배터리 셀 구조",
    "applicationNumber": "10-2023-0012345",
    "keywords": ["음극재", "에너지밀도"],
    "summary": "배터리 셀 구조 개선 특허",
    "opinion": "MAINTAIN",
    "comment": "핵심 기술로 유지를 권고합니다.",
    "status": "SUBMITTED",
    "submittedAt": "2026-06-07T10:30:00Z",
    "createdAt": "2026-06-01T09:00:00Z",
    "updatedAt": "2026-06-01T09:00:00Z",
    "reviewRequestedAt": "2026-06-07T01:00:00Z",
    "dueDate": "2026-06-30",
    "totalScore": 82.5,
    "valueGrade": "A"
  }
}
```

**에러**: `INVALID_REQUEST`(400 — 이미 제출 또는 기한 초과), `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 본인 부서 담당 특허 아님), `NOT_FOUND`(404)

---

### 11. 평가 보고서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 평가 보고서 목록 조회 | `GET` | `/api/v1/patents/{patentId}/reports` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 생성 요청 | `POST` | `/api/v1/patents/{patentId}/reports` | `GENERATING` 상태의 보고서 생성 요청 등록 후 RabbitMQ 메시지 발행 | `LEGAL` |
| 최신 평가 보고서 조회 | `GET` | `/api/v1/patents/{patentId}/reports/latest` | 가장 최근 생성된 평가 보고서 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 과거 평가 이력 조회 | `GET` | `/api/v1/patents/{patentId}/reports/history` | 최신 완료 보고서 1건을 제외한 과거 평가 이력 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 단일 조회 | `GET` | `/api/v1/patents/{patentId}/reports/{reportId}` | 완료된 보고서 상세 및 MinIO presigned URL 반환 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 처리 상태 조회 | `GET` | `/api/v1/patents/{patentId}/reports/{reportId}/status` | 보고서 생성 및 임베딩 상태 polling용 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 채팅 이력 조회 | `GET` | `/api/v1/patents/{patentId}/reports/{reportId}/chat/messages` | 보고서별 채팅 메시지 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 채팅 메시지 전송 | `POST` | `/api/v1/patents/{patentId}/reports/{reportId}/chat/messages` | 사용자 메시지 저장, AI 서버 채팅 API 호출, 응답 저장 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 채팅 스트리밍 전송 | `POST` | `/api/v1/patents/{patentId}/reports/{reportId}/chat/messages/stream` | 사용자 메시지 저장 후 AI 서버 SSE 스트리밍 응답 중계 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 채팅 초기화 | `DELETE` | `/api/v1/patents/{patentId}/reports/{reportId}/chat/messages` | 보고서별 채팅 메시지 전체 삭제 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 보고서 생성 완료 콜백 | `PATCH` | `/api/v1/internal/reports/{reportId}/report-complete` | AI Worker가 생성 완료 및 `reportKey` 전달. `/api/v1/internal/reports/{reportId}/complete`도 동일 처리 | Internal API Key |
| 임베딩 완료 콜백 | `PATCH` | `/api/v1/internal/reports/{reportId}/embedding-complete` | AI Worker가 임베딩 완료 전달 | Internal API Key |
| 평가 보고서 생성 실패 콜백 | `PATCH` | `/api/v1/internal/reports/{reportId}/fail` | AI Worker가 생성 실패 전달 | Internal API Key |

`BUSINESS` 사용자는 본인 부서 담당 특허의 보고서만 조회할 수 있습니다.
프론트는 MinIO object key를 직접 받지 않고, 백엔드가 생성한 presigned URL만 사용합니다.
AI Worker는 RabbitMQ 메시지를 소비해 보고서를 생성하고 MinIO에 저장한 뒤 내부 콜백 API를 호출합니다.

---

#### `GET /api/v1/patents/{patentId}/reports`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 평가 보고서 ID |
| patentId | long | 특허 ID |
| status | string | 생성 상태. `GENERATING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| totalScore | number | AI 평가 총점. 완료 전에는 `null` |
| valueGrade | string | AI 평가 등급. 완료 전에는 `null` |
| evaluatedAt | datetime | 평가 완료 시각. `REPORT_COMPLETED` 이후 값 있음 |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 7,
        "patentId": 1,
        "status": "EMBEDDING_COMPLETED",
        "totalScore": 82.5,
        "valueGrade": "A",
        "evaluatedAt": "2026-05-01T09:00:00Z",
        "createdAt": "2026-05-01T08:55:00Z",
        "updatedAt": "2026-05-01T09:05:00Z"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /api/v1/patents/{patentId}/reports`

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음. 호출 즉시 `GENERATING` 상태의 보고서가 등록되며, 백엔드는 RabbitMQ에 보고서 생성 요청 메시지를 발행합니다.
프론트는 응답의 `reportId`로 상태 polling을 시작합니다.

**RabbitMQ 메시지 예시**

```json
{
  "type": "REPORT_GENERATE",
  "reportId": 8001,
  "patentId": 1001
}
```

RabbitMQ 메시지 발행에 실패하면 생성 요청은 실패 처리되며 보고서 생성 상태가 시작되지 않습니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reportId": 8,
    "patentId": 1,
    "status": "GENERATING",
    "createdAt": "2026-06-07T08:55:00Z",
    "updatedAt": "2026-06-07T08:55:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `EXTERNAL_SERVICE_ERROR`(502 — RabbitMQ 발행 실패)

---

#### `GET /api/v1/patents/{patentId}/reports/latest`

**헤더**: `Authorization: Bearer {accessToken}`

가장 최근 생성된 평가 보고서 1건을 조회합니다. 완료된 보고서는 presigned URL과 AI 평가 총점/등급을 함께 반환하고, 아직 생성 중이면 `url`, `totalScore`, `valueGrade`, `evaluatedAt`이 `null`일 수 있습니다.

**Path Parameter**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentId | long | * | 특허 ID |

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 평가 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| url | string | MinIO presigned URL. 완료 전이면 `null` |
| totalScore | number | AI 평가 총점. 완료 전이면 `null` |
| valueGrade | string | AI 평가 등급. 완료 전이면 `null` |
| evaluatedAt | datetime | 평가 완료 시각. 완료 전이면 `null` |
| opinion | string | 해당 보고서에 대한 사업부 제출 의견. 없으면 `null` |
| comment | string | 해당 보고서에 대한 사업부 제출 코멘트. 없으면 `null` |
| submittedAt | datetime | 사업부 의견 제출일시. 없으면 `null` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 8,
    "patentId": 1,
    "status": "EMBEDDING_COMPLETED",
    "url": "https://minio.skipa.internal/skipa/reports/8/report.html?...",
    "totalScore": 82.5,
    "valueGrade": "A",
    "evaluatedAt": "2026-06-09T09:00:00Z",
    "opinion": "MAINTAIN",
    "comment": "핵심 특허로 판단되어 유지를 요청합니다.",
    "submittedAt": "2026-06-10T09:00:00Z",
    "createdAt": "2026-06-09T08:55:00Z",
    "updatedAt": "2026-06-09T09:05:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — BUSINESS가 본인 부서 담당 특허가 아닌 보고서 조회), `NOT_FOUND`(404 — 승인 완료 특허 또는 보고서 없음)

---

#### `GET /api/v1/patents/{patentId}/reports/history`

**헤더**: `Authorization: Bearer {accessToken}`

완료된 평가 보고서 중 최신 완료 보고서 1건을 제외한 과거 평가 이력을 조회합니다. 페이징 없이 `items` 배열로 반환합니다.

**Path Parameter**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentId | long | * | 특허 ID |

**응답**

| Name | Type | Description |
| --- | --- | --- |
| items | array | 과거 평가 이력 목록 |
| items[].id | long | 평가 보고서 ID |
| items[].patentId | long | 특허 ID |
| items[].totalScore | number | AI 평가 총점 |
| items[].valueGrade | string | AI 평가 등급 |
| items[].evaluatedAt | datetime | 평가 완료 시각 |
| items[].opinion | string | 사업부 제출 의견. 없으면 `null` |
| items[].comment | string | 사업부 제출 코멘트. 없으면 `null` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 7,
        "patentId": 1,
        "totalScore": 78.0,
        "valueGrade": "B",
        "evaluatedAt": "2026-05-01T09:00:00Z",
        "opinion": "MAINTAIN",
        "comment": "현재 제품군과 연관성이 있습니다."
      }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — BUSINESS가 본인 부서 담당 특허가 아닌 보고서 조회), `NOT_FOUND`(404 — 승인 완료 특허 없음)

---

#### `GET /api/v1/patents/{patentId}/reports/{reportId}`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 평가 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| url | string | MinIO presigned URL. `REPORT_COMPLETED` 또는 `EMBEDDING_COMPLETED` 상태에서만 포함 |
| totalScore | number | AI 평가 총점 |
| valueGrade | string | AI 평가 등급 |
| evaluatedAt | datetime | 평가 완료 시각 |
| opinion | string | 해당 보고서에 대한 사업부 제출 의견. 없으면 `null` |
| comment | string | 해당 보고서에 대한 사업부 제출 코멘트. 없으면 `null` |
| submittedAt | datetime | 사업부 의견 제출일시. 없으면 `null` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 7,
    "patentId": 1,
    "status": "REPORT_COMPLETED",
    "url": "https://minio.skipa.internal/skipa/reports/7/report.html?...",
    "totalScore": 82.5,
    "valueGrade": "A",
    "evaluatedAt": "2026-05-01T09:00:00Z",
    "opinion": "MAINTAIN",
    "comment": "핵심 특허로 판단되어 유지를 요청합니다.",
    "submittedAt": "2026-05-02T09:00:00Z",
    "createdAt": "2026-05-01T08:55:00Z",
    "updatedAt": "2026-05-01T09:05:00Z"
  }
}
```

`GENERATING` 또는 `FAILED` 상태의 보고서는 URL을 반환하지 않으며 `CONFLICT`를 반환합니다.

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 보고서 생성 미완료)

---

#### `GET /api/v1/patents/{patentId}/reports/{reportId}/status`

**헤더**: `Authorization: Bearer {accessToken}`

`GENERATING` 상태일 때 polling으로 호출합니다. `REPORT_COMPLETED` 응답 시 보고서 조회가 가능하며, `EMBEDDING_COMPLETED` 또는 `FAILED` 응답 시 전체 처리가 종료됩니다.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 평가 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` (polling 계속) / `REPORT_COMPLETED` (보고서 조회 가능) / `EMBEDDING_COMPLETED` (임베딩 완료) / `FAILED` (재시도) |
| totalScore | number | AI 평가 총점. 완료 전에는 `null` |
| valueGrade | string | AI 평가 등급. 완료 전에는 `null` |
| evaluatedAt | datetime | 완료 일시. 완료 전에는 `null` |
| updatedAt | datetime | 마지막 상태 변경 일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 8,
    "patentId": 1,
    "status": "GENERATING",
    "totalScore": null,
    "valueGrade": null,
    "evaluatedAt": null,
    "updatedAt": "2026-06-07T08:55:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /api/v1/patents/{patentId}/reports/{reportId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

평가 보고서 채팅 메시지 이력을 생성순으로 조회합니다.

**Path Parameter**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentId | long | * | 특허 ID |
| reportId | long | * | 보고서 ID |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 채팅 메시지 ID |
| patentId | long | 대상 ID. 응답 필드명은 `patentId`이나, 현재 구현상 보고서 채팅 target ID가 들어갑니다 |
| role | string | 메시지 역할. `USER` / `ASSISTANT` |
| content | string | 메시지 내용 |
| sourceCards | array | 답변 근거 카드. 없으면 빈 배열 |
| createdAt | datetime | 생성일시 |

**응답 예시**

```json
{
  "success": true,
  "data": [
    {
      "id": 100,
      "patentId": 8,
      "role": "USER",
      "content": "이 평가 보고서에서 가장 중요한 리스크는 무엇인가요?",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:00:00Z"
    },
    {
      "id": 101,
      "patentId": 8,
      "role": "ASSISTANT",
      "content": "가장 큰 리스크는 권리 범위가 경쟁 특허와 일부 중첩될 가능성입니다.",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:00:03Z"
    }
  ]
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403 — BUSINESS가 본인 부서 담당 특허가 아닌 보고서 조회), `NOT_FOUND`(404), `CONFLICT`(409 — 보고서 생성 미완료)

---

#### `POST /api/v1/patents/{patentId}/reports/{reportId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

사용자 메시지를 저장하고 AI 서버 채팅 API를 호출한 뒤 사용자 메시지와 AI 응답 메시지를 함께 반환합니다.

**Path Parameter**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| patentId | long | * | 특허 ID |
| reportId | long | * | 보고서 ID |

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| message | string | * | 사용자 채팅 메시지. 빈 문자열 불가 |

**요청 예시**

```json
{ "message": "이 평가 보고서에서 가장 중요한 리스크는 무엇인가요?" }
```

**응답**

| Name | Type | Description |
| --- | --- | --- |
| userMessage | object | 저장된 사용자 메시지 |
| assistantMessage | object | 저장된 AI 응답 메시지 |

`userMessage`, `assistantMessage` 필드는 `GET /api/v1/patents/{patentId}/reports/{reportId}/chat/messages`의 메시지 항목과 동일합니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "userMessage": {
      "id": 100,
      "patentId": 8,
      "role": "USER",
      "content": "이 평가 보고서에서 가장 중요한 리스크는 무엇인가요?",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:00:00Z"
    },
    "assistantMessage": {
      "id": 101,
      "patentId": 8,
      "role": "ASSISTANT",
      "content": "가장 큰 리스크는 권리 범위가 경쟁 특허와 일부 중첩될 가능성입니다.",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:00:03Z"
    }
  }
}
```

**에러**: `INVALID_REQUEST`(400 — message 누락), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 보고서 생성 미완료), `AI_SERVER_ERROR`(500 — AI 서버 호출 실패)

---

#### `POST /api/v1/patents/{patentId}/reports/{reportId}/chat/messages/stream`

**헤더**: `Authorization: Bearer {accessToken}`

응답 Content-Type은 `text/event-stream`입니다. 사용자 메시지를 저장하고 AI 서버 SSE 스트리밍 응답을 클라이언트에 그대로 중계한 뒤, `done` 이벤트 수신 시 최종 AI 응답 메시지를 저장합니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| message | string | * | 사용자 채팅 메시지. 빈 문자열 불가 |

**요청 예시**

```json
{ "message": "이 평가 보고서에서 가장 중요한 리스크는 무엇인가요?" }
```

**에러**: 스트림 내부 에러 이벤트 또는 `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 보고서 생성 미완료)

---

#### `DELETE /api/v1/patents/{patentId}/reports/{reportId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

해당 보고서 채팅 메시지를 모두 삭제합니다. 요청 Body 없음.

**응답 예시**

```json
{
  "success": true,
  "data": null
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `PATCH /api/v1/internal/reports/{reportId}/report-complete`

AI Worker가 보고서 생성 완료 후 호출합니다.

기존 호환을 위해 `/api/v1/internal/reports/{reportId}/complete`도 동일하게 동작합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| reportKey | string | * | MinIO object key. 전체 URL이 아닌 object key만 전달 |
| totalScore | number | * | AI 평가 총점 |
| valueGrade | string | * | AI 평가 등급. `S` / `A` / `B` / `C` / `D` |

**요청 예시**

```json
{
  "reportKey": "reports/8001/report.html",
  "totalScore": 82.5,
  "valueGrade": "A"
}
```

**처리**

- `reportKey` 저장
- 보고서 상태를 `REPORT_COMPLETED`로 변경
- `evaluatedAt`에 완료 시각 기록

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reportId": 8001,
    "status": "REPORT_COMPLETED",
    "totalScore": 82.5,
    "valueGrade": "A"
  }
}
```

---

#### `PATCH /api/v1/internal/reports/{reportId}/embedding-complete`

AI Worker가 평가 보고서 임베딩 완료 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

요청 Body 없음.

**처리**

- 보고서 상태를 `EMBEDDING_COMPLETED`로 변경

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reportId": 8001,
    "status": "EMBEDDING_COMPLETED",
    "totalScore": 82.5,
    "valueGrade": "A"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

#### `PATCH /api/v1/internal/reports/{reportId}/fail`

AI Worker가 보고서 생성 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| errorMessage | string | N | 선택 입력. 현재 서버 상태 응답에는 반영되지 않음 |

**요청 예시**

```json
{
  "errorMessage": "AI report generation failed"
}
```

**처리**

- 보고서 상태를 `FAILED`로 변경
- 요청 Body는 선택 사항이며, 현재 구현은 `errorMessage` 값을 별도로 반영하지 않음

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reportId": 8001,
    "status": "FAILED"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

### 12. 사전 평가

정식 특허 출원 전에 특허 아이디어의 가치와 심사 통과 가능성을 확인하기 위한 기능입니다.
사전 평가는 정식 `Patent`와 별도 이력으로 저장되며, `BUSINESS` 사용자 본인이 생성한 이력만 조회할 수 있습니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사전 평가 시작 | `POST` | `/api/v1/pre-evaluations` | 임시 특허 정보를 저장하고 AI 서버에 사전 평가 보고서 생성을 요청 | `BUSINESS` |
| 사전 평가 이력 목록 조회 | `GET` | `/api/v1/pre-evaluations` | 현재 사용자의 사전 평가 이력 목록 조회 | `BUSINESS` |
| 사전 평가 상세 조회 | `GET` | `/api/v1/pre-evaluations/{preEvaluationId}` | 사전 평가 입력 정보, 상태, 보고서 URL 조회 | `BUSINESS` |
| 사전 평가 처리 상태 조회 | `GET` | `/api/v1/pre-evaluations/{preEvaluationId}/status` | 보고서 생성 및 임베딩 상태 polling | `BUSINESS` |
| 사전 평가 이력 삭제 | `DELETE` | `/api/v1/pre-evaluations/{preEvaluationId}` | 사전 평가와 관련 채팅 메시지 삭제 | `BUSINESS` |
| 사전 평가 채팅 이력 조회 | `GET` | `/api/v1/pre-evaluations/{preEvaluationId}/chat/messages` | 사전 평가별 채팅 메시지 목록 조회 | `BUSINESS` |
| 사전 평가 채팅 메시지 전송 | `POST` | `/api/v1/pre-evaluations/{preEvaluationId}/chat/messages` | 사용자 메시지 저장, AI 서버 채팅 API 호출, 응답 저장 | `BUSINESS` |
| 사전 평가 채팅 스트리밍 전송 | `POST` | `/api/v1/pre-evaluations/{preEvaluationId}/chat/messages/stream` | 사용자 메시지 저장 후 AI 서버 SSE 스트리밍 응답 중계 | `BUSINESS` |
| 사전 평가 채팅 초기화 | `DELETE` | `/api/v1/pre-evaluations/{preEvaluationId}/chat/messages` | 해당 사전 평가의 채팅 메시지 전체 삭제 | `BUSINESS` |
| 사전 평가 보고서 생성 완료 callback | `PATCH` | `/api/v1/internal/pre-evaluations/{preEvaluationId}/report-complete` | AI 서버가 보고서 생성 완료 후 호출. `/api/v1/internal/pre-evaluations/{preEvaluationId}/complete`도 동일 처리 | Internal API Key |
| 사전 평가 임베딩 완료 callback | `PATCH` | `/api/v1/internal/pre-evaluations/{preEvaluationId}/embedding-complete` | AI 서버가 임베딩 완료 후 호출 | Internal API Key |
| 사전 평가 실패 callback | `PATCH` | `/api/v1/internal/pre-evaluations/{preEvaluationId}/fail` | AI 서버가 보고서 생성 실패 후 호출 | Internal API Key |

사전 평가 상태값은 다음과 같습니다.

| status | 설명 |
| --- | --- |
| `PROCESSING` | 사전 평가 보고서 생성 중 |
| `REPORT_COMPLETED` | 보고서 생성 완료 |
| `EMBEDDING_COMPLETED` | 임베딩 완료 |
| `FAILED` | 보고서 생성 실패 |

---

#### `POST /api/v1/pre-evaluations`

**헤더**: `Authorization: Bearer {accessToken}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| title | string | * | 특허명. 최대 200자 |
| technicalDescription | string | * | 기술 설명 |
| claims | array[string] | * | 청구항 목록. 빈 배열 불가 |
| relatedBusiness | string | N | 관련 사업. 최대 500자 |
| targetCountries | string | N | 출원 예정 국가. 예: `한국, 미국`. 최대 500자 |

**요청 예시**

```json
{
  "title": "배터리 열폭주 감지 시스템",
  "technicalDescription": "센서 데이터를 기반으로 배터리 열폭주 가능성을 조기에 감지하는 기술",
  "claims": [
    "센서부를 포함하는 배터리 열폭주 감지 시스템",
    "분석부가 센서 데이터를 기반으로 위험도를 산출하는 시스템"
  ],
  "relatedBusiness": "전기차 배터리 안전 관리",
  "targetCountries": "한국, 미국"
}
```

**처리**

- 사전 평가 row를 생성하고 `status = PROCESSING`으로 저장
- AI 서버에 사전 평가 보고서 생성 메시지 발행

**발행 메시지 예시**

```json
{
  "type": "PRE_EVALUATION_GENERATE",
  "preEvaluationId": 1,
  "userId": 10,
  "title": "배터리 열폭주 감지 시스템",
  "technicalDescription": "센서 데이터를 기반으로 배터리 열폭주 가능성을 조기에 감지하는 기술",
  "claims": [
    "센서부를 포함하는 배터리 열폭주 감지 시스템",
    "분석부가 센서 데이터를 기반으로 위험도를 산출하는 시스템"
  ],
  "relatedBusiness": "전기차 배터리 안전 관리",
  "targetCountries": "한국, 미국"
}
```

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| userId | long | 생성 사용자 ID |
| status | string | `PROCESSING` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 10,
    "status": "PROCESSING",
    "createdAt": "2026-06-10T08:55:00Z",
    "updatedAt": "2026-06-10T08:55:00Z"
  }
}
```

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `EXTERNAL_SERVICE_ERROR`(502)

---

#### `GET /api/v1/pre-evaluations`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

정렬은 최신 생성순(`id` 내림차순)입니다.

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| title | string | 평가 제목 |
| status | string | 평가 상태. `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| reportUrl | string | 결과 보고서 URL. 완료 전에는 `null` |
| completedAt | datetime | 완료 시각. 처리 중에는 `null` |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "배터리 열폭주 감지 시스템",
        "status": "EMBEDDING_COMPLETED",
        "reportUrl": "https://minio.example.com/pre-evaluations/1/report.html",
        "completedAt": "2026-06-10T09:01:00Z",
        "createdAt": "2026-06-10T08:55:00Z",
        "updatedAt": "2026-06-10T09:01:00Z"
      }
    ],
    "page": 0,
    "size": 50,
    "totalItems": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/pre-evaluations/{preEvaluationId}`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| userId | long | 생성 사용자 ID |
| title | string | 평가 제목 |
| technicalDescription | string | 기술 설명 |
| claims | array[string] | 청구항 목록 |
| relatedBusiness | string | 관련 사업 |
| targetCountries | string | 출원 예정 국가 |
| status | string | 평가 상태. `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| reportUrl | string | 결과 보고서 URL. 완료 전에는 `null` |
| completedAt | datetime | 완료 시각 |
| createdAt | datetime | 생성일시 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 10,
    "title": "배터리 열폭주 감지 시스템",
    "technicalDescription": "센서 데이터를 기반으로 배터리 열폭주 가능성을 조기에 감지하는 기술",
    "claims": [
      "센서부를 포함하는 배터리 열폭주 감지 시스템",
      "분석부가 센서 데이터를 기반으로 위험도를 산출하는 시스템"
    ],
    "relatedBusiness": "전기차 배터리 안전 관리",
    "targetCountries": "한국, 미국",
    "status": "REPORT_COMPLETED",
    "reportUrl": "https://minio.example.com/pre-evaluations/1/report.html",
    "completedAt": "2026-06-10T09:01:00Z",
    "createdAt": "2026-06-10T08:55:00Z",
    "updatedAt": "2026-06-10T09:01:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /api/v1/pre-evaluations/{preEvaluationId}/status`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| status | string | `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| completedAt | datetime | 완료 시각 |
| updatedAt | datetime | 수정일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "status": "PROCESSING",
    "completedAt": null,
    "updatedAt": "2026-06-10T08:55:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `DELETE /api/v1/pre-evaluations/{preEvaluationId}`

**헤더**: `Authorization: Bearer {accessToken}`

사전 평가 이력과 해당 사전 평가의 채팅 메시지를 삭제합니다.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /api/v1/pre-evaluations/{preEvaluationId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 채팅 메시지 ID |
| preEvaluationId | long | 대상 ID(사전 평가 ID) |
| role | string | 메시지 역할. `USER` / `ASSISTANT` |
| content | string | 메시지 내용 |
| sourceCards | array | 답변 근거 카드. 사용자 메시지는 빈 배열 |
| createdAt | datetime | 생성일시 |

**응답 예시**

```json
{
  "success": true,
  "data": [
    {
      "id": 100,
      "preEvaluationId": 1,
      "role": "USER",
      "content": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:10:00Z"
    },
    {
      "id": 101,
      "preEvaluationId": 1,
      "role": "ASSISTANT",
      "content": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다.",
      "sourceCards": [
        {
          "label": "S1",
          "title": "사전평가 보고서",
          "display_title": "사전평가 보고서",
          "source_type": "pre_evaluation",
          "page_no": 1,
          "url": "https://example.com/pre-evaluations/1/report.html",
          "location_label": "section 1",
          "source_path": "pre-evaluations/1/report.html",
          "match_terms": ["청구항"],
          "snippet": "청구항에서 센서 데이터 처리 알고리즘의 차별성을..."
        }
      ],
      "createdAt": "2026-06-10T09:10:02Z"
    }
  ]
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /api/v1/pre-evaluations/{preEvaluationId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| message | string | * | 사용자 채팅 메시지 |

**요청 예시**

```json
{
  "message": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?"
}
```

**처리**

- 사용자 메시지를 `USER` 역할로 저장
- 최근 질문/답변 5쌍을 `chat_history`로 구성해 AI 서버 채팅 API를 직접 호출
- AI 응답의 `answer`를 `ASSISTANT` 메시지로 저장하고, `source_cards`는 `metadata`를 제외해 함께 저장

**AI 서버 채팅 요청 예시**

`POST /api/v1/pre-eval/cases/{case_id}/chat`

```json
{
  "chat_history": [
    {
      "question": "이 사전평가 보고서의 주요 리스크를 알려줘",
      "answer": "주요 리스크는 청구항의 구체성 부족입니다."
    }
  ],
  "question": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?",
  "user_id": "10"
}
```

**AI 서버 채팅 응답 형식**

```json
{
  "query": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?",
  "patent_id": "1",
  "answer": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다.",
  "source_cards": [
    {
      "label": "S1",
      "title": "사전평가 보고서",
      "display_title": "사전평가 보고서",
      "source_type": "pre_evaluation",
      "page_no": 1,
      "url": "https://example.com/pre-evaluations/1/report.html",
      "location_label": "section 1",
      "source_path": "pre-evaluations/1/report.html",
      "match_terms": ["청구항"],
      "snippet": "청구항에서 센서 데이터 처리 알고리즘의 차별성을...",
      "metadata": {}
    }
  ],
  "metrics": {}
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "userMessage": {
      "id": 100,
      "preEvaluationId": 1,
      "role": "USER",
      "content": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?",
      "sourceCards": [],
      "createdAt": "2026-06-10T09:10:00Z"
    },
    "assistantMessage": {
      "id": 101,
      "preEvaluationId": 1,
      "role": "ASSISTANT",
      "content": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다.",
      "sourceCards": [
        {
          "label": "S1",
          "title": "사전평가 보고서",
          "display_title": "사전평가 보고서",
          "source_type": "pre_evaluation",
          "page_no": 1,
          "url": "https://example.com/pre-evaluations/1/report.html",
          "location_label": "section 1",
          "source_path": "pre-evaluations/1/report.html",
          "match_terms": ["청구항"],
          "snippet": "청구항에서 센서 데이터 처리 알고리즘의 차별성을..."
        }
      ],
      "createdAt": "2026-06-10T09:10:02Z"
    }
  }
}
```

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `AI_SERVER_ERROR`(500)

---

#### `POST /api/v1/pre-evaluations/{preEvaluationId}/chat/messages/stream`

**헤더**: `Authorization: Bearer {accessToken}`

응답 Content-Type은 `text/event-stream`입니다. 사용자 메시지를 저장하고 AI 서버 SSE 스트리밍 응답을 클라이언트에 그대로 중계한 뒤, `done` 이벤트 수신 시 최종 AI 응답 메시지를 저장합니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| message | string | * | 사용자 채팅 메시지. 빈 문자열 불가 |

**요청 예시**

```json
{ "message": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?" }
```

**에러**: 스트림 내부 에러 이벤트 또는 `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `DELETE /api/v1/pre-evaluations/{preEvaluationId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

해당 사전 평가의 채팅 메시지를 모두 삭제합니다.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/report-complete`

AI 서버가 사전 평가 보고서 생성 완료 후 호출합니다.

기존 호환을 위해 `/api/v1/internal/pre-evaluations/{preEvaluationId}/complete`도 동일하게 동작합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| reportKey | string | * | MinIO object key. 전체 URL이 아닌 object key만 전달 |

**요청 예시**

```json
{
  "reportKey": "pre-evaluations/1/report.html"
}
```

**처리**

- `reportKey` 저장
- 사전 평가 상태를 `REPORT_COMPLETED`로 변경
- `completedAt`에 완료 시각 기록

**응답 예시**

```json
{
  "success": true,
  "data": {
    "preEvaluationId": 1,
    "status": "REPORT_COMPLETED",
    "completedAt": "2026-06-10T09:01:00Z"
  }
}
```

---

#### `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/embedding-complete`

AI 서버가 사전 평가 임베딩 완료 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

요청 Body 없음.

**처리**

- 사전 평가 상태를 `EMBEDDING_COMPLETED`로 변경

**응답 예시**

```json
{
  "success": true,
  "data": {
    "preEvaluationId": 1,
    "status": "EMBEDDING_COMPLETED",
    "completedAt": "2026-06-10T09:01:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

#### `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/fail`

AI 서버가 사전 평가 보고서 생성 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

요청 Body는 선택 사항입니다.

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| errorMessage | string | N | 실패 사유. 현재 DB에는 저장하지 않음 |

**요청 예시**

```json
{
  "errorMessage": "AI pre-evaluation generation failed"
}
```

**처리**

- 사전 평가 상태를 `FAILED`로 변경
- `completedAt`에 실패 처리 시각 기록

**응답 예시**

```json
{
  "success": true,
  "data": {
    "preEvaluationId": 1,
    "status": "FAILED",
    "reportUrl": null,
    "completedAt": "2026-06-10T09:01:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

### 13. 대시보드

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| Legal 홈 대시보드 조회 | `GET` | `/api/v1/dashboard/legal` | Legal 홈 화면용 검토 현황, 분포, 최근 회신 조회 | `ADMIN`, `LEGAL` |
| Business 홈 대시보드 조회 | `GET` | `/api/v1/dashboard/business` | Business 홈 화면용 소속 부서 검토 현황과 특허 요약 조회 | `BUSINESS` |

#### `GET /api/v1/dashboard/legal`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| reviewCycleId | long | N | 검토 주기 ID. 미지정 시 현재 활성 검토 주기 기준 |

**응답 주요 필드**

| Name | Type | Description |
| --- | --- | --- |
| reviewCycle | object | 검토 주기 요약 |
| kpi | object | `requested`, `reviewing`, `decided`, `overdue`, `unread`, `unrequested` |
| cycleProgress | object | 현재 검토 주기의 대상 특허/보고서/사업부 검토 진행 현황 |
| departments | array | 부서별 `assigned`, `decided` |
| byTechField | array | 기술 분야별 건수 |
| byExpiryQuarter | array | 만료 분기별 건수 |
| recentReplies | array | 최근 사업부 회신 목록 |

**`cycleProgress` 필드**

| Name | Type | Description |
| --- | --- | --- |
| targetPatentCount | long | 이번 검토 주기의 대상 특허 수 |
| reports.notStarted | long | 보고서 생성 전 건수 |
| reports.generating | long | 보고서 생성 중 건수 |
| reports.completed | long | 보고서 생성 완료 건수 (`REPORT_COMPLETED`, `EMBEDDING_COMPLETED`) |
| reports.failed | long | 보고서 생성 실패 건수 |
| reviews.scheduled | long | 사업부 요청 전 건수 (`SCHEDULED`) |
| reviews.inReview | long | 사업부 검토 중 건수 (`PENDING`, `OVERDUE`) |
| reviews.submitted | long | 제출 완료 건수 (`SUBMITTED`) |
| statusLabel | string | 전체 진행 상태 라벨. 가능한 값은 아래 표 참고 |

**`cycleProgress.statusLabel` 값**

| Value | Meaning |
| --- | --- |
| `NO_TARGETS` | 검토 대상 특허가 아직 선정되지 않음 |
| `REPORT_NOT_STARTED` | 대상 특허 중 보고서가 아직 생성되지 않은 건이 있음 |
| `REPORT_GENERATING` | 대상 특허 중 보고서 생성 중인 건이 있음 |
| `REPORT_FAILED` | 대상 특허 중 보고서 생성 실패 건이 있음 |
| `REVIEW_NOT_REQUESTED` | 보고서는 준비됐지만 사업부 요청 전(`SCHEDULED`) 건이 있음 |
| `REVIEW_IN_PROGRESS` | 사업부 검토 중(`PENDING`, `OVERDUE`) 건이 있음 |
| `REVIEW_COMPLETED` | 사업부 검토가 모두 제출 완료됨 |

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/dashboard/business`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| reviewCycleId | long | N | 검토 주기 ID. 미지정 시 현재 활성 검토 주기 기준 |

**응답 주요 필드**

| Name | Type | Description |
| --- | --- | --- |
| reviewCycle | object | 검토 주기 요약 |
| kpi | object | `total`, `submitted`, `notSubmitted` |
| pendingPatents | array | 미제출 검토 대상 특허 목록 |
| recentSubmissions | array | 최근 제출 이력 |
| patentStatus | object | `active`, `inactive` |
| yearlyTrends | array | 연도별 출원/소멸 추이 |

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

### 14. 포트폴리오

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 포트폴리오 인사이트 조회 | `GET` | `/api/v1/portfolio/insights` | 포트폴리오 데이터 기반 AI 인사이트 조회 | `ADMIN`, `LEGAL` |
| 포트폴리오 분포 조회 | `GET` | `/api/v1/portfolio/distribution` | 평가 등급, 기술 분야, 국가, 사업부별 분포 조회 | `ADMIN`, `LEGAL` |
| 포트폴리오 추이 조회 | `GET` | `/api/v1/portfolio/trends` | 연도별 출원/등록/소멸 추이와 연차료 추이 조회 | `ADMIN`, `LEGAL` |
| 포트폴리오 결정 비율 조회 | `GET` | `/api/v1/portfolio/decisions` | 분기, 사업부, 기술 분야별 유지/포기 결정 현황 조회 | `ADMIN`, `LEGAL` |

---

#### `GET /api/v1/portfolio/insights`

**헤더**: `Authorization: Bearer {accessToken}`

포트폴리오 분포/추이/결정 데이터를 기반으로 AI 인사이트를 조회합니다. 캐시가 있으면 캐시된 인사이트를 반환합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| insights | array | AI가 생성한 포트폴리오 인사이트 문장 목록 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "insights": [
      "A등급 이상 특허는 배터리 분야에 집중되어 있어 핵심 포트폴리오 유지 전략이 필요합니다.",
      "최근 3년간 소멸 예정 특허가 증가하고 있어 연차료 대비 사업 기여도 점검이 필요합니다."
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `AI_SERVER_ERROR`(500 — AI 인사이트 생성 실패)

---

#### `GET /api/v1/portfolio/distribution`

**헤더**: `Authorization: Bearer {accessToken}`

평가 등급, 기술 분야, 출원국, 담당 부서별 포트폴리오 분포를 조회합니다.
기술 분야별(`byTechField`) 및 담당 부서별(`byDepartment`) 분포는 특허 수 내림차순으로 정렬하며, 상위 4개를 초과하는 항목은 `기타`로 합산해 최대 5개 항목만 반환합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| byGrade | array | 부서별 평가 등급 분포 |
| byGrade[].departmentId | long | 부서 ID |
| byGrade[].departmentName | string | 부서명 |
| byGrade[].s | long | S등급 특허 수 |
| byGrade[].a | long | A등급 특허 수 |
| byGrade[].b | long | B등급 특허 수 |
| byGrade[].c | long | C등급 특허 수 |
| byGrade[].d | long | D등급 특허 수 |
| byTechField | array | 기술 분야별 특허 수. 특허 수 내림차순, 최대 5개(상위 4개 + 기타) |
| byTechField[].name | string | 기술 분야명 |
| byTechField[].count | long | 특허 수 |
| byFilingCountry | array | 출원국별 특허 수 |
| byFilingCountry[].country | string | 출원국 |
| byFilingCountry[].count | long | 특허 수 |
| byDepartment | array | 담당 부서별 특허 수. 특허 수 내림차순, 최대 5개(상위 4개 + 기타) |
| byDepartment[].departmentId | long | 부서 ID. `기타` 항목은 `null` |
| byDepartment[].departmentName | string | 부서명. 합산 항목은 `기타` |
| byDepartment[].count | long | 특허 수 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "byGrade": [
      {
        "departmentId": 3,
        "departmentName": "에너지솔루션 사업부",
        "s": 1,
        "a": 8,
        "b": 12,
        "c": 4,
        "d": 1
      }
    ],
    "byTechField": [
      { "name": "배터리", "count": 26 },
      { "name": "반도체", "count": 18 },
      { "name": "기타", "count": 9 }
    ],
    "byFilingCountry": [
      { "country": "KR", "count": 18 },
      { "country": "US", "count": 8 }
    ],
    "byDepartment": [
      { "departmentId": 3, "departmentName": "에너지솔루션 사업부", "count": 26 },
      { "departmentId": 4, "departmentName": "반도체 사업부", "count": 18 },
      { "departmentId": null, "departmentName": "기타", "count": 9 }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/portfolio/trends`

**헤더**: `Authorization: Bearer {accessToken}`

연도별 출원/등록/소멸 추이와 연차료 비용 추이를 조회합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| yearlyPatentTrends | array | 연도별 특허 흐름 |
| yearlyPatentTrends[].year | integer | 연도 |
| yearlyPatentTrends[].applications | long | 해당 연도 출원 건수 |
| yearlyPatentTrends[].registrations | long | 해당 연도 등록 건수 |
| yearlyPatentTrends[].expiries | long | 해당 연도 소멸 예정/소멸 건수 |
| yearlyAnnuityCosts | array | 연도별 연차료 비용 |
| yearlyAnnuityCosts[].year | integer | 연도 |
| yearlyAnnuityCosts[].amount | long | 연차료 금액 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "yearlyPatentTrends": [
      {
        "year": 2026,
        "applications": 24,
        "registrations": 18,
        "expiries": 3
      }
    ],
    "yearlyAnnuityCosts": [
      { "year": 2026, "amount": 12500000 }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `GET /api/v1/portfolio/decisions`

**헤더**: `Authorization: Bearer {accessToken}`

분기별 유지/포기 결정 현황과 사업부·기술 분야별 세부 분포를 조회합니다.

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| byQuarter | array | 분기별 결정 현황 |
| byQuarter[].quarter | string | 분기 라벨. 예: `2026-Q2` |
| byQuarter[].maintain | long | 유지 의견 수 |
| byQuarter[].abandon | long | 포기 의견 수 |
| byQuarter[].byDepartment | array | 사업부별 유지/포기 현황 |
| byQuarter[].byDepartment[].departmentId | long | 부서 ID |
| byQuarter[].byDepartment[].departmentName | string | 부서명 |
| byQuarter[].byDepartment[].maintain | long | 유지 의견 수 |
| byQuarter[].byDepartment[].abandon | long | 포기 의견 수 |
| byQuarter[].byTechField | array | 기술 분야별 유지/포기 현황 |
| byQuarter[].byTechField[].name | string | 기술 분야명 |
| byQuarter[].byTechField[].maintain | long | 유지 의견 수 |
| byQuarter[].byTechField[].abandon | long | 포기 의견 수 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "byQuarter": [
      {
        "quarter": "2026-Q2",
        "maintain": 12,
        "abandon": 3,
        "byDepartment": [
          {
            "departmentId": 3,
            "departmentName": "에너지솔루션 사업부",
            "maintain": 8,
            "abandon": 1
          }
        ],
        "byTechField": [
          { "name": "배터리", "maintain": 8, "abandon": 1 }
        ]
      }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

**공통 에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)
