# TypeScript 마이그레이션 후 남은 작업 체크리스트

## 1. 환경 설정

- [ ] TypeScript 설치 (`typescript` 패키지)
- [ ] tsconfig.json 생성 (strict 모드, path alias)
- [ ] ESLint TypeScript 플러그인 설치 (`@typescript-eslint/*`)
- [ ] vite.config.js → vite.config.ts 변환
- [ ] eslint.config.js에 TypeScript 규칙 추가

```bash
npm install -D typescript @typescript-eslint/parser @typescript-eslint/eslint-plugin
```

---

## 2. 파일 정리

- [ ] 모든 .js/.jsx 원본 파일 삭제
- [ ] index.html 수정: `src/main.jsx` → `src/main.tsx`
- [ ] import 경로 확장자 변경 반영

---

## 3. 타입 보완

- [ ] 외부 라이브러리 `@types/*` 추가 (필요시)
- [ ] API 응답 타입과 백엔드 실제 응답 일치 확인
- [ ] 런타임 타입 가드 함수 추가 (필요시)

---

## 4. 빌드 및 검증

- [ ] `npx tsc --noEmit` - 타입 체크 통과
- [ ] `npm run lint` - 린트 통과
- [ ] `npm run build` - 빌드 성공
- [ ] `npm run dev` - 개발 서버 정상 실행

---

## 5. 문서화

- [ ] CLAUDE.md 기술 스택에 TypeScript 추가
- [ ] README 개발 환경 설정 업데이트

---

## 6. 선택적 개선

- [ ] Path alias 설정 (`@/` → `src/`)
- [ ] Strict 모드 단계적 강화
- [ ] 제너릭 훅 리팩토링 (공통 패턴 추출)

---

## 요약

| 우선순위 | 작업 | 상태 |
|----------|------|------|
| 1 | 환경 설정 (TypeScript, tsconfig, ESLint) | [ ] |
| 2 | 빌드 검증 (tsc, build) | [ ] |
| 3 | 파일 정리 (원본 삭제, 경로 수정) | [ ] |
| 4 | 문서화 | [ ] |
| 5 | 선택적 개선 | [ ] |
