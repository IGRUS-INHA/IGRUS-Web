# 배포 프로세스

## 브랜치 전략: GitHub Flow

```
feature/* ──→ PR ──→ main merge ──→ 스테이징 자동 배포 ──→ Release 워크플로우 실행 ──→ 프로덕션 자동 배포
```

- `main`: 항상 배포 가능한 상태 유지
- `feature/*`: 기능 개발 브랜치, main에서 분기
- 태그 (`v*`): 프로덕션 릴리스 마커

## 환경 정보

| 환경 | 프론트엔드 URL | 백엔드 API URL | 트리거 |
|------|---------------|---------------|--------|
| 스테이징 | https://staging.igrus.co.kr | https://staging-api.igrus.co.kr | main 브랜치 push |
| 프로덕션 | https://www.igrus.co.kr | https://api.igrus.co.kr | Git tag push (`v*`) |

## 스테이징 배포

### 자동 배포

`main` 브랜치에 PR이 머지되면 자동으로 스테이징 환경에 배포된다.

- **백엔드**: `backend/**` 경로 변경 시 → `backend-staging-cd.yaml` 실행
- **프론트엔드**: `frontend/**` 경로 변경 시 → `frontend-staging-cd.yaml` 실행

### 배포 확인

1. GitHub → Actions 탭에서 워크플로우 실행 상태 확인
2. 스테이징 URL 접속하여 기능 검증

## 프로덕션 배포

### Release 워크플로우 실행

스테이징 검증이 완료되면, **Release 워크플로우**(`release.yaml`)를 실행하여 프로덕션에 배포한다.

1. GitHub → Actions → **Release** 워크플로우 선택
2. **Run workflow** 클릭
3. bump 타입 선택 (`patch` / `minor` / `major`)
4. 워크플로우가 자동으로:
   - 최신 태그에서 다음 버전을 계산 (예: `v1.0.1` → `v1.0.2`)
   - Git 태그를 생성하고 push
   - Backend / Frontend 프로덕션 CD 파이프라인을 호출

> **주의**: 수동으로 `git tag`를 push하지 않는다. Release 워크플로우를 통해서만 태그를 생성해야 버전 단조 증가가 보장된다.

### 태그 규칙

[Semantic Versioning](https://semver.org/) 사용:
- `v{major}.{minor}.{patch}` (예: `v1.0.0`, `v1.1.0`, `v1.1.1`)
- **major**: 호환되지 않는 API 변경
- **minor**: 하위 호환성 있는 기능 추가
- **patch**: 하위 호환성 있는 버그 수정

### 배포 확인

1. GitHub → Actions 탭에서 Release 워크플로우 실행 상태 확인 (Step Summary에서 버전 변경 내역 확인 가능)
2. 프로덕션 URL 접속하여 기능 검증

## 롤백

### 프로덕션 롤백

다음 중 하나의 방법으로 롤백한다:

1. **Release 워크플로우 재실행** (권장): 롤백할 커밋을 main에 revert한 후, Release 워크플로우로 새 patch 버전 배포
2. **이전 CD 워크플로우 Re-run**: GitHub Actions에서 이전 production CD 워크플로우를 "Re-run"

### 스테이징 롤백

main에 revert 커밋을 머지하면 스테이징이 자동으로 롤백된다:

```bash
git revert <commit-hash>
# PR 생성 → 머지 → staging 자동 재배포
```

## 핫픽스

긴급 수정이 필요한 경우:

1. `main`에서 `hotfix/*` 브랜치 분기
2. 수정 후 `main`으로 PR 생성 및 머지 → 스테이징 자동 배포
3. 스테이징 검증 후 태그 push → 프로덕션 배포

## CI/CD 워크플로우 파일

| 파일 | 용도 | 트리거 |
|------|------|--------|
| `backend-ci.yaml` | 백엔드 빌드/테스트 | PR to main |
| `backend-staging-cd.yaml` | 백엔드 스테이징 배포 | push to main (backend/**) |
| `backend-prod-cd.yaml` | 백엔드 프로덕션 배포 | tag push (v*) / workflow_call |
| `frontend-staging-cd.yaml` | 프론트엔드 스테이징 배포 | push to main (frontend/**) |
| `frontend-prod-cd.yaml` | 프론트엔드 프로덕션 배포 | tag push (v*) / workflow_call |
| `release.yaml` | 릴리스 태그 생성 + 프로덕션 배포 | workflow_dispatch (수동) |
