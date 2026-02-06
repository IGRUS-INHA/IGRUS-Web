---
name: update-docs
description: 문서 기반 코드 수정 후 관련 문서를 자동 업데이트합니다. 코드 구현 완료 후 spec, test-case, task, ADR 등 관련 문서를 최신 상태로 동기화합니다. 문서를 참조하여 코드를 수정한 후 자동으로 호출됩니다.
tools: Glob, Grep, Read, Edit, Write, Bash, AskUserQuestion
model: sonnet
---

# Documentation Auto-Updater

코드 수정 작업 완료 후 관련 문서들을 최신 상태로 동기화합니다.

**이 skill은 문서를 참조하여 코드를 수정한 후 proactively 호출되어야 합니다.**

## 대상 문서 디렉토리

- `docs/feature/` - 기능 명세 (spec)
- `docs/adr/` - Architecture Decision Records
- `docs/qa/` - QA 전략
- `docs/backlog/` - 백로그
- `backend/docs/test-case/` - 테스트 케이스
- `backend/docs/task/` - 작업 문서
- `backend/docs/spec/` - 백엔드 스펙
- `frontend/docs/` - 프론트엔드 문서

## Workflow

### Phase 1: 컨텍스트 수집

1. **대화 기록 분석**: 현재 세션에서 참조된 문서 파일 경로 추출
   - 사용자가 읽기 요청한 문서
   - 코드 수정의 근거로 언급된 문서

2. **코드 변경 분석**: 현재 세션에서 수정된 코드 파일 식별

   ```bash
   git diff --name-only HEAD
   ```

3. **도메인 매핑**: 변경된 코드의 도메인 식별
   - `backend/src/main/java/igrus/web/{domain}/` → `{domain}`
   - `frontend/src/features/{domain}/` → `{domain}`
   - `frontend/src/pages/{domain}/` → `{domain}`

### Phase 2: 관련 문서 탐색

변경된 도메인에 해당하는 문서들을 자동으로 탐색:

```text
예시 - 도메인: board (게시판)
├── docs/feature/community/board-spec.md
├── backend/docs/task/community/board/tasks.md
├── backend/docs/test-case/community/board/board-test-cases.md
└── docs/adr/ (관련 ADR 검색)
```

**도메인-문서 매핑 규칙**:
| 코드 경로 패턴 | 문서 경로 |
|--------------|----------|
| `*/board/*` | `docs/feature/community/board-spec.md`, `backend/docs/task/community/board/` |
| `*/post/*` | `docs/feature/community/post-spec.md`, `backend/docs/task/community/post/` |
| `*/comment/*` | `docs/feature/community/comment-spec.md`, `backend/docs/task/community/comment/` |
| `*/auth/*` | `docs/feature/auth/auth-spec.md`, `backend/docs/task/auth/` |
| `*/user/*` | `docs/feature/auth/user-entity-design.md`, `backend/docs/task/user/` |

### Phase 3: 문서 유형별 업데이트 전략

#### 3.1 Spec 문서 (`docs/feature/**/*-spec.md`)

**트리거 조건**:
- 새로운 기능 요구사항 구현
- 기존 기능 동작 변경
- API 시그니처 변경

**업데이트 내용**:
- Clarifications 섹션에 결정 사항 추가
- Functional Requirements 수정/추가 (해당 시)
- Edge Cases 추가 (발견 시)

**업데이트 형식**:
```markdown
### Clarifications (Session {YYYY-MM-DD})

- Q: {결정이 필요했던 사항}? → A: {결정 내용} - {근거}
```

#### 3.2 Test-Case 문서 (`backend/docs/test-case/**/*-test-cases.md`)

**트리거 조건**:
- 테스트 코드 추가/수정
- 새로운 엣지 케이스 발견
- 버그 수정으로 인한 테스트 추가

**업데이트 내용**:
- 테스트 케이스 상태 변경 (⚠️ → ✅)
- 새 테스트 케이스 행 추가
- 변경 이력 테이블 업데이트

**상태 표시 규칙**:
- ✅ 구현 완료 / 테스트 통과
- ⚠️ 미구현 / 검토 필요

**변경 이력 형식**:
```markdown
| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.x | {YYYY-MM-DD} | Claude | {변경 내용 요약} |
```

#### 3.3 Task 문서 (`backend/docs/task/**/*.md`)

**트리거 조건**:
- 태스크 완료
- 태스크 스킵 (별도 방식으로 구현)
- 태스크 연기 (추후 구현 예정)

**업데이트 내용**:
- 체크박스 상태 변경: `- [ ]` → `- [x]`
- 완료/스킵/연기 태그 추가
- Summary 테이블 업데이트 (해당 시)

**태스크 상태 형식**:
```markdown
- [x] T001 설명...
- [x] T002 [SKIPPED] 설명 - {스킵 사유}
- [x] T003 [DEFERRED] 설명 - {연기 사유}
```

#### 3.4 ADR 문서 (`docs/adr/`)

**트리거 조건**:
- 아키텍처 관련 중요 결정 (새로운 패턴 도입, 기술 선택 등)
- 기존 구조의 significant 변경
- 향후 참고가 필요한 기술적 결정

**업데이트 내용**:
- 새 ADR 파일 생성 **제안** (실제 생성은 사용자 확인 후)
- 기존 ADR의 "후속 조치" 체크리스트 업데이트

**ADR 파일명 형식**: `v{YYYYMMDD}-{kebab-case-title}.md`

**ADR 템플릿**:
```markdown
# {제목}

## 배경

{결정이 필요했던 상황 설명}

## 선택지

1. **{선택지 1}**: {설명}
2. **{선택지 2}**: {설명}

## 결정

- **{채택된 선택지}** 채택

## 결정 이유

{결정 근거}

## 적용 범위

{어디에 적용되는지}

## 결과

{적용 결과 또는 예상 결과}

## 후속 조치

- [ ] {후속 작업 항목}
```

#### 3.5 Backlog 문서 (`docs/backlog/`)

**트리거 조건**:
- User Story 완료
- 기능 상태 변경

**업데이트 내용**:
- 상태 필드 업데이트 (To Do → In Progress → Done)
- 인수 조건 체크박스 상태 업데이트

### Phase 4: 사용자 확인 및 실행

1. **변경 사항 요약 제시**:

   ```markdown
   ## 문서 업데이트 계획

   ### 업데이트 대상 문서
   1. `{문서 경로 1}`
      - {변경 내용 요약}

   2. `{문서 경로 2}`
      - {변경 내용 요약}

   ### 새로 생성 제안 문서 (선택적)
   - `docs/adr/v{날짜}-{제목}.md`
   ```

2. **사용자 확인 요청**:
   - "위 업데이트를 진행할까요? (yes/no/선택적)"
   - 사용자가 특정 문서만 선택 가능

3. **업데이트 실행**:
   - 승인된 문서만 업데이트
   - 각 파일 수정 후 변경 내용 간단히 보고

4. **완료 보고**:
   ```markdown
   ## 문서 업데이트 완료

   | 문서 | 변경 유형 | 상태 |
   |------|----------|------|
   | {파일명} | {변경 유형} | ✅ |
   ```

### Phase 5: 연관 문서 검증

1. **상호 참조 검증**:
   - spec 문서의 FR이 test-case에 매핑되어 있는지 확인
   - task 완료 상태와 실제 코드 구현 일치 확인

2. **누락 항목 경고** (해당 시):
   ```markdown
   ⚠️ 검토 필요:
   - {FR-ID} (신규)에 대한 test-case가 없습니다
   - {US-ID} 상태가 문서 간 불일치합니다
   ```

## Error Handling

### 관련 문서를 찾을 수 없는 경우

```markdown
⚠️ 관련 문서를 찾을 수 없습니다:
- 도메인: {domain}
- 예상 경로: {expected_path}

다음 중 하나를 선택해주세요:
1. 새 문서 생성
2. 다른 경로 지정
3. 업데이트 건너뛰기
```

### 컨텍스트 부족

```markdown
⚠️ 업데이트할 문서를 결정하기 위한 정보가 부족합니다.

다음 정보를 제공해주세요:
- 수정한 기능/도메인명
- 참조한 문서 경로
- 변경 유형 (기능 추가/버그 수정/리팩토링/테스트 추가)
```

## 체크 스킵 조건

다음 경우 문서 업데이트를 스킵:
- 코드 변경 없이 문서만 수정한 경우
- 설정 파일만 변경된 경우 (`.json`, `.yaml`, `.yml`, `.properties`)
- 빌드/의존성 파일만 변경된 경우 (`build.gradle`, `package.json`)

## 주의사항

- 모든 문서 업데이트는 사용자 확인 후 진행
- ADR은 생성 제안만 하고, 실제 생성은 별도 확인 필요
- 변경 이력의 작성자는 "Claude"로 표시
- 기존 문서의 형식과 스타일 유지
- 업데이트 시 기존 내용 손상 주의
