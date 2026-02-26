# Test Case Writer Agent Memory

## 프로젝트 구조
- 테스트 케이스 문서 위치: `docs/test-case/{도메인}/`
- 검증 기준서 위치: `docs/criteria/{도메인}/`
- 파일 명명: `TC-{도메인}-{기능}.md` 또는 `image-presigned-url-test-cases.md` 등 서술적 이름
- 폴더 명명: 소문자 영어, 하이픈 구분 (예: `storage`, `event`, `user`)

## 검증 기준서 패턴
- 불변조건 ID 형식: `STOR-INV-XX`, `EVT-INV-XX` 등 도메인별 접두사
- 보안 정책 ID 형식: `SEC-STOR-XX`, `SEC-EVT-XX` 등
- 검증 기준서에는 8개 영역 적용: 도메인 규칙, 상태 모델, 시스템 경계, 외부 의존성, 입력 경계값, 보안, 관측 가능성, 테스트 전략

## API 패턴
- 엔드포인트 접두사: `/api/`
- 인증: JWT (Authorization 헤더), Spring Security
- 역할: ASSOCIATE, MEMBER, OPERATOR, ADMIN
- 에러 응답: 400 (입력 오류), 401 (미인증), 403 (권한 부족), 404 (미존재), 409 (충돌), 500 (서버 오류)

## 테스트 레벨 분류
- 단위 테스트: 비즈니스 로직, 검증 로직, Key 생성 등 (S3 의존성 없음)
- 서비스 통합 테스트: S3Client Mock(Mockito), DB 연동
- 통합 테스트 (Controller): MockMvc, 인증/인가 검증
- E2E 테스트: LocalStack 또는 실제 S3 (선택)

## 공통 사전조건 패턴
- 인증 상태: "인증된 사용자(ASSOCIATE 이상) 로그인 상태, 유효한 액세스 토큰 보유"
- S3 Mock: "S3Client를 Mock으로 대체"
- DB 상태: "REQUESTED/COMPLETED/FAILED/EXPIRED 상태의 메타데이터 DB 존재"

## 검증 기준서 목록 (확인됨)
- `docs/criteria/storage/image-presigned-url-verification-criteria.md`
- `docs/criteria/event/event-verification-criteria.md`
- `docs/criteria/event/event-registration-verification-criteria.md`
- `docs/criteria/inquiry-verification-criteria.md`
- `docs/criteria/verification-criteria.md` (회원가입/승인/강등)
- `docs/criteria/user/signup/interests-join-route-verification-criteria.md`
- `docs/criteria/user/signup/temporary-student-id-verification-criteria.md`
