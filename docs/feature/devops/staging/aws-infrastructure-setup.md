# AWS 스테이징 인프라 구축 가이드

## 개요

프로덕션과 동일한 AWS 계정에 별도 리소스로 스테이징 환경을 구축한다.

## 1. 데이터베이스 (RDS)

기존 프로덕션 RDS 인스턴스에 별도 스키마를 생성한다.

```sql
-- 프로덕션 RDS 인스턴스에 접속
CREATE DATABASE igrus_web_staging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- staging 전용 사용자 생성 (선택사항, 보안 강화)
CREATE USER 'igrus_staging'@'%' IDENTIFIED BY '<PASSWORD>';
GRANT ALL PRIVILEGES ON igrus_web_staging.* TO 'igrus_staging'@'%';
FLUSH PRIVILEGES;
```

> Flyway가 첫 배포 시 자동으로 마이그레이션을 실행하므로 테이블은 별도로 생성하지 않아도 된다.

## 2. Secrets Manager

프로덕션 시크릿(`igrus/web/server/prod`)과 동일한 구조로 staging 시크릿을 생성한다.

**시크릿 이름**: `igrus/web/server/staging`

**필수 키**:
| 키 | 설명 |
|---|---|
| `spring.datasource.username` | staging DB 사용자명 |
| `spring.datasource.password` | staging DB 비밀번호 |
| `app.jwt.secret` | JWT 서명 시크릿 (프로덕션과 다른 값 사용) |
| `spring.mail.username` | 메일 발송 계정 (프로덕션과 동일 가능) |
| `spring.mail.password` | 메일 발송 비밀번호 |

> JWT 시크릿은 반드시 프로덕션과 다른 값을 사용하여 환경 간 토큰이 호환되지 않도록 한다.

## 3. ECR (Elastic Container Registry)

staging Docker 이미지 저장소를 생성한다.

```bash
aws ecr create-repository \
  --repository-name igrus-web-spring-staging \
  --region ap-northeast-2
```

또는 프로덕션 ECR 리포지토리를 공유하고 태그로 구분할 수도 있다 (별도 리포 권장).

## 4. ECS (Elastic Container Service)

### 4.1 Task Definition

프로덕션 Task Definition을 복제하여 staging용으로 생성한다.

주요 변경점:
- Task Definition 이름: `igrus-web-staging-task` (또는 프로젝트 규칙에 맞게)
- 컨테이너 환경변수:
  ```json
  {
    "name": "SPRING_ACTIVE_PROFILE",
    "value": "staging"
  }
  ```
- 리소스(CPU/메모리): 프로덕션보다 낮게 설정 가능 (비용 절감)

### 4.2 ECS Service

```bash
aws ecs create-service \
  --cluster <CLUSTER_NAME> \
  --service-name igrus-web-staging-service \
  --task-definition igrus-web-staging-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<SUBNET_IDS>],securityGroups=[<SG_IDS>],assignPublicIp=ENABLED}"
```

> 스테이징은 `desired-count: 1`로 충분하다 (비용 절감).

### 4.3 ALB (Application Load Balancer)

- 기존 ALB에 새 타겟 그룹 추가 또는 별도 ALB 생성
- 호스트 기반 라우팅: `staging-api.igrus.co.kr` → staging 타겟 그룹

## 5. S3 (프론트엔드)

```bash
aws s3 mb s3://igrus-web-staging-frontend --region ap-northeast-2
```

S3 버킷 정적 웹사이트 호스팅 설정:
- 인덱스 문서: `index.html`
- 오류 문서: `index.html` (SPA 라우팅용)

## 6. CloudFront

프로덕션 CloudFront 배포를 복제하여 staging용으로 생성한다.

주요 설정:
- Origin: staging S3 버킷
- Alternate domain name (CNAME): `staging.igrus.co.kr`
- SSL 인증서: `*.igrus.co.kr` 와일드카드 인증서 또는 staging 전용 인증서
- Default root object: `index.html`
- Custom error response: 403/404 → `/index.html` (200) (SPA 라우팅용)

## 7. ACM (SSL 인증서)

`*.igrus.co.kr` 와일드카드 인증서가 이미 있다면 그대로 사용 가능.

없다면 생성:
```bash
aws acm request-certificate \
  --domain-name "*.igrus.co.kr" \
  --validation-method DNS \
  --region us-east-1  # CloudFront용은 반드시 us-east-1
```

> CloudFront에서 사용하는 인증서는 반드시 `us-east-1` 리전에 있어야 한다.

## 8. Route53 (DNS)

| 레코드 | 타입 | 값 |
|---|---|---|
| `staging.igrus.co.kr` | A (Alias) | staging CloudFront 배포 도메인 |
| `staging-api.igrus.co.kr` | A (Alias) | staging ALB 또는 ECS 엔드포인트 |

## 9. IAM Role (GitHub Actions OIDC)

기존 프로덕션 GHA OIDC Role을 공유하거나, staging 전용 Role을 생성한다.

필요한 권한:
- `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage` 등
- `ecs:UpdateService`, `ecs:DescribeTaskDefinition`, `ecs:RegisterTaskDefinition`
- `ecs:DescribeServices`

## 10. 체크리스트

- [ ] RDS staging 스키마 생성 완료
- [ ] Secrets Manager `igrus/web/server/staging` 생성 완료
- [ ] ECR staging 리포지토리 생성 완료
- [ ] ECS Task Definition 생성 완료
- [ ] ECS Service 생성 완료
- [ ] ALB staging 타겟 그룹 설정 완료
- [ ] S3 staging 버킷 생성 완료
- [ ] CloudFront staging 배포 생성 완료
- [ ] ACM 인증서 확인/생성 완료
- [ ] Route53 DNS 레코드 설정 완료
- [ ] IAM Role 권한 확인 완료
