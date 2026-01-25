# 인증 기능 미완료 태스크 구현 계획

## 개요

`auth_tasks.md`의 Phase 9 (Polish) 중 미완료 태스크들의 상세 구현 계획입니다.

## 태스크 목록

| 태스크 ID | 제목 | 우선순위 | 복잡도 | 문서 |
|----------|------|---------|--------|------|
| T059 | Refresh Token 정리 스케줄러 | 낮음 | 낮음 | [T059-refresh-token-cleanup-scheduler.md](./T059-refresh-token-cleanup-scheduler.md) |
| T060 | 탈퇴 후 개인정보 삭제 스케줄러 | 중간 | 중간 | [T060-withdrawn-user-cleanup-scheduler.md](./T060-withdrawn-user-cleanup-scheduler.md) |
| T061 | 이메일 재시도 로직 | 중간 | 중간 | [T061-email-retry-logic.md](./T061-email-retry-logic.md) |
| T062 | JWT 필터 계정 상태 검증 | 높음 | 낮음 | [T062-jwt-filter-account-status-validation.md](./T062-jwt-filter-account-status-validation.md) |

## 권장 구현 순서

### 1단계: 보안 강화 (우선)
1. **T062**: JWT 필터 계정 상태 검증
   - 보안 취약점 해결
   - 구현 복잡도 낮음
   - 다른 태스크와 의존성 없음

### 2단계: 스케줄러 구현
2. **T059**: Refresh Token 정리 스케줄러
   - 가장 단순한 스케줄러
   - 기존 스케줄러 패턴 참고 가능

3. **T060**: 탈퇴 후 개인정보 삭제 스케줄러
   - User 엔티티 수정 필요
   - 마이그레이션 스크립트 필요

### 3단계: 이메일 개선
4. **T061**: 이메일 재시도 로직
   - 새 의존성 추가 필요 (spring-retry)
   - 기존 서비스 코드 수정 필요

## 공통 체크리스트

각 태스크 구현 시 확인 사항:

- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성 (필요 시)
- [ ] 로깅 적절히 추가
- [ ] 예외 처리 확인
- [ ] `auth_tasks.md` 상태 업데이트
- [ ] Swagger 문서화 (API 변경 시)

## 예상 소요 시간

| 태스크 | 예상 시간 |
|-------|----------|
| T059 | 1-2시간 |
| T060 | 3-4시간 |
| T061 | 2-3시간 |
| T062 | 1-2시간 |
| **총계** | **7-11시간** |

## 관련 문서

- [auth_tasks.md](../auth_tasks.md) - 전체 태스크 현황
- [auth-spec.md](/docs/feature/auth/auth-spec.md) - 인증 기능 명세