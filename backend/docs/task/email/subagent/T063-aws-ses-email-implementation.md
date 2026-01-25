# T063: AWS SES 이메일 전송 구현

## 상태: 계획 완료 (구현 대기)

## 개요
프로덕션 환경에서 AWS SES를 사용하여 이메일을 전송하도록 변경한다.

### 변경 사항
- 기존 `SmtpEmailService` → `SesEmailService`로 대체 (삭제)
- `InquiryNotificationService`도 SES로 전환
- AWS 자격 증명: IAM Role 사용

---

## 수정 파일 목록

### 1. 의존성 추가
**파일**: `backend/build.gradle`
```groovy
implementation platform('software.amazon.awssdk:bom:2.25.0')
implementation 'software.amazon.awssdk:ses'
```

### 2. 설정 파일 수정
**파일**: `backend/src/main/resources/application.yml`
- `app.aws.ses.region` 설정 추가 (기본값: ap-northeast-2)

### 3. 신규 파일 생성

| 파일 | 설명 |
|------|------|
| `backend/src/main/java/igrus/web/common/config/AwsSesConfig.java` | SesClient 빈 설정 (`@Profile("!local & !test")`) |
| `backend/src/main/java/igrus/web/security/auth/common/service/SesEmailService.java` | SES 기반 EmailService 구현체 |
| `backend/src/main/java/igrus/web/inquiry/service/SesInquiryNotificationService.java` | SES 기반 InquiryNotificationService 구현체 |

### 4. 기존 파일 수정

| 파일 | 변경 내용 |
|------|----------|
| `backend/src/main/java/igrus/web/inquiry/service/LoggingInquiryNotificationService.java` | `@Profile({"local", "test"})` 추가 |

### 5. 삭제 파일

| 파일 | 이유 |
|------|------|
| `backend/src/main/java/igrus/web/security/auth/common/service/SmtpEmailService.java` | SesEmailService로 대체 |

---

## 구현 순서

### Step 1: 의존성 추가
- build.gradle에 AWS SDK v2 SES 의존성 추가

### Step 2: 설정 추가
- application.yml에 AWS SES 리전 설정 추가

### Step 3: AwsSesConfig 생성
- SesClient 빈 설정
- `@Profile("!local & !test")`로 프로덕션 전용
- DefaultCredentialsProvider 사용 (IAM Role 지원)

### Step 4: SesEmailService 구현
- EmailService 인터페이스 구현
- `@Profile("!local & !test")`
- sendVerificationEmail, sendPasswordResetEmail, sendWelcomeEmail 구현
- 기존 이메일 템플릿 내용 유지

### Step 5: SesInquiryNotificationService 구현
- InquiryNotificationService 인터페이스 구현
- `@Profile("!local & !test")`
- sendInquiryConfirmation, sendReplyNotification 구현

### Step 6: LoggingInquiryNotificationService 수정
- `@Profile({"local", "test"})` 어노테이션 추가

### Step 7: SmtpEmailService 삭제
- 파일 삭제

### Step 8: 테스트 작성
- SesEmailServiceTest 단위 테스트
- SesInquiryNotificationServiceTest 단위 테스트

### Step 9: 빌드 및 검증
- `./gradlew build` 실행하여 컴파일 및 테스트 통과 확인

---

## 검증 방법

### 1. 로컬 환경 테스트
- `SPRING_ACTIVE_PROFILE=local`로 실행
- 이메일 발송 시 로그만 출력되는지 확인

### 2. 단위 테스트
- `./gradlew test` 실행
- SesEmailService, SesInquiryNotificationService 테스트 통과 확인

### 3. 프로덕션 배포 전 확인사항
- AWS SES에서 발신자 이메일/도메인 검증 완료
- EC2/ECS에 SES 권한 포함된 IAM Role 부여
- SES 샌드박스 해제 (프로덕션 발송을 위해)

---

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `AWS_SES_REGION` | SES 리전 | `ap-northeast-2` |
| `MAIL_FROM_ADDRESS` | 발신자 이메일 | `noreply@igrus.inha.ac.kr` |

> IAM Role 사용 시 AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY 불필요

---

## AWS SES 사전 준비 사항

### AWS Console 설정

1. **SES 도메인/이메일 검증**
   - `igrus.inha.ac.kr` 도메인 또는 `noreply@igrus.inha.ac.kr` 이메일 검증
   - DNS TXT 레코드 추가 필요

2. **샌드박스 해제 요청**
   - 초기에는 검증된 이메일로만 발송 가능
   - 프로덕션 사용을 위해 AWS에 샌드박스 해제 요청

3. **IAM 정책 설정**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "ses:SendEmail",
           "ses:SendRawEmail"
         ],
         "Resource": "*"
       }
     ]
   }
   ```

---

## 관련 파일 참조

### 현재 구현
- [EmailService.java](../../../src/main/java/igrus/web/security/auth/common/service/EmailService.java) - 이메일 서비스 인터페이스
- [SmtpEmailService.java](../../../src/main/java/igrus/web/security/auth/common/service/SmtpEmailService.java) - 기존 SMTP 구현체 (삭제 대상)
- [LoggingEmailService.java](../../../src/main/java/igrus/web/security/auth/common/service/LoggingEmailService.java) - 로컬/테스트 구현체
- [InquiryNotificationService.java](../../../src/main/java/igrus/web/inquiry/service/InquiryNotificationService.java) - 문의 알림 인터페이스
- [LoggingInquiryNotificationService.java](../../../src/main/java/igrus/web/inquiry/service/LoggingInquiryNotificationService.java) - 문의 알림 로깅 구현체
