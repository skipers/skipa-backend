# API 명세서

변경 날짜: 2026-06-10

## 기본 정보

| 항목 | 내용 |
| --- | --- |
| Base URL | `https://api.skipa.internal` |
| URL 버전 prefix | 없음 |
| 인증 방식 | JWT Bearer Token (`Authorization: Bearer <token>`) |
| 내부 API 인증 방식 | Internal API Key (`X-Internal-Api-Key: <secret>`) |
| 토큰 발급 | `POST /auth/login` |
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
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

## 역할

| Role | 설명 |
| --- | --- |
| `ADMIN` | 사용자 승인, 부서 관리, 전체 조회, 특허 기본 정보 관리 |
| `LEGAL` | 특허 관리, 검토 요청, 검토 현황 조회, 보고서 관리 |
| `BUSINESS` | 본인 부서 담당 특허 조회, 요청받은 검토 확인 및 의견 제출 |

## Enum

| 구분 | 값 |
| --- | --- |
| 사용자 역할 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 사용자 상태 | `PENDING`, `ACTIVE` |
| 부서 상태 | `ACTIVE`, `INACTIVE` |
| 권리 상태 | `PUBLISHED`, `REGISTERED`, `REJECTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `WITHDRAWN` |
| 연차료 납부 상태 | `PAID`, `UNPAID`, `ABANDONED` |
| 특허 추출 작업 상태 | `UPLOAD_PENDING`, `ANALYZING`, `COMPLETED`, `FAILED` |
| 보고서 처리 상태 | `GENERATING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 사전 평가 처리 상태 | `PROCESSING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 사전 평가 채팅 역할 | `USER`, `ASSISTANT` |
| 검토 주기 유형 | `QUARTERLY`, `AD_HOC` |
| 검토 제출 상태 | `PENDING`, `SUBMITTED` |
| 사업부 의견 | `MAINTAIN`, `ABANDON` |

---

## API 목록

### 1. 인증

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/auth/login` | ID/PW 검증 후 access token, refresh token, 사용자 정보 반환 | 없음 |
| 회원가입 | `POST` | `/auth/register` | `PENDING` 상태의 사용자 계정 생성 | 없음 |
| 로그아웃 | `POST` | `/auth/logout` | 토큰 무효화 | 인증 사용자 |
| 내 정보 조회 | `GET` | `/auth/me` | 현재 로그인 사용자 정보 반환 | 인증 사용자 |
| 토큰 갱신 | `POST` | `/auth/refresh` | refresh token으로 access token 재발급 | 없음 |

---

#### `POST /auth/login`

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
| accessToken | string | 유효기간 10분. 만료 시 `/auth/refresh` 호출 |
| refreshToken | string | 유효기간 7일 |
| user.id | long | 사용자 DB ID |
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
| 401 | `UNAUTHORIZED` | 아이디 또는 비밀번호 불일치 |
| 403 | `FORBIDDEN` | 미승인 계정 (`status = PENDING`) |

---

#### `GET /auth/me`

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

#### `POST /auth/refresh`

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

#### `POST /auth/logout`

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401)

---

### 2. 사용자

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사용자 목록 조회 | `GET` | `/users` | 전체 사용자 목록 조회. 검색과 필터 사용 가능 | `ADMIN` |
| 사용자 생성 | `POST` | `/users` | 신규 사용자 등록 | `ADMIN` |
| 사용자 단일 조회 | `GET` | `/users/{userId}` | 특정 사용자 정보 조회 | `ADMIN` |
| 사용자 수정 | `PUT` | `/users/{userId}` | 이름, 이메일, 역할, 부서 수정 | `ADMIN` |
| 사용자 삭제 | `DELETE` | `/users/{userId}` | 사용자 삭제 | `ADMIN` |

### 2-1. 사용자 가입 승인

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사용자 가입 승인 | `PATCH` | `/admin/users/{userId}/approve` | 사용자를 `ACTIVE`로 변경. `BUSINESS` 역할은 활성 부서 지정 필수 | `ADMIN` |

---

### 3. 부서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 부서 목록 조회 | `GET` | `/departments` | 활성 부서 목록 조회. `page`, `size` 사용 가능 | `ADMIN`, `LEGAL` |
| 부서 단일 조회 | `GET` | `/departments/{departmentId}` | 부서 정보 조회. 비활성 부서도 조회 가능 | `ADMIN`, `LEGAL` |
| 부서 생성 | `POST` | `/departments` | 활성 부서 생성 | `ADMIN` |
| 부서 수정 | `PUT` | `/departments/{departmentId}` | 부서명 수정 | `ADMIN` |
| 부서 비활성화 | `DELETE` | `/departments/{departmentId}` | 삭제 대신 상태를 `INACTIVE`로 변경 | `ADMIN` |

비활성 부서는 기존 사용자, 특허, 검토 이력의 참조를 유지합니다.
비활성 부서는 신규 사용자 승인, 특허 담당 부서 변경, 신규 검토 요청에 사용할 수 없습니다.

---

#### `GET /departments`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 50) |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 부서 ID |
| name | string | 부서명 |
| status | string | `ACTIVE` / `INACTIVE` |

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
| 특허 목록 조회 | `GET` | `/patents` | 키워드·상태·국가·기술분야·사업부·검토상태·정렬 필터 포함 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 통계 조회 | `GET` | `/patents/stats` | 권리 상태별·기술분야별·국가별·사업부별 집계 | `ADMIN`, `LEGAL` |
| 특허 단일 조회 | `GET` | `/patents/{patentId}` | 특허 상세 정보 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 등록 | `POST` | `/patents` | 특허 정보 수동 등록 | `ADMIN`, `LEGAL` |
| 특허 수정 | `PUT` | `/patents/{patentId}` | 특허 정보 수정 | `ADMIN`, `LEGAL` |
| 담당 부서 변경 | `PATCH` | `/patents/{patentId}/department` | 현재 담당 부서를 활성 부서로 변경 | `ADMIN`, `LEGAL` |
| 특허 삭제 | `DELETE` | `/patents/{patentId}` | 특허와 권리 상태, 연차료, 검토, 보고서 삭제 | `ADMIN`, `LEGAL` |

`BUSINESS` 사용자는 현재 담당 부서가 본인 소속 부서와 같은 특허만 조회할 수 있습니다.

---

#### `GET /patents`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| query | string | N | 특허명 키워드 검색 |
| status | string (복수) | N | 권리 상태 필터. `PUBLISHED` / `REGISTERED` / `REJECTED` / `ABANDONED` / `EXPIRED` / `INVALIDATED` / `WITHDRAWN`. 복수 지정 가능 (`?status=EXPIRED&status=ABANDONED`) |
| filingCountry | string | N | 출원국 코드. `KR` / `US` / `EP` / `JP` / `CN` |
| techField | string | N | 기술 분야 필터 |
| departmentId | long | N | 현재 담당 사업부 ID. `-1` 지정 시 미배정 특허만 조회 |
| reviewStatus | string | N | 재평가 관리 탭 필터. `unread` / `unassigned` / `requested` / `overdue` / `done`. 생략 시 전체 (현재 활성 QUARTERLY 주기 기준 자동 적용) |
| decision | string | N | 결정 필터. `MAINTAIN` / `ABANDON` |
| sort | string | N | 정렬 기준. `expiryDate` / `applicationDate` / `citationCount` |
| page | integer | N | 페이지 번호 (기본값 0) |
| size | integer | N | 페이지 크기 (기본값 20) |

**reviewStatus 값과 서버 조건**

| reviewStatus | 서버 조건 |
| --- | --- |
| (생략) | 현재 활성 QUARTERLY 주기 내 전체 특허 |
| `unread` | `reviews.status = SUBMITTED AND confirmed_at IS NULL` |
| `unassigned` | `patents.current_department_id IS NULL` |
| `requested` | `reviews.status = PENDING AND due_date >= today` |
| `overdue` | `reviews.status = PENDING AND due_date < today` |
| `done` | `reviews.status = SUBMITTED` |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| registrationNumber | string | 등록번호 |
| applicationDate | string | 출원일 (`yyyy-MM-dd`) |
| expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |
| status | string | 최신 권리 상태 |
| techField | string | 기술 분야 |
| businessField | string | 관련사업 분야 |
| overview | string | 특허 개요 |
| citationCount | integer | 피인용 수 |
| filingCountry | string | 출원국 코드 |
| currentDepartmentId | long | 현재 담당 부서 ID. 미배정이면 `null` |
| currentDepartmentName | string | 현재 담당 부서명. 미배정이면 `null` |
| reviewStatus | string | 검토 상태. `reviewStatus` 파라미터 사용 시 포함 |
| decision | string | 사업부 결정. `MAINTAIN` / `ABANDON` / `null` |
| isOverdue | boolean | 기한 초과 여부. `reviewStatus` 파라미터 사용 시 포함 |

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
        "status": "REGISTERED",
        "techField": "배터리",
        "businessField": "에너지솔루션",
        "overview": "고용량 배터리 셀의 음극재 구조를 개선하여 에너지 밀도를 향상시킨 발명",
        "citationCount": 14,
        "filingCountry": "KR",
        "currentDepartmentId": 3,
        "currentDepartmentName": "에너지솔루션 사업부",
        "reviewStatus": "done",
        "decision": "MAINTAIN",
        "isOverdue": false
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

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404 — 활성 QUARTERLY 주기 없음, `reviewStatus` 사용 시)

---

#### `GET /patents/stats`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| total | long | 전체 특허 수 |
| byLegalStatus | object | 권리 상태별 건수. 키: `PUBLISHED` / `REGISTERED` / `REJECTED` / `ABANDONED` / `EXPIRED` / `INVALIDATED` / `WITHDRAWN` |
| expiring.in3Months | long | 90일 이내 만료 건수 |
| expiring.in6Months | long | 180일 이내 만료 건수 |
| expiring.in1Year | long | 365일 이내 만료 건수 |
| byTechField | array | 기술 분야별 건수 (`name`, `count`) |
| byExpiryQuarter | array | 분기별 만료 예정 건수 (`quarter`, `count`). 현재 분기부터 4분기 |
| byFilingCountry | array | 출원국별 건수 (`country`, `count`) |
| byDepartment | array | 담당 사업부별 건수 (`departmentId`, `departmentName`, `count`) |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "total": 247,
    "byLegalStatus": {
      "PUBLISHED": 10, "REGISTERED": 98, "REJECTED": 5,
      "ABANDONED": 20, "EXPIRED": 12, "INVALIDATED": 4, "WITHDRAWN": 3
    },
    "expiring": { "in3Months": 4, "in6Months": 9, "in1Year": 38 },
    "byTechField": [
      { "name": "반도체", "count": 82 },
      { "name": "배터리", "count": 58 }
    ],
    "byExpiryQuarter": [
      { "quarter": "2026Q2", "count": 12 },
      { "quarter": "2026Q3", "count": 28 }
    ],
    "byFilingCountry": [
      { "country": "KR", "count": 142 },
      { "country": "US", "count": 58 }
    ],
    "byDepartment": [
      { "departmentId": 2, "departmentName": "반도체 사업부", "count": 98 },
      { "departmentId": 3, "departmentName": "배터리 사업부", "count": 72 }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401)

---

#### `GET /patents/{patentId}`

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
| manageNumber | string | 관리번호 |
| applicationDate | string | 출원일 (`yyyy-MM-dd`) |
| registrationDate | string | 등록일 (`yyyy-MM-dd`) |
| publicationDate | string | 공개일 (`yyyy-MM-dd`) |
| announcementDate | string | 공고일 (`yyyy-MM-dd`) |
| expiryDate | string | 예상 소멸일 (`yyyy-MM-dd`) |
| ipcCode | string | IPC 코드 |
| cpcCode | string | CPC 코드 |
| applicant | string | 출원인명 |
| inventor | string | 발명자명 |
| citationCount | integer | 피인용 수 |
| originalPdfKey | string | 특허 원문 S3 키 |
| businessField | string | 관련사업 분야 |
| techField | string | 관련기술 분야 |
| relatedProducts | array | 관련 제품 목록 |
| filingCountry | string | 출원국 코드 |
| isJointApplication | boolean | 공동출원 여부 |
| jointApplicant | string | 공동출원인명. 없으면 `null` |
| initialDepartment | string | 최초 담당 부서명 |
| currentDepartmentId | long | 현재 담당 부서 ID |
| currentDepartmentName | string | 현재 담당 부서명 |
| latestLegalStatus | string | 최신 권리 상태 |
| keywords | array | AI 추출 주요 키워드 목록 |
| overview | string | AI 생성 특허 개요 |
| coreContent | string | AI 생성 핵심 내용 |

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
    "manageNumber": "MNG-2023-001",
    "applicationDate": "2023-03-15",
    "registrationDate": "2024-01-10",
    "publicationDate": "2023-09-15",
    "announcementDate": null,
    "expiryDate": "2043-03-15",
    "ipcCode": "H01M 10/00",
    "cpcCode": null,
    "applicant": "SK이노베이션",
    "inventor": "홍길동",
    "citationCount": 14,
    "originalPdfKey": "patents/1/original.pdf",
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
    "keywords": ["음극재", "에너지밀도"],
    "overview": "고용량 배터리 셀의 음극재 구조를 개선하여 에너지 밀도를 향상시킨 발명",
    "coreContent": "음극재 두께를 최적화하고 결합재 비율을 조정하여 에너지 밀도 20% 향상"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /patents`

**헤더**: `Authorization: Bearer {accessToken}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| title | string | * | 발명의 명칭(최종) |
| applicationNumber | string | * | 출원번호. 중복 불가 |
| registrationNumber | string | N | 등록번호 |
| applicationDate | string | N | 출원일 (`yyyy-MM-dd`) |
| registrationDate | string | N | 등록일 (`yyyy-MM-dd`) |
| ipcCode | string | N | IPC 코드 |
| cpcCode | string | N | CPC 코드 |
| applicant | string | N | 출원인명 |
| inventor | string | N | 발명자명 |
| expiryDate | string | N | 예상 소멸일 (`yyyy-MM-dd`) |
| manageNumber | string | N | 관리번호 |
| businessField | string | N | 관련사업 분야 |
| techField | string | N | 관련기술 분야 |
| relatedProducts | array | N | 관련 제품 목록 |
| filingCountry | string | N | 출원국 코드 |
| isJointApplication | boolean | N | 공동출원 여부 |
| jointApplicant | string | N | 공동출원인명 |
| keywords | array | N | 주요 키워드 목록 |
| overview | string | N | 특허 개요 |
| coreContent | string | N | 핵심 내용 |
| originalPdfKey | string | N | 기존 원문 PDF object key. `extractJobId`가 없을 때 그대로 저장 |
| extractJobId | long | N | 완료된 특허 추출 작업 ID. 값이 있으면 임시 PDF를 `patents/{applicationNumber}/patent.pdf`로 복사하고 최종 key를 저장 |

**응답 예시**

```json
{
  "success": true,
  "data": { "patentId": 42 }
}
```

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404 — 추출 작업 없음), `CONFLICT`(409 — 동일 출원번호 중복 또는 추출 미완료), `EXTERNAL_SERVICE_ERROR`(502 — MinIO 복사 실패)

---

#### `PATCH /patents/{patentId}/department`

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
| PDF 업로드 URL 발급 | `POST` | `/patent-extract-jobs/upload-url` | 추출 작업 생성 후 MinIO PUT presigned URL 반환 | `ADMIN`, `LEGAL` |
| PDF 업로드 완료 | `POST` | `/patent-extract-jobs/{extractJobId}/upload-complete` | PDF 존재 확인 후 RabbitMQ 메시지 발행 | `ADMIN`, `LEGAL` |
| 추출 작업 상태 조회 | `GET` | `/patent-extract-jobs/{extractJobId}/status` | 프론트 polling용 상태 조회 | `ADMIN`, `LEGAL` |
| 추출 결과 조회 | `GET` | `/patent-extract-jobs/{extractJobId}/result` | 완료된 추출 결과 JSON 조회 | `ADMIN`, `LEGAL` |
| 추출 완료 콜백 | `PATCH` | `/internal/patent-extract-jobs/{extractJobId}/complete` | AI Worker가 추출 결과 전달 | Internal API Key |
| 추출 실패 콜백 | `PATCH` | `/internal/patent-extract-jobs/{extractJobId}/fail` | AI Worker가 추출 실패 전달 | Internal API Key |

---

#### `POST /patent-extract-jobs/upload-url`

특허 신규 등록 전 원문 PDF를 업로드하기 위한 presigned URL을 발급합니다. 호출 즉시 `UPLOAD_PENDING` 상태의 `patent_extract_jobs` 레코드가 생성됩니다.

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| extractJobId | long | 추출 작업 ID |
| objectKey | string | 임시 PDF object key. `patents/extract-jobs/{extractJobId}/patent.pdf` |
| uploadUrl | string | MinIO PUT presigned URL |
| expiresInSeconds | integer | URL 만료 시간(초) |
| status | string | `UPLOAD_PENDING` |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "patents/extract-jobs/9001/patent.pdf",
    "uploadUrl": "https://minio.skipa.internal/skipa/patents/extract-jobs/9001/patent.pdf?...",
    "expiresInSeconds": 600,
    "status": "UPLOAD_PENDING",
    "createdAt": "2026-06-08T01:00:00Z",
    "updatedAt": "2026-06-08T01:00:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `EXTERNAL_SERVICE_ERROR`(502 — presigned URL 발급 실패)

---

#### `POST /patent-extract-jobs/{extractJobId}/upload-complete`

프론트가 presigned URL로 PDF 업로드를 완료한 뒤 호출합니다. 백엔드는 MinIO object 존재 여부를 확인하고, 성공 시 작업 상태를 `ANALYZING`으로 변경한 뒤 RabbitMQ에 추출 요청 메시지를 발행합니다.

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음.

**RabbitMQ 메시지 예시**

```json
{
  "type": "PATENT_EXTRACT",
  "extractJobId": 9001,
  "objectKey": "patents/extract-jobs/9001/patent.pdf"
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "patents/extract-jobs/9001/patent.pdf",
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

#### `GET /patent-extract-jobs/{extractJobId}/status`

프론트 polling용 API입니다. `COMPLETED` 또는 `FAILED` 응답 시 polling을 종료합니다.

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| extractJobId | long | 추출 작업 ID |
| objectKey | string | 임시 PDF object key |
| status | string | `UPLOAD_PENDING` / `ANALYZING` / `COMPLETED` / `FAILED` |
| errorMessage | string | 실패 사유. 실패가 아니면 `null` |
| uploadedAt | datetime | 업로드 완료 처리 시각 |
| completedAt | datetime | 완료 또는 실패 처리 시각 |

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /patent-extract-jobs/{extractJobId}/result`

완료된 추출 작업의 결과 JSON을 조회합니다. 프론트는 `result`를 특허 등록 폼에 자동 입력하고, 사용자가 수정한 뒤 `POST /patents`로 최종 생성합니다.

**헤더**: `Authorization: Bearer {accessToken}`

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "objectKey": "patents/extract-jobs/9001/patent.pdf",
    "status": "COMPLETED",
    "result": {
      "title": "반도체 패키지 구조",
      "applicationNumber": "10-2026-0000000",
      "registrationNumber": "10-1234567",
      "applicationDate": "2020-05-26",
      "ipcCode": "H01L 21/00",
      "applicant": "SK하이닉스",
      "inventor": "홍길동",
      "keywords": ["패키지", "반도체"],
      "overview": "특허 개요",
      "coreContent": "특허 핵심 내용"
    },
    "completedAt": "2026-06-08T01:05:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 추출 미완료)

---

#### `PATCH /internal/patent-extract-jobs/{extractJobId}/complete`

AI Worker가 PDF 분석을 완료한 뒤 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| result | object | * | AI 추출 결과 JSON. `patent_extract_jobs.result_json`에 저장 |

**요청 예시**

```json
{
  "result": {
    "title": "반도체 패키지 구조",
    "applicationNumber": "10-2026-0000000",
    "registrationNumber": "10-1234567",
    "applicationDate": "2020-05-26",
    "ipcCode": "H01L 21/00",
    "applicant": "SK하이닉스",
    "inventor": "홍길동",
    "keywords": ["패키지", "반도체"],
    "overview": "특허 개요",
    "coreContent": "특허 핵심 내용"
  }
}
```

**응답 예시**

```json
{
  "success": true,
  "data": {
    "extractJobId": 9001,
    "status": "COMPLETED"
  }
}
```

**에러**: `INVALID_REQUEST`(400 — result 누락), `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 완료 가능한 상태가 아님)

---

#### `PATCH /internal/patent-extract-jobs/{extractJobId}/fail`

AI Worker가 PDF 분석 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

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
    "status": "FAILED"
  }
}
```

**에러**: `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

### 4-2. 특허 담당 부서

특허 담당 부서는 별도 매핑 엔티티를 두지 않고, `patents.current_department_id`가 `departments.id`를 외래키로 참조하는 방식으로 관리합니다.
담당 부서 조회는 `GET /patents/{patentId}` 응답 필드로 확인합니다.

---

### 5. 권리 상태 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 권리 상태 이력 조회 | `GET` | `/patents/{patentId}/legal-status` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 권리 상태 이력 추가 | `POST` | `/patents/{patentId}/legal-status` | 권리 상태 수동 추가 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다.

---

### 6. 연차료 납부 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 연차료 납부 이력 조회 | `GET` | `/patents/{patentId}/annuities` | 납부 완료(`PAID`) 이력 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 연차료 납부 이력 추가 | `POST` | `/patents/{patentId}/annuities` | 납부 이력 수동 추가 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다. 조회 결과에는 `PAID` 상태의 납부 완료 이력만 포함됩니다.

---

### 7. 검토 주기

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 주기 생성 | `POST` | `/review-cycles` | 검토 주기 등록 | `LEGAL` |
| 검토 주기 목록 조회 | `GET` | `/review-cycles` | 최근 시작일 순 목록 조회 | `ADMIN`, `LEGAL` |
| 검토 주기 단일 조회 | `GET` | `/review-cycles/{reviewCycleId}` | 검토 주기 상세 조회 | `ADMIN`, `LEGAL` |
| 검토 주기 수정 | `PUT` | `/review-cycles/{reviewCycleId}` | 검토 주기 정보 수정 | `LEGAL` |
| 검토 주기 삭제 | `DELETE` | `/review-cycles/{reviewCycleId}` | 미사용 검토 주기 삭제 | `LEGAL` |

검토 주기의 기간은 서로 겹칠 수 없습니다. 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.

---

### 8. 검토 요청 전송

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 요청 전송 | `POST` | `/patents/{patentId}/reviews` | 현재 담당 부서와 현재 날짜가 포함된 검토 주기로 요청 생성. 최신 평가 보고서가 있으면 `reportId`로 함께 연결 | `LEGAL` |
| 검토 일괄 요청 전송 | `POST` | `/reviews/bulk` | 여러 특허에 검토 요청 생성. 생성 불가 특허는 사유와 함께 건너뜀 | `LEGAL` |

검토 요청의 회신 기한은 현재 활성 검토 주기의 `deadline`으로 서버에서 자동 설정합니다.
동일한 검토 주기, 특허, 부서 조합은 중복 요청할 수 없습니다.
일괄 요청은 최대 100건까지 처리하며, 특허별 결과를 `CREATED` 또는 `SKIPPED`로 반환합니다.

---

#### `POST /reviews/bulk`

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
| items[].result | string | `CREATED` / `SKIPPED` |
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
      { "patentId": 1, "result": "CREATED", "reason": null },
      { "patentId": 2, "result": "SKIPPED", "reason": "DUPLICATE_REVIEW_REQUEST" },
      { "patentId": 3, "result": "SKIPPED", "reason": "PATENT_DEPARTMENT_NOT_ASSIGNED" }
    ]
  }
}
```

**에러**: `INVALID_REQUEST`(400 — patentIds 누락 또는 100건 초과), `UNAUTHORIZED`(401), `NOT_FOUND`(404 — 활성 QUARTERLY 주기 없음)

---

### 9. 사업부 검토 - Legal 모니터링

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 재평가 통계 조회 | `GET` | `/reviews/stats` | 현재 활성 QUARTERLY 주기 기준 KPI 집계 | `ADMIN`, `LEGAL` |
| 검토 목록 조회 | `GET` | `/reviews` | `status`, `confirmed`, `departmentId`, `patentId`, `page`, `size`로 조회 | `ADMIN`, `LEGAL` |
| 검토 단일 조회 | `GET` | `/reviews/{reviewId}` | 검토 요청과 의견 제출 정보 조회. 응답에 `reportId` 포함 | `ADMIN`, `LEGAL` |
| 회신 확인 처리 | `PATCH` | `/reviews/{reviewId}/confirm` | Legal 팀 회신 확인 처리. `confirmed_at` 기록 | `LEGAL` |

---

#### `GET /reviews/stats`

**헤더**: `Authorization: Bearer {accessToken}`

현재 날짜 기준 활성 `QUARTERLY` 주기를 자동으로 찾아 집계합니다.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| reviewCycleId | long | 현재 활성 검토 주기 ID |
| reviewCycleName | string | 검토 주기명 (예: `2026-2Q`) |
| total | long | 전체 특허 수 |
| unassigned | long | 담당 부서 미배정 특허 수 |
| requested | long | 검토 요청 완료, 기한 내 미제출 수 |
| overdue | long | 기한 초과 미제출 수 |
| done | long | 회신 완료 수 |
| unread | long | 미확인 회신 수 (`confirmed_at IS NULL`) |
| maintain | long | 유지 의견 건수 |
| abandon | long | 포기 의견 건수 |
| progressRate | double | `done / total * 100`, 소수점 1자리 (%) |
| byDepartment | array | 사업부별 유지/포기 건수 (`departmentId`, `departmentName`, `maintain`, `abandon`) |
| byTechField | array | 기술 분야별 유지/포기 건수 (`name`, `maintain`, `abandon`) |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "reviewCycleId": 1,
    "reviewCycleName": "2026-2Q",
    "total": 42,
    "unassigned": 3,
    "requested": 18,
    "overdue": 2,
    "done": 15,
    "unread": 5,
    "maintain": 10,
    "abandon": 5,
    "progressRate": 35.7,
    "byDepartment": [
      { "departmentId": 2, "departmentName": "반도체 사업부", "maintain": 52, "abandon": 18 }
    ],
    "byTechField": [
      { "name": "반도체", "maintain": 48, "abandon": 14 }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `NOT_FOUND`(404 — 활성 QUARTERLY 주기 없음)

---

#### `GET /reviews`

**헤더**: `Authorization: Bearer {accessToken}`

**쿼리 파라미터**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| status | string | N | `PENDING` / `SUBMITTED` |
| confirmed | boolean | N | `false` 지정 시 `confirmed_at IS NULL` 필터 |
| departmentId | long | N | 특정 사업부 필터 |
| patentId | long | N | 특정 특허의 검토 목록 조회 |
| page | integer | N | 페이지 번호 |
| size | integer | N | 페이지 크기 |

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 검토 ID |
| patentId | long | 특허 ID |
| title | string | 특허명 |
| applicationNumber | string | 출원번호 |
| departmentId | long | 담당 사업부 ID |
| departmentName | string | 담당 사업부명 |
| reviewCycleId | long | 검토 주기 ID |
| status | string | `PENDING` / `SUBMITTED` |
| opinion | string | `MAINTAIN` / `ABANDON` / `null` |
| comment | string | 상세 의견 |
| submittedAt | datetime | 제출 일시 |
| confirmedAt | datetime | Legal 확인 일시. 미확인이면 `null` |
| dueDate | string | 회신 기한 (`yyyy-MM-dd`) |
| reportId | long | 참고 보고서 ID. 없으면 `null` |

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403)

---

#### `PATCH /reviews/{reviewId}/confirm`

**헤더**: `Authorization: Bearer {accessToken}`

요청 Body 없음. `confirmed_at`에 현재 시각을 기록합니다.

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 12,
    "confirmedAt": "2026-06-07T14:30:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 확인 처리됨)

---

### 10. 사업부 검토 현황 - 사업부

각 특허와 부서의 가장 최근 검토 요청을 기준으로 반환합니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 현황 목록 조회 | `GET` | `/assigned-patents` | 본인 부서에 요청된 최신 검토 현황 목록 조회 | `BUSINESS` |
| 검토 현황 단일 조회 | `GET` | `/assigned-patents/{patentId}` | 특허 상세 정보와 최신 검토 현황 조회 | `BUSINESS` |
| 의견 제출 | `POST` | `/reviews/{reviewId}/opinions` | 최신 `PENDING` 요청에 `MAINTAIN` 또는 `ABANDON` 제출 | `BUSINESS` |

회신 기한이 지난 요청과 이미 제출한 요청에는 의견을 제출할 수 없습니다.

---

#### `POST /reviews/{reviewId}/opinions`

**헤더**: `Authorization: Bearer {accessToken}`

`reviewId`는 해당 특허에 대해 본인 부서로 발송된 검토 요청 ID입니다.

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
    "reviewId": 10,
    "opinion": "MAINTAIN",
    "submittedAt": "2026-06-07T10:30:00Z"
  }
}
```

**에러**: `INVALID_REQUEST`(400 — 이미 제출 또는 기한 초과), `UNAUTHORIZED`(401), `FORBIDDEN`(403 — 본인 부서 담당 특허 아님), `NOT_FOUND`(404)

---

### 11. 평가 보고서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 보고서 목록 조회 | `GET` | `/patents/{patentId}/reports` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 보고서 생성 요청 | `POST` | `/patents/{patentId}/reports` | `GENERATING` 상태의 보고서 생성 요청 등록 후 RabbitMQ 메시지 발행 | `LEGAL` |
| 보고서 단일 조회 | `GET` | `/patents/{patentId}/reports/{reportId}` | 완료된 보고서 상세 및 MinIO presigned URL 반환 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 평가 처리 상태 조회 | `GET` | `/patents/{patentId}/reports/{reportId}/status` | 보고서 생성 및 임베딩 상태 polling용 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 보고서 생성 완료 콜백 | `PATCH` | `/internal/reports/{reportId}/report-complete` | AI Worker가 생성 완료 및 `reportKey` 전달 | Internal API Key |
| 임베딩 완료 콜백 | `PATCH` | `/internal/reports/{reportId}/embedding-complete` | AI Worker가 임베딩 완료 전달 | Internal API Key |
| 보고서 생성 실패 콜백 | `PATCH` | `/internal/reports/{reportId}/fail` | AI Worker가 생성 실패 전달 | Internal API Key |

`BUSINESS` 사용자는 본인 부서 담당 특허의 보고서만 조회할 수 있습니다.
프론트는 MinIO object key를 직접 받지 않고, 백엔드가 생성한 presigned URL만 사용합니다.
AI Worker는 RabbitMQ 메시지를 소비해 보고서를 생성하고 MinIO에 저장한 뒤 내부 콜백 API를 호출합니다.

---

#### `GET /patents/{patentId}/reports`

**헤더**: `Authorization: Bearer {accessToken}`

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| evaluatedAt | datetime | 평가 기준 일시. `REPORT_COMPLETED` 이후 값 있음 |
| createdAt | datetime | 생성 요청 일시 |

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
        "evaluatedAt": "2026-05-01T09:00:00Z",
        "createdAt": "2026-05-01T08:55:00Z"
      }
    ]
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /patents/{patentId}/reports`

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

#### `GET /patents/{patentId}/reports/{reportId}`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| url | string | MinIO presigned URL. `REPORT_COMPLETED` 또는 `EMBEDDING_COMPLETED` 상태에서만 포함 |
| evaluatedAt | datetime | 평가 기준 일시 |
| createdAt | datetime | 생성 요청 일시 |

**응답 예시**

```json
{
  "success": true,
  "data": {
    "id": 7,
    "patentId": 1,
    "status": "REPORT_COMPLETED",
    "url": "https://minio.skipa.internal/skipa/reports/7/report.html?...",
    "evaluatedAt": "2026-05-01T09:00:00Z",
    "createdAt": "2026-05-01T08:55:00Z"
  }
}
```

`GENERATING` 또는 `FAILED` 상태의 보고서는 URL을 반환하지 않으며 `CONFLICT`를 반환합니다.

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `CONFLICT`(409 — 보고서 생성 미완료)

---

#### `GET /patents/{patentId}/reports/{reportId}/status`

**헤더**: `Authorization: Bearer {accessToken}`

`GENERATING` 상태일 때 polling으로 호출합니다. `REPORT_COMPLETED` 응답 시 보고서 조회가 가능하며, `EMBEDDING_COMPLETED` 또는 `FAILED` 응답 시 전체 처리가 종료됩니다.

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 보고서 ID |
| patentId | long | 특허 ID |
| status | string | `GENERATING` (polling 계속) / `REPORT_COMPLETED` (보고서 조회 가능) / `EMBEDDING_COMPLETED` (임베딩 완료) / `FAILED` (재시도) |
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
    "evaluatedAt": null,
    "updatedAt": "2026-06-07T08:55:00Z"
  }
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `PATCH /internal/reports/{reportId}/report-complete`

AI Worker가 보고서 생성 완료 후 호출합니다.

기존 호환을 위해 `/internal/reports/{reportId}/complete`도 동일하게 동작합니다.

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

#### `PATCH /internal/reports/{reportId}/embedding-complete`

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

#### `PATCH /internal/reports/{reportId}/fail`

AI Worker가 보고서 생성 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

**요청**

| Name | Type | Required | Description |
| --- | --- | --- | --- |
| errorMessage | string | N | 실패 사유. 저장 또는 로그 용도 |

**요청 예시**

```json
{
  "errorMessage": "AI report generation failed"
}
```

**처리**

- 보고서 상태를 `FAILED`로 변경
- 실패 사유는 서버 로그 또는 별도 저장 필드가 있는 경우 저장

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
| 사전 평가 시작 | `POST` | `/pre-evaluations` | 임시 특허 정보를 저장하고 AI 서버에 사전 평가 보고서 생성을 요청 | `BUSINESS` |
| 사전 평가 목록 조회 | `GET` | `/pre-evaluations` | 현재 사용자의 사전 평가 이력 목록 조회 | `BUSINESS` |
| 사전 평가 상세 조회 | `GET` | `/pre-evaluations/{preEvaluationId}` | 사전 평가 입력 정보, 상태, 보고서 URL 조회 | `BUSINESS` |
| 사전 평가 처리 상태 조회 | `GET` | `/pre-evaluations/{preEvaluationId}/status` | 보고서 생성 및 임베딩 상태 polling | `BUSINESS` |
| 사전 평가 이력 삭제 | `DELETE` | `/pre-evaluations/{preEvaluationId}` | 사전 평가와 관련 채팅 메시지 삭제 | `BUSINESS` |
| 채팅 이력 조회 | `GET` | `/pre-evaluations/{preEvaluationId}/chat/messages` | 사전 평가별 채팅 메시지 목록 조회 | `BUSINESS` |
| 채팅 메시지 전송 | `POST` | `/pre-evaluations/{preEvaluationId}/chat/messages` | 사용자 메시지 저장, AI 서버 채팅 API 호출, 응답 저장 | `BUSINESS` |
| 채팅 초기화 | `DELETE` | `/pre-evaluations/{preEvaluationId}/chat/messages` | 해당 사전 평가의 채팅 메시지 전체 삭제 | `BUSINESS` |
| 사전 평가 보고서 생성 완료 callback | `PATCH` | `/internal/pre-evaluations/{preEvaluationId}/report-complete` | AI 서버가 보고서 생성 완료 후 호출 | Internal API Key |
| 사전 평가 임베딩 완료 callback | `PATCH` | `/internal/pre-evaluations/{preEvaluationId}/embedding-complete` | AI 서버가 임베딩 완료 후 호출 | Internal API Key |
| 사전 평가 실패 callback | `PATCH` | `/internal/pre-evaluations/{preEvaluationId}/fail` | AI 서버가 보고서 생성 실패 후 호출 | Internal API Key |

사전 평가 상태값은 다음과 같습니다.

| status | 설명 |
| --- | --- |
| `PROCESSING` | 사전 평가 보고서 생성 중 |
| `REPORT_COMPLETED` | 보고서 생성 완료 |
| `EMBEDDING_COMPLETED` | 임베딩 완료 |
| `FAILED` | 보고서 생성 실패 |

---

#### `POST /pre-evaluations`

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
| createdAt | datetime | 생성 시각 |
| updatedAt | datetime | 수정 시각 |

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

#### `GET /pre-evaluations`

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
| title | string | 특허명 |
| status | string | `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| reportUrl | string | 보고서 URL. 완료 전에는 `null` |
| completedAt | datetime | 완료 또는 실패 시각. 처리 중에는 `null` |
| createdAt | datetime | 생성 시각 |
| updatedAt | datetime | 수정 시각 |

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

#### `GET /pre-evaluations/{preEvaluationId}`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| userId | long | 생성 사용자 ID |
| title | string | 특허명 |
| technicalDescription | string | 기술 설명 |
| claims | array[string] | 청구항 목록 |
| relatedBusiness | string | 관련 사업 |
| targetCountries | string | 출원 예정 국가 |
| status | string | `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| reportUrl | string | 보고서 URL. 완료 전에는 `null` |
| completedAt | datetime | 완료 또는 실패 시각 |
| createdAt | datetime | 생성 시각 |
| updatedAt | datetime | 수정 시각 |

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

#### `GET /pre-evaluations/{preEvaluationId}/status`

**헤더**: `Authorization: Bearer {accessToken}`

**응답**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 사전 평가 ID |
| status | string | `PROCESSING` / `REPORT_COMPLETED` / `EMBEDDING_COMPLETED` / `FAILED` |
| completedAt | datetime | 완료 또는 실패 시각 |
| updatedAt | datetime | 수정 시각 |

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

#### `DELETE /pre-evaluations/{preEvaluationId}`

**헤더**: `Authorization: Bearer {accessToken}`

사전 평가 이력과 해당 사전 평가의 채팅 메시지를 삭제합니다.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `GET /pre-evaluations/{preEvaluationId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

**응답 items[] 필드**

| Name | Type | Description |
| --- | --- | --- |
| id | long | 메시지 ID |
| preEvaluationId | long | 사전 평가 ID |
| role | string | `USER` / `ASSISTANT` |
| content | string | 메시지 내용 |
| createdAt | datetime | 생성 시각 |

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
      "createdAt": "2026-06-10T09:10:00Z"
    },
    {
      "id": 101,
      "preEvaluationId": 1,
      "role": "ASSISTANT",
      "content": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다.",
      "createdAt": "2026-06-10T09:10:02Z"
    }
  ]
}
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `POST /pre-evaluations/{preEvaluationId}/chat/messages`

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
- 이전 대화 이력과 현재 메시지를 포함해 AI 서버 채팅 API를 직접 호출
- AI 응답을 `ASSISTANT` 역할로 저장

**AI 서버 채팅 요청 예시**

```json
{
  "preEvaluationId": 1,
  "userId": 10,
  "title": "배터리 열폭주 감지 시스템",
  "technicalDescription": "센서 데이터를 기반으로 배터리 열폭주 가능성을 조기에 감지하는 기술",
  "claims": [
    "센서부를 포함하는 배터리 열폭주 감지 시스템"
  ],
  "relatedBusiness": "전기차 배터리 안전 관리",
  "targetCountries": "한국, 미국",
  "message": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?",
  "history": [
    {
      "role": "USER",
      "content": "등록 가능성을 높이려면 어떤 부분을 보완해야 하나요?"
    }
  ]
}
```

**AI 서버 채팅 응답 형식**

```json
{
  "message": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다."
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
      "createdAt": "2026-06-10T09:10:00Z"
    },
    "assistantMessage": {
      "id": 101,
      "preEvaluationId": 1,
      "role": "ASSISTANT",
      "content": "청구항에서 센서 데이터 처리 알고리즘의 차별성을 더 구체화하는 것이 좋습니다.",
      "createdAt": "2026-06-10T09:10:02Z"
    }
  }
}
```

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404), `AI_SERVER_ERROR`(500)

---

#### `DELETE /pre-evaluations/{preEvaluationId}/chat/messages`

**헤더**: `Authorization: Bearer {accessToken}`

해당 사전 평가의 채팅 메시지를 모두 삭제합니다.

**응답 예시**

```json
{ "success": true, "data": null }
```

**에러**: `UNAUTHORIZED`(401), `FORBIDDEN`(403), `NOT_FOUND`(404)

---

#### `PATCH /internal/pre-evaluations/{preEvaluationId}/report-complete`

AI 서버가 사전 평가 보고서 생성 완료 후 호출합니다.

기존 호환을 위해 `/internal/pre-evaluations/{preEvaluationId}/complete`도 동일하게 동작합니다.

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

#### `PATCH /internal/pre-evaluations/{preEvaluationId}/embedding-complete`

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

**에러**: `INVALID_REQUEST`(400), `UNAUTHORIZED`(401 — 내부 API Key 불일치), `NOT_FOUND`(404), `CONFLICT`(409 — 이미 완료 또는 실패 처리됨)

---

#### `PATCH /internal/pre-evaluations/{preEvaluationId}/fail`

AI 서버가 사전 평가 보고서 생성 실패 후 호출합니다.

**헤더**: `X-Internal-Api-Key: {secret}`

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
| 재평가 진행 현황 | `GET` | `/dashboard/summary` | 진행률, 검토 대상, 평가, 의견 제출 건수 요약 | `ADMIN`, `LEGAL` |
| 담당 부서 배정 현황 | `GET` | `/dashboard/assignment` | 미배정, 배정 요청, 배정 완료 건수 | `ADMIN`, `LEGAL` |
| 특허 유형 분포 / 만료 현황 | `GET` | `/dashboard/distribution` | 특허 유형 분포 및 분기별 만료 현황 | `ADMIN`, `LEGAL` |
| 사업부별 검토 현황 | `GET` | `/dashboard/departments` | 부서별 검토 대상, 제출 완료, 미제출 건수 | `ADMIN`, `LEGAL` |
