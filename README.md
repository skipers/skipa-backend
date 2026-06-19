# skipa-backend

SKIPA는 사내 특허 데이터와 관련 업무를 통합 관리하기 위한 특허 관리 시스템입니다. 주요 관리 대상은 특허 등록 정보, 권리 상태, 연차료 이력, 사업부 검토, 평가 보고서, 사전 평가, 포트폴리오 통계입니다.

`skipa-backend`는 해당 시스템의 백엔드 애플리케이션으로, 도메인별 비즈니스 로직, 인증 및 인가, 데이터 영속화, 비동기 작업 처리, 외부 시스템 연동을 담당합니다. 애플리케이션은 Java 17과 Spring Boot를 기반으로 구성되며, 상세 API 계약은 `api-spec.md`에서 관리합니다.

## 주요 기능

| 구분 | 내용 |
| --- | --- |
| 인증 및 인가 | 회원가입, 로그인, 토큰 재발급, 로그아웃, 내 정보 조회 |
| 사용자 및 부서 관리 | 관리자 승인, 부서 생성·조회·수정·비활성화 |
| 특허 운영 관리 | 특허 생성, 조회, 수정, 삭제, 담당 부서 변경, 원문 PDF 접근 URL 조회 |
| 검토 및 평가 | 검토 주기 관리, 사업부 검토 요청·의견 제출, 평가 보고서 생성·조회·채팅 |
| AI 연계 기능 | 특허 PDF 추출 작업, 사전 평가 생성, 포트폴리오 인사이트 생성 |

## 역할 정책

| 역할 | 주요 권한 |
| --- | --- |
| `ADMIN` | 사용자 승인, 검토 주기 생성·수정·삭제, 부서/특허 관리, 전사 범위 조회 |
| `LEGAL` | 부서 관리, 특허 운영, 권리 상태 및 연차료 관리, 검토 요청·확인, 평가 보고서 생성, 포트폴리오·Legal 대시보드 조회 |
| `BUSINESS` | 담당 특허 및 사업부 검토 현황 조회, 검토 의견 제출, 사전 평가, Business 대시보드 조회 |

부서 관리 API(`POST/GET/PUT/DELETE /api/v1/departments`)와 특허 생성 API(`POST /api/v1/patents`)는 `ADMIN`과 `LEGAL`에 허용됩니다. 특허 생성 시 생성된 특허는 즉시 `APPROVED` 상태로 저장되며, 사업부 특허 생성 기능과 별도의 특허 승인 API는 제공하지 않습니다.

## 주요 업무 정책

### 부서 비활성화 정책

부서는 물리 삭제하지 않고 `INACTIVE` 상태로 전환합니다. 비활성 부서는 신규 사용자 승인, 특허 담당 부서 변경, 신규 검토 요청의 대상으로 사용할 수 없습니다.

### 사업부 접근 범위

`BUSINESS` 사용자의 담당 특허 목록, 사업부 검토 화면, Business 대시보드는 본인 소속 부서 범위를 기준으로 조회됩니다.

### 관리자 조회 범위

`ADMIN`은 전사 범위 조회와 관리자 기능에 접근합니다. 다만 검토 요청·검토 확인·권리 상태 관리·연차료 관리·평가 보고서 생성 등 실무 운영 기능은 `LEGAL`이 담당합니다.

## Tech Stack

| Category | Stack |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot |
| Build Tool | Gradle |
| Local Database | H2 Database |
| Production Database | PostgreSQL |
| ORM | Spring Data JPA |
| Security | Spring Security, JWT |
| Cache | Redis |
| Message Queue | RabbitMQ |
| Object Storage | MinIO |
| API Docs | springdoc-openapi |
| Library | Lombok, Validation |

## Database Profile

| Profile | Database | Purpose |
| --- | --- | --- |
| `local` | H2 Database (TCP) | 로컬 개발 및 빠른 기능 검증 |
| `local-postgres` | PostgreSQL | 로컬 PostgreSQL 연동 검증 |
| `prod` | PostgreSQL | 운영 및 통합 검증 환경 |

- `local`: H2 파일 DB, `ddl-auto: update`, Redis 자동 구성 제외, Flyway 비활성화, 샘플 데이터 자동 주입
- `local-postgres`: PostgreSQL, `ddl-auto: validate`, Flyway 비활성화
- `prod`: PostgreSQL, `ddl-auto: validate`, Flyway 기반 스키마 관리

## 아키텍처 개요

기능 중심 도메인 구조를 기준으로 `api`, `application`, `dao`/`domain`, `infra` 계층을 분리합니다.

- 기능 단위 응집도 유지
- 비즈니스 로직과 외부 연동 구현의 분리
- 테스트 용이성 및 변경 영향 범위 최소화
- 권한 검증과 예외 처리를 계층별로 명확히 분리

### 요청 처리 흐름

```text
Client
  ↓
API Controller
  ↓
Application Service
  ↓
Domain / Repository
  ↓
Database
```

- `api`: 요청 수신, 입력 검증, 인증 사용자 전달
- `application`: 권한 확인, 업무 규칙 수행, 트랜잭션 처리
- `domain` / `dao`: 엔티티 상태 변경 및 데이터 조회
- `infra`: RabbitMQ, MinIO, AI 서버 등 외부 연동 처리

### 외부 연계 포함 흐름

```text
Client
  ↓
API Controller
  ↓
Application Service
  ├─ Repository → Database
  └─ Infra → RabbitMQ / MinIO / AI Server
```

## 프로젝트 구조

```text
src/main/java/com/skipers/skipa
├── global      # 공통 설정, 보안, 예외, 응답
└── domain      # 기능별 비즈니스 도메인
```

```text
src/main/java/com/skipers/skipa
├── global                  # 공통 설정, 보안, 예외, 응답
└── domain
    ├── auth                # 인증/토큰
    ├── chat                # 공통 채팅 메시지/스트리밍
    ├── user                # 관리자 승인
    ├── department          # 부서 관리
    ├── patent              # 특허, 권리 상태, 연차료, 사업부 검토 화면
    ├── review              # 검토 주기, 검토 요청, 회신 확인
    ├── report              # 평가 보고서/채팅
    ├── preevaluation       # 사전 평가/채팅
    ├── patentextract       # 특허 PDF 추출 작업
    ├── portfolio           # 포트폴리오 통계/AI 인사이트
    └── dashboard           # Legal/Business 대시보드
```

외부 시스템 연동 구현은 별도 최상위 `infra` 패키지 대신 각 도메인 하위 `infra` 패키지에 배치합니다.

```text
global
├── common/entity          # 공통 엔티티(BaseTimeEntity 등)
├── config                 # Security, OpenAPI, Jackson, JPA, MinIO, RabbitMQ 설정
├── exception              # 공통 예외, 에러 코드, 전역 예외 처리
├── response               # 표준 API 응답 래퍼
└── security               # JWT 인증 필터, UserDetails, 인증/인가 처리

domain/<feature>
├── api                    # Controller 계층
├── application            # Service 및 비즈니스 로직
├── dao                    # Repository 인터페이스
├── domain                 # Entity, Enum 등 핵심 도메인 모델
├── dto                    # 요청/응답 DTO
├── exception              # 기능별 예외
└── infra                  # 외부 시스템 연동 구현
```

```text
domain/patent
├── api                    # PatentController, PatentAnnuityController 등
├── application            # PatentService, BusinessReviewService 등
├── dao                    # PatentRepository, PatentAnnuityRepository 등
├── domain                 # Patent, PatentAnnuity, PatentApprovalStatus 등
├── dto
│   ├── request            # PatentCreateRequest, PatentUpdateRequest 등
│   └── response           # PatentDetailResponse, PatentListResponse 등
├── exception              # PatentException
└── infra                  # PDF 저장소 구현 등

domain/report
├── api                    # ReportController, InternalReportController, ReportChatController
├── application            # 보고서 생성·조회·채팅 서비스
├── dao                    # ReportRepository
├── domain                 # Report, ReportStatus
├── dto
│   ├── request
│   └── response
├── exception              # ReportException
└── infra                  # RabbitMQ 발행, MinIO 저장소, AI 채팅 클라이언트
```

```text
src/main/resources
├── application.yaml
├── application-local.yaml
├── application-local-postgres.yaml
├── application-prod.yaml
└── db/migration          # Flyway 마이그레이션
```

```text
repository root
├── load-tests/           # k6 기반 부하 테스트 스크립트
└── .github/workflows/    # 배포 및 자동화 워크플로
```

테스트 코드는 운영 코드의 패키지 구조를 기준으로 동일하게 구성합니다.

```text
src/test/java/com/skipers/skipa
├── domain
└── global
```

## 실행 방법

1. `.env.example`을 복사하여 `.env` 파일을 생성합니다.
2. 최소 필수 환경 변수인 `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`, `INTERNAL_API_KEY`를 설정합니다.
3. 필요 시 로컬 H2 데이터베이스를 초기화합니다.

```bash
cp .env.example .env
./gradlew h2CreateLocalDb
./gradlew bootRun
```

| 명령어 | 설명 |
| --- | --- |
| `./gradlew h2CreateLocalDb` | 로컬 H2 데이터베이스 파일 생성 |
| `./gradlew bootRun` | 로컬 서버 실행 |
| `./gradlew test` | 테스트 실행 |
| `./gradlew clean bootJar` | 배포용 JAR 빌드 |
| `./gradlew h2Server` | H2 TCP 서버 및 웹 콘솔 실행 |

API prefix는 `/api/v1`이며, Swagger UI는 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.
OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 제공합니다.

## 인증 및 인가 방식

- Access Token 기반 JWT 인증을 사용하며, Refresh Token은 `HttpOnly Cookie`로 관리합니다.
- 컨트롤러 계층: `@PreAuthorize`를 통해 역할 기반 접근 제어 수행
- 서비스 계층: 부서 범위, 승인 상태, 리소스 소유 여부 등 세부 업무 규칙 검증
- 내부 콜백 API: `INTERNAL_API_KEY` 헤더 기반 보호

## 로컬 확인 순서

1. `.env` 구성 후 `./gradlew bootRun`을 실행합니다.
2. `http://localhost:8080/swagger-ui/index.html`에 접속합니다.
3. `POST /api/v1/auth/login`으로 샘플 계정에 로그인합니다.
4. 발급받은 JWT를 Swagger `Authorize`에 입력합니다.
5. 역할별 API를 호출하여 권한 및 응답 형식을 검증합니다.

| 구분 | ID | PWD |
| --- | --- | --- |
| `ADMIN` | `admin` | `1234` |
| `LEGAL` | `legal01` | `1234` |
| `BUSINESS` | `biz01` | `1234` |

| 항목 | 값 |
| --- | --- |
| H2 웹 콘솔 URL | `http://localhost:8082` |

H2 웹 콘솔 접속 시 사용하는 JDBC 연결 정보는 다음과 같습니다.

| 항목 | 값 |
| --- | --- |
| JDBC URL | `jdbc:h2:tcp://localhost/~/skipa` |
| 사용자명 | `sa` |
| 비밀번호 | 빈 값 |

## 로컬 개발 메모

- `local` 프로필에서는 샘플 부서, 사용자, 특허 및 검토 데이터가 자동 주입됩니다.
- 샘플 계정 비밀번호 기본값은 `LOCAL_SEED_PASSWORD=1234`입니다.
- 일부 AI 및 RabbitMQ 연동은 로컬에서 대체 구현으로 동작합니다.

## 환경 변수 가이드

| 구분 | 변수 |
| --- | --- |
| 필수 | `SPRING_PROFILES_ACTIVE`, `JWT_SECRET`, `INTERNAL_API_KEY` |
| 로컬 편의 | `LOCAL_SEED_PASSWORD` |
| DB 연동 | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| 캐시 연동 | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` |
| 메시지 큐 | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| AI 서버 | `AI_SERVER_BASE_URL` 및 기능별 path |
| 파일 저장소 | `MINIO_ENDPOINT`, `MINIO_PUBLIC_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |

`JWT_SECRET`은 충분한 길이의 안전한 비밀값을 사용해야 하며, 내부 콜백 보호용 `INTERNAL_API_KEY` 역시 별도의 공유 비밀값으로 관리해야 합니다.

## 주요 API 도메인

| Domain | Base Path | 설명 |
| --- | --- | --- |
| Auth | `/api/v1/auth` | 회원가입, 로그인, 토큰 재발급, 로그아웃 |
| Admin Users | `/api/v1/admin/users` | 관리자 승인 |
| Departments | `/api/v1/departments` | 부서 생성·조회·수정·비활성화 |
| Patents | `/api/v1/patents` | 특허 생성·조회·수정·삭제, 담당 부서 변경, 원문 PDF URL 조회 |
| Patent Annuities | `/api/v1/patents/{patentId}/annuities` | 연차료 이력 생성·조회·수정·삭제 |
| Patent Legal Status | `/api/v1/patents/{patentId}/legal-status` | 권리 상태 이력 생성·조회 |
| Expiring Patents | `/api/v1/patents/expiring` | 만료 예정 특허 요약, 목록, 캘린더 조회 |
| Patent Extract Jobs | `/api/v1/patent-extract-jobs` | 특허 PDF 업로드 URL 발급 및 추출 작업 상태 관리 |
| Review Cycles | `/api/v1/review-cycles` | 검토 주기 생성·조회·수정·마감일 변경·삭제 |
| Reviews | `/api/v1/reviews`, `/api/v1/review-targets`, `/api/v1/patents/{patentId}/reviews` | 검토 요청, 대상 조회, Legal 검토 현황 |
| Business Reviews | `/api/v1/business-reviews` | 사업부 검토 현황 조회 및 의견 제출 |
| Reports | `/api/v1/patents/{patentId}/reports` | 평가 보고서 생성·조회 |
| Report Chat | `/api/v1/patents/{patentId}/reports/{reportId}/chat/messages` | 평가 보고서 AI 채팅 조회·질의·스트리밍·초기화 |
| Pre-Evaluations | `/api/v1/pre-evaluations` | 사전 평가 생성·조회·삭제 |
| Pre-Evaluation Chat | `/api/v1/pre-evaluations/{preEvaluationId}/chat/messages` | 사전 평가 AI 채팅 조회·질의·스트리밍·초기화 |
| Portfolio | `/api/v1/portfolio` | 포트폴리오 통계 및 AI 인사이트 |
| Dashboard | `/api/v1/dashboard` | Legal(`/legal`) 및 Business(`/business`) 대시보드 |

## 데이터 관리 및 마이그레이션 정책

| 항목 | 정책 |
| --- | --- |
| 스키마 변경 | Flyway 마이그레이션으로 관리 |
| `local` 환경 | H2 기반 로컬 개발 환경, 샘플 데이터 자동 주입 |
| `local-postgres` 환경 | PostgreSQL 기반 로컬 연동 검증 환경, `validate` 중심 확인 |
| `prod` 환경 | PostgreSQL 기반 운영 환경, `validate` 중심 검증 |
| 부서 삭제 | 물리 삭제보다 비활성화 상태 전환 우선 적용 |

## 비동기 처리 흐름

- 평가 보고서 생성: Backend → RabbitMQ → AI Worker → 내부 콜백 API
- 특허 추출 작업: Presigned URL 업로드 → RabbitMQ → AI Worker → 내부 콜백 API
- 사전 평가: 요청 저장 → RabbitMQ 또는 AI 서버 연동 → 상태 polling/채팅

대표 내부 콜백 경로는 다음과 같습니다.

- 평가 보고서: `/api/v1/internal/reports/{reportId}/complete`, `/api/v1/internal/reports/{reportId}/embedding-complete`, `/api/v1/internal/reports/{reportId}/fail`
- 특허 추출 작업: `/api/v1/internal/patent-extract-jobs/{extractJobId}/complete`, `/api/v1/internal/patent-extract-jobs/{extractJobId}/fail`
- 사전 평가: `/api/v1/internal/pre-evaluations/{preEvaluationId}/complete`, `/api/v1/internal/pre-evaluations/{preEvaluationId}/embedding-complete`, `/api/v1/internal/pre-evaluations/{preEvaluationId}/fail`

내부 콜백 API는 `INTERNAL_API_KEY`를 기반으로 보호합니다.

## 비동기 연계 원칙

| 항목 | 원칙 |
| --- | --- |
| 상태 저장 | 요청 수신 시 작업 상태를 우선 저장 |
| 작업 위임 | RabbitMQ를 통해 AI Worker 또는 후속 처리 컴포넌트에 작업 위임 |
| 결과 반영 | 내부 콜백 API를 통해 상태 반영 |
| 파일 관리 | 파일 자체가 아닌 MinIO object key 기준으로 위치 관리 |
| 진행 조회 | 클라이언트는 상태 조회 API로 진행 상황 확인 |

### 비동기 처리 흐름도

```text
[Client]
   │
   │ 1. 작업 요청
   ▼
[Backend API]
   │
   │ 2. 작업 상태 저장
   ▼
[Database]
   ▲
   │
[Backend API]
   │
   │ 3. 메시지 발행 또는 외부 호출
   ▼
[RabbitMQ / AI Server / MinIO]
   │
   │ 4. 처리 결과 반환
   ▼
[Internal Callback API]
   │
   │ 5. 상태 갱신
   ▼
[Database]
   │
   │ 6. 클라이언트 polling 조회
   ▼
[Client]
```

- 평가 보고서 생성: 보고서 상태 저장 → RabbitMQ 발행 → AI Worker 처리 → 내부 완료 콜백
- 특허 추출: 업로드 URL 발급 → MinIO 업로드 → RabbitMQ 발행 → AI 추출 결과 콜백
- 사전 평가: 요청 저장 → 비동기 생성 처리 → 내부 완료/실패 콜백 → 상태 조회 및 채팅 연계

### 평가 보고서 생성 흐름

```json
{
  "type": "REPORT_GENERATE",
  "reportId": 8001,
  "patentId": 1001
}
```

- 외부 워커 완료 반영: `PATCH /api/v1/internal/reports/{reportId}/complete`
- 임베딩 완료 반영: `PATCH /api/v1/internal/reports/{reportId}/embedding-complete`
- 실패 반영: `PATCH /api/v1/internal/reports/{reportId}/fail`

### 특허 원문 PDF 추출 흐름

```json
{
  "type": "PATENT_EXTRACT",
  "extractJobId": 9001,
  "objectKey": "patents/extract-jobs/9001/patent.pdf"
}
```

- 업로드 완료 요청: `POST /api/v1/patent-extract-jobs/{extractJobId}/upload-complete`
- 추출 완료 반영: `PATCH /api/v1/internal/patent-extract-jobs/{extractJobId}/complete`
- 추출 실패 반영: `PATCH /api/v1/internal/patent-extract-jobs/{extractJobId}/fail`

### 사전 평가 생성 흐름

```json
{
  "type": "PRE_EVALUATION",
  "preEvaluationId": 7001,
  "title": "신규 기술 사전 평가"
}
```

- 완료 반영: `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/complete`
- 임베딩 완료 반영: `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/embedding-complete`
- 실패 반영: `PATCH /api/v1/internal/pre-evaluations/{preEvaluationId}/fail`

## 보안 및 운영 유의사항

| 항목 | 내용 |
| --- | --- |
| 민감 정보 | `JWT_SECRET`, `INTERNAL_API_KEY`, DB 비밀번호, MinIO 자격 정보는 저장소에 커밋하지 않음 |
| Swagger | 개발 및 검증 용도로 사용하며 운영 접근 제어 정책과 함께 적용 |
| Presigned URL | 한시적 접근을 전제로 사용하며 장기 고정 URL로 취급하지 않음 |
| 외부 장애 대응 | API, 메시지 큐, 파일 저장소, AI 서버 연계 구간을 분리 운영 |

## 상태값 및 Enum 정의

| 구분 | 값 |
| --- | --- |
| 권리 상태 | `PUBLISHED`, `REGISTERED`, `REJECTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `WITHDRAWN` |
| 연차료 납부 상태 | `PAID`, `UNPAID`, `ABANDONED` |
| 특허 추출 작업 상태 | `UPLOAD_PENDING`, `ANALYZING`, `COMPLETED`, `FAILED` |
| 보고서 처리 상태 | `GENERATING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 사전 평가 처리 상태 | `PROCESSING`, `REPORT_COMPLETED`, `EMBEDDING_COMPLETED`, `FAILED` |
| 검토 제출 상태 | `PENDING`, `SUBMITTED` |
| 사업부 의견 | `MAINTAIN`, `ABANDON` |

## 문서 운영 규칙

- API 경로, 요청/응답 DTO, 권한 정책이 변경되면 `api-spec.md`를 함께 갱신합니다.
- README는 시스템 개요, 구조, 실행 방법 중심으로 유지하고, `api-spec.md`는 엔드포인트 상세 계약 문서로 관리합니다.
- 권한 정책 설명은 컨트롤러의 `@PreAuthorize`와 서비스 계층의 검증 로직을 기준으로 작성합니다.

## 테스트 및 검증 원칙

| 항목 | 내용 |
| --- | --- |
| 자동 테스트 | `./gradlew test`로 전체 테스트 실행 |
| 수동 검증 | Swagger 기반으로 인증, 권한, 요청/응답 계약 확인 |
| 통합 검증 | 필요 시 H2, MinIO, RabbitMQ, AI 서버 연계 확인 |

## 개발 도구 참고

IntelliJ IDEA에서는 `H2 Create DB`, `H2 Server`, `SkipaBackendApplication (local)` 형태로 실행 구성을 분리하는 방식을 권장합니다.

## 배포 실행 예시

```bash
java -jar build/libs/skipa-backend.jar --spring.profiles.active=prod
```

운영 환경에서는 데이터베이스, Redis, RabbitMQ, MinIO, 내부 API 키 구성이 선행되어야 합니다.

## Convention

이슈 작성 규칙, 브랜치 전략, PR 제목 규칙, 커밋 컨벤션은 팀의 협업 가이드를 따릅니다.
