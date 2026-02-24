# 프로덕션 릴리스 버전 단조 증가 보장

## 배경

기존에는 수동으로 `git tag v1.x.x`를 push하여 프로덕션 배포를 트리거했다. 이 방식은 다음과 같은 문제가 있었다:

- 사람이 직접 버전 번호를 입력하므로 오타나 순서 역전 가능
- 누가 어떤 기준으로 버전을 올렸는지 추적 어려움
- 이미 존재하는 태그와 중복되거나 낮은 버전을 push할 수 있음

## 선택지

1. **워크플로우 검증 스텝**: CD 파이프라인 초반에 semver 비교 스텝을 추가하여 낮은 버전 태그 push 시 실패 처리
2. **semantic-release 자동화**: conventional commits 기반으로 버전을 완전 자동 결정. 수동 태그 push 불필요
3. **workflow_dispatch 기반 Release 워크플로우**: GitHub UI에서 bump 타입(patch/minor/major)만 선택하면 다음 버전을 자동 계산하여 태그 생성 및 배포

## 결정

- **workflow_dispatch 기반 Release 워크플로우** 채택

## 결정 이유

- **선택지 1 탈락**: 잘못된 태그 push 후 파이프라인이 실패할 뿐, 태그 자체는 이미 생성되어 수동 정리 필요
- **선택지 2 탈락**: Node.js 의존성 추가, 팀 전원의 엄격한 커밋 메시지 컨벤션 필요, 배포 시점 제어 어려움 (main merge = 즉시 릴리스)
- **선택지 3 채택**: 버전 번호를 사람이 타이핑하지 않으므로 실수 자체가 불가능. 최신 태그에서 자동 계산하여 단조 증가 보장. 추가 의존성 없이 GitHub Actions만으로 구현 가능

## 적용 범위

### 워크플로우 변경

- `release.yaml` (신규): `workflow_dispatch` 트리거, bump 타입 선택 → 버전 자동 계산 → 태그 생성 → CD 호출
- `backend-prod-cd.yaml`: `workflow_call` 트리거 추가, `inputs.version || github.ref_name`으로 이미지 태그 결정
- `frontend-prod-cd.yaml`: `workflow_call` 트리거 추가

### 배포 플로우

```
스테이징 검증 완료
  → GitHub Actions > Release > Run workflow
  → bump 타입 선택 (patch/minor/major)
  → 최신 태그에서 다음 버전 자동 계산
  → Git 태그 생성 및 push
  → Backend/Frontend 프로덕션 CD 호출 (workflow_call)
```

## 결과

- 버전 단조 증가가 구조적으로 보장됨
- 배포 시점을 명시적으로 제어 가능 (workflow_dispatch)
- GitHub Actions Step Summary에서 버전 변경 이력 확인 가능
- 기존 `push.tags: v*` 트리거도 유지되어 하위 호환성 보장

## 후속 조치

- [ ] Release 워크플로우를 main 브랜치에 머지 후 동작 검증
- [ ] 팀 공유: 프로덕션 배포 시 수동 `git tag` 대신 Release 워크플로우 사용 안내
- [ ] (선택) GitHub tag protection rule 설정으로 수동 태그 push 제한
