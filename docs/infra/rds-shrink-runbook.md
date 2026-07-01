# v2 RDS 100GB → 20GB 축소 런북 (prod + staging)

CDK 기본값(`allocatedStorage` 미지정 → 100GiB)으로 부풀려진 v2 RDS를 원본과 동일한 20GB로 축소.
RDS는 in-place 축소·동일이름 복원이 불가하므로 **삭제 후 seed 스냅샷에서 20GB로 재생성**한다.
(삭제는 자동모드 분류기가 막으므로 **사용자가 직접 실행**.)

> 전제: 신규가입 0 확인됨(seed 이후 `users` 추가 없음) → seed 복원 시 유실은 login 기록뿐(무해).
> 계정 `218736972976` / `ap-northeast-2`. 다운타임 허용.

```bash
export AWS_PROFILE=igrus AWS_DEFAULT_REGION=ap-northeast-2
```

## 공통 파라미터
| | prod | staging |
| --- | --- | --- |
| instance | `igrus-web-mysql-rds-v2` | `igrus-web-staging-mysql-rds-v2` |
| seed 스냅샷(20GB) | `igrus-web-mysql-rds-v2seed-20260629` | `igrus-web-staging-mysql-rds-v2seed-20260629` |
| subnet group | `igruswebv2stack-prodrdssubnetgroupd54553f6-hwsxtkf4bqfl` | `igruswebv2stack-stagingrdssubnetgroupa66d1ca6-pzq96lxcexij` |
| param group | `default.mysql8.0` | `default.mysql8.4` |
| backup 보존일 | 3 | 1 |
| 공통 | SG `sg-05c163b511ab48477 sg-02020c9b798510416`, gp2, db.t3.micro, private, multi-az 없음 | |

---

## ① staging (다운타임 무관 — 먼저 실행해 절차 검증)

```bash
# 1) 삭제 (seed 스냅샷 보유 → skip-final)
aws rds delete-db-instance --db-instance-identifier igrus-web-staging-mysql-rds-v2 --skip-final-snapshot
aws rds wait db-instance-deleted --db-instance-identifier igrus-web-staging-mysql-rds-v2

# 2) seed 스냅샷에서 20GB 로 복원
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier igrus-web-staging-mysql-rds-v2 \
  --db-snapshot-identifier igrus-web-staging-mysql-rds-v2seed-20260629 \
  --db-instance-class db.t3.micro \
  --db-subnet-group-name igruswebv2stack-stagingrdssubnetgroupa66d1ca6-pzq96lxcexij \
  --vpc-security-group-ids sg-05c163b511ab48477 sg-02020c9b798510416 \
  --db-parameter-group-name default.mysql8.4 \
  --storage-type gp2 --allocated-storage 20 \
  --no-publicly-accessible --no-multi-az --port 3306
aws rds wait db-instance-available --db-instance-identifier igrus-web-staging-mysql-rds-v2

# 3) 백업 보존일 1 로 맞춤
aws rds modify-db-instance --db-instance-identifier igrus-web-staging-mysql-rds-v2 \
  --backup-retention-period 1 --apply-immediately

# 4) staging 은 ECS desiredCount 0 유지 (중지 상태 그대로). 확인:
aws rds describe-db-instances --db-instance-identifier igrus-web-staging-mysql-rds-v2 \
  --query "DBInstances[0].{storage:AllocatedStorage,status:DBInstanceStatus}" --output table
```

---

## ② prod (다운타임 발생)

```bash
# 0) 앱 중지 (다운타임 시작, 쓰기 차단)
aws ecs update-service --cluster IGRUS-WEB-ECS-Cluster-v2 \
  --service igrus-web-server-ecs-service-v2 --desired-count 0

# 1) 안전 스냅샷 완료 확인 (Claude 가 미리 생성함)
aws rds wait db-snapshot-available --db-snapshot-identifier igrus-web-mysql-rds-v2-preshrink-20260630

# 2) 삭제 (preshrink + seed 스냅샷 2개 보유 → skip-final)
aws rds delete-db-instance --db-instance-identifier igrus-web-mysql-rds-v2 --skip-final-snapshot
aws rds wait db-instance-deleted --db-instance-identifier igrus-web-mysql-rds-v2

# 3) seed 스냅샷에서 20GB 로 복원
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier igrus-web-mysql-rds-v2 \
  --db-snapshot-identifier igrus-web-mysql-rds-v2seed-20260629 \
  --db-instance-class db.t3.micro \
  --db-subnet-group-name igruswebv2stack-prodrdssubnetgroupd54553f6-hwsxtkf4bqfl \
  --vpc-security-group-ids sg-05c163b511ab48477 sg-02020c9b798510416 \
  --db-parameter-group-name default.mysql8.0 \
  --storage-type gp2 --allocated-storage 20 \
  --no-publicly-accessible --no-multi-az --port 3306
aws rds wait db-instance-available --db-instance-identifier igrus-web-mysql-rds-v2

# 4) 백업 보존일 3 으로 맞춤
aws rds modify-db-instance --db-instance-identifier igrus-web-mysql-rds-v2 \
  --backup-retention-period 3 --apply-immediately
aws rds wait db-instance-available --db-instance-identifier igrus-web-mysql-rds-v2

# 5) 앱 재가동 (엔드포인트는 동일 이름이라 그대로 → ECS URL 변경 불필요)
aws ecs update-service --cluster IGRUS-WEB-ECS-Cluster-v2 \
  --service igrus-web-server-ecs-service-v2 --desired-count 1

# 6) 검증 (200 이면 정상)
sleep 90
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://api.igrus.co.kr/api/v1/auth/password/login \
  -H 'Content-Type: application/json' -d '{"studentId":"<본인학번>","password":"<비번>"}'
aws rds describe-db-instances --db-instance-identifier igrus-web-mysql-rds-v2 \
  --query "DBInstances[0].{storage:AllocatedStorage,status:DBInstanceStatus}" --output table
```

---

## ③ CDK 코드 정합 (drift 제거) — 축소 완료 후

`infra/cdk/lib/igrus-web-v2-stack.ts` 의 `DatabaseInstanceFromSnapshot` 에 `allocatedStorage: 20` 추가 후
`npx cdk deploy`. (라이브가 이미 20 이므로 modify(20→20) no-op → drift 0)

## 주의
- 위 작업 **진행 중에는 `cdk deploy` 금지** — 삭제 직후 deploy 하면 CFN 이 코드대로 100GB 재생성하여 충돌.
- 복원본은 **동일 instanceIdentifier** 라 엔드포인트 호스트명이 같음 → ECS 의 `SPRING_DATASOURCE_URL` 변경 불필요.
- 롤백: 문제 시 `igrus-web-mysql-rds-v2-preshrink-20260630`(100GB, 현재 데이터) 에서 복원하면 원상복구.
