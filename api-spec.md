# API 명세서
# 변경 날짜: 2026-06-01 (월)
## API 명세

[api-spec_v1.md](API%20%EB%AA%85%EC%84%B8%EC%84%9C/api-spec_v1.md)

### 기본 정보

| 항목 | 내용 |
| --- | --- |
| API 버전 | v3 |
| Base URL | `https://api.skipa.internal/v1` |
| 인증 방식 | JWT Bearer Token (`Authorization: Bearer <token>`) |
| 토큰 발급 | `POST /auth/login` 응답으로 access token 반환 |
| 토큰 만료 | 미확정(구현값 우선): access token 10분 / refresh token 7일 |

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
| `ADMIN` | 전체 관리 권한 |
| `LEGAL` | Legal AI팀 — 운영/요청/현황/특허 관리 |
| `BUSINESS` | 사업부 — 담당 특허 조회/의견 제출 |

---

### API 목록

### 1. 인증 (Auth)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/auth/login` | ID/PW로 JWT 발급 | 없음 |
| 로그아웃 | `POST` | `/auth/logout` | 토큰 무효화 | 미확정(미구현 가능) |
| 내 정보 조회 | `GET` | `/auth/me` | 현재 로그인 사용자 정보 반환 | 미확정(미구현 가능) |
| 토큰 갱신 | `POST` | `/auth/refresh` | refresh token으로 access token 재발급 | 미확정(미구현 가능) |

---

### 2. 사용자 (Users)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사용자 목록 조회 | `GET` | `/users` | 전체 사용자 목록 (검색/필터 가능) | `ADMIN` |
| 사용자 생성 | `POST` | `/users` | 신규 사용자 등록 | `ADMIN` |
| 사용자 단일 조회 | `GET` | `/users/{userId}` | 특정 사용자 정보 조회 | `ADMIN` |
| 사용자 수정 | `PUT` | `/users/{userId}` | 이름/이메일/역할/부서 수정 | `ADMIN` |
| 사용자 삭제 | `DELETE` | `/users/{userId}` | 사용자 삭제 | `ADMIN` |

---

### 3. 부서 (Departments)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 부서 목록 조회 | `GET` | `/departments` | 전체 부서 목록 조회 | `ADMIN`, `LEGAL` |
| 부서 생성 | `POST` | `/departments` | 신규 부서 등록 | `ADMIN` |
| 부서 수정 | `PUT` | `/departments/{deptId}` | 부서명 수정 | `ADMIN` |
| 부서 삭제 | `DELETE` | `/departments/{deptId}` | 부서 삭제 | `ADMIN` |

---

### 4. 특허 (Patents)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 특허 목록 조회 | `GET` | `/patents` | 특허 목록 조회. legal: 전체 / business: 배정된 것만 | `LEGAL`, `BUSINESS` |
| 특허 단일 조회 | `GET` | `/patents/{patentId}` | 특허 상세 정보 조회 | `LEGAL`, `BUSINESS` |
| 특허 등록 | `POST` | `/patents` | 특허 수동 등록 | 미확정(구현값 우선): `ADMIN`, `LEGAL` |
| 특허 수정 | `PUT` | `/patents/{patentId}` | 특허 정보 수정 | 미확정(구현값 우선): `ADMIN`, `LEGAL` |
| 특허 삭제 | `DELETE` | `/patents/{patentId}` | 특허 삭제 | 미확정(구현값 우선): `ADMIN`, `LEGAL` |

### 4-1. 특허 문서 (Patent Documents)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| PDF 업로드 | `POST` | `/patents/{patentId}/documents` | 특허 원문 PDF 업로드 | `LEGAL` |
| 메타데이터 추출 | `POST` | `/patents/{patentId}/documents/extract` | PDF에서 특허 정보 자동 추출 | `LEGAL` |
| 문서 삭제 | `DELETE` | `/patents/{patentId}/documents` | 원문 PDF 삭제 | `LEGAL` |

### 4-2. 특허 담당 부서 (Patent Departments)

특허 담당 부서는 별도 매핑 엔티티를 두지 않고, `patents.current_department_id`가 `departments.id`를 외래키로 참조하는 방식으로 관리합니다. 담당 부서 조회는 `GET /patents/{patentId}` 응답 필드로 확인합니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 담당 부서 변경 | `PATCH` | `/patents/{patentId}/department` | 특허의 현재 담당 부서(`current_department_id`) 변경 | 미확정(구현값 우선): `ADMIN`, `LEGAL` |

---

### 5. 권리 상태 이력 (Patent Legal Status)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 권리 상태 이력 조회 | `GET` | `/patents/{patentId}/legal-status` | 권리 상태 변경 이력 조회 | `LEGAL`, `BUSINESS` |
| 권리 상태 이력 추가 | `POST` | `/patents/{patentId}/legal-status` | 권리 상태 수동 추가 | `LEGAL` |

---

### 6. 특허 연차료 (Patent Annuities)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 납부 이력 조회 | `GET` | `/patents/{patentId}/annuities` | 연차료 납부 이력 전체 조회 | `LEGAL`, `BUSINESS` |
| 납부 이력 등록 | `POST` | `/patents/{patentId}/annuities` | 납부 이력 수동 등록 | `LEGAL` |

---

### 7. 검토 주기 (Review Cycles)

Legal 팀은 사업부 검토 요청에 사용할 검토 주기를 관리합니다.
검토 주기의 기간은 서로 겹칠 수 없으며, 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 주기 생성 | `POST` | `/review-cycles` | 검토 주기 등록 | `LEGAL` |
| 검토 주기 목록 조회 | `GET` | `/review-cycles` | 최근 시작일 순으로 검토 주기 목록 조회 | `LEGAL` |
| 검토 주기 단일 조회 | `GET` | `/review-cycles/{reviewCycleId}` | 검토 주기 상세 조회 | `LEGAL` |
| 검토 주기 수정 | `PUT` | `/review-cycles/{reviewCycleId}` | 검토 주기 정보 수정 | `LEGAL` |
| 검토 주기 삭제 | `DELETE` | `/review-cycles/{reviewCycleId}` | 미사용 검토 주기 삭제 | `LEGAL` |

---

### 8. 사업부 검토 요청 전송 (Reviews)

검토 요청 전송 시 특허의 현재 담당 부서와 활성 검토 주기를 대상으로 `reviews` 행이 생성됩니다.
회신 기한은 활성 검토 주기의 종료일로 저장됩니다.
동일한 검토 주기, 특허, 부서 조합은 중복 요청할 수 없으며, 다음 검토 주기에는 다시 요청할 수 있습니다.

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 검토 요청 전송 | `POST` | `/patents/{patentId}/reviews` | 현재 담당 부서와 활성 검토 주기로 검토 요청 전송. reviews 행 생성 | `LEGAL` |

---

### 9. 사업부 검토 (Reviews) — Legal 모니터링

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 사업부 검토 목록 조회 | `GET` | `/reviews` | 전체 검토 요청 및 의견 제출 현황 조회 (상태/부서/특허 필터 가능) | `LEGAL` |
| 사업부 검토 단일 조회 | `GET` | `/reviews/{reviewId}` | 검토 요청 및 의견 제출 상세 조회 | `LEGAL` |

---

### 10. 담당 특허 (Assigned Patents) — 사업부

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 담당 특허 목록 조회 | `GET` | `/assigned-patents` | 내 부서에 배정된 담당 특허 목록 조회 | `BUSINESS` |
| 담당 특허 단일 조회 | `GET` | `/assigned-patents/{patentId}` | 담당 특허 및 의견 제출 정보 조회 | `BUSINESS` |
| 의견 제출 | `POST` | `/assigned-patents/{patentId}/opinions` | 유지 의견/포기 의견 제출. opinion, comment, status, submitted_at 업데이트 | `BUSINESS` |

---

### 11. 평가 보고서 (Reports)

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 보고서 목록 조회 | `GET` | `/patents/{patentId}/reports` | 해당 특허의 보고서 목록 조회(page/size 기반) | `LEGAL`, `BUSINESS` |
| 보고서 생성 요청 | `POST` | `/patents/{patentId}/reports` | 평가 보고서 생성 요청(비동기). 생성 직후 상태는 `생성중` | `LEGAL` |
| 보고서 조회 | `GET` | `/patents/{patentId}/reports/{reportId}` | 보고서 상세 조회(상태/평가완료시각/reportKey). presigned URL은 미확정 | `LEGAL`, `BUSINESS` |
| 보고서 생성 상태 조회 | `GET` | `/patents/{patentId}/reports/{reportId}/status` | 생성 진행 상태 폴링 (생성중/완료/실패) | `LEGAL`, `BUSINESS` |

---

### 12. 대시보드 (Dashboard) — Legal

| 이름 | Method | URL | 설명 | 권한 |
| --- | --- | --- | --- | --- |
| 재평가 진행 현황 | `GET` | `/dashboard/summary` | 진행률, 검토 대상/평가/의견 제출 건수 요약 | `LEGAL` |
| 담당 부서 배정 현황 | `GET` | `/dashboard/assignment` | 미배정/배정 요청/배정 완료 건수 | `LEGAL` |
| 특허 유형 분포 / 만료 현황 | `GET` | `/dashboard/distribution` | 특허 유형 분포 및 분기별 만료 현황 | `LEGAL` |
| 사업부별 검토 현황 | `GET` | `/dashboard/departments` | 부서별 검토 대상/제출 완료/미제출 건수 | `LEGAL` |

.
