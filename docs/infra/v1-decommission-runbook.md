# v1 폐기(decommission) 런북

v2 로 완전 전환 완료(데이터 복제 + 스냅샷 보유 확인) 후, v1 콘솔 인프라를 삭제한다.
**S3 버킷은 전부 보존**(프론트 `igrus-web-bucket` + 서버 file-storage 버킷 모두). RDS/EC2/ECS 만 삭제.

> 계정 `218736972976` / `ap-northeast-2`. 삭제는 자동모드 분류기가 막으므로 **사용자가 직접 실행**.
> 전제: RDS 데이터는 seed 스냅샷(`*-v2seed-20260629`) + 자동 스냅샷으로 보존됨. v2 로 복제 완료.

```bash
export AWS_PROFILE=igrus
export AWS_DEFAULT_REGION=ap-northeast-2
aws sts get-caller-identity --query Account --output text   # 반드시 218736972976 확인!
```

## 1) ECS 서비스 2개 + 클러스터 삭제
```bash
aws ecs delete-service --cluster IGRUS-WEB-ECS-Cluster \
  --service igrus-web-server-ecs-service --force
aws ecs delete-service --cluster IGRUS-WEB-ECS-Cluster \
  --service igrus-web-server-staging-task-def-service --force
aws ecs delete-cluster --cluster IGRUS-WEB-ECS-Cluster
```

## 2) Bastion EC2 종료(terminate)
```bash
aws ec2 terminate-instances --instance-ids i-035c8933f40c3219a \
  --query "TerminatingInstances[].{id:InstanceId,state:CurrentState.Name}" --output table
```

## 3) RDS 2개 삭제 (스냅샷 이미 보유 → skip-final)
```bash
# v1 prod
aws rds delete-db-instance --db-instance-identifier igrus-web-mysql-rds --skip-final-snapshot
# v1 staging (public) — 삭제 시 묶여있던 EIP 15.164.14.76 도 자동 해제됨
aws rds delete-db-instance --db-instance-identifier igrus-web-staging-mysql-rds --skip-final-snapshot
```
> 더 안전하게 가려면 `--skip-final-snapshot` 대신
> `--final-db-snapshot-identifier igrus-web-mysql-rds-final-20260630` 사용.

## 4) 검증
```bash
# v1 ECS/RDS 사라졌는지
aws ecs list-clusters --query "clusterArns" --output text          # v2 만 남아야
aws rds describe-db-instances --query "DBInstances[].DBInstanceIdentifier" --output text  # -v2 만 남아야
aws ec2 describe-instances --filters "Name=tag:Name,Values=IGRUS-Web-RDS-SSM-EC2" \
  --query "Reservations[].Instances[].State.Name" --output text     # terminated
# 실서비스 정상 확인
curl -s -o /dev/null -w "%{http_code}\n" https://api.igrus.co.kr/actuator/health
```

## 보존 목록 (삭제 금지)
- 프론트: `igrus-web-bucket` + CloudFront
- S3 서버 버킷 전부: `igrus-web-file-storage-bucket`(v1), `*-v2` 등 — **이번엔 전부 유지**
- v2 전체 (ECS-Cluster-v2, RDS-v2, ALB-v2, bastion-v2)
- 공유: ECR, Secrets Manager, Route53 zone, default VPC/SG

## 비용 영향 (삭제 완료 시)
- v1 RDS 2개 스토리지 -$4.6/mo, bastion EBS -$0.8/mo, EIP -$3.6/mo
- (ALB-v1 -$18/mo 은 이미 삭제 반영됨)
