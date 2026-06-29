# IGRUS-Web v2 드리프트 reconcile 런북

cutover 과정에서 **CLI로 적용한 변경**(인증서·DNS·S3 override 등)을 **CDK 코드와 일치**시키는 절차.
목표: `cdk diff` 가 **"There were no differences"** 가 될 때까지 정렬.

> 전제: `cdk deploy`/`cdk import` 는 자동 모드 가드가 막으므로 **사용자가 직접 실행**한다.
> 계정 `218736972976` / `ap-northeast-2` / 스택 `IgrusWebV2Stack`.

## 0. 현재 드리프트 (코드엔 있는데 라이브 CFN 스택엔 없음)

| 항목 | 상태 | reconcile |
| --- | --- | --- |
| 443 리스너 `api.igrus.co.kr` 인증서 | CLI 부착됨 | **import** |
| 8080 리스너 `staging-api.igrus.co.kr` 인증서 | CLI 부착됨 | **import** |
| Route53 `api.igrus.co.kr` A레코드 | CLI/콘솔 존재 | **import** |
| Route53 `staging-api.igrus.co.kr` A레코드 | CLI/콘솔 존재 | **import** |
| ECS 태스크 S3 override env | 라이브는 미적용(:3), 코드엔 있음 | **deploy** (새 리비전) |
| 리스너 SSL 정책(PQ) | 코드 최신, 라이브 구버전 | **deploy** |
| v2 S3 버킷 4개 | 재생성됨(이름 동일) → CFN 참조 유효 | 조치 불필요 |

## 1. 사전 준비

```bash
cd infra/cdk
export AWS_ACCESS_KEY_ID=...        # .env 의 AWS_ACCESS_KEY (218736972976)
export AWS_SECRET_ACCESS_KEY=...    # .env 의 AWS_SECRET
export AWS_DEFAULT_REGION=ap-northeast-2
npm install
npx cdk diff        # 현재 차이 확인 (cert/DNS create + taskdef/ssl update 가 보여야 정상)
```

## 2. 무중단 reconcile (권장) — `cdk import` 로 기존 자원 흡수

CLI/콘솔로 이미 존재하는 인증서·DNS 를 **삭제 없이** 스택으로 가져온다.

```bash
npx cdk import IgrusWebV2Stack
```

프롬프트에서 아래 물리 ID 를 입력(나머지는 Enter 로 skip 가능):

- **ApiCert (ListenerCertificate, 443)**
  - Listener ARN: `arn:aws:elasticloadbalancing:ap-northeast-2:218736972976:listener/app/IGRUS-Web-ALB-v2/3daa1e0dcb04d1ed/79da5b3860a73554`
  - Certificate ARN: `arn:aws:acm:ap-northeast-2:218736972976:certificate/189f0561-f2d4-4cf8-821d-0a0012ce9aaa`
- **StagingApiCert (ListenerCertificate, 8080)**
  - Listener ARN: `aws elbv2 describe-listeners --load-balancer-arn <IGRUS-Web-ALB-v2 ARN> --query "Listeners[?Port==\`8080\`].ListenerArn"` 로 확인
  - Certificate ARN: `arn:aws:acm:ap-northeast-2:218736972976:certificate/6280a8cb-0941-48b3-b6e1-1d75f4666e10`
- **ApiAlias (Route53 RecordSet)**: HostedZoneId `Z0288891201KKBESMOAHJ`, Name `api.igrus.co.kr`, Type `A`
- **StagingApiAlias (Route53 RecordSet)**: HostedZoneId `Z0288891201KKBESMOAHJ`, Name `staging-api.igrus.co.kr`, Type `A`

## 3. 나머지 변경 적용

```bash
npx cdk deploy --require-approval never
```
- ECS 태스크 새 리비전(= S3 override 포함) 등록 → 서비스가 **v2 서버 버킷** 사용 시작
- 리스너 SSL 정책 등 코드대로 정렬

## 4. 검증

```bash
npx cdk diff
# → "There were no differences" 이면 reconcile 완료 (코드 = 라이브)

# 동작 확인
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://api.igrus.co.kr/api/v1/auth/password/login \
  -H 'Content-Type: application/json' -d '{"studentId":"<학번>","password":"<비번>"}'
```

## 대안 (간단, 짧은 무중단) — import 대신 삭제 후 재생성

import 가 번거로우면, 충돌 자원을 먼저 제거하고 `cdk deploy` 로 재생성한다 (api/staging-api 에 수십 초 공백 발생, 트래픽 ≈0 이라 영향 미미).

```bash
# 1) CLI 부착 인증서 제거
aws elbv2 remove-listener-certificates --listener-arn <443 리스너> \
  --certificates CertificateArn=arn:aws:acm:...189f0561...
aws elbv2 remove-listener-certificates --listener-arn <8080 리스너> \
  --certificates CertificateArn=arn:aws:acm:...6280a8cb...
# 2) Route53 api/staging-api A레코드 삭제 (change-resource-record-sets DELETE)
# 3) 배포
npx cdk deploy --require-approval never
# 4) cdk diff 로 No differences 확인
```

## 롤백
- 문제 시 `api`/`staging-api` DNS 를 **v1 ALB**(`dualstack.igrus-web-alb-535342735...`)로 되돌리고,
  v1 ECS 서비스를 `--desired-count 1` 로 스케일업하면 v1 으로 즉시 복귀.

## reconcile 완료 후
- v1(옛 콘솔 인프라)은 롤백창(며칠) 후 삭제. 단 **프론트 `igrus-web-bucket`·CloudFront·공유 시크릿/ECR 은 유지**.
