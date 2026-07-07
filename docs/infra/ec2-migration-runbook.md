# EC2 + Caddy 전환 런북 (Fargate+ALB → 단일 t3.small)

근거·비용·목표 아키텍처: `docs/infra/ec2-caddy-migration-rationale.md` (옵션 A).
구현: `infra/cdk/lib/igrus-web-v2-stack.ts` 의 **`MIGRATION_PHASE` 상수(1→2→2.5→3)** 를 올려가며 배포한다.

> 계정 `218736972976` / `ap-northeast-2`. `cdk deploy` 는 자동모드 가드가 막으므로 **사용자가 직접 실행**.
> 각 단계는 독립적으로 롤백 가능: 플래그를 이전 값으로 되돌려 재배포하면 원복된다(phase 2.5 부터는 재생성 소요 — 아래 참조).

```bash
cd infra/cdk
export AWS_PROFILE=igrus AWS_DEFAULT_REGION=ap-northeast-2
npm install && npm run build
```

---

## Phase 1 — 병행 프로비저닝 (무중단)

`MIGRATION_PHASE = 1` (현재 코드 상태). 기존 Fargate+ALB 는 그대로 두고 추가만 한다:

- **EC2 t3.small** `IGRUS-Web-App-EC2` (AL2023, 30GB gp3 암호화, IMDSv2 hop-limit 2)
- **user-data**: swap 5GB(`vm.swappiness=10`) + Docker/compose + ECR credential helper +
  `/opt/igrus/{docker-compose.yml,Caddyfile,.env}` + `/usr/local/bin/igrus-deploy` + `docker compose up -d`
- **Caddy**(컨테이너): `api.igrus.co.kr`, `ec2.igrus.co.kr` 자동 Let's Encrypt + `app:8080` 리버스 프록시
- **app**(컨테이너): 동일 ECR 이미지, `SPRING_ACTIVE_PROFILE=prod`, v2 RDS datasource, `-Xmx1g`
- **EIP 1개** + 검증용 A레코드 `ec2.igrus.co.kr`
- IAM 롤 `IGRUS-Web-App-EC2-ROLE`: SSM + ECR pull + S3 + prod 시크릿 read (기존 taskRole 동등)
- RDS SG 에 EC2 SG 3306 인바운드 추가

```bash
npx cdk deploy
```

### 검증
```bash
# 부팅 + user-data(도커 설치·이미지 pull·앱 기동) 3~5분 대기 후
curl -s -o /dev/null -w "%{http_code}\n" https://ec2.igrus.co.kr/   # 200 (actuator 는 prod 미노출 → / 사용)

# 실동작: 로그인 (EC2 경로가 prod RDS/시크릿/S3 를 정상 사용하는지)
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://ec2.igrus.co.kr/api/v1/auth/password/login \
  -H 'Content-Type: application/json' -d '{"studentId":"<학번>","password":"<비번>"}'

# 문제 시 인스턴스 진입 (bastion 불필요 — SSM)
aws ssm start-session --target <AppEc2InstanceId 출력값>
#   sudo cat /var/log/cloud-init-output.log      # user-data 로그
#   sudo docker ps / sudo docker logs igrus-app-1
```

> ⚠️ 이 시점에 EC2 앱도 **prod RDS 에 실제로 붙는다** (Fargate 와 동일 DB 병행 — 스케줄러 중복 실행이
> 걱정되면 검증 후 cutover 까지의 간격을 짧게 유지할 것).
> ⚠️ Caddy 는 `api.igrus.co.kr` 인증서 발급을 DNS cutover 전까지 실패-재시도(백오프)한다. 정상이다.

### GitHub Actions 롤 권한 추가 (phase 2 전 필수, 1회)
`AWS_BACKEND_GITHUB_ACTIONS_ROLE_ARN` 롤에 인라인 정책 추가 (SSM 배포용):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "ec2:DescribeInstances", "Resource": "*" },
    { "Effect": "Allow", "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:ap-northeast-2:218736972976:instance/*",
        "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript"
      ],
      "Condition": { "StringEquals": { "ssm:resourceTag/Name": "IGRUS-Web-App-EC2" } } },
    { "Effect": "Allow", "Action": "ssm:GetCommandInvocation", "Resource": "*" }
  ]
}
```
> `ssm:resourceTag` 조건은 instance ARN 에만 유효하다. document ARN 쪽 조건 불일치로 거부되면
> 조건을 instance statement 로 분리할 것.

---

## Phase 2 — cutover (수 분 내 완료, 트래픽 ≈0)

`MIGRATION_PHASE = 2` 로 수정 후:

```bash
npx cdk deploy
```
변경: `api.igrus.co.kr` A레코드 **ALB alias → EIP (TTL 60s)**, prod Fargate `desiredCount 1 → 0`.

```bash
# DNS 전파 확인 (EIP 가 나와야)
dig +short api.igrus.co.kr

# Caddy 인증서 즉시 재발급 (백오프 대기 없이)
aws ssm send-command --document-name AWS-RunShellScript \
  --targets "Key=tag:Name,Values=IGRUS-Web-App-EC2" \
  --parameters 'commands=cd /opt/igrus && docker compose restart caddy'

# 검증
curl -s -o /dev/null -w "%{http_code}\n" https://api.igrus.co.kr/   # 200
```

이후 **prod CD 파이프라인 변경(PR)을 머지**한다 (`.github/workflows/backend-prod-cd.yaml`
— ECS 배포 → ECR push + SSM `igrus-deploy`). cutover 전에 머지하면 릴리즈가 EC2 에만 반영되므로 순서 주의.

### 롤백 (수 분)
`MIGRATION_PHASE = 1` 로 되돌려 `npx cdk deploy` → api 레코드가 ALB alias 로, Fargate desired 1 로 원복.

---

## Phase 2.5 — cleanup: 미사용 레거시 제거 (무중단)

`MIGRATION_PHASE = 2.5` 로 수정 후:

```bash
npx cdk deploy
```
제거: ALB(+리스너/TG/clone 인증서/clone·staging-clone·staging-api 레코드, **퍼블릭 IPv4 4개**),
ECS 클러스터/서비스/태스크데프, ECS·ALB·bastion SG 와 RDS SG 의 해당 인바운드, task/execution/SSM IAM 롤,
**bastion EC2** (앱 EC2 의 SSM 이 대체), **staging RDS** (RemovalPolicy=SNAPSHOT → 최종 스냅샷 자동 보존).
라이브 경로(EC2→prod RDS)는 무변경 → 무중단. 비용 ~$105 → **~$50/월**.

주의:
- 이 단계부터 플래그 원복만으로 즉시 롤백되지 않는다(ALB/ECS 재생성 ~20분, staging RDS 는 스냅샷에서 재생성).
- CloudWatch 로그 그룹(`/ecs/...`)은 RETAIN 이라 보존된다.
- **staging CD**(`backend-staging-cd.yaml`)는 배포 대상(ECS)이 없어져 자동 트리거를 제거했다(workflow_dispatch 만 잔존) —
  staging 을 다시 쓸 때 EC2 방식으로 재설계할 것.
- GitHub `production` environment 의 ECS 관련 vars(`ECS_CLUSTER`, `ECS_SERVICE_SPRING`,
  `ECS_TASK_DEFINITION_NAME_SPRING`, `CONTAINER_NAME_SPRING`)는 더 이상 사용되지 않는다(정리 가능).

---

## Phase 3 — prod RDS ARM 전환 (짧은 다운타임)

`MIGRATION_PHASE = 3` 으로 수정 후 `npx cdk deploy`.
변경: prod RDS `db.t3.micro → db.t4g.micro` (ARM, −10%, **재부팅 수 분 발생** — 저트래픽 시간대 권장).

### 검증 & 비용 확인
```bash
aws elbv2 describe-load-balancers --query "LoadBalancers[].LoadBalancerName"   # ALB-v2 없어야
aws ecs list-clusters --query clusterArns                                      # v2 클러스터 없어야
aws rds describe-db-instances \
  --query "DBInstances[].{id:DBInstanceIdentifier,class:DBInstanceClass}"      # prod 만, t4g.micro
curl -s -o /dev/null -w "%{http_code}\n" https://api.igrus.co.kr/
```
목표: 월 ~$175 → **~$47** (EC2 $19.3 + EBS $2.7 + EIP $3.7 + RDS t4g.micro $20 + ECR/Secrets ~$1.7).
다음 달 Cost Explorer 에서 ECS/ALB/VPC-IPv4 라인이 사라졌는지 확인.

---

## 운영 메모 (전환 후)

- **배포**: 태그 push(v*) → CI 가 ECR push 후 SSM 으로 `igrus-deploy <tag>` 실행 (컨테이너 재기동 ~60초 다운타임,
  트래픽 ≈0 이라 수용. 무중단이 필요해지면 blue-green 을 후속 도입).
- **인스턴스 교체**: user-data 수정 시 인스턴스가 교체된다(`userDataCausesReplacement`). EIP/DNS 는 유지되고
  Caddy 인증서는 재발급된다(Let's Encrypt 중복 발급 한도 주 5회 유의).
- **장애 복구**: EC2 는 시스템 장애 시 simplified auto-recovery 로 자동 복구. 심각 시 스택 재배포로 재현.
- **RDS 접속**: `aws ssm start-session --target <앱 EC2>` 후 mysql 클라이언트 또는 포트포워딩 (bastion 대체).
- **모니터링(후속 과제)**: CloudWatch 에 `mem_used_percent`/swap 알람 추가 검토 (rationale §7).
- **보안(미해결)**: `MEMO.md` 의 평문 IAM 키 rotate + 삭제 (rationale §7 경고, git 미추적이어도 로컬 유출 리스크).

## 관련 문서
- `docs/infra/ec2-caddy-migration-rationale.md` — 의사결정 근거·비용·트레이드오프
- `docs/infra/v1-decommission-runbook.md` — v1 자원 삭제 (선행 작업)
- `docs/infra/rds-shrink-runbook.md` — RDS 20GB 축소 (선행 작업)
- `infra/cdk/README.md` — CDK 사용법
