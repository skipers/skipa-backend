# SKIPA Kubernetes 설정

이 디렉토리는 SKIPA 백엔드에서 사용할 PostgreSQL, Redis Kubernetes manifest를 관리합니다.

## Namespace

```text
skala3-finalproj-class2-team8
```

## 리소스 구성

### PostgreSQL

- Service: `skipa-postgres`
- Headless Service: `skipa-postgres-headless`
- StatefulSet: `skipa-postgres`
- Secret: `skipa-postgres-secret`
- Port: `5432`
- StorageClass: `gp3`
- Storage: `2Gi`

백엔드 내부 접속 주소:

```text
skipa-postgres:5432
```

### Redis

- Service: `skipa-redis`
- Headless Service: `skipa-redis-headless`
- StatefulSet: `skipa-redis`
- Secret: `skipa-redis-secret`
- Port: `6379`
- StorageClass: `gp3`
- Storage: `1Gi`

백엔드 내부 접속 주소:

```text
skipa-redis:6379
```

## Service와 StatefulSet 역할

### Service

Kubernetes에서 Pod는 재시작되면 IP가 바뀔 수 있습니다.  
Service는 이런 Pod 앞에 고정된 내부 접속 주소를 제공합니다.

백엔드 서버는 PostgreSQL Pod나 Redis Pod의 IP를 직접 바라보지 않고, 아래 Service 주소로 접근합니다.

```text
PostgreSQL: skipa-postgres:5432
Redis: skipa-redis:6379
```

### Headless Service

StatefulSet이 각 Pod를 안정적으로 식별하기 위해 사용하는 Service입니다.  
일반적인 백엔드 접속용 주소로는 `skipa-postgres`, `skipa-redis` Service를 사용합니다.

### StatefulSet

PostgreSQL, Redis처럼 데이터를 저장하는 애플리케이션은 Pod가 재시작되어도 같은 저장소를 다시 사용해야 합니다.  
StatefulSet은 고정된 Pod 이름과 PVC를 기반으로 상태가 있는 애플리케이션을 안정적으로 실행합니다.

예시:

```text
skipa-postgres-0
skipa-redis-0
```

## Secret 생성

실제 비밀번호 값은 GitHub에 커밋하지 않습니다.  
아래 명령어로 클러스터에 직접 Secret을 생성합니다.

### PostgreSQL Secret

```bash
POSTGRES_PASSWORD=$(openssl rand -hex 24)

kubectl create secret generic skipa-postgres-secret \
  --from-literal=username="skipa" \
  --from-literal=password="$POSTGRES_PASSWORD" \
  --from-literal=database="skipa"
```

### Redis Secret

```bash
REDIS_PASSWORD=$(openssl rand -hex 24)

kubectl create secret generic skipa-redis-secret \
  --from-literal=password="$REDIS_PASSWORD"
```

## Manifest 적용 순서

```bash
kubectl apply -f infra/k8s/postgres-service.yml
kubectl apply -f infra/k8s/postgres-statefulset.yml
kubectl apply -f infra/k8s/redis-service.yml
kubectl apply -f infra/k8s/redis-statefulset.yml
```

## 상태 확인

```bash
kubectl get pods
kubectl get svc
kubectl get pvc
```

정상 실행 예시:

```text
skipa-postgres-0   1/1   Running
skipa-redis-0      1/1   Running
```

PVC 예시:

```text
postgres-data-skipa-postgres-0   Bound   2Gi
redis-data-skipa-redis-0         Bound   1Gi
```

## 백엔드 환경변수 예시

```text
DB_HOST=skipa-postgres
DB_PORT=5432
DB_NAME=skipa
DB_USERNAME=skipa
DB_PASSWORD=<PostgreSQL Secret password>

REDIS_HOST=skipa-redis
REDIS_PORT=6379
REDIS_PASSWORD=<Redis Secret password>
```

Spring Boot 예시:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
```

## 주의사항

- Secret 값은 GitHub에 커밋하지 않습니다.
- `.env` 파일은 GitHub에 커밋하지 않습니다.
- PVC를 삭제하면 연결된 실제 볼륨 데이터도 삭제될 수 있으므로 주의합니다.
- `gp3` StorageClass의 ReclaimPolicy가 `Delete`이므로 PVC 삭제 시 데이터가 함께 삭제될 수 있습니다.
