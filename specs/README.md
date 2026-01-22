# Specs

이 폴더는 **Speckit** 워크플로우를 통해 생성된 기능 명세 및 구현 계획을 관리합니다.

## 폴더 구조

```
specs/
├── README.md
└── {feature-id}/          # 기능별 폴더 (예: 001-enhanced-prd)
    ├── spec.md            # 기능 명세서
    ├── plan.md            # 구현 계획서
    ├── tasks.md           # 작업 목록
    └── checklists/        # 체크리스트
        └── requirements.md
```

## Speckit 워크플로우

Speckit은 기능 개발을 체계적으로 진행하기 위한 도구입니다.

| 단계 | 명령어 | 산출물 | 설명 |
|------|--------|--------|------|
| 1 | `/specify` | `spec.md` | 기능 요구사항 명세 |
| 2 | `/plan` | `plan.md` | 구현 설계 및 계획 |
| 3 | `/tasks` | `tasks.md` | 세부 작업 목록 |
| 4 | `/implement` | 코드 | 작업 기반 구현 |

## docs 폴더와의 차이

| 폴더 | 용도 |
|------|------|
| `docs/` | 프로젝트 레벨 문서 (ADR, PRD, 가이드 등) |
| `specs/` | 기능별 구현 명세 (Speckit 워크플로우 산출물) |

## 참고

- 각 기능 폴더는 브랜치명과 동일하게 관리됩니다
- 기능 구현 완료 후에도 명세는 보존하여 히스토리로 활용합니다
