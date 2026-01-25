# 이메일 기능 태스크 구현 계획

## 개요

이메일 관련 기능의 태스크 구현 계획입니다.

## 태스크 목록

| 태스크 ID | 제목 | 우선순위 | 복잡도 | 상태 | 문서 |
|----------|------|---------|--------|------|------|
| T063 | AWS SES 이메일 전송 구현 | 중간 | 중간 | 계획 완료 | [T063-aws-ses-email-implementation.md](./T063-aws-ses-email-implementation.md) |

## 구현 순서

### 1단계: AWS SES 통합
1. **T063**: AWS SES 이메일 전송 구현
   - SmtpEmailService → SesEmailService 대체
   - InquiryNotificationService SES 전환
   - 단위 테스트 작성

## 공통 체크리스트

각 태스크 구현 시 확인 사항:

- [ ] 단위 테스트 작성
- [ ] 로깅 적절히 추가
- [ ] 예외 처리 확인
- [ ] 프로파일 분리 확인 (local/test vs prod)

## 관련 문서

- [auth_tasks.md](../auth/auth_tasks.md) - 인증 관련 태스크 현황 (이메일 인증 포함)
