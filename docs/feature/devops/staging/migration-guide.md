# dev → GitHub Flow 마이그레이션 가이드

## 개요

기존 `dev` → `main` 브랜치 전략에서 GitHub Flow(`main` + feature branch)로 전환하는 단계별 가이드.

## 전제 조건

- [ ] AWS 스테이징 인프라 프로비저닝 완료 ([aws-infrastructure-setup.md](./aws-infrastructure-setup.md))
- [ ] GitHub `staging` 환경 설정 완료 ([github-environment-setup.md](./github-environment-setup.md))

## 마이그레이션 단계

### 1단계: dev와 main 동기화 확인

```bash
# dev에 있고 main에 없는 커밋 확인
git log main..dev --oneline

# 커밋이 있다면 dev → main PR 생성하여 머지
```

main에 dev의 모든 변경사항이 포함되어야 한다.

### 2단계: 진행 중인 PR retarget

`dev` 대상 PR을 `main`으로 변경한다.

```bash
# dev 대상 PR 목록 확인
gh pr list --base dev

# 각 PR의 base branch를 main으로 변경
gh pr edit <PR_NUMBER> --base main
```

### 3단계: 워크플로우 변경 PR 머지

GitHub Flow 전환 코드 변경사항이 담긴 PR을 `main`에 머지한다.

머지 시 발생하는 동작:
- `backend-staging-cd.yaml` → staging 자동 배포 트리거
- `frontend-staging-cd.yaml` → staging 자동 배포 트리거
- `backend-prod-cd.yaml` → tag 트리거로 변경되어 프로덕션 배포 **안 됨**

### 4단계: 스테이징 검증

1. GitHub Actions에서 staging CD 워크플로우 실행 확인
2. `https://staging.igrus.co.kr` 접속하여 프론트엔드 동작 확인
3. `https://staging-api.igrus.co.kr` Swagger/API 동작 확인
4. 로그인, 회원가입 등 핵심 기능 검증

### 5단계: 프로덕션 배포 확인 (첫 태그)

```bash
# 현재 main의 HEAD에 태그 생성
git tag v1.0.x
git push origin v1.0.x
```

1. GitHub Actions에서 production CD 워크플로우 실행 확인
2. `https://www.igrus.co.kr` 및 `https://api.igrus.co.kr` 동작 확인

### 6단계: dev 브랜치 삭제

```bash
# 원격 dev 브랜치 삭제
git push origin --delete dev

# 로컬 dev 브랜치 삭제
git branch -d dev
```

### 7단계: main 브랜치 보호 규칙 설정

GitHub 리포지토리 → Settings → Branches → "Add branch protection rule":

- Branch name pattern: `main`
- [x] Require a pull request before merging
  - [x] Require approvals (최소 1명)
- [x] Require status checks to pass before merging
  - 필수 체크: `build-and-test`, `flyway-validate`
- [x] Require branches to be up to date before merging
- [ ] Include administrators (필요시)

### 8단계: 팀 공유

팀원에게 다음 사항을 공유한다:

1. **새로운 워크플로우**: feature branch → PR to main → merge → staging 자동 배포
2. **프로덕션 배포**: 관리자가 태그 push (`v*`) 시 프로덕션 자동 배포
3. **dev 브랜치 삭제**: 더 이상 dev 브랜치 사용하지 않음
4. **PR 대상 브랜치**: 모든 PR은 `main` 대상으로 생성

## 롤백 절차

문제 발생 시:
1. 워크플로우 파일을 이전 상태로 되돌리는 PR 생성 및 머지
2. `dev` 브랜치 재생성: `git checkout -b dev main && git push origin dev`

## 체크리스트

- [ ] dev → main 완전 동기화 확인
- [ ] 진행 중인 PR retarget 완료
- [ ] 워크플로우 변경 PR 머지
- [ ] 스테이징 환경 동작 확인
- [ ] 첫 태그 기반 프로덕션 배포 확인
- [ ] dev 브랜치 삭제
- [ ] main 브랜치 보호 규칙 설정
- [ ] 팀 공유 완료
