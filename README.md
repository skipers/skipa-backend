# skipa-backend

SKIPA의 특허 관리 기능과 핵심 비즈니스 로직을 담당하는 백엔드 API 서버입니다.

<br/>

## 📌 프로젝트 소개

**SKIPA(SK IP Agent)**는 사내 특허의 가치 평가와 Life Cycle 관리를 지원하는 AI 기반 특허 관리 서비스입니다.

`skipa-backend`는 SKIPA 서비스에서 다음 역할을 담당합니다.

- 사용자 인증 및 권한 관리
- 특허 정보 등록, 조회, 수정, 삭제
- 특허 문서 업로드 및 메타데이터 관리
- 특허별 담당 부서 배정
- 연차료 납부 이력 관리
- 권리 상태 이력 관리
- 사업부 검토 요청 및 유지/포기 결정 관리
- AI 평가 보고서 생성 요청 및 조회
- Legal 팀 대시보드 데이터 제공

<br/>

## 🛠️ Tech Stack

| Category | Stack |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot |
| Build Tool | Gradle |
| Local Database | H2 Database |
| Production Database | PostgreSQL |
| ORM | Spring Data JPA |
| Configuration | YAML |
| Library | Lombok, Validation |

<br/>

## 🗄️ Database Profile

로컬 개발 환경에서는 **H2 Database**를 사용하고, 배포 환경에서는 **PostgreSQL**을 사용합니다.

| Profile | Database | Description |
|---|---|---|
| `local` | H2 Database | 로컬 개발 및 테스트용 |
| `prod` | PostgreSQL | 배포 환경용 |

### Resource Structure

```text
src/main/resources
├── application.yml
├── application-local.yml
└── application-prod.yml
```

### application.yml

공통 설정을 관리합니다.

```yaml
spring:
  profiles:
    active: local

  application:
    name: skipa-backend

server:
  port: 8080
```

### application-local.yml

로컬 개발 환경에서 사용하는 H2 설정입니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:skipa
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
    show-sql: true
```

### application-prod.yml

배포 환경에서 사용하는 PostgreSQL 설정입니다.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
    show-sql: false
```

배포 환경에서는 아래 환경 변수를 설정해야 합니다.

```text
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

<br/>

## 📁 Project Structure

```text
src/main/java/com/skipers/skipa
├── SkipaBackendApplication.java
│
├── global
│   ├── config
│   │   ├── JpaAuditingConfig.java
│   │   ├── WebConfig.java
│   │   └── SecurityConfig.java
│   │
│   ├── exception
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── response
│   │   ├── ApiResponse.java
│   │   └── ErrorResponse.java
│   │
│   └── security
│       ├── JwtProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── CustomUserDetailsService.java
│
├── domain
│   ├── auth
│   │   ├── controller
│   │   │   └── AuthController.java
│   │   ├── service
│   │   │   └── AuthService.java
│   │   └── dto
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       ├── TokenRefreshRequest.java
│   │       └── MeResponse.java
│   │
│   ├── user
│   │   ├── controller
│   │   │   └── UserController.java
│   │   ├── service
│   │   │   └── UserService.java
│   │   ├── repository
│   │   │   └── UserRepository.java
│   │   ├── entity
│   │   │   └── User.java
│   │   ├── dto
│   │   │   ├── UserCreateRequest.java
│   │   │   ├── UserUpdateRequest.java
│   │   │   └── UserResponse.java
│   │   └── type
│   │       └── UserRole.java
│   │
│   ├── department
│   │   ├── controller
│   │   │   └── DepartmentController.java
│   │   ├── service
│   │   │   └── DepartmentService.java
│   │   ├── repository
│   │   │   └── DepartmentRepository.java
│   │   ├── entity
│   │   │   └── Department.java
│   │   └── dto
│   │       ├── DepartmentCreateRequest.java
│   │       ├── DepartmentUpdateRequest.java
│   │       └── DepartmentResponse.java
│   │
│   ├── patent
│   │   ├── controller
│   │   │   ├── PatentController.java
│   │   │   ├── PatentDocumentController.java
│   │   │   ├── PatentDepartmentController.java
│   │   │   ├── PatentLegalStatusController.java
│   │   │   └── PatentAnnuityController.java
│   │   ├── service
│   │   │   ├── PatentService.java
│   │   │   ├── PatentDocumentService.java
│   │   │   ├── PatentDepartmentService.java
│   │   │   ├── PatentLegalStatusService.java
│   │   │   └── PatentAnnuityService.java
│   │   ├── repository
│   │   │   ├── PatentRepository.java
│   │   │   ├── PatentDepartmentRepository.java
│   │   │   ├── PatentLegalStatusRepository.java
│   │   │   └── AnnuityHistoryRepository.java
│   │   ├── entity
│   │   │   ├── Patent.java
│   │   │   ├── PatentDepartment.java
│   │   │   ├── PatentLegalStatus.java
│   │   │   └── AnnuityHistory.java
│   │   ├── dto
│   │   │   ├── PatentCreateRequest.java
│   │   │   ├── PatentUpdateRequest.java
│   │   │   ├── PatentListResponse.java
│   │   │   ├── PatentDetailResponse.java
│   │   │   ├── PatentDocumentExtractResponse.java
│   │   │   ├── PatentDepartmentAssignRequest.java
│   │   │   ├── PatentLegalStatusCreateRequest.java
│   │   │   ├── AnnuityCreateRequest.java
│   │   │   └── AnnuityUpdateRequest.java
│   │   └── type
│   │       ├── PatentLegalStatusType.java
│   │       └── AnnuityStatus.java
│   │
│   ├── report
│   │   ├── controller
│   │   │   └── ReportController.java
│   │   ├── service
│   │   │   └── ReportService.java
│   │   ├── repository
│   │   │   └── ReportRepository.java
│   │   ├── entity
│   │   │   └── Report.java
│   │   ├── dto
│   │   │   ├── ReportCreateResponse.java
│   │   │   ├── ReportResponse.java
│   │   │   └── ReportStatusResponse.java
│   │   └── type
│   │       └── ReportStatus.java
│   │
│   ├── decision
│   │   ├── controller
│   │   │   ├── DecisionController.java
│   │   │   └── InboxController.java
│   │   ├── service
│   │   │   ├── DecisionService.java
│   │   │   └── InboxService.java
│   │   ├── repository
│   │   │   └── DecisionRepository.java
│   │   ├── entity
│   │   │   └── Decision.java
│   │   ├── dto
│   │   │   ├── DecisionCreateRequest.java
│   │   │   ├── DecisionResponse.java
│   │   │   ├── InboxResponse.java
│   │   │   └── DecisionSubmitRequest.java
│   │   └── type
│   │       ├── DecisionStatus.java
│   │       └── DecisionType.java
│   │
│   └── dashboard
│       ├── controller
│       │   └── DashboardController.java
│       ├── service
│       │   └── DashboardService.java
│       └── dto
│           ├── DashboardSummaryResponse.java
│           ├── DashboardAssignmentResponse.java
│           ├── DashboardDistributionResponse.java
│           └── DashboardDepartmentResponse.java
│
└── infra
    ├── ai
    │   ├── AiServerClient.java
    │   └── dto
    │       ├── AiReportRequest.java
    │       └── AiReportResponse.java
    │
    ├── storage
    │   ├── S3Client.java
    │   └── S3Service.java
    │
    └── kipris
        ├── KiprisClient.java
        └── dto
            └── KiprisPatentResponse.java
```

<br/>

## 📦 Package Description

| Package | Description |
|---|---|
| `global` | 전역 설정, 공통 응답, 예외 처리, 보안 관련 공통 모듈 |
| `domain.auth` | 로그인, 로그아웃, 토큰 갱신, 내 정보 조회 |
| `domain.user` | 사용자 관리 |
| `domain.department` | 부서 관리 |
| `domain.patent` | 특허 기본 정보, 문서, 담당 부서, 권리 상태, 연차료 관리 |
| `domain.report` | AI 평가 보고서 생성 요청, 조회, 상태 확인 |
| `domain.decision` | 사업부 검토 요청, 결정 제출, Legal 모니터링, Inbox |
| `domain.dashboard` | Legal 팀 대시보드 통계 |
| `infra.ai` | AI 서버 연동 |
| `infra.storage` | S3 등 파일 저장소 연동 |
| `infra.kipris` | KIPRIS API 연동 |

<br/>

## 🧭 API Domain

| Domain | Base Path | Description |
|---|---|---|
| Auth | `/auth` | 인증 및 토큰 관리 |
| Users | `/users` | 사용자 관리 |
| Departments | `/departments` | 부서 관리 |
| Patents | `/patents` | 특허 관리 |
| Patent Documents | `/patents/{patentId}/documents` | 특허 문서 관리 |
| Patent Departments | `/patents/{patentId}/departments` | 특허 담당 부서 관리 |
| Patent Legal Status | `/patents/{patentId}/legal-status` | 권리 상태 이력 관리 |
| Annuity History | `/patents/{patentId}/annuities` | 연차료 납부 이력 관리 |
| Decisions | `/decisions` | Legal 팀 결정 현황 모니터링 |
| Inbox | `/inbox` | 사업부 검토 요청함 |
| Reports | `/patents/{patentId}/reports` | AI 평가 보고서 관리 |
| Dashboard | `/dashboard` | Legal 팀 대시보드 |

<br/>

## 🚀 Getting Started

### 1. 로컬 실행

기본 profile은 `local`입니다.

```bash
./gradlew bootRun
```

Windows 환경에서는 아래 명령어를 사용할 수 있습니다.

```bash
gradlew.bat bootRun
```

### 2. 배포 환경 실행

배포 환경에서는 `prod` profile을 사용합니다.

```bash
java -jar build/libs/skipa-backend.jar --spring.profiles.active=prod
```

### 3. 테스트 실행

```bash
./gradlew test
```

<br/>

## 📝 Convention

자세한 이슈 작성 규칙, 브랜치 전략, PR 제목 규칙, 커밋 컨벤션은 조직의 `CONTRIBUTING.md`를 참고해 주세요.
