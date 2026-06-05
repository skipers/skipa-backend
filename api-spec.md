# API 명세서

변경 날짜: 2026-06-05

## 기본 정보

| 항목 | 내용 |
| --- | --- |
| Base URL | `https://api.skipa.internal` |
| URL 버전 prefix | 없음 |
| 인증 방식 | JWT Bearer Token (`Authorization: Bearer <token>`) |
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
| 보고서 생성 상태 | `GENERATING`, `COMPLETED`, `FAILED` |
| 검토 주기 유형 | `QUARTERLY`, `AD_HOC` |
| 검토 제출 상태 | `PENDING`, `SUBMITTED` |
| 사업부 의견 | `MAINTAIN`, `ABANDON` |

## API 목록

### 1. 인증

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/auth/login` | ID/PW 검증 후 access token, refresh token, 사용자 정보 반환 | 없음 |
| 회원가입 | `POST` | `/auth/register` | `PENDING` 상태의 사용자 계정 생성 | 없음 |
| 로그아웃 | `POST` | `/auth/logout` | 토큰 무효화 | 인증 사용자 |
| 내 정보 조회 | `GET` | `/auth/me` | 현재 로그인 사용자 정보 반환 | 인증 사용자 |
| 토큰 갱신 | `POST` | `/auth/refresh` | refresh token으로 access token 재발급 | 없음 |

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

### 3. 부서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 부서 목록 조회 | `GET` | `/departments` | 활성 부서 목록 조회. `keyword`, `page`, `size` 사용 가능 | `ADMIN`, `LEGAL` |
| 부서 단일 조회 | `GET` | `/departments/{departmentId}` | 부서 정보 조회. 비활성 부서도 조회 가능 | `ADMIN`, `LEGAL` |
| 부서 생성 | `POST` | `/departments` | 활성 부서 생성 | `ADMIN` |
| 부서 수정 | `PUT` | `/departments/{departmentId}` | 부서명 수정 | `ADMIN` |
| 부서 비활성화 | `DELETE` | `/departments/{departmentId}` | 삭제 대신 상태를 `INACTIVE`로 변경 | `ADMIN` |

비활성 부서는 기존 사용자, 특허, 검토 이력의 참조를 유지합니다.
비활성 부서는 신규 사용자 승인, 특허 담당 부서 변경, 신규 검토 요청에 사용할 수 없습니다.

### 4. 특허

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 특허 목록 조회 | `GET` | `/patents` | 특허명 `keyword`, `page`, `size` 사용 가능 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 단일 조회 | `GET` | `/patents/{patentId}` | 특허 상세 정보 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 특허 등록 | `POST` | `/patents` | 특허 정보 수동 등록 | `ADMIN`, `LEGAL` |
| 특허 수정 | `PUT` | `/patents/{patentId}` | 특허 정보 수정 | `ADMIN`, `LEGAL` |
| 담당 부서 변경 | `PATCH` | `/patents/{patentId}/department` | 현재 담당 부서를 활성 부서로 변경 | `ADMIN`, `LEGAL` |
| 특허 삭제 | `DELETE` | `/patents/{patentId}` | 특허와 권리 상태, 연차료, 검토, 보고서 삭제 | `ADMIN`, `LEGAL` |

`BUSINESS` 사용자는 현재 담당 부서가 본인 소속 부서와 같은 특허만 조회할 수 있습니다.

### 4-1. 특허 문서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| PDF 업로드 | `POST` | `/patents/{patentId}/documents` | 특허 원문 PDF 업로드 | `LEGAL` |
| 메타데이터 추출 | `POST` | `/patents/{patentId}/documents/extract` | PDF에서 특허 정보 자동 추출 | `LEGAL` |
| 문서 삭제 | `DELETE` | `/patents/{patentId}/documents` | 원문 PDF 삭제 | `LEGAL` |

### 4-2. 특허 담당 부서

특허 담당 부서는 별도 매핑 엔티티를 두지 않고, `patents.current_department_id`가 `departments.id`를 외래키로 참조하는 방식으로 관리합니다.
담당 부서 조회는 `GET /patents/{patentId}` 응답 필드로 확인합니다.

### 5. 권리 상태 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 권리 상태 이력 조회 | `GET` | `/patents/{patentId}/legal-status` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 권리 상태 이력 추가 | `POST` | `/patents/{patentId}/legal-status` | 권리 상태 수동 추가 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다.

### 6. 연차료 납부 이력

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 연차료 납부 이력 조회 | `GET` | `/patents/{patentId}/annuities` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 연차료 납부 이력 추가 | `POST` | `/patents/{patentId}/annuities` | 납부 이력 수동 추가 | `LEGAL` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 이력만 조회할 수 있습니다.

### 7. 검토 주기

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 주기 생성 | `POST` | `/review-cycles` | 검토 주기 등록 | `LEGAL` |
| 검토 주기 목록 조회 | `GET` | `/review-cycles` | 최근 시작일 순 목록 조회 | `ADMIN`, `LEGAL` |
| 검토 주기 단일 조회 | `GET` | `/review-cycles/{reviewCycleId}` | 검토 주기 상세 조회 | `ADMIN`, `LEGAL` |
| 검토 주기 수정 | `PUT` | `/review-cycles/{reviewCycleId}` | 검토 주기 정보 수정 | `LEGAL` |
| 검토 주기 삭제 | `DELETE` | `/review-cycles/{reviewCycleId}` | 미사용 검토 주기 삭제 | `LEGAL` |

검토 주기의 기간은 서로 겹칠 수 없습니다. 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.

### 8. 검토 요청 전송

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 요청 전송 | `POST` | `/patents/{patentId}/reviews` | 현재 담당 부서와 현재 날짜가 포함된 검토 주기로 요청 생성. 최신 평가 보고서가 있으면 `reportId`로 함께 연결 | `LEGAL` |
| 검토 일괄 요청 전송 | `POST` | `/reviews/bulk` | 여러 특허에 검토 요청 생성. 생성 불가 특허는 사유와 함께 건너뜀 | `LEGAL` |

검토 요청의 회신 기한은 검토 주기 종료일입니다.
동일한 검토 주기, 특허, 부서 조합은 중복 요청할 수 없습니다.
일괄 요청은 최대 100건까지 처리하며, 특허별 결과를 `CREATED` 또는 `SKIPPED`로 반환합니다.
검토 응답에는 참고한 평가 보고서 ID인 `reportId`가 포함됩니다. 연결할 평가 보고서가 없으면 `null`입니다.

### 9. 사업부 검토 - Legal 모니터링

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 목록 조회 | `GET` | `/reviews` | `status`, `departmentId`, `patentId`, `page`, `size`로 조회. 각 항목에 `reportId` 포함 | `ADMIN`, `LEGAL` |
| 검토 단일 조회 | `GET` | `/reviews/{reviewId}` | 검토 요청과 의견 제출 정보 조회. 응답에 `reportId` 포함 | `ADMIN`, `LEGAL` |

### 10. 사업부 검토 현황 - 사업부

프론트엔드 호환을 위해 `/assigned-patents` 경로를 유지합니다.
각 특허와 부서의 가장 최근 검토 요청을 기준으로 반환합니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 현황 목록 조회 | `GET` | `/assigned-patents` | 본인 부서에 요청된 최신 검토 현황 목록 조회 | `BUSINESS` |
| 검토 현황 단일 조회 | `GET` | `/assigned-patents/{patentId}` | 특허 상세 정보와 최신 검토 현황 조회 | `BUSINESS` |
| 의견 제출 | `POST` | `/assigned-patents/{patentId}/opinions` | 최신 `PENDING` 요청에 `MAINTAIN` 또는 `ABANDON` 제출 | `BUSINESS` |

회신 기한이 지난 요청과 이미 제출한 요청에는 의견을 제출할 수 없습니다.

### 11. 평가 보고서

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 보고서 목록 조회 | `GET` | `/patents/{patentId}/reports` | 최신 등록순 목록 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 보고서 생성 요청 | `POST` | `/patents/{patentId}/reports` | `GENERATING` 상태의 보고서 생성 요청 등록 | `LEGAL` |
| 보고서 단일 조회 | `GET` | `/patents/{patentId}/reports/{reportId}` | 보고서 상세 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |
| 보고서 생성 상태 조회 | `GET` | `/patents/{patentId}/reports/{reportId}/status` | `GENERATING`, `COMPLETED`, `FAILED` 상태 조회 | `ADMIN`, `LEGAL`, `BUSINESS` |

`BUSINESS` 사용자는 본인 부서 담당 특허의 보고서만 조회할 수 있습니다.

### 12. 대시보드

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 재평가 진행 현황 | `GET` | `/dashboard/summary` | 진행률, 검토 대상, 평가, 의견 제출 건수 요약 | `ADMIN`, `LEGAL` |
| 담당 부서 배정 현황 | `GET` | `/dashboard/assignment` | 미배정, 배정 요청, 배정 완료 건수 | `ADMIN`, `LEGAL` |
| 특허 유형 분포 / 만료 현황 | `GET` | `/dashboard/distribution` | 특허 유형 분포 및 분기별 만료 현황 | `ADMIN`, `LEGAL` |
| 사업부별 검토 현황 | `GET` | `/dashboard/departments` | 부서별 검토 대상, 제출 완료, 미제출 건수 | `ADMIN`, `LEGAL` |

대시보드 API는 모두 조회 전용이며 요청 Body는 없습니다.
활성 검토 주기가 없으면 진행 현황과 사업부별 검토 현황은 `0` 또는 빈 배열로 반환합니다.

#### 12-1. `GET /dashboard/summary`

재평가 대시보드 KPI 카드 요약입니다.

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| `progressRate` | `number` | 전체 검토 요청 대비 제출 완료 비율(%) |
| `delayed` | `number` | 회신 기한이 지났지만 아직 `PENDING` 상태인 건수 |
| `kpi.requested` | `number` | 현재 활성 검토 주기의 전체 검토 요청 건수 |
| `kpi.reviewing` | `number` | 현재 활성 검토 주기의 `PENDING` 건수 |
| `kpi.decided` | `number` | 현재 활성 검토 주기의 `SUBMITTED` 건수 |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "progressRate": 60,
    "delayed": 2,
    "kpi": {
      "requested": 10,
      "reviewing": 4,
      "decided": 6
    }
  }
}
```

#### 12-2. `GET /dashboard/assignment`

특허 담당 부서 배정 현황 요약입니다.

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| `unassigned` | `number` | 현재 담당 부서가 없는 특허 건수 |
| `assigned` | `number` | 현재 담당 부서가 지정된 특허 건수 |
| `completed` | `number` | 현재 활성 검토 주기 기준 `SUBMITTED` 건수 |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "unassigned": 3,
    "assigned": 7,
    "completed": 5
  }
}
```

#### 12-3. `GET /dashboard/distribution`

기술 분야 분포와 분기별 만료 예정 현황입니다.

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| `byTechField` | `array` | `techField` 기준 분포 목록 |
| `byTechField[].name` | `string` | 기술 분야명 |
| `byTechField[].count` | `number` | 해당 기술 분야 특허 건수 |
| `byExpiryQuarter` | `array` | 만료 분기 기준 분포 목록 |
| `byExpiryQuarter[].quarter` | `string` | 분기 라벨 (`YYYYQn`) |
| `byExpiryQuarter[].count` | `number` | 해당 분기의 만료 예정 특허 건수 |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "byTechField": [
      {
        "name": "반도체",
        "count": 12
      },
      {
        "name": "배터리",
        "count": 8
      }
    ],
    "byExpiryQuarter": [
      {
        "quarter": "2026Q3",
        "count": 5
      },
      {
        "quarter": "2026Q4",
        "count": 7
      }
    ]
  }
}
```

#### 12-4. `GET /dashboard/departments`

현재 활성 검토 주기 기준 사업부별 검토 진행 현황입니다.

| 응답 필드 | 타입 | 설명 |
| --- | --- | --- |
| `items` | `array` | 부서별 현황 목록 |
| `items[].departmentId` | `number` | 부서 ID |
| `items[].departmentName` | `string` | 부서명 |
| `items[].assigned` | `number` | 해당 부서에 배정된 검토 요청 건수 |
| `items[].reviewing` | `number` | 해당 부서의 `PENDING` 건수 |
| `items[].decided` | `number` | 해당 부서의 `SUBMITTED` 건수 |
| `items[].progressRate` | `number` | 해당 부서의 제출 완료 비율(%) |

**Response 예시**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "departmentId": 1,
        "departmentName": "반도체 사업부",
        "assigned": 12,
        "reviewing": 5,
        "decided": 7,
        "progressRate": 58
      }
    ]
  }
}
```
