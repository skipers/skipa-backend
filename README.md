# skipa-backend

SKIPA의 특허 관리 기능과 핵심 비즈니스 로직을 담당하는 백엔드 API 서버입니다.

## 프로젝트 소개

SKIPA(SK IP Agent)는 사내 특허의 가치 평가와 Life Cycle 관리를 지원하는 AI 기반 특허 관리 서비스입니다.

`skipa-backend`는 다음 기능을 담당합니다.

- 회원가입, 로그인, 관리자 가입 승인
- 부서 생성, 조회, 수정, 비활성화
- 특허 정보 등록, 조회, 수정, 삭제, 담당 부서 변경
- 권리 상태 이력 및 연차료 납부 이력 관리
- 검토 주기 관리와 사업부 단건·일괄 검토 요청
- Legal 팀의 검토 현황 조회
- 사업부의 요청받은 검토 조회와 의견 제출
- 특허 평가 보고서 생성 요청과 조회
- 로그아웃, 토큰 갱신, 내 정보 조회
- 관리자 사용자 관리
- 특허 원문 PDF 업로드와 메타데이터 추출
- RabbitMQ 기반 AI Worker 연동, MinIO 보고서 파일 연동, KIPRIS 연동
- Legal 팀 대시보드

API의 상세 내용은 [api-spec.md](api-spec.md)를 참고해 주세요.

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
| Message Queue | RabbitMQ |
| Object Storage | MinIO |
| API Docs | springdoc-openapi |
| Library | Lombok, Validation |

## Database Profile

로컬 개발 환경에서는 H2 Database를 사용하고, 배포 환경에서는 PostgreSQL을 사용합니다.

| Profile | Database | Description |
| --- | --- | --- |
| `local` | H2 Database (TCP) | 로컬 개발용 |
| `prod` | PostgreSQL | 배포 환경용 |

리소스 파일은 다음과 같이 구성됩니다.

```text
src/main/resources
├── application.yaml
├── application-local.yaml
└── application-prod.yaml
```

`local` profile은 `ddl-auto: update`를 사용합니다. `users` 테이블이 비어 있으면 관리자 1명, Legal 사용자 4명, 사업부 사용자 5명의 샘플 계정을 생성합니다.

초기 샘플 계정의 기본 비밀번호는 `1234`이며 `LOCAL_SEED_PASSWORD`로 변경할 수 있습니다.

| Role | Login ID |
| --- | --- |
| `ADMIN` | `admin` |
| `LEGAL` | `legal01`, `legal02`, `legal03`, `legal04` |
| `BUSINESS` | `business01`, `business02`, `business03`, `business04`, `business05` |

`prod` profile은 PostgreSQL과 `ddl-auto: validate`를 사용합니다. 스키마 변경은 배포 전에 DB에 별도로 반영해야 합니다.

## Project Structure

```text
src/main/java/com/skipers/skipa
├── global
│   ├── common/entity          # 생성·수정 시각 공통 엔티티
│   ├── config                 # 보안, JPA auditing, Jackson, OpenAPI, 로컬 seed 설정
│   ├── exception              # 공통 예외 처리
│   ├── response               # 공통 API 응답
│   └── security               # JWT 인증 필터와 사용자 인증 정보
└── domain
    ├── auth                   # 회원가입과 로그인
    ├── user                   # 관리자 사용자 승인
    ├── department             # 부서 관리
    ├── patent                 # 특허, 권리 상태, 연차료, 사업부 검토 화면
    ├── review                 # 검토 주기, 검토 요청, Legal 모니터링
    └── report                 # 평가 보고서 생성 요청, RabbitMQ 발행, MinIO URL 조회
```

확장 구조는 다음과 같습니다.

```text
src/main/java/com/skipers/skipa
├── domain
│   └── dashboard             # Legal 팀 대시보드 통계
└── infra
    ├── ai                    # AI 서버 연동
    ├── storage               # MinIO 등 파일 저장소 연동
    └── kipris                # KIPRIS API 연동
```

| 패키지 | 역할 |
| --- | --- |
| `domain.user` 확장 | 관리자 사용자 목록 조회, 생성, 수정, 삭제 |
| `domain.patent` 문서 확장 | 특허 원문 업로드, 삭제, 메타데이터 추출 |
| `domain.dashboard` | Legal 팀 대시보드 통계 |
| `infra.ai` | AI 서버 연동 |
| `infra.storage` | MinIO 등 파일 저장소 연동 |
| `infra.kipris` | KIPRIS API 연동 |

## 주요 정책

### 부서 비활성화

부서는 삭제하지 않고 `INACTIVE` 상태로 변경합니다. 기존 사용자, 특허, 검토 이력의 참조는 유지됩니다.

비활성 부서는 신규 사용자 승인, 특허 담당 부서 변경, 신규 검토 요청에 사용할 수 없습니다.

### 사업부 접근 범위

`BUSINESS` 사용자는 현재 담당 부서가 본인 소속 부서와 같은 특허만 조회할 수 있습니다. 특허의 권리 상태, 연차료, 보고서 조회에도 동일한 제한이 적용됩니다.

`/assigned-patents`는 사업부 화면의 검토 현황 API입니다. 각 특허와 부서의 가장 최근 검토 요청을 기준으로 목록과 상세 정보를 반환합니다.

### 관리자 조회 범위

`ADMIN` 사용자는 전체 조회 API를 사용할 수 있습니다. 권리 상태, 연차료, 보고서 생성, 검토 요청, 검토 주기 변경과 같은 실무 변경 작업은 `LEGAL` 사용자만 수행합니다.

### 평가 보고서 생성 흐름

프론트는 백엔드의 `POST /patents/{patentId}/reports`만 호출합니다. 백엔드는 인증과 `LEGAL` 권한을 확인한 뒤 보고서를 `GENERATING` 상태로 저장하고 RabbitMQ에 생성 메시지를 발행합니다.

AI Worker는 RabbitMQ 메시지를 소비해 보고서를 생성하고 MinIO에 저장합니다. 저장 후 백엔드 내부 API를 호출해 완료 또는 실패 상태를 전달합니다.

```text
Frontend -> Backend -> RabbitMQ -> AI Worker -> MinIO -> Backend internal API -> Frontend polling
```

RabbitMQ 메시지 payload는 다음 형식입니다.

```json
{
  "type": "REPORT_GENERATE",
  "reportId": 8001,
  "patentId": 1001
}
```

AI Worker는 완료 시 MinIO 전체 URL이 아니라 object key만 백엔드에 전달합니다.

```http
PATCH /internal/reports/{reportId}/complete
X-Internal-Api-Key: <secret>
```

```json
{
  "reportKey": "reports/8001/report.html"
}
```

실패 시에는 다음 내부 API를 호출합니다.

```http
PATCH /internal/reports/{reportId}/fail
X-Internal-Api-Key: <secret>
```

```json
{
  "errorMessage": "AI report generation failed"
}
```

프론트는 `GET /patents/{patentId}/reports/{reportId}/status`를 polling하고, `COMPLETED`가 되면 `GET /patents/{patentId}/reports/{reportId}`로 백엔드가 생성한 MinIO presigned URL을 받습니다. 프론트 응답에는 MinIO object key를 직접 노출하지 않습니다.

### Enum

API와 DB에 저장되는 enum 문자열은 영어 대문자로 통일합니다.

| 구분 | 값 |
| --- | --- |
| 권리 상태 | `PUBLISHED`, `REGISTERED`, `REJECTED`, `ABANDONED`, `EXPIRED`, `INVALIDATED`, `WITHDRAWN` |
| 연차료 납부 상태 | `PAID`, `UNPAID`, `ABANDONED` |
| 보고서 생성 상태 | `GENERATING`, `COMPLETED`, `FAILED` |
| 검토 제출 상태 | `PENDING`, `SUBMITTED` |
| 사업부 의견 | `MAINTAIN`, `ABANDON` |

## API Domain

| Domain | Base Path | Description |
| --- | --- | --- |
| Auth | `/auth` | 회원가입과 로그인 |
| Admin Users | `/admin/users` | 관리자 사용자 승인 |
| Users | `/users` | 관리자 사용자 관리 |
| Departments | `/departments` | 부서 관리 |
| Patents | `/patents` | 특허 관리와 담당 부서 변경 |
| Patent Documents | `/patents/{patentId}/documents` | 특허 문서 관리 |
| Patent Legal Status | `/patents/{patentId}/legal-status` | 권리 상태 이력 관리 |
| Patent Annuities | `/patents/{patentId}/annuities` | 연차료 납부 이력 관리 |
| Review Cycles | `/review-cycles` | 검토 주기 관리 |
| Reviews | `/reviews`, `/patents/{patentId}/reviews` | Legal 팀 검토 요청과 현황 조회 |
| Business Reviews | `/assigned-patents` | 사업부 검토 현황과 의견 제출 |
| Reports | `/patents/{patentId}/reports` | 평가 보고서 관리 |
| Dashboard | `/dashboard` | Legal 팀 대시보드 |

Swagger UI는 서버 실행 후 `http://localhost:8080/swagger-ui/index.html`에서 확인할 수 있습니다.
OpenAPI JSON은 `http://localhost:8080/v3/api-docs`에서 제공됩니다.

## Getting Started

### 1. 환경 변수 준비

루트 디렉터리의 `.env.example`을 참고하여 `.env` 파일을 생성합니다. `JWT_SECRET`은 Base64 인코딩된 32 byte 이상의 랜덤 키를 사용합니다. `INTERNAL_API_KEY`는 AI Worker가 백엔드 내부 API를 호출할 때 사용하는 공유 secret입니다.

```properties
JWT_SECRET=your-base64-encoded-secret
INTERNAL_API_KEY=your-internal-api-key
SPRING_PROFILES_ACTIVE=local
LOCAL_SEED_PASSWORD=1234

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### 2. 로컬 실행

보고서 생성 요청과 완료 보고서 조회까지 로컬에서 확인하려면 RabbitMQ와 MinIO도 함께 실행되어 있어야 합니다.

처음 한 번, 또는 로컬 DB 파일을 삭제한 뒤 다시 시작할 때 파일 DB를 생성합니다.

```bash
./gradlew h2CreateLocalDb
```

H2 TCP 서버를 실행합니다.

```bash
./gradlew h2Server
```

다른 터미널에서 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

H2 웹 콘솔은 `http://localhost:8082`에서 접속할 수 있습니다.

| 항목 | 값 |
| --- | --- |
| JDBC URL | `jdbc:h2:tcp://localhost/~/skipa` |
| 사용자명 | `sa` |
| 비밀번호 | 빈 값 |

#### IntelliJ IDEA 실행 설정

IntelliJ를 사용하는 경우에는 다음과 같이 세 개의 Run Configuration을 만들 수 있습니다.

| Name | Type / Main class | Program arguments / Settings |
| --- | --- | --- |
| `H2 Create DB` | Application / `org.h2.tools.Shell` | `-url jdbc:h2:file:~/skipa -user sa -password "" -sql "SELECT 1;"` |
| `H2 Server (tcp)` | Application / `org.h2.tools.Server` | `-tcp -tcpPort 9092 -web -webPort 8082` |
| `SkipaBackendApplication (local)` | Spring Boot / `com.skipers.skipa.SkipaBackendApplication` | Working directory: project root |

애플리케이션은 project root의 `.env`를 직접 불러옵니다. IDE에서 별도로 active profile이나 환경 변수 파일을 등록할 필요는 없습니다.

### 3. 배포 환경 실행

```bash
java -jar build/libs/skipa-backend.jar --spring.profiles.active=prod
```

배포 환경에서는 아래 환경 변수를 설정해야 합니다.

```text
DB_HOST=<host>
DB_PORT=5432
DB_NAME=<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>

REDIS_HOST=<host>
REDIS_PORT=6379
REDIS_PASSWORD=<password>

RABBITMQ_HOST=<host>
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=<username>
RABBITMQ_PASSWORD=<password>
REPORT_EXCHANGE=skipa.report.exchange
REPORT_GENERATE_QUEUE=skipa.report.generate
REPORT_GENERATE_ROUTING_KEY=report.generate

MINIO_ENDPOINT=<endpoint>
MINIO_ACCESS_KEY=<access-key>
MINIO_SECRET_KEY=<secret-key>
MINIO_REPORT_BUCKET=skipa-reports
MINIO_REGION=us-east-1
MINIO_PRESIGNED_URL_EXPIRY_SECONDS=600

INTERNAL_API_KEY=<shared-secret-for-ai-worker>
```

### 4. 테스트 실행

```bash
./gradlew test
```

## Convention

이슈 작성 규칙, 브랜치 전략, PR 제목 규칙, 커밋 컨벤션은 팀의 협업 가이드를 따릅니다.
