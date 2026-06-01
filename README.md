# skipa-backend

SKIPA의 특허 관리 기능과 핵심 비즈니스 로직을 담당하는 백엔드 API 서버입니다.

<br/>

## 📌 프로젝트 소개

SKIPA(SK IP Agent)는 사내 특허의 가치 평가와 Life Cycle 관리를 지원하는 AI 기반 특허 관리 서비스입니다.

`skipa-backend`는 SKIPA 서비스에서 다음 역할을 담당합니다.

- 사용자 인증 및 권한 관리
- 특허 정보 등록, 조회, 수정, 삭제
- 특허 문서 업로드 및 메타데이터 관리
- 특허별 담당 부서 배정
- 연차료 납부 이력 관리
- 권리 상태 이력 관리
- 사업부 검토 요청 및 유지 의견/포기 의견 제출 관리
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
| `local` | H2 Database (TCP) | 로컬 개발용 |
| `prod` | PostgreSQL | 배포 환경용 |

### Resource Structure

```text
src/main/resources
├── application.yaml
├── application-local.yaml
└── application-prod.yaml
```

### application.yaml

공통 설정을 관리합니다.

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE}

  application:
    name: skipa-backend

server:
  port: 8080
```

### application-local.yaml

로컬 개발 환경에서는 실행 중인 H2 TCP 서버에 연결합니다.
`users` 테이블이 비어 있으면 관리자 1명, 법무팀 4명, 사업부 5명의 샘플 계정을 한 번 생성하며, 이후 재시작에서는 기존 데이터를 유지합니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/skipa
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
    show-sql: true

app:
  local:
    seed:
      password: ${LOCAL_SEED_PASSWORD:1234}
```

초기 샘플 계정의 공통 비밀번호는 `1234`입니다.

| Role | Login ID |
|---|---|
| `ADMIN` | `admin` |
| `LEGAL` | `legal01`, `legal02`, `legal03`, `legal04` |
| `BUSINESS` | `business01`, `business02`, `business03`, `business04`, `business05` |

### application-prod.yaml

배포 환경에서 사용하는 PostgreSQL 설정입니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
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
DB_HOST=<host>
DB_PORT=5432
DB_NAME=<database>
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
│   ├── common
│   │   └── entity
│   │       └── BaseTimeEntity.java
│   │
│   ├── config
│   │   ├── JacksonConfig.java
│   │   ├── JpaAuditingConfig.java
│   │   ├── LocalDataInitializer.java
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
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
│       ├── CustomAuthenticationEntryPoint.java
│       ├── CustomUserDetails.java
│       ├── CustomUserDetailsService.java
│       ├── JwtAuthenticationFilter.java
│       └── JwtProvider.java
│
├── domain
│   ├── auth
│   │   ├── api
│   │   │   └── AuthController.java
│   │   ├── application
│   │   │   └── AuthService.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── TokenRefreshRequest.java
│   │   │   └── response
│   │   │       ├── LoginResponse.java
│   │   │       └── MeResponse.java
│   │   └── exception
│   │       └── AuthException.java
│   │
│   ├── user
│   │   ├── api
│   │   │   └── UserController.java
│   │   ├── application
│   │   │   └── UserService.java
│   │   ├── dao
│   │   │   └── UserRepository.java
│   │   ├── domain
│   │   │   ├── User.java
│   │   │   └── UserRole.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   └── UserUpdateRequest.java
│   │   │   └── response
│   │   │       └── UserResponse.java
│   │   └── exception
│   │       ├── UserNotFoundException.java
│   │       └── DuplicateEmailException.java
│   │
│   ├── department
│   │   ├── api
│   │   │   └── DepartmentController.java
│   │   ├── application
│   │   │   └── DepartmentService.java
│   │   ├── dao
│   │   │   └── DepartmentRepository.java
│   │   ├── domain
│   │   │   └── Department.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   ├── DepartmentCreateRequest.java
│   │   │   │   └── DepartmentUpdateRequest.java
│   │   │   └── response
│   │   │       └── DepartmentResponse.java
│   │   └── exception
│   │       └── DepartmentNotFoundException.java
│   │
│   ├── patent
│   │   ├── api
│   │   │   ├── PatentController.java
│   │   │   ├── PatentDocumentController.java
│   │   │   ├── PatentDepartmentController.java
│   │   │   ├── PatentLegalStatusController.java
│   │   │   ├── PatentAnnuityController.java
│   │   │   └── AssignedPatentController.java
│   │   ├── application
│   │   │   ├── PatentService.java
│   │   │   ├── PatentDocumentService.java
│   │   │   ├── PatentDepartmentService.java
│   │   │   ├── PatentLegalStatusService.java
│   │   │   ├── PatentAnnuityService.java
│   │   │   ├── AssignedPatentService.java
│   │   │   └── BusinessPatentAccessValidator.java
│   │   ├── dao
│   │   │   ├── PatentRepository.java
│   │   │   ├── PatentLegalStatusRepository.java
│   │   │   └── PatentAnnuityRepository.java
│   │   ├── domain
│   │   │   ├── Patent.java
│   │   │   ├── PatentLegalStatus.java
│   │   │   ├── PatentAnnuity.java
│   │   │   ├── PatentLegalStatusType.java
│   │   │   └── PatentAnnuityStatus.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   ├── PatentCreateRequest.java
│   │   │   │   ├── PatentUpdateRequest.java
│   │   │   │   ├── PatentDepartmentAssignRequest.java
│   │   │   │   ├── PatentLegalStatusCreateRequest.java
│   │   │   │   ├── PatentAnnuityCreateRequest.java
│   │   │   │   └── PatentAnnuityUpdateRequest.java
│   │   │   └── response
│   │   │       ├── PatentListResponse.java
│   │   │       ├── PatentDetailResponse.java
│   │   │       ├── PatentDocumentExtractResponse.java
│   │   │       ├── PatentDepartmentResponse.java
│   │   │       ├── PatentLegalStatusResponse.java
│   │   │       ├── PatentAnnuityResponse.java
│   │   │       ├── AssignedPatentResponse.java
│   │   │       └── AssignedPatentDetailResponse.java
│   │   └── exception
│   │       ├── PatentNotFoundException.java
│   │       ├── DuplicateApplicationNumberException.java
│   │       ├── PatentDepartmentNotFoundException.java
│   │       ├── PatentLegalStatusNotFoundException.java
│   │       └── PatentAnnuityNotFoundException.java
│   │
│   ├── report
│   │   ├── api
│   │   │   └── ReportController.java
│   │   ├── application
│   │   │   └── ReportService.java
│   │   ├── dao
│   │   │   └── ReportRepository.java
│   │   ├── domain
│   │   │   ├── Report.java
│   │   │   └── ReportStatus.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   └── ReportCreateRequest.java
│   │   │   └── response
│   │   │       ├── ReportCreateResponse.java
│   │   │       ├── ReportResponse.java
│   │   │       └── ReportStatusResponse.java
│   │   └── exception
│   │       ├── ReportNotFoundException.java
│   │       └── ReportGenerationFailedException.java
│   │
│   ├── review
│   │   ├── api
│   │   │   └── ReviewController.java
│   │   ├── application
│   │   │   └── ReviewService.java
│   │   ├── dao
│   │   │   ├── ReviewRepository.java
│   │   │   └── ReviewCycleRepository.java
│   │   ├── domain
│   │   │   ├── Review.java
│   │   │   ├── ReviewCycle.java
│   │   │   ├── ReviewCycleType.java
│   │   │   ├── ReviewStatus.java
│   │   │   └── BusinessOpinion.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   │   └── ReviewSubmitRequest.java
│   │   │   └── response
│   │   │       └── ReviewResponse.java
│   │   └── exception
│   │       ├── ReviewNotFoundException.java
│   │       └── OpinionAlreadySubmittedException.java
│   │
│   └── dashboard
│       ├── api
│       │   └── DashboardController.java
│       ├── application
│       │   └── DashboardService.java
│       └── dto
│           └── response
│               ├── DashboardSummaryResponse.java
│               ├── DashboardAssignmentResponse.java
│               ├── DashboardDistributionResponse.java
│               └── DashboardDepartmentResponse.java
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
| `domain.review` | 사업부 검토 요청, 의견 제출, Legal 모니터링 |
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
| Patent Annuities | `/patents/{patentId}/annuities` | 특허 연차료 관리 |
| Reviews | `/reviews` | Legal 팀 검토 요청 및 의견 제출 현황 모니터링 |
| Assigned Patents | `/assigned-patents` | 사업부 담당 특허 |
| Reports | `/patents/{patentId}/reports` | AI 평가 보고서 관리 |
| Dashboard | `/dashboard` | Legal 팀 대시보드 |

Swagger UI는 서버 실행 후 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.
OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 제공됩니다.

<br/>

## 🚀 Getting Started

### 1. 로컬 실행

로컬 애플리케이션은 `jdbc:h2:tcp://localhost/~/skipa`에 접속합니다. H2가 사용하는 파일 DB 이름은 `skipa`로 맞춰야 합니다.

#### 1-1. 환경 변수 준비

루트 디렉터리에 `.env.example`을 참고하여 `.env` 파일을 생성합니다. `JWT_SECRET`은 Base64 인코딩된 32 byte 이상의 랜덤 키를 사용합니다.

```properties
JWT_SECRET=your-base64-encoded-secret
SPRING_PROFILES_ACTIVE=local
LOCAL_SEED_PASSWORD=1234
```

`JWT_SECRET`에는 로컬 개발용으로 생성한 랜덤 Base64 키를 입력합니다.

#### 1-2. 로컬 실행 순서

처음 한 번, 또는 로컬 DB 파일을 삭제한 뒤 다시 시작할 때 파일 DB를 생성합니다.

```bash
./gradlew h2CreateLocalDb
```

다음으로 H2 TCP 서버를 실행합니다. 이 터미널은 애플리케이션을 사용하는 동안 계속 열어 둡니다.

```bash
./gradlew h2Server
```

다른 터미널에서 Spring Boot 애플리케이션을 실행합니다. `local` profile은 앞서 작성한 `.env`에서 적용됩니다.

```bash
./gradlew bootRun
```

H2 웹 콘솔은 `http://localhost:8082`에서 접속할 수 있으며, JDBC URL은 `jdbc:h2:tcp://localhost/~/skipa`, 사용자명은 `sa`, 비밀번호는 빈 값입니다. 애플리케이션이 처음 비어 있는 `users` 테이블을 만나면 샘플 계정을 생성하고, 공통 비밀번호는 `.env`의 `LOCAL_SEED_PASSWORD`로 변경할 수 있습니다.

#### 1-3. IntelliJ IDEA 실행 설정

IntelliJ를 사용하는 경우에는 다음과 같이 세 개의 Run Configuration을 만들 수 있습니다.

| Name | Type / Main class | Program arguments / Settings |
|---|---|---|
| `H2 Create DB` | Application / `org.h2.tools.Shell` | `-url jdbc:h2:file:~/skipa -user sa -password "" -sql "SELECT 1;"` |
| `H2 Server (tcp)` | Application / `org.h2.tools.Server` | `-tcp -tcpPort 9092 -web -webPort 8082` |
| `SkipaBackendApplication (local)` | Spring Boot / `com.skipers.skipa.SkipaBackendApplication` | Working directory: project root |

애플리케이션은 project root의 `.env`를 직접 불러옵니다. IDE에서 별도로 active profile이나 환경 변수 파일을 등록할 필요는 없습니다.

`H2 Create DB`는 DB가 아직 없을 때만 실행하면 되고, 평소 개발 실행 순서는 `H2 Server (tcp)` 실행 후 `SkipaBackendApplication (local)` 실행입니다. 다른 에디터나 IDE에서는 위 Gradle 명령을 터미널에서 실행하여 같은 환경을 사용할 수 있습니다.

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
