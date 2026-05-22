<!--
작성자: 고길훈
작성일: 2026-05-22 (Asia/Seoul)
브랜치: feat/3-departments-api
목적: 오늘 작업한 변경사항을 기록해 공유/추적 가능하게 한다.
-->

# 2026-05-22 작업 정리 (feat/3-departments-api)

## 1) 오늘 한 일 요약

- 부서(사업부) 도메인 기본 골격(Entity/DTO/Repository/Service/Controller) 구현
- 공통 응답 포맷(`success/data/error`) 및 전역 예외 처리 추가
- 목록 API의 페이지 응답을 프론트 친화적인 고정 포맷(`items/page/size/total...`)으로 통일
- 엔티티 공통 생성/수정 일시를 UTC 기준 `Instant`로 기록하도록 변경
- Gradle Java Toolchain(17) 요구사항(JDK 17)을 맞춰 로컬에서 `./gradlew test` 실행 가능 상태로 정리

## 2) 구현/변경 파일

### (A) 공통(Entity Time)
- `src/main/java/com/skipers/skipa/global/common/entity/BaseEntity.java`
  - 모든 엔티티 공통 `created_at/updated_at` 제공
  - 저장/수정 시각을 UTC 기준 `Instant.now(Clock.systemUTC())`로 세팅/갱신

### (B) Department(Entity/DTO/Repository/Service/Controller)
- `src/main/java/com/skipers/skipa/domain/department/domain/Department.java`
  - `departments` 테이블 엔티티(부서명 `name`)
- `src/main/java/com/skipers/skipa/domain/department/dto/request/DepartmentCreateRequest.java`
- `src/main/java/com/skipers/skipa/domain/department/dto/request/DepartmentUpdateRequest.java`
- `src/main/java/com/skipers/skipa/domain/department/dto/response/DepartmentResponse.java`
  - 응답에 `createdAt/updatedAt` 포함(현재 기준)
- `src/main/java/com/skipers/skipa/domain/department/dao/DepartmentRepository.java`
  - 중복 방지/단건 조회/검색 메서드 추가(대소문자 무시 포함)
- `src/main/java/com/skipers/skipa/domain/department/application/DepartmentService.java`
  - 생성/조회/수정/삭제/검색 및 page/size 검색 제공
- `src/main/java/com/skipers/skipa/domain/department/exception/DepartmentNotFoundException.java`
  - 표준 에러 코드 기반 예외(`BusinessException`)로 정리
- `src/main/java/com/skipers/skipa/domain/department/api/DepartmentController.java`
  - 엔드포인트 제공(공통 응답 `ApiResponse`로 래핑)

### (C) 공통 응답/예외 처리
- `src/main/java/com/skipers/skipa/global/response/ApiResponse.java`
  - 공통 응답 래퍼(`success/data/error`)
- `src/main/java/com/skipers/skipa/global/response/ErrorResponse.java`
  - 공통 에러 응답(`code/message`)
- `src/main/java/com/skipers/skipa/global/response/PageResponse.java`
  - 목록 페이지 응답 고정 포맷(`items/page/size/totalItems/totalPages/hasNext/hasPrevious`)
- `src/main/java/com/skipers/skipa/global/exception/ErrorCode.java`
  - 표준 에러 코드 정의(예: `DEPARTMENT_NOT_FOUND`, `DUPLICATE_DEPARTMENT_NAME`)
- `src/main/java/com/skipers/skipa/global/exception/BusinessException.java`
  - `ErrorCode` 기반 표준 비즈니스 예외
- `src/main/java/com/skipers/skipa/global/exception/GlobalExceptionHandler.java`
  - 전역 예외 → `ApiResponse.failure(...)` 변환

### (D) 기타
- `.gitignore`
  - 로컬 파일(`SKIPA Database.png`) 제외 추가

## 3) Department API(현재 구현 기준)

- `POST /departments` : 부서 생성
- `GET /departments/{departmentId}` : 부서 ID 단건 조회
- `GET /departments/by-name?name=...` : 부서명 단건 조회
- `PUT /departments/{departmentId}` : 부서명 수정
- `DELETE /departments/{departmentId}` : 부서 삭제
- `GET /departments?keyword=&page=&size=` : 부서 목록/검색(page/size)
  - 응답 `data`는 `PageResponse<DepartmentResponse>` 고정 포맷

## 4) 실행/검증

- 테스트: `./gradlew test`

## 5) 수동 API 테스트 방법(Departments)

### 5-0. 어디에 입력하나?

- **입력 위치**: VSCode 터미널(또는 macOS Terminal/iTerm)
- **입력 폴더**: 프로젝트 루트(= `gradlew`가 있는 경로, 예: `~/Desktop/skipa-backend`)
- **권장 흐름**
  - 터미널 1: `./gradlew bootRun` 실행(서버 계속 실행)
  - 터미널 2: `curl ...` 명령으로 API 호출

### 5-1. 서버 실행

```bash
./gradlew bootRun
```

확인 포인트
- 서버가 `8080` 포트로 정상 기동되는지 확인

### 5-2. API 호출(curl) & 확인 포인트

공통 확인 포인트
- 성공: `success=true`, `data` 존재, `error=null`
- 실패: `success=false`, `data=null`, `error.code/error.message` 존재

#### (1) 부서 생성(성공)

```bash
curl -i -X POST "http://localhost:8080/departments" \
  -H "Content-Type: application/json" \
  -d '{"name":"사업부A"}'
```

확인 포인트
- HTTP `201`
- `data.name="사업부A"`
- `data.createdAt`, `data.updatedAt` 값 존재(UTC 기준)

#### (2) 부서 생성(중복 → 실패)

```bash
curl -i -X POST "http://localhost:8080/departments" \
  -H "Content-Type: application/json" \
  -d '{"name":"사업부A"}'
```

확인 포인트
- HTTP `409`
- `error.code="DEPARTMENT_409"`

#### (3) 부서 단건 조회(성공)

```bash
curl -i "http://localhost:8080/departments/1"
```

확인 포인트
- HTTP `200`
- `data.id=1`

#### (4) 부서 단건 조회(없는 ID → 실패)

```bash
curl -i "http://localhost:8080/departments/999999"
```

확인 포인트
- HTTP `404`
- `error.code="DEPARTMENT_404"`

#### (5) 부서명 단건 조회

```bash
# 한글 같은 비-ASCII 문자가 포함되면 URL 인코딩이 필요할 수 있음
curl -i -G "http://localhost:8080/departments/by-name" --data-urlencode "name=사업부A"
```

확인 포인트
- HTTP `200`
- `data.name="사업부A"`

#### (6) 부서 목록/검색(page/size)

```bash
curl -i "http://localhost:8080/departments?page=0&size=20"

# 한글 같은 비-ASCII 문자가 포함되면 URL 인코딩이 필요할 수 있음
curl -i -G "http://localhost:8080/departments" \
  --data-urlencode "keyword=사업부" \
  --data-urlencode "page=0" \
  --data-urlencode "size=20"
```

확인 포인트
- `data.items` 배열 존재
- `data.page`, `data.size`, `data.totalItems`, `data.totalPages`, `data.hasNext`, `data.hasPrevious` 존재

## 6) 진행 과정/결과/이슈 정리(상세)

### 6-1. 개발 진행 흐름

1) BaseEntity 공통 컬럼 세팅
- 목표: 모든 엔티티가 `created_at/updated_at`을 공통으로 갖게 함
- 결과: `BaseEntity` 상속만으로 자동 세팅/갱신(UTC 기준) 동작

2) Department 도메인 구현
- Entity → DTO → Repository → Service → Controller 순서로 연결
- 결과: 부서 생성/조회/수정/삭제/검색(page/size) API까지 수동 테스트 가능 상태로 완성

3) 공통 응답/예외 처리 통일
- 목표: 모든 API 응답을 `success/data/error` 구조로 통일
- 결과: 정상/에러 모두 동일한 JSON 구조로 내려가도록 전역 예외 처리 추가

### 6-2. 빌드/테스트 이슈(Gradle Toolchain JDK 17)

증상
- `./gradlew test` 실행 시 아래 오류로 실패
  - `Cannot find a Java installation ... matching: {languageVersion=17 ...}`

원인
- 프로젝트 `build.gradle`에서 Java toolchain을 17로 고정했는데, 로컬에 JDK 17이 설치/인식되지 않음

해결
- Homebrew로 Temurin JDK 17 설치 및 인식 확인
  - 설치: `brew install --cask temurin@17`
  - 설치 과정에서 `sudo` 비밀번호 입력 필요(터미널에서 `Password:` 프롬프트에 macOS 로그인 비밀번호 입력)
  - 설치 확인: `/usr/libexec/java_home -V`
  - 필요 시 터미널에서 전환:
    - `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
    - `export PATH="$JAVA_HOME/bin:$PATH"`

결과
- JDK 17 인식 후 `./gradlew test` 정상 통과

### 6-3. 컴파일 이슈(ErrorResponse 문법 오류)

증상
- `./gradlew test` 중 컴파일 에러 발생
  - `ErrorResponse.java: class, interface, enum, or record expected`

원인
- `ErrorResponse.java` 파일에 닫는 중괄호(`}`)가 1개 더 들어가 문법 오류

해결
- 마지막 불필요한 `}` 제거

결과
- 컴파일 정상화 후 테스트 통과

### 6-4. 수동 API 테스트 중 400(Bad Request) 이슈(한글 파라미터)

증상
- 다음 요청에서 HTTP 400 + Tomcat 기본 HTML 에러 페이지 반환
  - `GET /departments/by-name?name=사업부A`
  - `GET /departments?keyword=사업부&page=0&size=20`

특징(중요)
- 우리 공통 응답(JSON `ApiResponse`)이 아니라 Tomcat 기본 HTML이 내려옴  
  → 애플리케이션(Controller/ExceptionHandler)까지 요청이 도달하기 전에 톰캣 레벨에서 요청 자체가 거절된 상황

원인
- 한글 같은 비-ASCII 문자가 URL에 그대로 포함되어, 환경/설정에 따라 톰캣이 “잘못된 요청”으로 판단할 수 있음

해결
- curl에서 URL 인코딩을 강제해서 요청(권장)
  - 부서명 조회:
    - `curl -i -G "http://localhost:8080/departments/by-name" --data-urlencode "name=사업부A"`
  - 검색:
    - `curl -i -G "http://localhost:8080/departments" --data-urlencode "keyword=사업부" --data-urlencode "page=0" --data-urlencode "size=20"`

결과(실제 확인)
- `GET /departments?page=0&size=20` → 200 OK
- `GET /departments?keyword=사업부&page=0&size=20`(인코딩 버전) → 200 OK
- 응답 예시(요약)
  - `success=true`
  - `data.items[0].name="사업부A"`
  - `data.page=0`, `data.size=20`, `data.totalItems=1`

### 6-5. 현재 API 응답 포맷(고정 포맷)

목록/검색 응답(`GET /departments`)의 `data`는 아래 키로 고정
- `items`: 현재 페이지 아이템 배열
- `page`: 페이지 번호(0부터)
- `size`: 페이지 크기
- `totalItems`: 전체 건수
- `totalPages`: 전체 페이지 수
- `hasNext`, `hasPrevious`: 다음/이전 페이지 존재 여부
