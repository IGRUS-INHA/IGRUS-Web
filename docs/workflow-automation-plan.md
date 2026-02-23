# 팀 워크플로 자동화 계획

> 백엔드-프론트엔드 팀 간 소통 개선을 위한 GitHub Projects + n8n + MCP 기반 자동화

## 개요

| 항목 | 내용 |
|------|------|
| 목표 | 팀 간 작업 현황 공유, PR 연동 자동화, AI 일일 브리핑 |
| 핵심 도구 | GitHub Projects, n8n, MCP, LLM |
| 대상 | IGRUS-Web 백엔드/프론트엔드 팀 |

## 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                    GitHub Projects                   │
│          (칸반보드 · PR 연동 · 담당자 관리)            │
└────────────────────────┬────────────────────────────┘
                         │ Webhook
                         ▼
┌─────────────────────────────────────────────────────┐
│                       n8n                            │
│              (워크플로 자동화 엔진)                     │
│                                                      │
│  ┌───────────────┐  ┌───────────────────────────┐   │
│  │ PR 이벤트 처리 │  │ 일일 브리핑 스케줄러 (매일)  │   │
│  └───────┬───────┘  └─────────────┬─────────────┘   │
│          │                        │                  │
│          ▼                        ▼                  │
│  보드 상태 자동 변경         MCP 서버 호출             │
│                          ┌────┴────┐                 │
│                          ▼         ▼                 │
│                    GitHub MCP  Projects MCP          │
│                          │         │                 │
│                          ▼         ▼                 │
│                     LLM 브리핑 생성                    │
│                          │                           │
│                          ▼                           │
│                   Discord/Slack 전송                  │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1: GitHub Projects 칸반보드 세팅

### 목표

GitHub Projects를 팀 공용 칸반보드로 활용하여 작업 현황을 시각적으로 관리한다.

### 보드 컬럼 구성

```
Backlog → Todo → In Progress → In Review → Done
```

| 컬럼 | 설명 |
|------|------|
| **Backlog** | 아직 착수하지 않은 작업 |
| **Todo** | 이번 스프린트/주에 처리할 작업 |
| **In Progress** | 현재 진행 중인 작업 |
| **In Review** | PR이 올라가 리뷰 대기 중인 작업 |
| **Done** | 머지 완료된 작업 |

### 카드 규칙

- 카드 1개 = GitHub Issue 1개
- 라벨로 팀 구분: `team:frontend`, `team:backend`, `team:shared`
- 담당자(Assignee)를 반드시 지정하여 누가 작업 중인지 표시
- Issue와 PR을 연결(Link)하여 추적 가능하게 설정

### 크로스팀 코멘트 운영

- 다른 팀에서 카드에 코멘트를 남기면 라벨을 `needs-revision`으로 변경
- `needs-revision` 라벨이 붙으면 n8n이 자동으로 카드를 In Progress로 되돌림 (Phase 2에서 구현)

### 설정 체크리스트

- [ ] GitHub Projects 보드 생성 (Organization 레벨)
- [ ] 컬럼 5개 구성 (Backlog / Todo / In Progress / In Review / Done)
- [ ] 라벨 생성: `team:frontend`, `team:backend`, `team:shared`, `needs-revision`
- [ ] Issue 템플릿 작성 (제목, 설명, 팀 라벨, 담당자 필수)
- [ ] 팀원 전원 보드 접근 권한 설정

---

## Phase 2: GitHub PR 연동 자동화 (n8n)

### 목표

PR 이벤트에 따라 칸반보드 카드 상태를 자동으로 변경한다.

### 자동화 규칙

| 트리거 (GitHub 이벤트) | 액션 (보드 상태 변경) |
|------------------------|----------------------|
| PR 생성 (`opened`) | 연결된 카드 → **In Review** |
| PR에 변경 요청 (`changes_requested`) | 연결된 카드 → **In Progress** |
| PR 승인 (`approved`) | 상태 유지 (In Review) |
| PR 머지 (`closed` + `merged`) | 연결된 카드 → **Done** |
| Issue에 `needs-revision` 라벨 추가 | 연결된 카드 → **In Progress** |

### n8n 워크플로 구성

#### 워크플로 1: PR → 보드 상태 동기화

```
[GitHub Webhook: PR 이벤트]
        │
        ▼
[Switch: 이벤트 타입 분기]
        │
   ┌────┼────┬────────────┐
   ▼    ▼    ▼            ▼
opened  changes_req  merged  closed(not merged)
   │    │            │       │
   ▼    ▼            ▼       ▼
In Review  In Progress  Done   (무시)
        │
        ▼
[GitHub API: Project Item 상태 변경]
        │
        ▼
[Discord 알림: "PR #42가 In Review로 이동"]
```

#### 워크플로 2: needs-revision 라벨 감지

```
[GitHub Webhook: Issue 라벨 변경]
        │
        ▼
[IF: 라벨 = "needs-revision"]
        │
        ▼
[GitHub API: 카드를 In Progress로 이동]
        │
        ▼
[Discord 알림: "@담당자 - 수정 요청이 있습니다"]
```

### n8n 환경 설정

- [ ] n8n 인스턴스 배포 (Docker / 클라우드)
- [ ] GitHub App 또는 Personal Access Token 발급 (repo, project 권한)
- [ ] GitHub Webhook 연결 (PR events, Issue label events)
- [ ] n8n Credential 설정 (GitHub, Discord/Slack)
- [ ] 워크플로 1, 2 구축 및 테스트

### 필요한 GitHub API

```
# 프로젝트 아이템 상태 변경 (GraphQL)
mutation {
  updateProjectV2ItemFieldValue(
    input: {
      projectId: "<PROJECT_ID>"
      itemId: "<ITEM_ID>"
      fieldId: "<STATUS_FIELD_ID>"
      value: { singleSelectOptionId: "<OPTION_ID>" }
    }
  ) {
    projectV2Item { id }
  }
}
```

---

## Phase 3: MCP 기반 AI 일일 브리핑

### 목표

매일 아침 각 팀원에게 맞춤형 브리핑을 AI가 생성하여 전송한다.

### MCP 서버 구성

```
┌──────────────────────────────────────────┐
│             MCP 서버 (자체 구축)           │
│                                          │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │ GitHub Tool  │  │ Projects Tool   │   │
│  │             │  │                 │    │
│  │ - 내 PR 목록 │  │ - 내 카드 목록   │   │
│  │ - 리뷰 요청  │  │ - 카드 상태      │   │
│  │ - 코멘트     │  │ - 체류 시간      │   │
│  └─────────────┘  └─────────────────┘   │
│                                          │
│  ┌─────────────────────────────────────┐ │
│  │ Context Tool                        │ │
│  │                                     │ │
│  │ - 팀 정보 (누가 어떤 팀인지)          │ │
│  │ - 우선순위 규칙                      │ │
│  │ - 마감일 정보                        │ │
│  └─────────────────────────────────────┘ │
└──────────────────────────────────────────┘
```

### 브리핑 생성 워크플로 (n8n)

```
[Cron: 매일 09:00]
        │
        ▼
[팀원 목록 조회]
        │
        ▼
[For Each 팀원]
        │
        ▼
[MCP 서버 호출]
  ├── GitHub Tool: 해당 유저의 PR, 리뷰 요청, 이슈 조회
  ├── Projects Tool: 해당 유저의 카드 상태, 체류 시간 조회
  └── Context Tool: 팀 정보, 우선순위 규칙 로드
        │
        ▼
[LLM (Claude API)]
  - 시스템 프롬프트: 브리핑 생성 규칙
  - 수집된 데이터를 기반으로 브리핑 작성
        │
        ▼
[Discord/Slack DM으로 개인별 전송]
```

### 브리핑 출력 형식

```
📋 일일 브리핑 - 홍길동 (Frontend)
2025-01-15 09:00

🔴 긴급
  • 회원가입 API 변경 PR #38에 백엔드팀 코멘트 (3일 경과, needs-revision)

🟡 오늘 할 일
  • 로그인 페이지 리디자인 (In Progress, 2일차)
  • PR #42 코드 리뷰 요청 from 김OO

🟢 참고
  • 어제 머지된 PR: #40 마이페이지 반응형 개선
  • 백엔드팀 진행 중: 게시판 API 개발 (예상 완료: 내일)
```

### 우선순위 판단 기준

| 우선순위 | 조건 |
|----------|------|
| 🔴 긴급 | `needs-revision` 라벨 + 2일 이상 경과, 리뷰 요청 3일 이상 방치 |
| 🟡 오늘 할 일 | In Progress 상태인 내 카드, 할당된 리뷰 요청 |
| 🟢 참고 | 어제 완료된 항목, 다른 팀 진행 상황 |

### MCP 서버 기술 스택

| 항목 | 선택 |
|------|------|
| 런타임 | Node.js (TypeScript) |
| MCP SDK | `@modelcontextprotocol/sdk` |
| GitHub API | Octokit (`@octokit/rest`) |
| 배포 | Docker (n8n과 같은 환경) |

### 설정 체크리스트

- [ ] MCP 서버 프로젝트 초기화 (TypeScript + MCP SDK)
- [ ] GitHub Tool 구현 (PR, 리뷰, 이슈 조회)
- [ ] Projects Tool 구현 (카드 상태, 체류 시간 조회)
- [ ] Context Tool 구현 (팀 정보, 우선순위 규칙)
- [ ] n8n에서 MCP 서버 연동 워크플로 구축
- [ ] LLM 브리핑 프롬프트 작성 및 테스트
- [ ] Discord/Slack 봇 연동
- [ ] 전체 파이프라인 E2E 테스트

---

## 단계별 일정 (권장)

| 단계 | 작업 | 예상 산출물 |
|------|------|------------|
| **Phase 1** | GitHub Projects 세팅 | 운영 가능한 칸반보드 |
| **Phase 2** | n8n + PR 연동 자동화 | PR↔보드 자동 동기화 |
| **Phase 3** | MCP 서버 + AI 브리핑 | 매일 아침 개인별 브리핑 |

> Phase 1은 별도 개발 없이 바로 시작 가능하고, Phase 2-3은 순차적으로 진행한다.
