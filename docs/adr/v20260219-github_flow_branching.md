# GitHub Flow 브랜치 전략 및 스테이징 환경 도입

## 배경

기존에는 `dev` → `main` 브랜치 전략을 사용하여, 기능 브랜치를 `dev`에 머지한 뒤 `dev`를 `main`에 머지하면 프로덕션에 바로 배포되었다. 이 구조에는 다음과 같은 문제가 있었다:

- 프로덕션 배포 전 검증 단계가 없어 배포 후 버그가 발견될 위험
- `dev`와 `main` 사이의 머지 충돌과 동기화 문제
- 배포 시점을 명시적으로 제어할 수 없음

## 선택지

1. **현행 유지 (dev → main)**: dev에서 통합 후 main으로 머지하여 배포
2. **Git Flow**: develop, release, hotfix 등 다수 브랜치 운영
3. **GitHub Flow + 스테이징**: main + feature branch만 사용, 스테이징 환경으로 사전 검증

## 결정

- **GitHub Flow + 스테이징 환경** 채택
- `dev` 브랜치 제거, `main` + feature branch만 사용
- main 머지 → 스테이징 자동 배포, Git tag push → 프로덕션 배포

## 결정 이유

- **단순성**: main과 feature branch만 관리하여 브랜치 복잡도 최소화
- **사전 검증**: 스테이징 환경에서 프로덕션 배포 전 검증 가능
- **명시적 릴리스**: Git tag(`v*`)로 프로덕션 배포를 명시적으로 제어
- **선택지 1 탈락**: 배포 전 검증 단계가 없는 근본적 문제 미해결
- **선택지 2 탈락**: 소규모 팀에 불필요한 브랜치 복잡도 (release, hotfix 등)

## 적용 범위

### 배포 플로우

```
feature/* → PR → main merge → 스테이징 자동 배포 → Git tag (v*) push → 프로덕션 자동 배포
```

### 환경 URL

| 환경 | 프론트엔드 | 백엔드 API |
|------|-----------|-----------|
| 스테이징 | https://staging.igrus.co.kr | https://staging-api.igrus.co.kr |
| 프로덕션 | https://www.igrus.co.kr | https://api.igrus.co.kr |

### 주요 변경 파일

- `.github/workflows/backend-ci.yaml`: trigger 브랜치 정리 (main만)
- `.github/workflows/backend-prod-cd.yaml`: tag 기반 트리거로 전환
- `.github/workflows/frontend-prod-cd.yaml`: tag 기반 트리거로 전환
- `.github/workflows/backend-staging-cd.yaml`: 신규 (main push → staging 배포)
- `.github/workflows/frontend-staging-cd.yaml`: 신규 (main push → staging 배포)
- `backend/src/main/resources/application-staging.yml`: 스테이징 Spring 프로필

### 인프라

- 같은 AWS 계정, 별도 리소스 (ECS, S3, CloudFront, RDS 스키마)
- GitHub Environment: `staging` (기존 `production`과 분리)

## 결과

- 프로덕션 배포 전 스테이징에서 검증 가능
- 태그 기반 릴리스로 배포 시점을 명시적으로 제어
- `dev` 브랜치 제거로 브랜치 관리 단순화
- 쿠키 도메인 분리로 환경 간 세션 격리 보장

## 후속 조치

- [ ] AWS 스테이징 인프라 프로비저닝
- [ ] GitHub `staging` 환경 생성 및 variables/secrets 설정
- [ ] `dev` 브랜치 삭제
- [ ] `main` 브랜치 보호 규칙 설정
- [ ] 팀 공유 및 워크플로우 안내
