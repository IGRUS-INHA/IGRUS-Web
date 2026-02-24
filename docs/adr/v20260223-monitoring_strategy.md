# 모니터링 전략 - 하이브리드 접근 채택

## 선택지

1. **AWS 네이티브 스택**: CloudWatch Metrics + CloudWatch RUM + CloudWatch Alarms
2. **오픈소스 스택**: Prometheus + Grafana + Loki + Sentry (자체 호스팅)
3. **하이브리드 접근**: Actuator + Prometheus + Grafana Cloud (SaaS) + Sentry (SaaS) + CloudWatch Logs

## 결정

- **하이브리드 접근** 채택
- 메트릭 수집: Spring Boot Actuator + Micrometer + Prometheus
- 에러 트래킹: Sentry (SaaS Free Tier)
- 시각화/알림: Grafana Cloud (SaaS Free Tier)
- 로그: CloudWatch Logs (기존 AWS 인프라) + Grafana Loki
- 프론트엔드: Sentry React SDK + Web Vitals

## 결정 이유

### 1. 비용 효율성

동아리 프로젝트 특성상 예산이 제한적이다. 하이브리드 접근은 Sentry Free (5K events/월), Grafana Cloud Free (10K metrics), CloudWatch Logs (최소 비용)를 조합하여 월 $5 이하로 전체 관측성 체계를 구축할 수 있다.

| 옵션 | 월간 예상 비용 |
|------|-------------|
| AWS 네이티브 | ~$15~30 (CloudWatch RUM, Metrics, Alarms) |
| 오픈소스 자체 호스팅 | ~$20~40 (EC2 인스턴스 비용) |
| **하이브리드** | **~$5 이하** |

### 2. 운영 부담 최소화

자체 호스팅 방식은 Prometheus, Grafana, Loki 서버를 직접 관리해야 하며, 동아리의 제한된 인력으로는 부담이 크다. SaaS 기반 Free Tier를 활용하면 인프라 관리 없이 바로 사용할 수 있다.

### 3. 학습 가치

AWS CloudWatch만 사용하면 vendor lock-in이 심하고 범용적인 모니터링 기술 학습이 어렵다. Prometheus + Grafana는 업계 표준 도구로, 동아리원의 실무 역량 향상에 도움이 된다.

### 4. 점진적 도입 가능

하이브리드 접근은 Phase 1(Actuator + Prometheus)부터 Phase 4(비즈니스 메트릭)까지 단계별 도입이 가능하다. 한 번에 전체 스택을 구축할 필요 없이, 기본 인프라부터 시작하여 필요에 따라 확장할 수 있다.

### 5. 기존 인프라와의 통합

ECS stdout → CloudWatch Logs는 이미 동작 중이거나 쉽게 활성화할 수 있다. 기존 MDC 기반 traceId 체계를 그대로 활용하며, Sentry에서도 동일한 traceId로 연관 분석이 가능하다.

## 고려한 대안

### AWS 네이티브 스택

**장점:**
- 추가 인프라 불필요, AWS 콘솔에서 통합 관리
- ECS, RDS 등 AWS 서비스와 기본 연동

**채택하지 않은 이유:**
- CloudWatch RUM은 유료이며, CloudWatch Metrics 커스텀 메트릭 비용이 높음
- Grafana 대비 시각화/쿼리 기능이 제한적
- vendor lock-in이 심하여 학습 범용성이 낮음

### 오픈소스 자체 호스팅

**장점:**
- 완전한 커스터마이징 가능
- 데이터 소유권 확보
- vendor lock-in 없음

**채택하지 않은 이유:**
- 별도 EC2 인스턴스(Prometheus, Grafana, Loki 서버) 운영 비용 발생
- 인프라 관리 인력 필요 (보안 패치, 백업, 스케일링)
- 동아리 규모에 과도한 운영 복잡성

## 결과

- `docs/feature/monitoring/monitoring-spec.md`에 상세 기능 명세 작성
- `docs/feature/monitoring/monitoring-implementation-guide.md`에 Phase별 구현 가이드 작성
- 4단계 로드맵으로 점진적 도입
  - Phase 1: Actuator + Micrometer + Prometheus (기본 인프라)
  - Phase 2: Sentry + Web Vitals (에러 트래킹)
  - Phase 3: Grafana Cloud + Loki + Slack 알림 (시각화)
  - Phase 4: 커스텀 비즈니스 메트릭 (고급)
