# GitHub Environment 설정 가이드

## 개요

스테이징 배포를 위해 GitHub Environment `staging`을 생성하고, 필요한 Variables/Secrets를 설정한다.

## 1. staging Environment 생성

1. GitHub 리포지토리 → Settings → Environments
2. "New environment" 클릭
3. 이름: `staging` 입력 후 "Configure environment" 클릭

## 2. Environment Variables 설정 (Backend)

`backend-staging-cd.yaml`과 `backend-prod-cd.yaml`에서 `vars.*`로 참조하는 변수들이다.
`env:` 블록이 job 내부(`environment:` 아래)에 위치하므로, environment 레벨 변수가 우선 적용된다.
같은 값이면 repo 레벨에 설정해도 되고, 환경별로 다른 값은 반드시 environment 레벨에 설정해야 한다.

### staging Environment Variables

| Variable | 값 | 설명 | repo 레벨 공유 가능 |
|---|---|---|---|
| `ECR_REPOSITORY_SPRING` | *(staging ECR 리포 이름)* | Docker 이미지 저장소 | X (환경별 다름) |
| `CONTAINER_NAME_SPRING` | *(staging 컨테이너명)* | ECS 컨테이너 이름 | X (환경별 다름) |
| `ECS_SERVICE_SPRING` | *(staging ECS 서비스명)* | ECS 서비스 이름 | X (환경별 다름) |
| `ECS_TASK_DEFINITION_NAME_SPRING` | *(staging task definition명)* | ECS Task Definition 이름 | X (환경별 다름) |
| `SPRING_ACTIVE_PROFILE` | `staging` | Spring 활성 프로필 | X (환경별 다름) |
| `AWS_REGION` | `ap-northeast-2` | AWS 리전 | O (동일) |
| `ECS_CLUSTER` | *(ECS 클러스터명)* | ECS 클러스터 | O (동일 클러스터 공유 시) |
| `AWS_BACKEND_GITHUB_ACTIONS_ROLE_ARN` | *(GHA OIDC IAM Role ARN)* | GitHub Actions AWS 접근용 IAM Role | O (동일 Role 공유 시) |

### production Environment Variables

기존 repo 레벨 변수를 production environment로 이관한다:

| Variable | 값 | 설명 |
|---|---|---|
| `ECR_REPOSITORY_SPRING` | *(프로덕션 ECR 리포 이름)* | Docker 이미지 저장소 |
| `CONTAINER_NAME_SPRING` | *(프로덕션 컨테이너명)* | ECS 컨테이너 이름 |
| `ECS_SERVICE_SPRING` | *(프로덕션 ECS 서비스명)* | ECS 서비스 이름 |
| `ECS_TASK_DEFINITION_NAME_SPRING` | *(프로덕션 task definition명)* | ECS Task Definition 이름 |
| `SPRING_ACTIVE_PROFILE` | `prod` | Spring 활성 프로필 |

> **중요**: `env:` 블록이 workflow 레벨이 아닌 job 레벨에 있어야 environment 변수가 올바르게 해석된다.
> repo 레벨과 environment 레벨에 같은 이름의 변수가 있으면 environment 레벨이 우선 적용된다.

## 3. Environment Secrets 설정 (Frontend)

`frontend-staging-cd.yaml`에서 `secrets.*`로 참조하는 시크릿들:

| Secret | 값 | 설명 |
|---|---|---|
| `VITE_API_URL` | `https://staging-api.igrus.co.kr` | 프론트엔드 API 요청 URL |
| `AWS_ACCESS_KEY_ID` | *(staging AWS Access Key)* | AWS 인증 |
| `AWS_SECRET_ACCESS_KEY` | *(staging AWS Secret Key)* | AWS 인증 |
| `AWS_REGION` | `ap-northeast-2` | AWS 리전 |
| `S3_BUCKET_NAME` | *(staging S3 버킷명)* | 프론트엔드 정적 파일 배포 대상 |
| `CF_DISTRIBUTION_ID` | *(staging CloudFront 배포 ID)* | CloudFront 캐시 무효화 대상 |

## 4. 기존 production 시크릿 이관

현재 `frontend-prod-cd.yaml`은 repo-level secrets를 사용 중이다. 환경 분리를 위해:

1. GitHub 리포지토리 → Settings → Environments → `production`
2. 위 Frontend Secrets와 동일한 키로 프로덕션 값 설정:
   - `VITE_API_URL`: `https://api.igrus.co.kr`
   - `AWS_ACCESS_KEY_ID`: 프로덕션 AWS Access Key
   - `AWS_SECRET_ACCESS_KEY`: 프로덕션 AWS Secret Key
   - `AWS_REGION`: `ap-northeast-2`
   - `S3_BUCKET_NAME`: 프로덕션 S3 버킷명
   - `CF_DISTRIBUTION_ID`: 프로덕션 CloudFront 배포 ID
3. `frontend-prod-cd.yaml`에 `environment: production` 추가 완료 확인
4. 기존 repo-level secrets는 production environment로 이관 후 삭제

## 5. 검증

- staging 워크플로우를 수동 실행하여 Variables/Secrets가 올바르게 읽히는지 확인
- 워크플로우 실행 로그에서 환경 이름이 `staging`으로 표시되는지 확인
