# IGRUS Web Frontend

IGRUS 동아리 웹사이트 프론트엔드

## 기술 스택

- React 19 + Vite 7
- TypeScript
- Tanstack Query + Orval
- Tailwind CSS
- Zustand
- React Hook Form + Zod

## 시작하기

### 1. 의존성 설치

```bash
pnpm install
```

### 2. 환경 변수 설정

`.env.local` 파일 생성:

```bash
VITE_API_URL=http://localhost:8080
VITE_SWAGGER_URL=http://localhost:8080/v3/api-docs
```

### 3. 개발 서버 실행

```bash
pnpm dev
```

http://localhost:5173 에서 확인

## 스크립트

| 명령어 | 설명 |
|--------|------|
| `pnpm dev` | 개발 서버 실행 |
| `pnpm build` | 프로덕션 빌드 |
| `pnpm preview` | 빌드 결과 미리보기 |
| `pnpm orval` | API 클라이언트 생성 |

## Orval (API 클라이언트 생성)

백엔드 OpenAPI 스펙을 기반으로 API 호출 훅을 자동 생성합니다.

### 사용법

1. **백엔드 서버 실행** (OpenAPI 스펙 제공 필요)

2. **Orval 실행**
   ```bash
   pnpm orval
   ```

3. **생성된 파일 확인**
   - `src/api/model/` 폴더에 생성됨
   - 자동 생성된 파일은 직접 수정하지 말 것

### 생성된 훅 사용 예시

```tsx
import { useGetUsers, useCreateUser } from "@/api";

// 조회
const { data, isLoading } = useGetUsers();

// 생성
const { mutate } = useCreateUser();
mutate({ data: { name: "홍길동" } });
```

## 폴더 구조

```
src/
├── api/           # API 클라이언트 (Orval 생성)
│   ├── client.ts  # fetch 설정
│   └── model/     # 자동 생성 (수정 금지)
├── assets/        # 정적 파일
├── components/    # 컴포넌트
├── hooks/         # 커스텀 훅
├── pages/         # 페이지
├── stores/        # Zustand 스토어
└── utils/         # 유틸리티
```

## 개발 규칙

자세한 내용은 `CLAUDE.md` 참고
