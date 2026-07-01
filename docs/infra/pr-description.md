# PR: IGRUS-Web 인프라 IaC화(CDK v2) + 운영 cutover + 비용 최적화

> `infra` → `main` PR 본문 초안. (account 218736972976 / ap-northeast-2)

## 개요
콘솔로 수동 생성돼 있던 IGRUS-Web 운영/스테이징 인프라를 **AWS CDK(TypeScript)로 IaC화**하고,
실제 운영 도메인을 v2로 **cutover**한 뒤, 불필요 자원을 **정리·축소**한 작업입니다.

## 변경 범위 (코드)
- 신규 CDK 프로젝트 `infra/cdk/` (단일 스택 `IgrusWebV2Stack`)
- 운영 문서 `docs/infra/` (다이어그램 + 런북 4종 + 본 PR 문서)
- 루트 `CLAUDE.md`/`.gitignore` 규칙 보강

## 주요 변경 (커밋 8개)
| 영역 | 내용 |
| --- | --- |
| **CDK 스택** | ECS(Fargate prod/staging) · RDS(스냅샷 복원) · ALB(3 리스너) · TG · S3 file-storage · Bastion · IAM · SG · ACM · Route53 전부 코드화. 수동 생성물 이름엔 `-v2` 접미사 |
| **운영 cutover** | `api`/`staging-api.igrus.co.kr` Route53 + 운영 인증서(SNI)를 v2 ALB로 편입 (코드 관리 전환) |
| **ECS 안정화** | healthCheckGracePeriod 240s, circuitBreaker(rollback), minHealthy 100% — 배포 중 무중단·실패 시 자동 롤백 |
| **S3 정리** | 웹 정적 버킷(`igrus-web-bucket`) v2 복제 제외 — CloudFront 전용이라 백엔드 무관. file-storage 버킷만 복제 |
| **비용 최적화** | v2 RDS `allocatedStorage:20` 명시(CDK 기본값 100 → 20GB 축소), staging `desiredCount:0`(중지) |
| **문서** | reconcile / rds-shrink / v1-decommission 런북 + EC2-Caddy 전환 검토 |

## 현재 AWS 상태 (작업 후)
- ✅ 실서비스: v2 prod (ECS 1 + RDS 20GB + ALB-v2), `api.igrus.co.kr` 200
- ⏸️ v2 staging: 중지(ECS 0 / RDS stop)
- 🗑️ v1 ALB + 타겟그룹: **삭제 완료**
- ⏸️ v1 ECS·RDS·Bastion: 정지(삭제는 후속)
- `cdk diff` = **0** (코드 = 라이브)

## 프론트 도메인 연동 (코드화 포함)
- **`https://project.igrus.co.kr/` → Vercel(`igrus-project`) 연결** (200 확인)
  - CDK `route53.CnameRecord`로 코드화: `project` → `cname.vercel-dns.com` (`deleteExisting:true`로 기존 CLI 레코드 흡수)
  - Vercel 대시보드에서 도메인 등록 + SSL 자동 발급
  - → igrus.co.kr 존 DNS를 IaC 한 곳에서 관리

## IaC 범위 밖 / 후속 수동 작업
1. **GitHub Actions 변수**(`ECS_CLUSTER`/`ECS_SERVICE_SPRING`/`ECS_TASK_DEFINITION_NAME_SPRING`)를 `production`·`staging` environment에서 `-v2`로 변경해야 CD가 v2로 배포됨
2. **v1 폐기**: ECS/RDS/Bastion 삭제 (`docs/infra/v1-decommission-runbook.md`, S3는 보존)
3. RDS stop은 7일 후 자동 재시작(AWS 사양), EIP 1개는 v1 staging RDS 삭제 시 해제 — CFN 비관리 운영 작업

## 알려진 리스크 (v1에서 인수한 기술부채, 본 PR 신규 도입 아님)
- 보안그룹: RDS 3306 / Bastion 22 가 `0.0.0.0/0` 개방 — 운영 현황 그대로 복제. 별도 정리 과제

## 검증
- `npm run build`(tsc) 통과 / 평문 시크릿·컴파일물 미추적 확인
- `cdk diff` = "No differences"
- `curl api.igrus.co.kr/.../login` → 200
- RDS 축소 전 신규가입 0 + 체크섬 비교로 데이터 무손실 확인

## 체크리스트
- [x] 빌드/타입체크 통과
- [x] 시크릿 미포함
- [ ] (머지 후) GitHub CD 변수 v2로 변경
- [ ] (머지 후) v1 자원 삭제
