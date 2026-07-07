# EC2 + Caddy 전환 의사결정 문서 (근거 · 비용 · 면접 방어)

> 계정 `218736972976` / `ap-northeast-2` / default VPC.
> 측정 기준일: 2026-06-30. 모든 수치는 AWS CloudWatch / Cost Explorer / 리소스 describe 실측.
> 이 문서는 "왜 Fargate+ALB 구조를 버리고 단일 EC2 + Caddy 로 가는가"를 데이터로 방어하기 위한 것이다.

---

## 0. 한 줄 요약

상시 저부하(평균 CPU < 1%, 메모리 ~1GB) 단일 컨테이너 워크로드를 **Fargate + ALB + 다수 퍼블릭 IPv4** 위에 올려둔 탓에
실사용 대비 과금이 과대했다. 컨테이너화는 유지(같은 ECR 이미지)하되 **런타임을 Fargate → Docker-on-EC2 로,
TLS·리버스 프록시를 ALB → Caddy 로** 바꿔 월 ~$175 → ~$49 (**약 72% 절감**)을 노린다.

---

## 1. 현재 아키텍처 인벤토리 (실측)

| 구성요소 | 실제 리소스 | 상태 |
|---|---|---|
| ECS 클러스터 | `IGRUS-WEB-ECS-Cluster-v2` (+ 폐기 예정 v1) | ACTIVE |
| 앱 서비스(prod) | `igrus-web-server-ecs-service-v2`, **Fargate**, desired=1 | Running 1 |
| 앱 서비스(staging) | `igrus-web-server-staging-task-def-service-v2`, desired=0 | 중지(비용절감) |
| 태스크 스펙 | `igrus-web-server-task-def-v2:1` → **1 vCPU(1024) / 2GB(2048)** | 과다 할당 |
| 로드밸런서 | `IGRUS-Web-ALB-v2` (internet-facing, **4 AZ**: 2a/2b/2c/2d) | active |
| ALB 리스너 | 443(HTTPS), 8080(HTTPS), 8000(HTTP) + ACM 인증서 | TLS 종단 |
| DB | `igrus-web-mysql-rds-v2` db.t3.micro / 20GB gp2 / Single-AZ | available |
| 퍼블릭 IPv4 | EIP 5개: **ALB ENI ×4** + RDS ENI ×1 | in-use |
| NAT 게이트웨이 | 없음 (퍼블릭 서브넷 + public IP 방식) | — |
| 베스천 | `IGRUS-Web-RDS-SSM-EC2-v2` t3.micro (SSM용) | **stopped** |
| 컨테이너 레지스트리 | ECR `igrus/web/spring` | 유지 |
| 비밀 | Secrets Manager `igrus/web/server/{prod,staging}` | 유지 |

**핵심 관찰**: ALB 가 4개 AZ에 걸쳐 퍼블릭 IPv4 **4개**를 점유한다. 그런데 그 ALB가 가리키는 타깃은
**단일 Fargate 태스크(1개 AZ)** 였다 → 즉 LB 계층은 4-AZ인데 컴퓨팅은 SPOF. 멀티AZ HA는 사실상 장식이었다.

---

## 2. 비용 증거 (Cost Explorer, 세금 제외)

### 2-1. 2026-05 청구 라인 (사용자 제공 = 실청구)

| 서비스 | 월 비용 | 비중 | 내역 |
|---|---:|---:|---|
| **ECS (Fargate)** | **$84.50** | 48% | vCPU 1,488h $69.29 + Mem 2,976h $15.21 |
| RDS MySQL | $39.55 | 23% | db.t3.micro 1,320h $34.31 + 스토리지 40GB-Mo $5.24 |
| **VPC Public IPv4** | **$22.38** | 13% | in-use 4,308h $21.54 + idle 168h $0.84 |
| **ALB** | **$16.75** | 10% | LB-hour 744h $16.74 + LCU **0.763h $0.01** |
| EC2 (베스천) | $10.40 | 6% | t3.micro 744h $9.67 + EBS $0.73 |
| ECR | $0.87 | — | 스토리지 8.7GB-Mo |
| Secrets Manager | $0.80 | — | 2 secrets |
| **합계** | **$175.25** | 100% | (세금 별도 ~$17.5) |

### 2-2. 3개월 추세 — "일시 현상"이 아니라 상시 구조 비용

| 월 | ECS | ALB | RDS | VPC(IPv4) | EC2 | (Tax) |
|---|---:|---:|---:|---:|---:|---:|
| 2026-04 | $81.77 | $16.21 | $42.68 | $21.65 | $9.36 | $17.41 |
| 2026-05 | $84.50 | $16.75 | $39.55 | $22.38 | $9.67 | $17.54 |
| 2026-06 | $76.89 | $15.24 | $40.10 | $20.37 | $8.77 | $16.37 |

→ ECS ~$80, ALB ~$16, IPv4 ~$21 이 **매달 반복**. v1↔v2 병렬 테스트 때문에 잠깐 오른 게 아니라, 구조 자체가 고정비.

### 2-3. 결정적 모순: LCU 0.763 시간

ALB는 시간당 고정요금 $16.74를 내지만 **실제 트래픽 처리량(LCU)은 한 달 0.763시간어치 = $0.01**.
**트래픽이 거의 0인 로드밸런서에 매달 $16.74를 내고 있었다.** 단일 타깃이라 분산할 대상도 없다.

---

## 3. 사용률 증거 (CloudWatch, v1 운영 서비스 5개월 실측)

`igrus-web-server-ecs-service` (2026-01-25 ~ 06-29):

| 지표 | 평균 | 최댓값 | 해석 |
|---|---|---|---|
| **CPU** | **0.1 ~ 0.5%** (최고일 2.8%) | 일부 날 100% | 100%는 배포 시 JVM 기동 1분 스파이크일 뿐, 일평균 1% 미만 |
| **메모리** | 24 ~ 48% | ~52% (2026-02 초) | 할당 2GB 중 ~1GB. 한계 근처 도달 0회 |
| **LiveTaskCount** | 1 | 2 (3일뿐) | 2가 된 날 = 롤링 배포 중 구·신 태스크 겹침. **부하 기반 오토스케일 0건** |

→ **1 vCPU 할당 / 실사용 ~0.01 vCPU.** Fargate의 이산적 사이징(최소 0.25 vCPU 단위)과 always-on 과금이
이 "작고 일정한" 워크로드와 최악의 궁합. 스케일이 필요한 적이 5개월간 한 번도 없었다.

---

## 4. 목표 아키텍처: 단일 EC2 + Caddy + Docker

```
 인터넷
   │  (1 EIP, IPv4 1개)
   ▼
 ┌──────────────────── EC2 (t3.small 2GB, 퍼블릭 서브넷) ────────────────┐
 │  RAM 2GB (앱 워킹셋) + swap 5GB(EBS, OOM 안전망)                       │
 │  Caddy  ── 자동 Let's Encrypt TLS, 리버스 프록시, graceful reload      │
 │    └─▶ Docker: igrus-web-spring (ECR 동일 이미지, :8080, -Xmx1g)      │
 └──────────────────────────────┬───────────────────────────────────────┘
                                 │ 3306 (SG 제한)
                                 ▼
                       RDS db.t4g.micro (ARM, 관리형 유지)
```

- **컨테이너화 유지**: 동일 ECR 이미지를 그대로 `docker run`/`compose`. 디컨테이너화 아님.
- **Caddy 가 ALB 대체**: TLS 자동 발급·갱신(ACM 불필요), 리버스 프록시, 헬스체크, graceful reload 무중단 배포.
- **퍼블릭 IPv4 5~6개 → 1개**: EIP 1개만 인스턴스에 고정.
- **메모리 안전망(swap)**: 앱 실측 ~1GB → t3.small(2GB)에 `-Xmx1g`로 힙 고정, swap 5GB는 예외적 폭증/누수 시
  하드 크래시 방지용 보험. swap은 EBS라 느리므로 상시 의존이 아닌 안전망 포지셔닝(`vm.swappiness=10`).
- **IaC 재현**: CDK 로 EC2 + EIP + SG + user-data(swap 생성 + Docker/Caddy 설치 + 이미지 pull) 프로비저닝. 단일 진실점 유지.

> **user-data로 swap 5GB 자동 구성** (콘솔 수작업 없이 부팅 시 적용, /etc/fstab 영속화):
> ```bash
> fallocate -l 5G /swapfile && chmod 600 /swapfile
> mkswap /swapfile && swapon /swapfile
> echo "/swapfile none swap sw 0 0" >> /etc/fstab
> sysctl -w vm.swappiness=10 && echo "vm.swappiness=10" >> /etc/sysctl.conf
> ```
> → swap 5GB + Docker 이미지/로그 누적을 감안해 EBS 루트 볼륨을 30GB 로 잡는다(디스크 full 장애 방지 안전마진).

---

## 5. 비용 projection

### 옵션 A (권장): EC2 + Caddy, **관리형 RDS 유지**

| 항목 | 월 비용 |
|---|---:|
| EC2 t3.small (앱 + Caddy) | $19.34 |
| EBS 30GB gp3 | ~$2.74 |
| EIP ×1 | $3.72 |
| RDS **db.t4g.micro**(ARM) 단일 + 20GB | ~$20.02 |
| ECR | $0.87 |
| Secrets Manager (또는 SSM PS=$0) | $0.80 |
| **합계** | **~$47.49** |

**$175.25 → $47.49 = 월 -$127.8 (−73%)**

> EBS 30GB: 실사용은 ~14GB(OS ~3 + swap 5 + 이미지 ~2 + 로그/헤드룸)지만, Docker가 배포마다 이미지 레이어·
> 구버전 태그·빌드 캐시·컨테이너 로그를 누적하므로 **디스크 full 장애 방지를 위해 넉넉히 30GB** 확보(차액 ~$1.3/월).
> 자동 정리(`docker image prune`, 로그 rotate)와 별개로 운영 안전마진 우선.

> RDS는 동급 스펙(1GB/2vCPU)에서 x86(t3) → ARM(t4g)로만 변경 → 약 10% 저렴.
> 데이터·엔진 불변, `modify-db-instance --db-instance-class db.t4g.micro` 짧은 재부팅만 발생.
> (앱 EC2는 t3.small 유지 — 아래 '추가 절감 여지'에서 t4g.small 검토 가능)

### 옵션 B (공격적): EC2 한 대에 MySQL 까지 self-host

ECR/EIP/EBS + EC2(t3.small) ≈ **~$27/mo (−85%)**. 단, 백업/복구를 직접 스크립트로 책임져야 하고
2GB RAM에 Spring + MySQL 동거는 메모리 압박 → 권장하지 않음(아래 트레이드오프 참조).

### 추가 절감 여지
- **t4g.small(ARM Graviton)**: Spring Boot ARM 구동 가능 → ~$15.5/mo (t3.small 대비 −20%)
- **Savings Plan / RI 1년**: EC2·RDS 약 −30~40%
- **CloudFront 뒤로 넣어 EIP 제거**: IPv4 비용 추가 절감 가능

---

## 6. 트레이드오프 — 무엇을 잃고, 왜 감수 가능한가

| 잃는 것 | 영향 | 감수 근거 |
|---|---|---|
| ALB 멀티-AZ LB | LB 계층 단일화 | 어차피 타깃이 단일 태스크/단일 AZ였음. HA는 명목뿐이었다 |
| Fargate 관리형 런타임 | 호스트 OS 패치 책임 | 동아리 사이트, 무중단 SLA 없음. user-data + 주기 패치로 충분 |
| ALB connection draining | 배포 시 미세 커넥션 끊김 가능 | LCU 0.76h/월 = RPS 거의 0. 체감 영향 없음. Caddy graceful reload 로 대부분 커버 |
| 컴퓨팅 자동 확장 | 트래픽 급증 시 수동 대응 | 5개월간 스케일 이벤트 0건. 필요 시 인스턴스 타입 상향으로 대응 |
| (옵션 B) 관리형 백업 | DR 부담 ↑ | 그래서 옵션 A(관리형 RDS 유지) 권장 |

---

## 7. 리스크 & 완화

- **SPOF (EC2 1대)**: 원래도 단일 태스크라 SPOF였음. CloudWatch 인스턴스 auto-recovery 또는
  ASG min=max=1 로 하드웨어 장애 시 자동 복구. RTO 수 분 → 비핵심 서비스에 수용 가능.
- **메모리 압박**: t3.small(2GB)에 앱(~1GB)+OS+Caddy 동거. `-Xmx1g`로 힙 상한 고정 + swap 5GB 안전망으로
  하드 OOM 방지. 단 swap은 EBS라 상시 점유 시 GC 지연 → CloudWatch `mem_used_percent`/swap 사용량 알람으로 감시,
  지속 초과 시 t3.medium(4GB) 또는 t4g.small 상향으로 대응.
- **롤백 경로**: v2 Fargate+ALB 스택은 CDK에 그대로 정의 유지. `desiredCount=1` 재배포 + Route53 A레코드
  원복으로 수 분 내 복귀 가능. ECR 이미지 보존.
- **인증서**: Caddy 가 Let's Encrypt 자동 갱신. 80/443 아웃바운드 + 도메인 A레코드만 맞으면 무인 운영.
- **보안**: ⚠️ `MEMO.md` 평문 IAM 키 즉시 rotate + 삭제. 레거시 `igrus-web-staging-mysql-rds`(publiclyAccessible=true)는
  v1 폐기 런북대로 제거. RDS는 SG로 3306을 앱/베스천 SG에만 허용.

---

## 8. 면접 예상 질문 & 방어 답변

**Q. 관리형(Fargate)에서 비관리형(EC2)으로 가는 건 퇴보 아닌가?**
A. 컨테이너화는 유지하고 *배치(placement)* 만 바꾼 것. Fargate 가치는 탄력적·임시 워크로드의 per-task 과금인데,
우리는 5개월간 스케일 0건의 상시 단일 컨테이너다. 1 vCPU 예약을 24/7 사면서 0.01 vCPU 를 쓴다.
일정 베이스라인엔 예약형 EC2 단가가 압도적으로 싸다. "관리형이 항상 옳다"가 아니라 워크로드 형태에 맞춘 선택.

**Q. ALB 없이 TLS·헬스체크·무중단 배포는?**
A. TLS는 Caddy 자동 Let's Encrypt(ACM+ALB 대비 부품 ↓, $0). 헬스체크/프록시는 Caddy `reverse_proxy`.
무중단은 신 컨테이너 기동→Caddy graceful reload→구 컨테이너 종료(blue-green). LCU 0.76h/월이라 드레이닝 손실은 무의미.

**Q. 멀티AZ HA를 버리는 것 아닌가?**
A. ALB는 4AZ였지만 타깃은 1개 AZ의 단일 태스크 → 컴퓨팅 HA는 처음부터 없었다. 명목 HA를 위해 IPv4 4개·LB 고정비를
내고 있었을 뿐. 진짜 HA가 필요해지면 ASG+다중 인스턴스로 *그때* 재설계. 지금은 비용/복잡도 대비 가치 없음.

**Q. 왜 Public IPv4가 $22씩 나왔나?**
A. 2024-02부터 모든 퍼블릭 IPv4 $0.005/h 과금. ALB가 4AZ에 4개 + 퍼블릭 서브넷 Fargate 태스크마다 1개씩 →
상시 5~6개 = $22/월. EC2 단일 + EIP 1개 = $3.7/월. 필요하면 CloudFront 뒤로 넣어 0개도 가능.

**Q. Fargate를 0.25 vCPU로 다운사이징하면 되지 않나?**
A. 부분 해결일 뿐. (1) 0.5GB엔 Spring(~1GB heap) 못 올림. (2) 무엇보다 ALB $16.75 + IPv4 $22 는
Fargate를 줄여도 그대로 남는다. 3대 비용원(Fargate·ALB·IPv4)을 한 번에 없애려면 ALB/Fargate 네트워킹 모델 자체를 떠나야 한다.

**Q. 단일 EC2 장애 시?**
A. auto-recovery/ASG(min=max=1)로 자동 재기동. DB는 관리형 RDS라 분리 보존. 비핵심 서비스 RTO 수 분 수용.
롤백은 CDK의 Fargate 스택 재배포로 즉시 가능.

**Q. swap 5GB 잡으면 t3.micro(1GB)로도 되는 것 아닌가?**
A. 아니다. swap은 EBS(디스크) 위라 RAM보다 수백 배 느리고, JVM 힙이 swap으로 밀리면 GC가 디스크를 휘저어
응답이 수 초씩 멈춘다. swap은 "RAM 대체"가 아니라 "하드 OOM 방지 안전망"이다. 따라서 앱 워킹셋(~1GB)은
물리 RAM 안에 들어와야 하고(t3.small 2GB + `-Xmx1g`), swap은 예외적 폭증만 흡수한다. `vm.swappiness=10`으로
평상시 swap을 거의 안 쓰게 한다.

**Q. 이 결정의 재현성/IaC는?**
A. v2 마이그레이션과 동일하게 CDK로 EC2+EIP+SG+user-data 코드화. swap 생성·Docker/Caddy 설치까지 user-data에
넣어 부팅 한 번에 재현. 콘솔 수작업 import 대신 코드가 단일 진실점.

---

## 9. 추후 계획 (로드맵)

전환(옵션 A) 이후, 단계적으로 더 최적화한다. **핵심 원칙: 단순 self-host가 아니라 "고정비 분산"이 목적.**

### Phase 2 — t3.medium 1년 예약 + Docker 기반 MySQL 자체 호스팅

관리형 RDS 를 같은 EC2 의 Docker MySQL 컨테이너로 흡수하고, **인스턴스를 t3.medium(2vCPU/4GB)로 키우되 1년 예약(RI/Savings Plan)** 으로 단가를 낮춘다.

| | 관리형 RDS 유지(옵션 A) | self-host (t3.medium 온디맨드) | **self-host (t3.medium 1년 예약)** |
|---|---:|---:|---:|
| EC2 | t3.small $19.34 | $38.69 | **~$24.4** |
| RDS | $20.03 | $0 | $0 |
| **합계** | **~$39.4** | ~$38.7 | **~$24.4** |

→ **포인트는 1년 예약이다.** 온디맨드로 인스턴스를 키워 DB를 흡수하면 옵션 A와 거의 본전이지만,
**1년 예약 시 t3.medium 단가가 ~37% 빠지면서**(온디맨드 $38.69 → ~$24.4) MySQL을 4GB 안에 함께 올리고도
옵션 A 대비 **월 ~$15 추가 절감**. 4GB 면 앱(~1GB heap) + MySQL(~1GB) + OS/Docker 가 swap 포함 여유롭게 들어간다.
- **감수 부담**: 관리형 백업/스냅샷/복구를 직접 책임 → 선행 조건으로 `mysqldump`(또는 `xtrabackup`) → S3 정기 백업
  + EBS 스냅샷 자동화 + 컨테이너 볼륨 영속화를 **먼저** 갖춘다.
- **약정 리스크**: 1년 예약은 중도 해지 불가 → 트래픽/방향이 안정화된 뒤(옵션 A 운영 검증 후) 커밋.

### Phase 3 — Over-provision + 동아리 차원 서버 공유

위 t3.medium(예약) 한 대에 여유 용량을 활용해 **여러 워크로드를 Docker 로 격리·공존**:
IGRUS 웹(앱+DB) + 다른 동아리 프로젝트 + 내부 도구(스테이징, 봇, 대시보드 등). (4GB 한도 내에서 워크로드 수 조절)

**왜 이게 진짜 절감인가** — 고정비(인스턴스 base, EIP, EBS base, Caddy 운영)는 워크로드 수와 무관하게 1회만 든다.
워크로드 N개를 한 대에 얹으면 워크로드당 비용 = 고정비/N 로 떨어진다. 각 서비스를 따로 띄우면 N배.

- **Caddy 멀티 도메인**: 한 Caddy 가 `a.igrus.co.kr`, `b.igrus.co.kr` … 를 컨테이너별로 라우팅(자동 TLS 각각).
- **격리**: 컨테이너 단위 리소스 제한(`--memory`, `--cpus`)으로 noisy-neighbor 방지.
- **공유 사업 가능성**: 동아리 구성원/팀에게 서브도메인+컨테이너 슬롯을 제공하는 내부 PaaS 형태로 확장 여지.

**감수할 리스크(문서화 대상)**: 단일 인스턴스에 다수 서비스 → 장애 blast radius 확대, 백업/격리 책임 증가.
핵심 서비스(IGRUS 웹)와 실험성 워크로드의 SLA 를 구분하고, 핵심 DB 는 관리형 유지 또는 별도 백업 강제.

> 결론: Phase 2 는 Phase 3 의 전제(컨테이너화된 DB)로서 의미가 있고, **절감의 본체는 Phase 3 의 고정비 분산**이다.
> 옵션 A 로 먼저 73% 를 확보한 뒤, 공유 수요가 실제로 생길 때 Phase 2~3 을 진행한다(성급한 추상화 금지).

---

## 10. 관련 문서

- **`docs/infra/ec2-migration-runbook.md` — 이 결정의 실행 절차 (MIGRATION_PHASE 1→2→3)**
- `docs/infra/v1-decommission-runbook.md` — v1 자원 삭제 절차
- `docs/infra/reconcile-runbook.md` — v2 드리프트 reconcile
- `docs/infra/rds-shrink-runbook.md` — RDS 스토리지 축소
- `infra/cdk/lib/igrus-web-v2-stack.ts` — IaC (EC2+Caddy construct 구현 완료, phase 플래그로 전환)
