# Docker 로컬 개발 환경 가이드

## 개요

Docker Compose를 사용하여 로컬에 Java, Gradle, Node.js, MySQL 등을 직접 설치하지 않고 개발 환경을 구축할 수 있습니다.

**백엔드 개발 환경**과 **프론트엔드 개발 환경**이 별도로 제공됩니다.

## 사전 요구사항

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치
- Git (소스 코드 클론용)

## 백엔드 개발 환경

백엔드 개발자를 위한 환경입니다. MySQL과 Spring Boot를 도커 컨테이너에서 실행합니다.

### 컨테이너 구성

| 서비스 | 이미지 | 설명 | 호스트 포트 |
|--------|--------|------|-------------|
| `mysql` | mysql:8.0 | MySQL 데이터베이스 (utf8mb4) | 3307 |
| `backend` | eclipse-temurin:21-jdk | Spring Boot (Gradle bootRun) | 8080 |

### 실행 방법

```bash
# 1. 환경변수 파일 생성 (최초 1회)
cp backend/.env.example backend/.env

# 2. 컨테이너 실행
docker compose -f backend/docker-compose.dev.yml up

# 백그라운드 실행 시
docker compose -f backend/docker-compose.dev.yml up -d
```

### 접속 확인

- Spring Boot API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- MySQL: `localhost:3307` (사용자: `igrus`, 비밀번호: `igrus1234`, 데이터베이스: `igrus-web`)

### 코드 변경 반영

소스 코드가 볼륨 마운트되어 있어, 로컬에서 코드를 수정하면 컨테이너 안에 바로 반영됩니다. 단, Java 소스 변경 시 **자동 재컴파일은 되지 않으므로** 컨테이너를 재시작해야 합니다:

```bash
docker compose -f backend/docker-compose.dev.yml restart backend
```

### 종료

```bash
# 컨테이너 종료 (데이터 유지)
docker compose -f backend/docker-compose.dev.yml down

# 컨테이너 종료 + 데이터(MySQL, Gradle 캐시) 삭제
docker compose -f backend/docker-compose.dev.yml down --volumes
```

---

## 프론트엔드 개발 환경

프론트엔드 개발자를 위한 환경입니다. MySQL, Spring Boot 백엔드, Vite 개발 서버를 모두 도커 컨테이너에서 실행합니다. Java나 Gradle 설치가 필요 없습니다.

### 컨테이너 구성

| 서비스 | 이미지 | 설명 | 호스트 포트 |
|--------|--------|------|-------------|
| `mysql` | mysql:8.0 | MySQL 데이터베이스 (utf8mb4) | 3307 |
| `backend` | Dockerfile.dev 빌드 | Spring Boot (JAR 실행) | 8080 |
| `frontend` | node:22-alpine | Vite 개발 서버 (HMR) | 5173 |

### 실행 방법

```bash
# 1. 환경변수 파일 생성 (최초 1회)
cp frontend/.env.example frontend/.env

# 2. 컨테이너 실행 (최초 실행 시 백엔드 빌드로 시간이 소요됩니다)
docker compose -f frontend/docker-compose.dev.yml up

# 백그라운드 실행 시
docker compose -f frontend/docker-compose.dev.yml up -d
```

### 접속 확인

- 프론트엔드 (Vite): http://localhost:5173
- 백엔드 API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### HMR (Hot Module Replacement)

프론트엔드 소스 코드가 볼륨 마운트되어 있어, 로컬에서 `frontend/src/` 코드를 수정하면 Vite HMR이 즉시 반영합니다.

백엔드는 빌드된 JAR로 실행되므로 백엔드 코드 변경 시 컨테이너를 재빌드해야 합니다:

```bash
docker compose -f frontend/docker-compose.dev.yml up -d --build backend
```

### 종료

```bash
# 컨테이너 종료 (데이터 유지)
docker compose -f frontend/docker-compose.dev.yml down

# 컨테이너 종료 + 데이터(MySQL, node_modules) 삭제
docker compose -f frontend/docker-compose.dev.yml down --volumes
```

---

## 환경변수

### `backend/.env.example`

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_USERNAME` | `igrus` | MySQL 사용자명 |
| `DB_PASSWORD` | `igrus1234` | MySQL 비밀번호 |
| `DB_ROOT_PASSWORD` | `root` | MySQL root 비밀번호 |
| `SPRING_ACTIVE_PROFILE` | `local` | Spring 프로파일 |

### `frontend/.env.example`

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `VITE_API_URL` | `http://localhost:8080` | 백엔드 API 주소 |
| `DB_USERNAME` | `igrus` | MySQL 사용자명 (docker-compose에서 사용) |
| `DB_PASSWORD` | `igrus1234` | MySQL 비밀번호 (docker-compose에서 사용) |
| `DB_ROOT_PASSWORD` | `root` | MySQL root 비밀번호 (docker-compose에서 사용) |

---

## 관련 파일 구조

```
IGRUS-Web/
├── backend/
│   ├── docker-compose.dev.yml   # 백엔드 개발 환경 (MySQL + Spring Boot)
│   ├── Dockerfile.dev           # 프론트엔드 환경에서 백엔드 빌드용
│   ├── Dockerfile               # 프로덕션 배포용 (변경 없음)
│   └── .env.example             # 백엔드 환경변수 템플릿
├── frontend/
│   ├── docker-compose.dev.yml   # 프론트엔드 개발 환경 (MySQL + Backend + Vite)
│   └── .env.example             # 프론트엔드 환경변수 템플릿
└── docs/
    └── setup/
        └── docker-dev-guide.md  # 이 문서
```

---

## 트러블슈팅

### 포트 충돌

로컬에 MySQL이 이미 실행 중이면 3307 포트도 충돌할 수 있습니다. 이 경우 로컬 MySQL을 중지하거나, `docker-compose.dev.yml`에서 포트를 변경하세요:

```yaml
ports:
  - "3308:3306"  # 호스트 포트를 3308로 변경
```

8080 포트도 마찬가지로 다른 서비스와 충돌할 수 있습니다.

### 볼륨 초기화

DB 스키마가 꼬이거나 초기 상태로 돌아가고 싶을 때:

```bash
# 모든 볼륨 삭제 후 재시작
docker compose -f backend/docker-compose.dev.yml down --volumes
docker compose -f backend/docker-compose.dev.yml up
```

### 백엔드 빌드 실패 (프론트엔드 환경)

`Dockerfile.dev`로 백엔드 빌드 시 실패하면 캐시를 무시하고 재빌드하세요:

```bash
docker compose -f frontend/docker-compose.dev.yml build --no-cache backend
docker compose -f frontend/docker-compose.dev.yml up
```

### 두 환경을 동시에 실행할 수 없음

백엔드 개발 환경과 프론트엔드 개발 환경은 동일한 포트(3307, 8080)를 사용하므로 동시에 실행할 수 없습니다. 하나를 종료한 후 다른 환경을 실행하세요.
