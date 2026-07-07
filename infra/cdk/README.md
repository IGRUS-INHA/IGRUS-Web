# IGRUS-Web 인프라 (AWS CDK)

운영(Production) 인프라를 **토씨 하나 같게** 복제한 v2(Fargate+ALB) IaC 에,
**EC2+Caddy 전환(v3)** 이 `MIGRATION_PHASE` 플래그로 구현되어 있다.

> 계정 `218736972976` / 리전 `ap-northeast-2` / 기준 스냅샷 2026-06-29

## EC2 + Caddy 전환 (진행 중)

상시 저부하 워크로드의 Fargate/ALB/IPv4 고정비 제거 (월 ~$175 → ~$47).
`lib/igrus-web-v2-stack.ts` 상단 `MIGRATION_PHASE`(1→2→2.5→3)를 올려가며 배포한다:

| Phase | 내용 |
| --- | --- |
| 1 | EC2 t3.small(`IGRUS-Web-App-EC2`) + Caddy + EIP + `ec2.igrus.co.kr` 병행 프로비저닝. 기존 인프라 유지 |
| 2 | cutover — `api.igrus.co.kr` → EIP, prod Fargate desiredCount 0 (플래그 원복 = 즉시 롤백) |
| 2.5 | cleanup — ALB(+IPv4)/ECS/bastion/staging RDS 제거 (무중단) |
| 3 | prod RDS t4g.micro 전환 (짧은 재부팅) |

근거: `docs/infra/ec2-caddy-migration-rationale.md` / 절차: `docs/infra/ec2-migration-runbook.md`

배포는 태그 릴리즈 시 GitHub Actions 가 ECR push 후 SSM 으로 인스턴스의
`/usr/local/bin/igrus-deploy <tag>` 를 실행한다 (`.github/workflows/backend-prod-cd.yaml`).

## 무엇을 만드나 (운영 → v2)

| 운영 (수동 생성 이름) | 복제본 |
| --- | --- |
| `IGRUS-WEB-ECS-Cluster` | `IGRUS-WEB-ECS-Cluster-v2` |
| `igrus-web-server-ecs-service` (Fargate 1vCPU/2GB ×1) | `igrus-web-server-ecs-service-v2` |
| `igrus-web-server-task-def` | `igrus-web-server-task-def-v2` |
| `igrus-web-mysql-rds` (MySQL 8.0.44, db.t3.micro, 20GB gp2, single-AZ, private) | `igrus-web-mysql-rds-v2` |
| `IGRUS-Web-ALB` (internet-facing, 443) | `IGRUS-Web-ALB-v2` |
| `IGRUS-Web-Spring-ECS-TG` (8080, HC `/` → 200) | `IGRUS-Web-Spring-ECS-TG-v2` |
| `igrus-web-file-storage-bucket`, `igrus-web-staging-file-storage-bucket` | `…-v2` |
| `igrus-web-bucket`(웹/정적, CloudFront 전용) | **복제 안 함** — 백엔드가 접근 안 하므로 v2 불필요, 프론트는 기존 버킷 그대로 사용 |
| `IGRUS-WEB-ALB-SG`, `igrus-web-server-ecs-service-sg`, `IGRUS-Web-MySQL-RDS-SG`, `launch-wizard-1` | `…-v2` |
| `igrus-web-server-prod-ecs-task-role`, `…-execution-role`, `EC2_SSM_ROLE` | `…-v2` |
| `IGRUS-Web-RDS-SSM-EC2` (t3.micro, 8GB gp3) | `IGRUS-Web-RDS-SSM-EC2-v2` |
| (신규) `clone.igrus.co.kr` ACM 인증서 + Route53 A레코드 → ALB-v2 | |

기존 **default VPC**, **default 보안그룹(`sg-05c163b511ab48477`)**, **`igrus.co.kr` Route53 존**은 운영과 동일하게 그대로 참조한다.

## 운영과 동일하게 유지한 것 (의도적으로 안 건드림)

- ECS 태스크 정의의 **환경변수·시크릿 0개** (운영과 동일). 즉 컨테이너 이미지(`igrus/web/spring:v1.1.8`)를 그대로 사용한다.
- 보안그룹 규칙도 운영 그대로 (RDS 3306 의 `0.0.0.0/0` 포함). 정리는 별도 작업 대상.

## 사용법 (실제 배포는 아직 안 함 — 코드만 작성됨)

```bash
cd infra/cdk
npm install

# 자격증명: 프로젝트 루트 .env 의 IGRUS 계정 키 사용 (218736972976)
export AWS_ACCESS_KEY_ID=...        # .env 의 AWS_ACCESS_KEY
export AWS_SECRET_ACCESS_KEY=...    # .env 의 AWS_SECRET
export AWS_DEFAULT_REGION=ap-northeast-2

npm run build      # 타입 체크
npx cdk synth      # CloudFormation 템플릿 생성 (배포 X)
npx cdk diff       # 현재 계정과 차이 확인

# 최초 1회만
npx cdk bootstrap aws://218736972976/ap-northeast-2

npx cdk deploy     # 실제 프로비저닝 (배포 시 월 ~$80 추가)
```

## 참고

- RDS 마스터 자격증명은 CDK 가 Secrets Manager 에 자동 생성한다(`admin` + 랜덤 비번).
- `clone.igrus.co.kr` 인증서는 DNS 검증이며, `igrus.co.kr` 존에 검증 레코드가 자동 추가된다.
- 스택 삭제 시 RDS 는 `SNAPSHOT` 정책으로 마지막 스냅샷을 남긴다.
