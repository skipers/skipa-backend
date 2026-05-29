# API 명세서

## API 명세

[api-spec_v1.md](API%20%EB%AA%85%EC%84%B8%EC%84%9C/api-spec_v1.md)

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API 버전 | v1 |
| Base URL | `https://api.skipa.internal/v1` |
| 인증 방식 | JWT Bearer Token (`Authorization: Bearer <token>`) |
| 토큰 발급 | `POST /auth/login` 응답으로 access token 반환 |
| 토큰 만료 | access token 1시간 / refresh token 7일 |

---

### 응답 형식

모든 응답은 `application/json`이며 아래 공통 구조를 따릅니다.

**성공**

json

```
{
  "success": true,
  "data": { }
}
```

**실패**

json

```
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  }
}
```

---

### 공통 에러 코드

| HTTP Status | code | 설명 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 요청 파라미터 오류 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 또는 상태 충돌 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

### 권한 역할 정의

| Role | 설명 |
| --- | --- |
| `admin` | 전체 관리 권한 |
| `legal` | Legal AI팀 — 운영/요청/현황/특허 관리 |
| `business` | 사업부 — 받은 요청함/결정 제출 |

---

### API 목록

### 1. 인증 (Auth)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/auth/login` | ID/PW로 JWT 발급 | 없음 |
| 로그아웃 | `POST` | `/auth/logout` | 토큰 무효화 | 전체 |
| 내 정보 조회 | `GET` | `/auth/me` | 현재 로그인 사용자 정보 반환 | 전체 |
| 토큰 갱신 | `POST` | `/auth/refresh` | refresh token으로 access token 재발급 | 전체 |

---

### 2. 사용자 (Users)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사용자 목록 조회 | `GET` | `/users` | 전체 사용자 목록 (검색/필터 가능) | `admin` |
| 사용자 생성 | `POST` | `/users` | 신규 사용자 등록 | `admin` |
| 사용자 단건 조회 | `GET` | `/users/{userId}` | 특정 사용자 정보 조회 | `admin` |
| 사용자 수정 | `PUT` | `/users/{userId}` | 이름/이메일/역할/부서 수정 | `admin` |
| 사용자 삭제 | `DELETE` | `/users/{userId}` | 사용자 삭제 | `admin` |

---

### 3. 부서 (Departments)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 부서 목록 조회 | `GET` | `/departments` | 전체 부서 목록 조회 | `admin`, `legal` |
| 부서 생성 | `POST` | `/departments` | 신규 부서 등록 | `admin` |
| 부서 수정 | `PUT` | `/departments/{deptId}` | 부서명 수정 | `admin` |
| 부서 삭제 | `DELETE` | `/departments/{deptId}` | 부서 삭제 | `admin` |

---

### 4. 특허 (Patents)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 특허 목록 조회 | `GET` | `/patents` | 특허 목록 조회. legal: 전체 / business: 배정된 것만 | `legal`, `business` |
| 특허 단건 조회 | `GET` | `/patents/{patentId}` | 특허 상세 정보 조회 | `legal`, `business` |
| 특허 등록 | `POST` | `/patents` | 특허 단건 수동 등록 | `legal` |
| 특허 수정 | `PUT` | `/patents/{patentId}` | 특허 정보 수정 | `legal` |
| 특허 삭제 | `DELETE` | `/patents/{patentId}` | 특허 삭제 | `legal`, `admin` |

### 4-1. 특허 문서 (Patent Documents)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| PDF 업로드 | `POST` | `/patents/{patentId}/documents` | 특허 원문 PDF 업로드 | `legal` |
| 메타데이터 추출 | `POST` | `/patents/{patentId}/documents/extract` | PDF에서 특허 정보 자동 추출 | `legal` |
| 문서 삭제 | `DELETE` | `/patents/{patentId}/documents` | 원문 PDF 삭제 | `legal` |

### 4-2. 특허 담당 부서 (Patent Departments)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 담당 부서 조회 | `GET` | `/patents/{patentId}/departments` | 해당 특허의 담당 부서 조회 | `legal` |
| 담당 부서 배정 | `POST` | `/patents/{patentId}/departments` | 담당 부서 신규 배정 | `legal` |
| 담당 부서 변경 | `PUT` | `/patents/{patentId}/departments/{deptId}` | 배정된 부서 변경 | `legal` |
| 담당 부서 해제 | `DELETE` | `/patents/{patentId}/departments/{deptId}` | 담당 부서 해제 | `legal` |

---

### 5. 권리 상태 이력 (Patent Legal Status)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 권리 상태 이력 조회 | `GET` | `/patents/{patentId}/legal-status` | 권리 상태 변경 이력 조회 | `legal`, `business` |
| 권리 상태 이력 추가 | `POST` | `/patents/{patentId}/legal-status` | 권리 상태 수동 추가 | `legal` |

---

### 6. 연차료 납부 이력 (Annuity History)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 납부 이력 조회 | `GET` | `/patents/{patentId}/annuities` | 연차료 납부 이력 전체 조회 | `legal`, `business` |
| 납부 이력 등록 | `POST` | `/patents/{patentId}/annuities` | 납부 이력 수동 등록 | `legal` |
| 납부 이력 수정 | `PUT` | `/patents/{patentId}/annuities/{annuityId}` | 납부 이력 수정 | `legal` |

---

### 7. 결정 요청 전송 (Decisions)

검토 요청 전송 시 `decisions` 행이 생성됩니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사업부 전송 | `POST` | `/patents/{patentId}/decisions` | 사업부로 검토 요청 전송. decisions 행 생성 | `legal` |

---

### 8. 결정 (Decisions) — Legal 모니터링

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 결정 목록 조회 | `GET` | `/decisions` | 전체 결정 현황 조회 (상태/부서/특허 필터 가능) | `legal` |
| 결정 단건 조회 | `GET` | `/decisions/{decisionId}` | 결정 상세 조회 | `legal` |

---

### 9. 받은 요청함 (Inbox) — 사업부

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 요청 목록 조회 | `GET` | `/inbox` | 내 부서에 배정된 검토 요청 목록 조회 | `business` |
| 요청 단건 조회 | `GET` | `/inbox/{decisionId}` | 요청 상세 및 특허 정보 조회 | `business` |
| 결정 제출 | `POST` | `/inbox/{decisionId}/decide` | 유지/포기 결정 제출. decision 및 decided_at 업데이트 | `business` |

---

### 10. 평가 보고서 (Reports)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 보고서 목록 조회 | `GET` | `/patents/{patentId}/reports` | 해당 특허의 보고서 목록 조회 | `legal`, `business` |
| 보고서 생성 요청 | `POST` | `/patents/{patentId}/reports` | 평가 보고서 생성 요청 (비동기) | `legal` |
| 보고서 조회 | `GET` | `/patents/{patentId}/reports/{reportId}` | 보고서 presigned URL 반환 | `legal`, `business` |
| 보고서 생성 상태 조회 | `GET` | `/patents/{patentId}/reports/{reportId}/status` | 생성 진행 상태 폴링 (생성중/완료/실패) | `legal`, `business` |

---

### 11. 대시보드 (Dashboard) — Legal

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 분기 진행 현황 | `GET` | `/dashboard/summary` | 진행률, 대상/평가/결정 건수 요약 | `legal` |
| 담당 부서 배정 현황 | `GET` | `/dashboard/assignment` | 미배정/배정 요청/배정 완료 건수 | `legal` |
| 특허 유형 분포 / 만기 현황 | `GET` | `/dashboard/distribution` | 특허 유형 분포 및 분기별 만기 현황 | `legal` |
| 사업부별 처리 현황 | `GET` | `/dashboard/departments` | 부서별 담당/결정 완료/미결정 건수 | `legal` |

.
