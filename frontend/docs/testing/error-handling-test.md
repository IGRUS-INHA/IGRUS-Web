# 에러 처리 테스트 가이드

에러 처리 리팩토링 후 자동/수동 테스트 가이드입니다.

## 목차
- [자동 테스트](#자동-테스트)
- [수동 테스트](#수동-테스트)
- [테스트 체크리스트](#테스트-체크리스트)

---

## 자동 테스트

### 1. TypeScript 타입 체크

```bash
cd frontend
npm run type-check
```

**확인 사항:**
- ✅ `any` 타입 에러 없음
- ✅ 타입 단언 에러 없음
- ✅ ApiError 관련 import 에러 없음
- ✅ 헬퍼 함수 타입 에러 없음

### 2. 빌드 테스트

```bash
cd frontend
npm run build
```

**확인 사항:**
- ✅ 빌드 에러 없음
- ✅ 경고 메시지 없음
- ✅ dist 디렉토리 정상 생성

### 3. 유닛 테스트 (향후 작성)

**utils/error.ts 테스트:**

```typescript
// tests/utils/error.test.ts (예시)
import { describe, it, expect } from 'vitest';
import { ApiError } from '@/types/error';
import {
  isApiError,
  hasErrorCode,
  isBoardReadDenied,
  isForbiddenError,
  getErrorMessage,
} from '@/utils/error';

describe('isApiError', () => {
  it('should return true for ApiError instance', () => {
    const error = new ApiError(404, 'NOT_FOUND', 'Not found');
    expect(isApiError(error)).toBe(true);
  });

  it('should return false for regular Error', () => {
    const error = new Error('message');
    expect(isApiError(error)).toBe(false);
  });
});

describe('isForbiddenError', () => {
  it('should return true for 403 status', () => {
    const error = new ApiError(403, 'HTTP_403', 'Forbidden');
    expect(isForbiddenError(error)).toBe(true);
  });

  it('should return true for BOARD_READ_DENIED code', () => {
    const error = new ApiError(403, 'BOARD_READ_DENIED', 'Access denied');
    expect(isForbiddenError(error)).toBe(true);
  });

  it('should return false for non-forbidden error', () => {
    const error = new ApiError(404, 'NOT_FOUND', 'Not found');
    expect(isForbiddenError(error)).toBe(false);
  });
});

describe('getErrorMessage', () => {
  it('should return mapped message for known error code', () => {
    const error = new ApiError(403, 'BOARD_READ_DENIED', 'Some message');
    expect(getErrorMessage(error)).toBe('게시판 읽기 권한이 없습니다.');
  });

  it('should return backend message if no mapping exists', () => {
    const error = new ApiError(500, 'UNKNOWN_CODE', 'Backend error message');
    expect(getErrorMessage(error)).toBe('Backend error message');
  });

  it('should return HTTP status message as fallback', () => {
    const error = new ApiError(500, 'UNKNOWN_CODE', '');
    expect(getErrorMessage(error)).toBe('서버 오류가 발생했습니다.');
  });
});
```

**client.ts 통합 테스트:**

```typescript
// tests/api/client.test.ts (예시)
import { describe, it, expect, vi } from 'vitest';
import { ApiError } from '@/types/error';
import { customFetch } from '@/api/client';

describe('customFetch error handling', () => {
  it('should throw ApiError with code from backend', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({
        status: 403,
        code: 'BOARD_READ_DENIED',
        message: '게시판 읽기 권한이 없습니다',
        timestamp: '2024-01-01T00:00:00Z',
      }),
    });

    await expect(customFetch('/test')).rejects.toThrow(ApiError);
    await expect(customFetch('/test')).rejects.toMatchObject({
      status: 403,
      code: 'BOARD_READ_DENIED',
      message: '게시판 읽기 권한이 없습니다',
    });
  });

  it('should use default code when backend does not provide code', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({
        message: 'Internal server error',
      }),
    });

    await expect(customFetch('/test')).rejects.toMatchObject({
      status: 500,
      code: 'HTTP_500',
      message: 'Internal server error',
    });
  });
});
```

---

## 수동 테스트

### 1. 에러 코드 있는 경우 (정상 케이스)

#### 테스트 시나리오 1: 게시판 읽기 권한 없음 (403)

**테스트 단계:**
1. 비회원 또는 준회원 계정으로 로그인
2. 일반게시판(/board/general) 또는 정보공유(/board/info) 접근
3. 403 에러 발생

**기대 결과:**
- ✅ `error.code === 'BOARD_READ_DENIED'`
- ✅ "정회원 승인 후 게시판 이용이 가능합니다." 메시지 표시
- ✅ 로그아웃되지 않음

#### 테스트 시나리오 2: 게시글 삭제 권한 없음 (403)

**테스트 단계:**
1. 사용자 A로 로그인
2. 사용자 B가 작성한 게시글 상세 페이지 접근
3. "삭제" 버튼 클릭 (권한 체크 우회 테스트)
4. 403 에러 발생

**기대 결과:**
- ✅ `error.status === 403`
- ✅ "삭제 권한이 없습니다." 메시지 표시
- ✅ 로그아웃되지 않음

#### 테스트 시나리오 3: 게시글 없음 (404)

**테스트 단계:**
1. 존재하지 않는 게시글 ID로 접근 (예: /board/general/999999)
2. 404 에러 발생

**기대 결과:**
- ✅ `error.status === 404`
- ✅ "게시글을 찾을 수 없습니다." 메시지 표시

#### 테스트 시나리오 4: 게시글 작성 권한 없음 (403)

**테스트 단계:**
1. 준회원 계정으로 로그인
2. 자유게시판 또는 정보공유 게시글 작성 페이지 접근
3. 게시글 작성 후 "작성" 버튼 클릭
4. 403 에러 발생

**기대 결과:**
- ✅ "권한이 없습니다" 안내 메시지 표시
- ✅ 게시판별 필요 권한 안내 포함

### 2. 에러 코드 없는 경우 (Default 코드 생성)

#### 테스트 시나리오 5: 백엔드에서 code 필드 누락

**시뮬레이션:**
```typescript
// 백엔드 응답 (code 필드 없음)
{
  message: 'Some error occurred',
  status: 500
}
```

**기대 결과:**
- ✅ `error.code === 'HTTP_500'`
- ✅ 에러가 정상적으로 throw되고 catch됨
- ✅ 사용자에게 적절한 에러 메시지 표시

#### 테스트 시나리오 6: 네트워크 에러

**테스트 단계:**
1. 백엔드 서버 중단
2. API 요청 수행
3. 네트워크 에러 발생

**기대 결과:**
- ✅ 적절한 에러 메시지 표시 (connection refused 등)
- ✅ 애플리케이션이 크래시되지 않음

### 3. 인증 관련 에러

#### 테스트 시나리오 7: 토큰 만료 (401)

**테스트 단계:**
1. 로그인 후 대기 (토큰 만료 시간까지)
2. API 요청 수행
3. 401 에러 발생 → 토큰 자동 갱신

**기대 결과:**
- ✅ 토큰 자동 갱신 시도
- ✅ 갱신 성공 시 원래 요청 재시도
- ✅ 갱신 실패 시 로그인 페이지로 이동

#### 테스트 시나리오 8: 로그인 필요 (401)

**테스트 단계:**
1. 비로그인 상태
2. 로그인 필요 페이지 접근 (게시글 작성 등)
3. 401 에러 발생

**기대 결과:**
- ✅ "로그인이 필요합니다" 메시지 표시
- ✅ 로그인 페이지로 리다이렉트

---

## 테스트 체크리스트

### 게시판 기능

**게시판 목록:**
- [ ] 비회원 게시판 접근 시 권한 에러 표시
- [ ] 게시글 목록 정상 로딩
- [ ] 빈 게시판 처리 (빈 배열 응답)
- [ ] 404 에러 처리 (없는 게시판)

**게시글 상세:**
- [ ] 게시글 상세 정상 로딩
- [ ] 404 에러 처리 (없는 게시글)
- [ ] 삭제된 게시글 처리 (410 Gone)

**게시글 작성:**
- [ ] 게시글 작성 권한 체크
- [ ] 제목 길이 제한 에러 (POST_TITLE_TOO_LONG)
- [ ] 이미지 개수 제한 에러 (POST_IMAGE_LIMIT_EXCEEDED)
- [ ] Rate limit 에러 (POST_RATE_LIMIT_EXCEEDED)

**게시글 수정:**
- [ ] 게시글 수정 권한 체크
- [ ] 404 에러 처리 (없는 게시글)
- [ ] 403 에러 처리 (수정 권한 없음)

**게시글 삭제:**
- [ ] 게시글 삭제 권한 체크
- [ ] 404 에러 처리 (없는 게시글)
- [ ] 403 에러 처리 (삭제 권한 없음)

### 댓글 기능

**댓글 작성:**
- [ ] 댓글 내용 비어있음 에러 (COMMENT_CONTENT_EMPTY)
- [ ] 댓글 길이 제한 에러 (COMMENT_CONTENT_TOO_LONG)
- [ ] 대댓글 제한 에러 (REPLY_TO_REPLY_NOT_ALLOWED)
- [ ] 삭제된 게시글 댓글 작성 에러 (POST_DELETED_CANNOT_COMMENT)

**댓글 삭제:**
- [ ] 댓글 삭제 권한 체크
- [ ] 404 에러 처리 (없는 댓글)

**댓글 좋아요:**
- [ ] 자신의 댓글 좋아요 에러 (CANNOT_LIKE_OWN_COMMENT)
- [ ] 중복 좋아요 에러 (ALREADY_LIKED_COMMENT)

### 에러 메시지

**메시지 우선순위:**
- [ ] API_ERROR_MESSAGES 매핑된 메시지 우선 표시
- [ ] 백엔드 메시지 2순위 표시
- [ ] HTTP 상태 코드 기반 기본 메시지 3순위 표시

**사용자 친화성:**
- [ ] 사용자 친화적 메시지 표시
- [ ] 기술적 에러 코드 노출되지 않음
- [ ] 적절한 액션 가이드 포함

### 타입 안정성

**TypeScript 타입 체크:**
- [ ] `any` 타입 사용 없음
- [ ] 타입 단언 최소화
- [ ] ApiError 타입 가드 정상 작동
- [ ] 헬퍼 함수 타입 추론 정상

### 하위 호환성

**기존 동작 유지:**
- [ ] 에러 메시지 동일
- [ ] UI 동작 동일
- [ ] 로그아웃 시점 동일
- [ ] 리다이렉트 동작 동일

---

## 회귀 테스트 전략

### 단계별 검증

**Phase 완료 시마다:**
1. TypeScript 컴파일 실행
2. 빌드 테스트 실행
3. 수동 주요 시나리오 테스트

**마이그레이션한 페이지:**
1. E2E 테스트로 검증 (향후)
2. 모든 에러 케이스 수동 테스트
3. 에러 로깅 확인

### 주의사항

**변경 금지 사항:**
- ❌ 기존 동작 변경 금지 (에러 메시지는 그대로 유지)
- ❌ 새로운 버그 유입 방지
- ❌ 사용자 경험 변경 금지

**모니터링:**
- ✅ 에러 로깅 추가로 모니터링 강화
- ✅ 프로덕션 배포 후 에러 발생 빈도 체크
- ✅ 사용자 피드백 모니터링

---

## 테스트 결과 기록

### 테스트 실행 기록

**날짜:** YYYY-MM-DD
**테스터:** [이름]
**환경:** Dev / Staging / Production

| 테스트 항목 | 상태 | 비고 |
|-----------|------|------|
| TypeScript 타입 체크 | ⬜ Pass / ⬜ Fail | |
| 빌드 테스트 | ⬜ Pass / ⬜ Fail | |
| 게시판 권한 에러 | ⬜ Pass / ⬜ Fail | |
| 게시글 CRUD 에러 | ⬜ Pass / ⬜ Fail | |
| 댓글 에러 처리 | ⬜ Pass / ⬜ Fail | |
| 토큰 갱신 | ⬜ Pass / ⬜ Fail | |
| Default 코드 생성 | ⬜ Pass / ⬜ Fail | |

**발견된 이슈:**
- (이슈 내용)

**해결 방법:**
- (해결 방법)

---

## 참고 문서

- [에러 처리 구현 계획](../../plans/crispy-honking-marble.md)
- [마이그레이션 가이드](../migration/error-handling-migration.md)
- [CLAUDE.md - 에러 처리 규칙](../../CLAUDE.md)
