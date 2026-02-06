# 아이그루스 시스템 명세서 V2

**버전**: 2.1
**작성일**: 2026-01-22
**최종 수정일**: 2026-02-06
**상태**: In Progress

---

## 구현 현황 요약

> 이 섹션은 현재 구현 상태를 반영합니다. (2026-02-05 기준)

### 백엔드 구현 현황

| 기능 | 엔티티 | API | 서비스 | 상태 |
|------|--------|-----|--------|------|
| 회원가입/로그인/토큰 | ✅ | ✅ | ✅ | 완전 구현 |
| 게시판/게시글/댓글 | ✅ | ✅ | ✅ | 완전 구현 |
| 좋아요/북마크 | ✅ | ✅ | ✅ | 완전 구현 |
| 행사/신청 | ✅ | ✅ | ✅ | 완전 구현 |
| 문의 (회원/비회원/관리) | ✅ | ✅ | ✅ | 완전 구현 |
| 관리자 기능 | ✅ | ✅ | ✅ | 완전 구현 |
| 학기별 회원 명단 | ✅ | ✅ | ✅ | 완전 구현 |
| 인증/보안 | ✅ | ✅ | ✅ | 완전 구현 |
| 건의 사항 | ❌ | ❌ | ❌ | 미구현 |

### 프론트엔드 구현 현황

| 기능 | 구현 상태 | 비고 |
|------|----------|------|
| 로그인/회원가입 | ✅ 완전 구현 | 이메일 인증 flow 포함 |
| 비밀번호 찾기/재설정 | ✅ 완전 구현 | |
| 게시판/게시글 | ✅ 완전 구현 | 검색, 정렬, 페이지네이션 |
| 좋아요/북마크 | ✅ 완전 구현 | |
| 댓글 | ⚠️ 부분 구현 | API 연동 진행 중 |
| 행사 목록/상세 | ✅ 완전 구현 | 신청/취소 기능 포함 |
| 행사 작성/수정 | ⚠️ 부분 구현 | 페이지 정의됨 |
| 문의 | ✅ 완전 구현 | 회원/비회원 구분 |
| 마이페이지 | ⚠️ 부분 구현 | Mock 데이터 사용 중 |
| 관리자 대시보드 | ⚠️ 부분 구현 | 기본 UI 구현 |
| 관리자 회원관리 | ❌ 미구현 | 스텁만 존재 |
| 관리자 문의관리 | ❌ 미구현 | 스텁만 존재 |
| 건의 사항 작성 | ❌ 미구현 | |
| 건의 사항 관리 (운영진) | ❌ 미구현 | |

---

## Clarifications

### Session 2026-02-06 (9차)
- 건의 사항 기능 추가 (기능 명세 9번)
  - 정회원 이상 작성 가능
  - 익명/실명 선택 가능
  - 익명 건의: 운영진은 작성자 확인 불가, 관리자만 확인 가능
  - 건의 목록 조회: 운영진 이상만 가능
  - 정회원: 본인 건의만 마이페이지에서 조회 가능
  - 답변 시 작성자에게 이메일 자동 발송
- 구현 현황: 백엔드/프론트엔드 모두 미구현 상태
- API: `/api/v1/suggestions` 경로 명세 추가

### Session 2026-02-05 (8차)
- Q: PRD와 실제 구현 상태 정합성 검토? → A: 구현 현황 섹션 추가, 실제 구현된 엔티티/API 반영
- 추가 구현된 기능:
  - 댓글 좋아요 (CommentLike 엔티티)
  - 댓글 신고 (CommentReport 엔티티, 관리자 검토 기능)
  - 댓글 멘션 (@사용자명 기능, 인앱/이메일 알림)
  - 조회수 중복 방지 (PostView 엔티티)
  - 로그인 시도 제한 (LoginAttempt 엔티티, 계정 잠금)
  - 학기별 회원 명단 (SemesterMember 엔티티)
  - 행사 선착순/선발제 구분 (EventRegistrationType)
  - 비회원 문의 비밀번호 검증 (GuestInquiry)
- API 버전: `/api/v1/` prefix 적용
- 이메일 발송: SMTP 또는 로깅 방식으로 구현

### Session 2026-01-22 (7차)
- Q: PRD와 백엔드 구현 정합성 검토? → A: 백엔드 구현에 맞게 PRD 데이터 모델 수정
- UserAuth → PasswordCredential 명칭 변경 (비밀번호 기반 인증이므로 더 명확한 네이밍)
- User.phone → User.phoneNumber 필드명 변경 (컬럼명 일관성)
- UserRoleHistory 엔티티 추가 (역할 변경 감사 이력 관리)
- SoftDeletableEntity 패턴 적용: deleted, deletedAt, deletedBy 필드 추가
- BaseEntity 패턴 적용: createdBy, updatedBy 필드 추가 (Audit 강화)

### Session 2026-01-22 (6차)
- Q: 칭호(title) 필드 처리? → A: 칭호 대신 직책(Position) 엔티티로 대체
- 직책 예시: 기술부, 기술부장, 회장, 부회장 등
- 다대다 관계: 한 사용자가 여러 직책 보유 가능 (UserPosition 중간 테이블)
- User.title 필드 제거, Position/UserPosition 테이블 추가

### Session 2026-01-22 (5차)
- Q: 탈퇴한 계정 복구 및 재가입 정책? → A: 탈퇴 후 5일 이내 복구 가능, 동일 학번 재가입 5일간 제한
- 복구 방법: 탈퇴한 학번+비밀번호로 로그인 시도 시 복구 확인 화면 표시
- 5일 선정 근거: 개인정보보호법 제21조 파기 기한(5일 이내)과 일치, 법적 정합성 확보
- 5일 경과 후: 개인정보 완전 파기, 복구 불가, 신규 가입 가능

### Session 2026-01-22 (4차)
- Q: 개인정보보호법 적용 범위? → A: 한국 개인정보보호법 전면 적용
- 적용 내용: 수집·이용 동의 (제15조, 제22조), 정보주체 권리 보장 (제35조~제38조), 파기 의무 (제21조), 처리방침 공개 (제30조), 안전성 확보 조치 (제29조)
- 회원가입 시: 개인정보 수집·이용 동의 필수, 수집목적/항목/보유기간/거부 시 불이익 고지
- 탈퇴 시: 5일 이내 개인정보 복구 불가능하게 파기 (법령 의무 보존 항목 제외)
- 파기 예외: 문의 내역 3년 (전자상거래법), 로그인 기록 3개월 (통신비밀보호법)
- 개인정보 처리방침: 웹사이트 Footer에 링크 공개 필수

### Session 2026-01-22 (3차)
- Q: 데이터 모델 정규화? → A: User 테이블을 User(기본정보+역할), UserAuth(인증자격증명), UserSuspension(정지이력)으로 분리
- 분리 사유: 3NF 위반 해소 (status→suspendedUntil 이행 종속), 보안/책임 분리, 정지 이력 관리
- Q: role 위치? → A: User에 배치 (조직 내 위치를 나타내는 프로필 속성, 인가 개념이므로 인증 테이블에서 분리)

### Session 2026-01-22 (2차)
- Q: 회원가입 방식 변경? → A: 동아리 가입 = 웹 회원가입 동시 진행, MemberList 사전 검증 제거, 누구나 가입 신청 가능
- Q: 이메일 인증 필요 여부? → A: 필요 - 회원가입 시 이메일 인증 필수, 자체 이메일 서버 구현
- Q: 역할 체계 변경? → A: 비회원 → 준회원(가입신청, 미승인) → 정회원(승인됨) → 운영진 → 관리자 5단계
- Q: 준회원의 시스템 접근 권한? → A: 게시판별로 다른 읽기/쓰기 권한 적용
- Q: 준회원 게시판별 권한? → A: 공지사항(준회원 공개 설정된 글만 읽기) / 자유게시판(접근불가) / 정보공유(접근불가)
- Q: 준회원→정회원 승인 권한? → A: 관리자(ADMIN)만 승인 가능
- Q: 회원가입 필수 입력 정보? → A: 학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기
- Q: 준회원 행사 신청 권한? → A: 신청 불가, 정회원 승인 후에만 행사 신청 가능

### Session 2026-01-22 (1차)
- Q: 사용자 계정이 정지/탈퇴될 때 활성 세션(토큰) 처리 방식? → A: 즉시 무효화 - 정지/탈퇴 시 모든 활성 토큰 즉시 폐기
- Q: 게시글/댓글 삭제 처리 방식? → A: Soft Delete - is_deleted=true로 표시, DB 보존, UI에서 "삭제된 게시글/댓글입니다" 표시
- Q: 관리자 역할 분리? → A: ADMIN(최고관리자)과 OPERATOR(운영진) 2단계로 분리
  - OPERATOR 권한: 공지사항 관리, 행사 관리, 문의 처리, 대시보드 통계 조회
  - ADMIN 전용: 회원 권한 변경, 회원 정지/강제탈퇴, 준회원 승인

---

## 개요

### 목적
IGRUS(인하대학교 컴퓨터 연구 동아리) 회원들을 위한 웹 기반 커뮤니티 플랫폼 구축. 회원 관리, 게시판 운영, 행사 관리, 문의 처리 기능을 제공하여 동아리 운영 효율화 및 회원 간 소통 활성화를 목표로 한다.

### 대상 사용자
| 사용자 유형 | 역할 코드 | 설명 | 주요 목표 |
|------------|----------|------|----------|
| 비회원 | - | 미가입 사용자 | 회원가입 신청, 문의하기 |
| 준회원 | ASSOCIATE | 가입 신청 완료, 관리자 승인 대기 상태 | 준회원 공개 공지사항 열람, 승인 대기 |
| 정회원 | MEMBER | 관리자 승인 완료된 활성 회원 | 게시글/댓글 작성, 행사 신청, 정보 공유 |
| 운영진 | OPERATOR | 동아리 임원진으로 콘텐츠/행사/문의 관리 권한 보유 | 공지사항 관리, 행사 관리, 문의 처리, 통계 조회 |
| 관리자 | ADMIN | 최고 관리자로 모든 권한 보유 | 준회원 승인, 회원 권한/상태 관리, 시스템 전체 관리 |

### 역할별 권한 요약

| 기능 | ASSOCIATE | MEMBER | OPERATOR | ADMIN |
|------|-----------|--------|----------|-------|
| 공지사항 읽기 (준회원 공개) | O | O | O | O |
| 공지사항 읽기 (전체) | X | O | O | O |
| 자유게시판/정보공유 접근 | X | O | O | O |
| 게시글/댓글 작성 | X | O | O | O |
| 행사 신청/취소 | X | O | O | O |
| 공지사항 작성/수정/삭제 | X | X | O | O |
| 행사 등록/수정/삭제/조기마감 | X | X | O | O |
| 문의 처리 (상태변경, 메모) | X | X | O | O |
| 대시보드 통계 조회 | X | X | O | O |
| 준회원 → 정회원 승인 | X | X | X | O |
| 회원 권한 변경 | X | X | X | O |
| 회원 정지/강제탈퇴 | X | X | X | O |

---

## 기능 명세

### 1. 회원가입 및 승인

**핵심 흐름**
1. 사용자가 개인정보 수집·이용 동의 (필수)
2. 사용자가 회원가입 정보 입력 (학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기)
3. 입력한 이메일로 인증 코드 발송
4. 사용자가 이메일 인증 코드 입력하여 인증 완료
5. 인증 완료 시 준회원(ASSOCIATE)으로 등록
6. 준회원 상태에서 로그인 가능 (제한된 기능만 접근)
7. 관리자(ADMIN)가 준회원을 정회원(MEMBER)으로 승인
8. 정회원 전환 후 모든 기능 이용 가능

**개인정보 수집·이용 동의 (개인정보보호법 제15조, 제22조)**
| 고지 항목 | 내용 |
|----------|------|
| 수집 목적 | 회원 식별 및 가입의사 확인, 동아리 서비스 제공, 연락 수단 확보 |
| 수집 항목 | 학번, 본명, 이메일, 전화번호, 학과, 가입 동기 |
| 보유 기간 | 회원 탈퇴 시까지 (탈퇴 후 5일 이내 파기) |
| 동의 거부 시 불이익 | 회원가입 불가 |

- 동의 체크박스 미선택 시 가입 버튼 비활성화
- 개인정보 처리방침 전문 링크 제공

**상세 규칙**
- 학번: 정확히 8자리 숫자
- 본명: 실명 입력
- 비밀번호: 영문 대/소문자 + 숫자 + 특수문자 조합, 최소 8자 이상
- 이메일: 유효한 이메일 형식, 중복 불가
- 전화번호: 유효한 전화번호 형식
- 학과: 필수 입력
- 가입 동기: 필수 입력
- 동일 학번 중복 가입 불가
- 이메일 인증 필수 (인증 완료 후 로그인 가능)

**이메일 인증**
- 인증 코드: 6자리 숫자
- 인증 코드 유효 기간: 10분
- 재발송 제한: 1분 대기 후 재발송 가능
- 최대 시도 횟수: 5회 (초과 시 새 코드 발급 필요)
- 인증 미완료 시: 회원가입 미완료 상태, 24시간 후 임시 데이터 삭제

**준회원 권한**
- 준회원 공개 설정된 공지사항만 읽기 가능
- 자유게시판, 정보공유 게시판 접근 불가
- 행사 신청 불가
- 게시글/댓글 작성 불가

**Acceptance Scenarios**
1. Given 비회원이, When 학번, 본명, 비밀번호, 이메일, 전화번호, 학과, 가입 동기를 입력하여 가입 신청하면, Then 이메일로 인증 코드가 발송된다
2. Given 인증 코드를 받은 사용자가, When 올바른 인증 코드를 입력하면, Then 준회원으로 등록되고 로그인이 가능하다
3. Given 인증 코드를 받은 사용자가, When 10분 내에 인증을 완료하지 않으면, Then 인증 코드가 만료되고 재발송이 필요하다
4. Given 이미 가입된 학번으로 재가입 시도할 때, When 가입 신청을 제출하면, Then "이미 가입된 계정입니다" 메시지가 표시된다
5. Given 준회원 상태에서, When 관리자(ADMIN)가 해당 회원을 승인하면, Then 정회원으로 전환되고 모든 게시판과 행사 신청 기능에 접근할 수 있다
6. Given 준회원 상태에서, When 자유게시판에 접근하면, Then "정회원 승인 후 이용 가능합니다" 메시지가 표시된다

---

### 2. 로그인/인증

**로그인**
- 학번 + 비밀번호로 인증
- 이메일 인증 완료된 사용자만 로그인 가능
- 로그인 시 역할(ASSOCIATE/MEMBER/OPERATOR/ADMIN) 정보 반환

**토큰 관리**
| 토큰 유형 | 유효 기간 | 용도 |
|----------|----------|------|
| Access Token | 1시간 | API 요청 인증 |
| Refresh Token | 7일 | Access Token 재발급 |

**비밀번호 재설정**
1. 사용자가 학번 입력
2. 해당 계정의 이메일로 재설정 링크 발송 (30분 유효)
3. 링크 클릭 후 새 비밀번호 설정
4. 재설정 완료 시 모든 기존 토큰 무효화

---

### 3. 계정 상태

| 상태 | 코드 | 설명 | 로그인 가능 |
|------|------|------|------------|
| Active | ACTIVE | 정상 이용 가능 | O |
| Suspended | SUSPENDED | 관리자에 의해 정지됨 | X |
| Withdrawn | WITHDRAWN | 본인 탈퇴 | X |

**역할 (Role)**
| 역할 | 코드 | 설명 |
|------|------|------|
| 준회원 | ASSOCIATE | 가입 완료, 승인 대기 |
| 정회원 | MEMBER | 관리자 승인 완료 |
| 운영진 | OPERATOR | 콘텐츠/행사/문의 관리 권한 |
| 관리자 | ADMIN | 모든 권한 보유 |

**상태 전환 시 처리**
- Suspended/Withdrawn 전환 시: 모든 활성 토큰 즉시 무효화

---

### 4. 게시판

#### 4.1 게시판 종류

| 게시판 | 코드 | 읽기 권한 | 작성 권한 | 익명 허용 | 질문 태그 |
|--------|------|----------|----------|----------|----------|
| 공지사항 | notices | 준회원: 공개 설정된 글만 / 정회원 이상: 전체 | OPERATOR 이상 | X | X |
| 자유게시판 | general | MEMBER 이상 | MEMBER 이상 | O | O |
| 정보공유 | insight | MEMBER 이상 | MEMBER 이상 | X | X |

#### 4.2 게시글

**작성 규칙**
- 제목: 최대 100자
- 내용: 제한 없음
- 이미지: 최대 5개, 각 10MB 이하
- 익명 설정: 자유게시판에서만 가능, 작성 후 수정 불가
- 질문 태그: 자유게시판에서만 "질문으로 등록" 옵션 선택 가능
- 준회원 공개: 공지사항에서만 "준회원에게 공개" 옵션 선택 가능 (기본값: 비공개)

**수정/삭제**
- 본인 게시글만 수정/삭제 가능 (관리자 예외)
- 삭제 시 Soft Delete (is_deleted=true)
- 삭제된 게시글: UI에 "삭제된 게시글입니다" 표시
- 삭제된 게시글의 댓글: 유지됨 (부모 글이 삭제되어도 댓글 존속)

**Acceptance Scenarios**
1. Given 로그인한 회원이 자유게시판에서, When 제목과 내용을 입력하고 익명 옵션을 선택하여 글을 작성하면, Then 게시글이 등록되고 작성자는 "익명"으로 표시된다
2. Given 게시글에 댓글이 있는 상태에서, When 게시글을 삭제하면, Then 게시글은 "삭제된 게시글입니다"로 표시되고 댓글은 유지된다

#### 4.3 댓글

**작성 규칙**
- 내용: 최대 500자
- 대댓글: 1단계까지만 허용 (댓글의 댓글까지, 대댓글의 대댓글 불가)
- 익명: 해당 게시판이 익명 허용인 경우에만 선택 가능

**삭제**
- 본인 댓글만 삭제 가능 (관리자 예외)
- Soft Delete 적용
- UI에 "삭제된 댓글입니다" 표시
- 대댓글이 있는 댓글 삭제 시: 부모 댓글은 "삭제된 댓글입니다"로 표시, 대댓글은 유지

#### 4.4 좋아요

- 게시글당 1인 1회
- 토글 방식: 좋아요 → 좋아요 취소 → 좋아요 반복 가능
- 본인 게시글에도 좋아요 가능

#### 4.5 북마크

- 게시글당 1인 1회
- 토글 방식
- 북마크한 글 목록은 마이페이지에서 조회

---

### 5. 행사

#### 5.1 행사 유형 (구현 완료)

| 유형 | 코드 | 설명 |
|------|------|------|
| 선착순 | AUTO_APPROVE | 신청 즉시 승인, 정원 도달 시 자동 마감 |
| 선발제 | MANUAL_APPROVE | 관리자 수동 승인, 대기 상태로 신청 |

#### 5.2 행사 상태

| 상태 | 코드 | 설명 |
|------|------|------|
| 예정 | UPCOMING | 신청 시작 전 |
| 신청중 | OPEN | 신청 가능 |
| 마감 | CLOSED | 정원/마감일/수동 마감 |
| 완료 | COMPLETED | 행사 종료 |
| 취소 | CANCELED | 행사 취소 |

#### 5.3 운영 기능 (OPERATOR 이상)
- 행사 등록: 제목, 설명, 시작/종료 일시, 장소, 정원, 신청 마감일, **신청 유형(선착순/선발제)**
- 행사 수정: 모든 필드 수정 가능 (진행 중인 행사도 수정 가능)
- 행사 삭제: 신청자가 있어도 삭제 가능 (삭제 전 경고)
- 행사 취소: 진행 중인 행사를 취소 처리
- 조기 마감: 마감일 전에 수동으로 신청 종료
- 신청자 목록: 엑셀 다운로드 지원
- **선발제 행사**: 신청자 승인/거절 처리

#### 5.4 회원 기능 (정회원 이상)
- 신청: 마감 전, 정원 미달 시 신청 가능
- 취소: 마감일 전까지 취소 가능
- 중복 신청 불가
- **준회원은 행사 신청 불가**
- **선착순**: 신청 즉시 APPROVED 상태
- **선발제**: 신청 시 PENDING 상태, 관리자 승인 대기

#### 5.5 신청 불가 조건
- 정원 초과
- 신청 마감일 경과
- 관리자 조기 마감
- 이미 신청한 행사
- 준회원 (정회원 승인 필요)

**Acceptance Scenarios**
1. Given 정원 50명인 행사에 35명이 신청한 상태에서, When 정회원이 신청하면, Then 신청이 완료되고 현재 신청자 수가 36명으로 업데이트된다
2. Given 정원이 가득 찬 행사에서, When 정회원이 신청하면, Then "정원이 마감되었습니다" 메시지가 표시되고 신청되지 않는다
3. Given 동시에 여러 명이 마지막 정원 1자리에 신청할 때, When 신청 요청이 처리되면, Then 선착순으로 1명만 신청 성공하고 나머지는 정원 마감 메시지를 받는다
4. Given 준회원이 행사에 신청하려 할 때, When 신청 버튼을 클릭하면, Then "정회원 승인 후 신청 가능합니다" 메시지가 표시된다

---

### 6. 문의

**문의 유형**
| 유형 | 코드 | 설명 |
|------|------|------|
| 가입문의 | JOIN | 명단 미등록 등 가입 관련 |
| 행사문의 | EVENT | 행사 관련 문의 |
| 신고 | REPORT | 부적절한 콘텐츠/사용자 신고 |
| 계정문의 | ACCOUNT | 비밀번호, 이메일 등 계정 관련 |
| 기타 | OTHER | 기타 문의 |

**작성 규칙**
- 비로그인 시: 이메일 필수 입력
- 로그인 시: 이메일 선택 (기본값: 계정 이메일)
- 첨부파일: 최대 3개
- 문의 번호: 자동 생성 (INQ-YYYYMMDD#####)

**처리 상태**
| 상태 | 코드 | 설명 |
|------|------|------|
| 접수 | PENDING | 문의 등록 완료 |
| 처리중 | IN_PROGRESS | 관리자 검토 중 |
| 완료 | COMPLETED | 처리 완료 |

**본인 문의 조회 (로그인 사용자)**
- 준회원/정회원 포함 모든 로그인 사용자는 본인이 작성한 문의 목록 조회 가능
- 문의 상세 내용 및 처리 상태 확인 가능
- 운영진 답변 확인 가능

**비회원 문의 조회**
- 문의 번호 + 이메일로 본인 확인 후 조회 가능
- 처리 상태 및 답변 확인 가능

**답변 이메일 발송**
- 비회원 문의: 답변 작성 시 입력된 이메일로 답변 내용 자동 발송
- 로그인 사용자 문의: 계정 이메일로 답변 알림 발송 (선택)

**운영 기능 (OPERATOR 이상)**
- 상태 변경
- 답변 작성 (문의자에게 공개)
- 내부 메모 추가 (문의자에게 비공개)
- 문의 목록 조회 및 필터링

**Acceptance Scenarios**
1. Given 준회원이 로그인한 상태에서, When 내 문의 목록을 조회하면, Then 본인이 작성한 문의 목록과 각 문의의 처리 상태가 표시된다
2. Given 비회원이 문의를 작성한 후, When 문의 번호와 이메일을 입력하여 조회하면, Then 해당 문의의 상세 내용과 답변을 확인할 수 있다
3. Given 비회원이 작성한 문의에, When 운영진이 답변을 작성하면, Then 비회원이 입력한 이메일로 답변 내용이 발송된다
4. Given 로그인 사용자가 작성한 문의에, When 운영진이 답변을 작성하면, Then 사용자는 마이페이지에서 답변을 확인할 수 있다
5. Given 다른 사용자의 문의 ID로, When 본인 문의 상세 조회를 시도하면, Then "본인의 문의만 조회할 수 있습니다" 오류가 반환된다

---

### 7. 마이페이지

**조회 정보**
- 학번
- 본명
- 이메일
- 전화번호
- 학과
- 역할 (준회원/정회원/운영진/관리자)
- 가입일
- 승인일 (정회원 이상)

**수정 가능 정보**
- 이메일
- 전화번호
- 비밀번호: 현재 비밀번호 확인 후 변경

**활동 내역**
- 내 게시글 목록
- 내 댓글 목록
- 좋아요한 게시글 목록
- 북마크한 게시글 목록
- 신청한 행사 목록
- 내 문의 목록

**탈퇴**
- 비밀번호 확인 필수
- 탈퇴 사유 입력 (선택)
- 탈퇴 즉시 모든 토큰 무효화
- 계정 상태 WITHDRAWN으로 변경
- 작성한 콘텐츠는 유지 (작성자명은 "탈퇴한 회원"으로 표시)

**탈퇴 후 복구 및 재가입 정책**
- 복구 가능 기간: 탈퇴일로부터 **5일 이내**
- 복구 방법: 탈퇴한 계정의 학번+비밀번호로 로그인 시도 시 복구 확인 화면 표시 → "계정 복구" 선택 시 상태 ACTIVE로 전환
- 재가입 제한: 탈퇴한 학번으로 5일 이내 신규 가입 시도 시 차단 ("탈퇴 후 5일이 지나야 재가입할 수 있습니다")
- 5일 경과 후: 개인정보 완전 파기되어 복구 불가, 동일 학번으로 신규 가입 가능
- 탈퇴 시 고지 문구: "탈퇴 후 5일 이내에 로그인하면 계정을 복구할 수 있습니다. 5일이 지나면 모든 개인정보가 영구 삭제되며 복구가 불가능합니다."

**개인정보 파기 (개인정보보호법 제21조)**
- 탈퇴일로부터 5일 이내 개인정보 복구 불가능하게 영구 삭제
- 파기 대상: 학번, 본명, 이메일, 전화번호, 학과, 가입 동기, 비밀번호
- 파기 예외 (분리 보관):
  - 문의 내역: 3년 (전자상거래법)
  - 로그인 기록: 3개월 (통신비밀보호법)
- 게시글/댓글 내용은 익명화 처리 후 보존 (작성자 연결 해제)

---

### 8. 관리 기능

#### 8.1 대시보드 통계 (OPERATOR 이상)
- 오늘 방문자 수
- 오늘 게시글 수
- 오늘 댓글 수
- 이번 주 신규 가입자 수
- 대기 중 문의 수
- **승인 대기 준회원 수**

#### 8.2 준회원 승인 (ADMIN 전용)
- 승인 대기 준회원 목록 조회
- 준회원 상세 정보 조회 (학번, 본명, 학과, 가입 동기 등)
- 일괄 승인 / 개별 승인 기능
- 승인 시 즉시 정회원(MEMBER)으로 전환

#### 8.3 회원 관리 (ADMIN 전용)

**회원 목록 조회**
- 회원 목록 조회 및 검색 (OPERATOR 이상)
- 회원 상세 정보 조회 (OPERATOR 이상)

**권한 관리 (ADMIN 전용)**
- 권한 변경: MEMBER ↔ OPERATOR ↔ ADMIN
- 상태 변경: 정지 (기간 설정 가능), 정지 해제
- 강제 탈퇴: 사유 입력 후 처리

**제한 사항**
- 자기 자신 정지/탈퇴 불가
- 자기 자신 권한 변경 불가
- 마지막 ADMIN은 권한 변경 불가

---

### 9. 건의 사항

**목적**
정회원 이상의 회원이 동아리 운영에 대한 건의사항, 개선 요청, 불만 사항 등을 제출할 수 있는 기능. 작성자는 익명 또는 실명을 선택할 수 있으며, 건의사항은 운영진 이상만 조회 가능하다. 익명 선택 시 운영진에게는 익명으로 표시되고 관리자만 작성자를 확인할 수 있다. 답변 시 작성자에게 이메일로 자동 알림이 발송된다.

**핵심 흐름**
1. 정회원 이상이 건의 사항 작성 페이지 접근
2. 익명/실명 여부 선택
3. 카테고리 선택 및 건의 내용 작성
4. 제출 → 접수 확인 이메일 발송 (작성자에게)
5. 운영진/관리자가 건의 사항 목록 조회 및 처리
6. 처리 결과를 공개 답변으로 작성 → 답변 알림 이메일 발송 (작성자에게)

**공개 유형**
| 유형 | 코드 | 설명 | 작성자 표시 |
|------|------|------|------------|
| 익명 | ANONYMOUS | 작성자 정보 비공개 | 운영진: "익명", 관리자: 작성자 확인 가능 |
| 실명 | IDENTIFIED | 작성자 정보 공개 | 운영진/관리자에게 작성자명 표시 |

**건의 카테고리**
| 카테고리 | 코드 | 설명 |
|---------|------|------|
| 행사 건의 | EVENT | 행사 기획/운영 관련 건의 |
| 시설/장비 | FACILITY | 동아리 공간, 장비 관련 건의 |
| 운영 방식 | OPERATION | 동아리 운영 방식 개선 건의 |
| 홈페이지 | WEBSITE | 웹사이트 기능/UI 개선 건의 |
| 기타 | OTHER | 기타 건의 사항 |

**작성 규칙**
- 작성 권한: 정회원(MEMBER) 이상
- 준회원(ASSOCIATE)은 작성 불가 (정회원 승인 후 이용 가능)
- 공개 유형: 필수 선택 (익명/실명)
- 제목: 필수, 최대 100자
- 내용: 필수, 최대 2000자
- 카테고리: 필수 선택
- 첨부파일: 최대 3개 (이미지, 문서)
- 건의 번호: 자동 생성 (SUG-YYYYMMDD#####)

**조회 권한**
| 역할 | 건의 사항 목록 조회 | 본인 건의 조회 |
|------|-------------------|---------------|
| 정회원 (MEMBER) | 불가 | 본인이 작성한 건의만 조회 가능 |
| 운영진 (OPERATOR) | 가능 | - |
| 관리자 (ADMIN) | 가능 | - |

**익명성 정책 (익명 건의 선택 시)**
| 역할 | 작성자 정보 조회 |
|------|-----------------|
| 운영진 (OPERATOR) | 불가 - "익명"으로 표시 |
| 관리자 (ADMIN) | 가능 - 악용 방지 및 조치 목적 |

- 익명 건의의 작성자 ID는 데이터베이스에 저장됨
- OPERATOR에게는 익명 건의의 작성자 정보가 API 응답에서 제외됨
- ADMIN은 익명 건의 상세 조회 시 작성자 정보 확인 가능
- 관리자의 작성자 확인 이력 로깅 (누가, 언제, 어떤 건의의 작성자를 조회했는지 기록)

**실명 공개 정책 (실명 건의 선택 시)**
- 운영진/관리자에게 작성자 이름 표시
- 작성자 본인이 실명 공개를 선택한 것이므로 별도 동의 불필요

**처리 상태**
| 상태 | 코드 | 설명 |
|------|------|------|
| 접수 | PENDING | 건의 등록 완료 |
| 검토중 | REVIEWING | 운영진 검토 중 |
| 처리완료 | RESOLVED | 처리 완료 (답변 작성됨) |
| 반려 | REJECTED | 부적절한 건의로 반려 |

**이메일 알림**
| 시점 | 수신자 | 이메일 내용 |
|------|-------|------------|
| 건의 접수 시 | 작성자 | 건의 번호, 접수 확인, 처리 예상 안내 |
| 답변 작성 시 | 작성자 | 건의 번호, 답변 내용, 상태 변경 알림 |
| 상태 변경 시 | 작성자 | 건의 번호, 변경된 상태 알림 |

**운영진 기능 (OPERATOR)**
- 건의 사항 목록 조회 (필터: 카테고리, 상태, 공개 유형, 기간)
- 상태 변경 (PENDING → REVIEWING → RESOLVED/REJECTED)
- 공개 답변 작성 (작성자에게 이메일 발송)
- 내부 메모 추가 (운영진/관리자만 조회 가능)
- 부적절한 건의 반려 처리 (사유 기록)
- **익명 건의의 작성자 확인 불가**

**관리자 기능 (ADMIN)**
- 운영진의 모든 기능 포함
- **익명 건의의 작성자 정보 조회 가능** (학번, 이름)
- 악용 회원에 대한 경고/제재 조치 가능
- 작성자 조회 이력 확인

**정회원 기능 (MEMBER)**
- 건의 사항 작성
- 본인이 작성한 건의 사항 목록 조회 (마이페이지)
- 본인 건의의 처리 상태 및 답변 확인

**악용 방지**
- 동일 사용자 1시간 내 최대 5건 작성 제한
- 욕설, 비방, 명예훼손 등 부적절한 내용 신고 기능
- 관리자가 익명 건의 작성자 확인 후 경고 또는 제재 가능
- 반복적 악용 시 건의 작성 권한 제한 가능

**Acceptance Scenarios**
1. Given 정회원이 로그인한 상태에서, When 익명을 선택하여 건의 사항을 제출하면, Then 건의가 등록되고 접수 확인 이메일이 작성자에게 발송된다
2. Given 정회원이 로그인한 상태에서, When 실명을 선택하여 건의 사항을 제출하면, Then 건의가 등록되고 운영진이 조회 시 작성자 이름이 표시된다
3. Given 준회원이 로그인한 상태에서, When 건의 사항 작성 페이지에 접근하면, Then "정회원 승인 후 이용 가능합니다" 메시지가 표시된다
4. Given 정회원이 건의 사항 목록 페이지에 접근하려 할 때, When 접근을 시도하면, Then "권한이 없습니다" 메시지가 표시된다
5. Given 정회원이 마이페이지에서 본인 건의 목록을 조회할 때, When 목록을 확인하면, Then 본인이 작성한 건의 사항과 처리 상태를 확인할 수 있다
6. Given 운영진이 익명 건의 사항 상세를 조회할 때, When 작성자 정보를 확인하려 하면, Then 작성자 정보는 표시되지 않고 "익명"으로만 표시된다
7. Given 관리자가 익명 건의 사항 상세를 조회할 때, When 작성자 정보를 확인하면, Then 작성자의 학번과 이름이 표시되고 조회 이력이 기록된다
8. Given 운영진이 건의 사항에 답변을 작성하면, When 답변이 등록되면, Then 작성자에게 답변 내용이 포함된 이메일이 자동 발송된다
9. Given 정회원이 1시간 내에 5건의 건의 사항을 작성한 후, When 추가로 건의 사항을 작성하려 하면, Then "잠시 후 다시 시도해주세요" 메시지가 표시된다
10. Given 관리자가 익명 건의에서 악용 사례를 발견했을 때, When 해당 건의의 작성자를 확인하고 제재 조치하면, Then 해당 회원의 건의 작성 권한이 제한된다

---

## 비기능 요구사항

### 성능
- 게시글 목록 로딩: 3초 이내
- 동시 접속자 100명 이상 지원
- 이미지 업로드: 10MB 이하 파일 10초 이내 완료

### 보안
- 비밀번호: 단방향 해시 저장
- 토큰: 서명 검증 필수
- HTTPS 전용 통신
- SQL Injection, XSS 방지

### 개인정보 처리 (개인정보보호법 준수)

#### 수집 및 동의 (제15조, 제22조)

**회원가입 시 필수 고지 및 동의**
| 항목 | 내용 |
|------|------|
| 수집 목적 | 회원 식별, 서비스 제공, 연락 수단 확보 |
| 수집 항목 | 학번, 본명, 이메일, 전화번호, 학과, 가입 동기 |
| 보유 기간 | 회원 탈퇴 시까지 (탈퇴 후 5일 이내 파기) |

- 동의 거부 시 가입 불가 사항 명시
- 비밀번호는 수집 항목이 아닌 인증 수단으로 별도 처리

**선택적 수집 항목**: 없음 (최소 수집 원칙 준수)

#### 정보주체의 권리 (제35조~제38조)

| 권리 | 설명 | 처리 기한 |
|------|------|----------|
| 열람권 | 본인 개인정보 열람 요구 | 10일 이내 |
| 정정권 | 개인정보 정정 요구 | 지체 없이 |
| 삭제권 | 개인정보 삭제 요구 (탈퇴) | 지체 없이 |
| 처리정지권 | 개인정보 처리 정지 요구 | 지체 없이 |

**마이페이지에서 제공할 권리행사 기능**
- 내 정보 조회 (열람권)
- 이메일/전화번호 수정 (정정권)
- 회원 탈퇴 (삭제권)
- 문의하기를 통한 처리정지 요청 안내

#### 개인정보 파기 (제21조)

| 파기 사유 | 파기 기한 | 파기 방법 |
|----------|----------|----------|
| 회원 탈퇴 | 탈퇴일로부터 5일 이내 | 복구 불가능하게 영구 삭제 |
| 보유기간 경과 | 경과일로부터 5일 이내 | 복구 불가능하게 영구 삭제 |
| 목적 달성 | 달성일로부터 5일 이내 | 복구 불가능하게 영구 삭제 |

**파기 예외 (분리 보관)**
| 보존 항목 | 보존 기간 | 근거 법령 |
|----------|----------|----------|
| 문의 내역 | 3년 | 전자상거래법 제6조 (소비자 불만/분쟁처리 기록) |
| 로그인 기록 | 3개월 | 통신비밀보호법 제15조의2 |

#### 개인정보 처리방침 (제30조)

**웹사이트에 공개해야 할 필수 항목**
1. 개인정보 처리 목적
2. 처리하는 개인정보 항목
3. 개인정보 보유 및 이용 기간
4. 개인정보의 제3자 제공 현황 (해당 시)
5. 개인정보 처리 위탁 현황 (해당 시)
6. 정보주체의 권리·의무 및 행사 방법
7. 개인정보 파기 절차 및 방법
8. 개인정보보호책임자 정보
9. 개인정보 자동수집장치 설치·운영 및 거부 (쿠키 등)
10. 개인정보 침해 구제 방법

**공개 위치**: 웹사이트 하단 (Footer) 또는 설정/회원가입 화면에서 쉽게 접근 가능한 위치

#### 14세 미만 아동 (제22조)

- 대학교 동아리 특성상 14세 미만 가입자는 없을 것으로 예상
- 단, 시스템적으로 생년월일 미확인으로 14세 미만 가입 시 법정대리인 동의 불가능
- **제한 조치**: 회원가입 안내에 "본 서비스는 인하대학교 재학생을 대상으로 합니다" 명시

#### 개인정보 안전성 확보 조치 (제29조, 시행령 제30조)

| 조치 | 구현 방법 |
|------|----------|
| 접근권한 관리 | 역할 기반 접근 제어 (RBAC) |
| 접근통제 | API 인증 필수, 본인 데이터만 접근 |
| 암호화 | 비밀번호 해시, HTTPS 통신 |
| 접속기록 보관 | 최소 3개월 이상 로그 보관 |
| 보안프로그램 | XSS/SQL Injection 방지 |

#### 위반 시 제재

| 위반 내용 | 제재 |
|----------|------|
| 동의 없이 개인정보 수집·이용 | 5천만원 이하 과태료 |
| 개인정보 미파기 | 3천만원 이하 과태료 |
| 정정·삭제 요구 불이행 | 3천만원 이하 과태료 |
| 안전조치 의무 위반 | 3천만원 이하 과태료 |

### 이메일 발송 (자체 구현)
- SMTP 서버를 통한 이메일 발송
- 발송 실패 시 최대 3회 자동 재시도
- 재시도 간격: 1분, 5분, 15분 (지수 백오프)
- 발송 이력 로깅 (성공/실패 여부, 발송 시각)

### 가용성
- 주요 기능 성공률 99% 이상
- 이메일 발송 실패 시 자동 재시도 (최대 3회)

---

## API 명세

> **Note**: 모든 API는 `/api/v1/` prefix를 사용합니다. (구현 완료)

### 인증 `/api/v1/auth/password`

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /signup | 회원가입 정보 입력 및 인증 코드 발송 |
| POST | /verify-email | 이메일 인증 코드 확인 및 준회원 등록 |
| POST | /resend-verification | 인증 코드 재발송 |
| POST | /login | 로그인 |
| POST | /logout | 로그아웃 |
| POST | /refresh | 토큰 갱신 |
| GET | /account/recovery-check | 탈퇴 계정 복구 가능 여부 확인 |
| POST | /account/recover | 탈퇴 계정 복구 (5일 이내) |
| POST | /reset-request | 비밀번호 재설정 요청 |
| POST | /reset-confirm | 비밀번호 재설정 확인 |
| GET | /reset-validate | 재설정 토큰 검증 |

**POST /signup**
```json
// Request
{
  "studentId": "20231234",
  "name": "홍길동",
  "password": "Pass123!",
  "email": "hong@example.com",
  "phone": "010-1234-5678",
  "department": "컴퓨터공학과",
  "motivation": "웹 개발에 관심이 있어서 가입하고 싶습니다.",
  "privacyPolicyConsent": true,  // 개인정보 처리방침 동의 (필수)
  "privacyPolicyVersion": "2026-01-22"  // 동의한 정책 버전
}

// Response 200 - 인증 코드 발송됨
{
  "email": "hong@example.com",
  "expiresIn": 600,
  "message": "입력하신 이메일로 인증 코드가 발송되었습니다."
}

// Error 400 - 개인정보 동의 누락
{ "code": "AUTH010", "message": "개인정보 처리방침에 동의해야 합니다" }

// Error 409 - 이미 가입됨
{ "code": "AUTH002", "message": "이미 가입된 계정입니다" }

// Error 409 - 탈퇴 후 재가입 제한
{ "code": "AUTH011", "message": "탈퇴 후 5일이 지나야 재가입할 수 있습니다", "availableAt": "2026-01-27T00:00:00Z" }
```

**POST /signup/verify**
```json
// Request
{
  "email": "hong@example.com",
  "verificationCode": "123456"
}

// Response 201 - 인증 완료 및 준회원 등록
{
  "id": 1,
  "studentId": "20231234",
  "name": "홍길동",
  "role": "ASSOCIATE",
  "message": "이메일 인증이 완료되었습니다. 준회원으로 가입되었습니다."
}

// Error 400 - 잘못된 인증 코드
{ "code": "AUTH014", "message": "인증 코드가 올바르지 않습니다" }

// Error 400 - 인증 코드 만료
{ "code": "AUTH015", "message": "인증 코드가 만료되었습니다. 재발송해주세요" }

// Error 429 - 시도 횟수 초과
{ "code": "AUTH016", "message": "인증 시도 횟수를 초과했습니다. 새 코드를 발급받아주세요" }
```

**POST /signup/resend**
```json
// Request
{
  "email": "hong@example.com"
}

// Response 200
{
  "email": "hong@example.com",
  "expiresIn": 600,
  "message": "인증 코드가 재발송되었습니다."
}

// Error 429 - 재발송 제한
{ "code": "AUTH017", "message": "1분 후에 다시 시도해주세요" }
```

**POST /login**
```json
// Request
{ "studentId": "20231234", "password": "Pass123!" }

// Response 200
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "홍길동",
    "role": "ASSOCIATE"  // ASSOCIATE, MEMBER, OPERATOR, ADMIN 중 하나
  }
}

// Error 401 - 인증 실패
{ "code": "AUTH003", "message": "학번 또는 비밀번호가 일치하지 않습니다" }

// Error 403 - 계정 정지
{ "code": "AUTH005", "message": "계정이 정지되었습니다", "suspendedUntil": "2025-02-01T00:00:00Z" }

// Response 200 - 탈퇴 계정 복구 가능 (5일 이내)
{
  "code": "AUTH012",
  "message": "탈퇴한 계정입니다. 복구하시겠습니까?",
  "recoverable": true,
  "withdrawnAt": "2026-01-20T10:00:00Z",
  "recoverableUntil": "2026-01-25T10:00:00Z"
}
```

**POST /recover** (탈퇴 계정 복구)
```json
// Request
{ "studentId": "20231234", "password": "Pass123!" }

// Response 200
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "홍길동",
    "role": "MEMBER"
  },
  "message": "계정이 복구되었습니다"
}

// Error 400 - 복구 기간 만료
{ "code": "AUTH013", "message": "복구 가능 기간이 만료되었습니다. 신규 가입해 주세요." }
```

---

### 게시글 `/api/posts/:board`

> board: `notices`, `general`, `insight`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:board | 목록 조회 |
| POST | /:board | 작성 |
| GET | /:board/:id | 상세 조회 |
| PUT | /:board/:id | 수정 |
| DELETE | /:board/:id | 삭제 |
| POST | /:board/:id/like | 좋아요 |
| DELETE | /:board/:id/like | 좋아요 취소 |
| POST | /:board/:id/bookmark | 북마크 |
| DELETE | /:board/:id/bookmark | 북마크 취소 |

**GET /general** `?page=1&limit=20&sort=latest&search=키워드&isQuestion=true`
```json
// Response 200
{
  "posts": [
    {
      "id": 1,
      "title": "제목",
      "author": "익명",
      "authorId": null,
      "isQuestion": false,
      "viewCount": 45,
      "likeCount": 3,
      "commentCount": 5,
      "createdAt": "2025-01-20T10:00:00Z",
      "isDeleted": false
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 10,
    "totalCount": 200
  }
}
```

**POST /general** (정회원 이상)
```json
// Request
{
  "title": "제목",
  "content": "내용",
  "isAnonymous": true,
  "isQuestion": false,
  "images": [
    { "url": "https://...", "order": 1 }
  ]
}

// Response 201
{
  "id": 1,
  "title": "제목",
  "createdAt": "2025-01-21T09:00:00Z"
}

// Error 400 - 제목 길이 초과
{ "code": "POST003", "message": "제목은 100자 이내여야 합니다" }

// Error 400 - 이미지 개수 초과
{ "code": "POST004", "message": "이미지는 최대 5개까지 첨부 가능합니다" }

// Error 403 - 준회원 접근 불가
{ "code": "POST006", "message": "정회원 승인 후 이용 가능합니다" }
```

**POST /notices** (OPERATOR 이상)
```json
// Request
{
  "title": "공지사항 제목",
  "content": "공지 내용",
  "isVisibleToAssociate": true,  // 준회원 공개 여부
  "images": [
    { "url": "https://...", "order": 1 }
  ]
}

// Response 201
{
  "id": 1,
  "title": "공지사항 제목",
  "isVisibleToAssociate": true,
  "createdAt": "2025-01-21T09:00:00Z"
}
```

**GET /general/:id**
```json
// Response 200
{
  "id": 1,
  "title": "제목",
  "content": "내용",
  "author": "익명",
  "authorId": null,
  "isQuestion": false,
  "viewCount": 46,
  "likeCount": 3,
  "commentCount": 5,
  "images": [
    { "url": "https://...", "order": 1 }
  ],
  "isLiked": false,
  "isBookmarked": true,
  "isDeleted": false,
  "createdAt": "2025-01-20T10:00:00Z",
  "updatedAt": "2025-01-20T10:00:00Z"
}

// Response 200 - 삭제된 게시글
{
  "id": 1,
  "isDeleted": true,
  "message": "삭제된 게시글입니다"
}
```

---

### 댓글 `/api/comments/:board`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:board/:postId | 목록 |
| POST | /:board/:postId | 작성 |
| PUT | /:board/:id | 수정 |
| DELETE | /:board/:id | 삭제 |

**GET /general/:postId**
```json
// Response 200
{
  "comments": [
    {
      "id": 1,
      "content": "댓글 내용",
      "author": "홍길동",
      "authorId": 1,
      "isDeleted": false,
      "createdAt": "2025-01-21T09:30:00Z",
      "replies": [
        {
          "id": 2,
          "content": "대댓글 내용",
          "author": "익명",
          "authorId": null,
          "isDeleted": false,
          "createdAt": "2025-01-21T09:35:00Z"
        }
      ]
    },
    {
      "id": 3,
      "isDeleted": true,
      "message": "삭제된 댓글입니다",
      "replies": []
    }
  ]
}
```

**POST /general/:postId**
```json
// Request
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}

// Response 201
{
  "id": 1,
  "content": "댓글 내용",
  "author": "홍길동",
  "createdAt": "2025-01-21T09:30:00Z"
}

// Error 400 - 대댓글 depth 초과
{ "code": "COMMENT001", "message": "대댓글에는 답글을 달 수 없습니다" }
```

---

### 행사 `/api/events`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | / | 목록 | 전체 |
| POST | / | 생성 | OPERATOR 이상 |
| GET | /:id | 상세 | 전체 |
| PUT | /:id | 수정 | OPERATOR 이상 |
| DELETE | /:id | 삭제 | OPERATOR 이상 |
| POST | /:id/register | 신청 | MEMBER 이상 (준회원 불가) |
| DELETE | /:id/register | 취소 | MEMBER 이상 (준회원 불가) |
| POST | /:id/close | 조기마감 | OPERATOR 이상 |
| GET | /:id/registrations | 신청자목록 | OPERATOR 이상 |
| GET | /:id/registrations/export | 신청자 엑셀 다운로드 | OPERATOR 이상 |

**GET /** `?status=upcoming&page=1`
```json
// Response 200
{
  "events": [
    {
      "id": 1,
      "title": "신입생 환영회",
      "startDatetime": "2025-02-01T18:00:00Z",
      "endDatetime": "2025-02-01T21:00:00Z",
      "location": "학생회관",
      "capacity": 50,
      "currentCount": 35,
      "registrationDeadline": "2025-01-31T23:59:59Z",
      "status": "UPCOMING",
      "isRegistered": false
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 5,
    "totalCount": 45
  }
}
```

**POST /** (OPERATOR 이상)
```json
// Request
{
  "title": "환영회",
  "description": "설명",
  "startDatetime": "2025-02-01T18:00:00Z",
  "endDatetime": "2025-02-01T21:00:00Z",
  "location": "학생회관",
  "capacity": 50,
  "registrationDeadline": "2025-01-31T23:59:59Z"
}

// Response 201
{
  "id": 1,
  "title": "환영회",
  "status": "UPCOMING",
  "createdAt": "2025-01-15T10:00:00Z"
}
```

**POST /:id/register** (정회원 이상)
```json
// Response 200
{
  "message": "행사 신청이 완료되었습니다",
  "currentCount": 36
}

// Error 400 - 정원 초과
{ "code": "EVENT001", "message": "정원이 마감되었습니다" }

// Error 400 - 기간 종료
{ "code": "EVENT002", "message": "신청 기간이 종료되었습니다" }

// Error 409 - 중복 신청
{ "code": "EVENT003", "message": "이미 신청한 행사입니다" }

// Error 403 - 준회원 접근 불가
{ "code": "EVENT006", "message": "정회원 승인 후 신청 가능합니다" }
```

---

### 문의 `/api/inquiries`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | / | 작성 | 전체 (비로그인 포함) |
| GET | /my | 내 문의 목록 | 로그인 (ASSOCIATE 이상) |
| GET | /my/:id | 내 문의 상세 | 로그인 (본인 문의만) |
| POST | /lookup | 비회원 문의 조회 | 전체 (문의번호+이메일 검증) |
| GET | / | 전체 목록 | OPERATOR 이상 |
| GET | /:id | 상세 | OPERATOR 이상 |
| PUT | /:id/status | 상태변경 | OPERATOR 이상 |
| POST | /:id/reply | 답변 작성 | OPERATOR 이상 |
| POST | /:id/memo | 내부메모 추가 | OPERATOR 이상 |

**POST /**
```json
// Request
{
  "type": "JOIN",
  "title": "가입 문의",
  "content": "내용",
  "email": "user@example.com",
  "attachments": [
    { "url": "https://..." }
  ]
}

// Response 201
{
  "id": 1,
  "inquiryNumber": "INQ-2025012100001",
  "status": "PENDING"
}
```

**GET /my** (로그인 사용자) `?status=PENDING&page=1`
```json
// Response 200
{
  "inquiries": [
    {
      "id": 1,
      "inquiryNumber": "INQ-2025012100001",
      "type": "JOIN",
      "title": "가입 문의",
      "status": "COMPLETED",
      "hasReply": true,
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalCount": 3
  }
}
```

**GET /my/:id** (로그인 사용자, 본인 문의만)
```json
// Response 200
{
  "id": 1,
  "inquiryNumber": "INQ-2025012100001",
  "type": "JOIN",
  "title": "가입 문의",
  "content": "내용",
  "status": "COMPLETED",
  "createdAt": "2025-01-21T10:00:00Z",
  "attachments": [
    { "url": "https://..." }
  ],
  "reply": {
    "content": "답변 내용입니다.",
    "createdAt": "2025-01-22T09:00:00Z"
  }
}

// Error 403 - 본인 문의가 아님
{ "code": "INQUIRY002", "message": "본인의 문의만 조회할 수 있습니다" }
```

**POST /lookup** (비회원 문의 조회)
```json
// Request
{
  "inquiryNumber": "INQ-2025012100001",
  "email": "user@example.com"
}

// Response 200
{
  "id": 1,
  "inquiryNumber": "INQ-2025012100001",
  "type": "JOIN",
  "title": "가입 문의",
  "content": "내용",
  "status": "COMPLETED",
  "createdAt": "2025-01-21T10:00:00Z",
  "reply": {
    "content": "답변 내용입니다.",
    "createdAt": "2025-01-22T09:00:00Z"
  }
}

// Error 404 - 문의 없음 또는 이메일 불일치
{ "code": "INQUIRY001", "message": "문의를 찾을 수 없습니다" }
```

**GET /** (OPERATOR 이상) `?status=PENDING&type=JOIN&page=1`
```json
// Response 200
{
  "inquiries": [
    {
      "id": 1,
      "inquiryNumber": "INQ-2025012100001",
      "type": "JOIN",
      "title": "가입 문의",
      "email": "user@example.com",
      "status": "PENDING",
      "hasReply": false,
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 3,
    "totalCount": 25
  }
}
```

**POST /:id/reply** (OPERATOR 이상)
```json
// Request
{
  "content": "문의 주셔서 감사합니다. 답변 내용입니다.",
  "sendEmail": true  // 이메일 발송 여부 (기본값: true)
}

// Response 201
{
  "id": 1,
  "content": "문의 주셔서 감사합니다. 답변 내용입니다.",
  "createdAt": "2025-01-22T09:00:00Z",
  "emailSent": true
}

// Error 400 - 이미 답변 존재
{ "code": "INQUIRY003", "message": "이미 답변이 등록된 문의입니다" }
```

---

### 건의 사항 `/api/v1/suggestions`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | / | 건의 작성 | MEMBER 이상 |
| GET | /my | 내 건의 목록 | MEMBER 이상 (본인 건의만) |
| GET | /my/:id | 내 건의 상세 | MEMBER 이상 (본인 건의만) |
| GET | / | 전체 목록 | OPERATOR 이상 |
| GET | /:id | 상세 | OPERATOR 이상 |
| PUT | /:id/status | 상태 변경 | OPERATOR 이상 |
| POST | /:id/reply | 답변 작성 | OPERATOR 이상 |
| POST | /:id/memo | 내부 메모 추가 | OPERATOR 이상 |
| GET | /:id/author | 익명 건의 작성자 조회 | ADMIN 전용 |

**POST /** (MEMBER 이상)
```json
// Request
{
  "category": "EVENT",
  "visibility": "ANONYMOUS",
  "title": "행사 개선 건의",
  "content": "환영회 시간대를 저녁으로 변경해주시면 좋겠습니다.",
  "attachments": [
    { "url": "https://..." }
  ]
}

// Response 201
{
  "id": 1,
  "suggestionNumber": "SUG-2025012100001",
  "status": "PENDING",
  "message": "건의가 접수되었습니다. 확인 이메일이 발송되었습니다."
}

// Error 400 - 제목 길이 초과
{ "code": "SUG001", "message": "제목은 100자 이내여야 합니다" }

// Error 400 - 내용 길이 초과
{ "code": "SUG002", "message": "내용은 2000자 이내여야 합니다" }

// Error 403 - 준회원 접근 불가
{ "code": "SUG003", "message": "정회원 승인 후 이용 가능합니다" }

// Error 429 - 작성 제한 초과
{ "code": "SUG004", "message": "잠시 후 다시 시도해주세요" }
```

**GET /my** (MEMBER 이상) `?status=PENDING&category=EVENT&page=1`
```json
// Response 200
{
  "suggestions": [
    {
      "id": 1,
      "suggestionNumber": "SUG-2025012100001",
      "category": "EVENT",
      "visibility": "ANONYMOUS",
      "title": "행사 개선 건의",
      "status": "REVIEWING",
      "hasReply": false,
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalCount": 3
  }
}
```

**GET /my/:id** (MEMBER 이상, 본인 건의만)
```json
// Response 200
{
  "id": 1,
  "suggestionNumber": "SUG-2025012100001",
  "category": "EVENT",
  "visibility": "ANONYMOUS",
  "title": "행사 개선 건의",
  "content": "환영회 시간대를 저녁으로 변경해주시면 좋겠습니다.",
  "status": "RESOLVED",
  "createdAt": "2025-01-21T10:00:00Z",
  "attachments": [
    { "url": "https://..." }
  ],
  "reply": {
    "content": "검토 결과 다음 행사부터 저녁 시간대로 조정하겠습니다.",
    "createdAt": "2025-01-22T09:00:00Z"
  }
}

// Error 403 - 본인 건의가 아님
{ "code": "SUG005", "message": "본인의 건의만 조회할 수 있습니다" }
```

**GET /** (OPERATOR 이상) `?status=PENDING&category=EVENT&visibility=ANONYMOUS&page=1`
```json
// Response 200
{
  "suggestions": [
    {
      "id": 1,
      "suggestionNumber": "SUG-2025012100001",
      "category": "EVENT",
      "visibility": "ANONYMOUS",
      "title": "행사 개선 건의",
      "author": "익명",  // ANONYMOUS인 경우
      "status": "PENDING",
      "hasReply": false,
      "createdAt": "2025-01-21T10:00:00Z"
    },
    {
      "id": 2,
      "suggestionNumber": "SUG-2025012100002",
      "category": "OPERATION",
      "visibility": "IDENTIFIED",
      "title": "운영 방식 건의",
      "author": "홍길동",  // IDENTIFIED인 경우
      "status": "REVIEWING",
      "hasReply": false,
      "createdAt": "2025-01-21T11:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 3,
    "totalCount": 25
  }
}
```

**GET /:id** (OPERATOR 이상)
```json
// Response 200 - OPERATOR가 익명 건의 조회
{
  "id": 1,
  "suggestionNumber": "SUG-2025012100001",
  "category": "EVENT",
  "visibility": "ANONYMOUS",
  "title": "행사 개선 건의",
  "content": "환영회 시간대를 저녁으로 변경해주시면 좋겠습니다.",
  "author": "익명",
  "status": "PENDING",
  "createdAt": "2025-01-21T10:00:00Z",
  "attachments": [
    { "url": "https://..." }
  ],
  "memos": [
    {
      "id": 1,
      "content": "다음 임원 회의에서 논의 예정",
      "createdBy": "관리자명",
      "createdAt": "2025-01-22T10:00:00Z"
    }
  ]
}

// Response 200 - OPERATOR가 실명 건의 조회
{
  "id": 2,
  "suggestionNumber": "SUG-2025012100002",
  "category": "OPERATION",
  "visibility": "IDENTIFIED",
  "title": "운영 방식 건의",
  "content": "정기 모임 요일을 변경해주세요.",
  "author": "홍길동",
  "authorId": 123,
  "status": "REVIEWING",
  "createdAt": "2025-01-21T11:00:00Z",
  "attachments": [],
  "memos": []
}
```

**GET /:id/author** (ADMIN 전용 - 익명 건의 작성자 조회)
```json
// Response 200
{
  "authorId": 123,
  "studentId": "20231234",
  "name": "홍길동",
  "viewedAt": "2025-01-22T15:00:00Z"
}

// Error 400 - 실명 건의
{ "code": "SUG006", "message": "실명 건의는 작성자 정보가 이미 공개되어 있습니다" }

// Error 403 - ADMIN 권한 필요
{ "code": "SUG007", "message": "관리자만 익명 건의 작성자를 조회할 수 있습니다" }
```

**PUT /:id/status** (OPERATOR 이상)
```json
// Request
{
  "status": "REVIEWING"
}

// Response 200
{
  "id": 1,
  "status": "REVIEWING",
  "updatedAt": "2025-01-22T10:00:00Z",
  "emailSent": true
}
```

**POST /:id/reply** (OPERATOR 이상)
```json
// Request
{
  "content": "검토 결과 다음 행사부터 저녁 시간대로 조정하겠습니다."
}

// Response 201
{
  "id": 1,
  "content": "검토 결과 다음 행사부터 저녁 시간대로 조정하겠습니다.",
  "createdAt": "2025-01-22T09:00:00Z",
  "emailSent": true
}

// Error 400 - 이미 답변 존재
{ "code": "SUG008", "message": "이미 답변이 등록된 건의입니다" }
```

**POST /:id/memo** (OPERATOR 이상)
```json
// Request
{
  "content": "다음 임원 회의에서 논의 예정"
}

// Response 201
{
  "id": 1,
  "content": "다음 임원 회의에서 논의 예정",
  "createdBy": "관리자명",
  "createdAt": "2025-01-22T10:00:00Z"
}
```

---

### 마이페이지 `/api/users/:userId`

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /:userId | 내 정보 |
| PUT | /:userId | 정보 수정 |
| PUT | /:userId/password | 비밀번호 변경 |
| POST | /:userId/logout | 로그아웃 |
| DELETE | /:userId | 탈퇴 |
| GET | /:userId/posts | 내 게시글 |
| GET | /:userId/comments | 내 댓글 |
| GET | /:userId/likes | 좋아요 목록 |
| GET | /:userId/bookmarks | 북마크 목록 |
| GET | /:userId/events | 신청 행사 |

**GET /:userId**
```json
// Response 200
{
  "id": 1,
  "studentId": "20231234",
  "name": "홍길동",
  "email": "hong@example.com",
  "role": "MEMBER",
  "createdAt": "2025-01-01T10:00:00Z"
}
```

**PUT /:userId/password**
```json
// Request
{
  "currentPassword": "Pass123!",
  "newPassword": "NewPass456!"
}

// Response 200
{ "message": "비밀번호가 변경되었습니다" }

// Error 401
{ "code": "AUTH003", "message": "현재 비밀번호가 일치하지 않습니다" }
```

**DELETE /:userId**
```json
// Request
{
  "password": "Pass123!",
  "reason": "탈퇴 사유"
}

// Response 200
{ "message": "탈퇴가 완료되었습니다" }
```

---

### 관리자 `/api/admin`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | /dashboard | 통계 | OPERATOR 이상 |
| GET | /associates | 승인 대기 준회원 목록 | OPERATOR 이상 |
| POST | /associates/:id/approve | 준회원 승인 | ADMIN |
| POST | /associates/approve-batch | 준회원 일괄 승인 | ADMIN |
| GET | /users | 회원 목록 | OPERATOR 이상 |
| GET | /users/:id | 회원 상세 | OPERATOR 이상 |
| PUT | /users/:id/role | 권한 변경 | ADMIN |
| PUT | /users/:id/status | 상태 변경 | ADMIN |
| DELETE | /users/:id | 강제 탈퇴 | ADMIN |

**GET /dashboard**
```json
// Response 200
{
  "todayVisitors": 45,
  "todayPosts": 12,
  "todayComments": 35,
  "weeklyNewUsers": 8,
  "pendingInquiries": 5,
  "pendingAssociates": 3
}
```

**GET /associates** (ADMIN 전용)
```json
// Response 200
{
  "associates": [
    {
      "id": 1,
      "studentId": "20231234",
      "name": "홍길동",
      "department": "컴퓨터공학과",
      "motivation": "웹 개발에 관심이 있습니다.",
      "createdAt": "2025-01-21T10:00:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalCount": 3
  }
}
```

**POST /associates/:id/approve** (ADMIN 전용)
```json
// Response 200
{
  "id": 1,
  "name": "홍길동",
  "role": "MEMBER",
  "message": "정회원으로 승인되었습니다"
}
```

**PUT /users/:id/status**
```json
// Request - 정지
{
  "status": "SUSPENDED",
  "suspendedUntil": "2025-02-01T00:00:00Z",
  "reason": "규칙 위반"
}

// Request - 정지 해제
{
  "status": "ACTIVE"
}

// Response 200
{
  "id": 1,
  "status": "SUSPENDED",
  "message": "모든 활성 토큰이 무효화되었습니다"
}

// Error 400 - 자기 자신 정지 불가
{ "code": "ADMIN002", "message": "본인 계정은 정지할 수 없습니다" }
```

**POST /members/upload** `multipart/form-data`
```
file: [CSV 파일]
semester: "2025-1"
```
```json
// Response 200
{
  "totalRows": 100,
  "added": 25,
  "updated": 70,
  "errors": [
    { "row": 5, "studentId": "2023123", "error": "학번은 8자리여야 합니다" }
  ],
  "inactivated": 15
}
```

---

### 개인정보 `/api/privacy`

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| GET | /policy | 개인정보 처리방침 조회 | 전체 |
| GET | /policy/versions | 처리방침 버전 목록 | 전체 |
| GET | /consents | 내 동의 이력 조회 | 로그인 |

**GET /policy**
```json
// Response 200
{
  "version": "2026-01-22",
  "effectiveDate": "2026-01-22",
  "content": "개인정보 처리방침 전문...",
  "sections": [
    {
      "title": "1. 개인정보의 처리 목적",
      "content": "..."
    },
    {
      "title": "2. 처리하는 개인정보 항목",
      "content": "..."
    }
  ]
}
```

**GET /consents** (로그인 필수)
```json
// Response 200
{
  "consents": [
    {
      "consentType": "PRIVACY_POLICY",
      "version": "2026-01-22",
      "consentedAt": "2026-01-22T10:00:00Z"
    }
  ]
}
```

---

## 에러 코드

### 인증 관련 (AUTH)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| AUTH002 | 409 | 이미 가입된 계정입니다 |
| AUTH003 | 401 | 학번 또는 비밀번호가 일치하지 않습니다 |
| AUTH005 | 403 | 계정이 정지되었습니다 |
| AUTH007 | 401 | 토큰이 만료되었습니다 |
| AUTH008 | 401 | 유효하지 않은 토큰입니다 |
| AUTH009 | 403 | 정회원 승인 후 이용 가능합니다 |
| AUTH010 | 400 | 개인정보 처리방침에 동의해야 합니다 |
| AUTH011 | 409 | 탈퇴 후 5일이 지나야 재가입할 수 있습니다 |
| AUTH012 | 200 | 탈퇴한 계정입니다. 복구하시겠습니까? |
| AUTH013 | 400 | 복구 가능 기간이 만료되었습니다 |
| AUTH014 | 400 | 인증 코드가 올바르지 않습니다 |
| AUTH015 | 400 | 인증 코드가 만료되었습니다 |
| AUTH016 | 429 | 인증 시도 횟수를 초과했습니다 |
| AUTH017 | 429 | 재발송 대기 시간입니다 |
| AUTH018 | 403 | 이메일 인증이 완료되지 않았습니다 |

### 게시글 관련 (POST)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| POST001 | 404 | 게시글을 찾을 수 없습니다 |
| POST002 | 403 | 수정 권한이 없습니다 |
| POST003 | 400 | 제목은 100자 이내여야 합니다 |
| POST004 | 400 | 이미지는 최대 5개까지 첨부 가능합니다 |
| POST005 | 400 | 이미지 크기는 10MB 이하여야 합니다 |
| POST006 | 403 | 정회원 승인 후 이용 가능합니다 |

### 댓글 관련 (COMMENT)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| COMMENT001 | 400 | 대댓글에는 답글을 달 수 없습니다 |
| COMMENT002 | 400 | 댓글은 500자 이내여야 합니다 |
| COMMENT003 | 404 | 댓글을 찾을 수 없습니다 |

### 행사 관련 (EVENT)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| EVENT001 | 400 | 정원이 마감되었습니다 |
| EVENT002 | 400 | 신청 기간이 종료되었습니다 |
| EVENT003 | 409 | 이미 신청한 행사입니다 |
| EVENT004 | 404 | 행사를 찾을 수 없습니다 |
| EVENT005 | 400 | 신청 내역이 없습니다 |
| EVENT006 | 403 | 정회원 승인 후 신청 가능합니다 |

### 관리자 관련 (ADMIN)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| ADMIN001 | 403 | 운영진 이상의 권한이 필요합니다 |
| ADMIN002 | 400 | 본인 계정은 정지할 수 없습니다 |
| ADMIN003 | 400 | 마지막 관리자는 권한을 변경할 수 없습니다 |
| ADMIN004 | 403 | 관리자 권한이 필요합니다 |
| ADMIN005 | 400 | 본인 권한은 변경할 수 없습니다 |

### 문의 관련 (INQUIRY)
| 코드 | HTTP | 메시지 |
|------|------|--------|
| INQUIRY001 | 404 | 문의를 찾을 수 없습니다 |
| INQUIRY002 | 403 | 본인의 문의만 조회할 수 있습니다 |
| INQUIRY003 | 400 | 이미 답변이 등록된 문의입니다 |

---

## 데이터 모델

### 공통 엔티티 패턴

**BaseEntity (기본 엔티티)**
모든 엔티티가 상속하는 기본 클래스로, JPA Auditing을 적용하여 생성/수정 정보를 자동 관리한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| createdAt | DateTime | 생성일 (NOT NULL, 수정 불가) |
| updatedAt | DateTime | 수정일 (NOT NULL) |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |

**SoftDeletableEntity (소프트 삭제 엔티티)**
BaseEntity를 상속하며, 논리적 삭제(Soft Delete)를 지원한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| deleted | Boolean | 삭제 여부 (NOT NULL, 기본값: false) |
| deletedAt | DateTime | 삭제일 (nullable) |
| deletedBy | Long | 삭제자 ID (nullable) |

**Soft Delete 적용 대상**
- User, PasswordCredential, Position: SoftDeletableEntity 상속 (삭제된 데이터 복구 가능)
- UserPosition, UserSuspension, UserRoleHistory: BaseEntity만 상속 (감사 이력 또는 중간 테이블)

---

### 정규화 설계 원칙

**적용된 정규화:**
- **1NF**: 모든 컬럼이 원자값(Atomic Value)을 가짐
- **2NF**: 모든 비키 속성이 기본키에 완전 함수 종속
- **3NF**: 이행적 종속 제거 (비키 속성 간 종속성 분리)

**테이블 분리 근거:**
| 분리 전 | 분리 후 | 분리 사유 |
|---------|---------|-----------|
| User.password | PasswordCredential.passwordHash | 프로필 정보와 인증 자격증명 분리 (보안 및 책임 분리) |
| User.status | PasswordCredential.status | 계정 상태는 인증 도메인에 귀속 (로그인 가능 여부 결정) |
| User.suspendedUntil | UserSuspension 테이블 | 정지 이력 관리 및 3NF 위반 해소 (status→suspendedUntil 이행 종속) |
| User.approvedAt | PasswordCredential.approvedAt | 승인 정보는 인증 도메인에 귀속 |

**role을 User에 배치한 이유:**
- role은 "조직 내 사용자의 위치"를 나타내는 프로필 속성
- 인증(Authentication)이 아닌 인가(Authorization) 개념
- 대부분의 조회에서 User + role이 함께 필요 → JOIN 불필요

**테이블 관계:**
```
User (1) ─────── (1) PasswordCredential
  │
  └─────────────── (N) UserSuspension
  │
  └─────────────── (N) UserRoleHistory
  │
  └─────────────── (N) UserPosition (N) ─────── (1) Position
  │
  └─────────────── (N) RefreshToken
  │
  └─────────────── (N) Post
  │
  └─────────────── (N) Comment
  │
  └─────────────── (N) EventRegistration
```

---

### User (사용자 기본정보)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| studentId | String(8) | 학번 (Unique) |
| name | String(50) | 본명 |
| email | String | 이메일 (Unique) |
| phoneNumber | String(20) | 전화번호 (Unique) |
| department | String(50) | 학과 |
| motivation | Text | 가입 동기 |
| role | Enum | ASSOCIATE, MEMBER, OPERATOR, ADMIN |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |
| deleted | Boolean | 삭제 여부 (기본값: false) |
| deletedAt | DateTime | 삭제일 (nullable) |
| deletedBy | Long | 삭제자 ID (nullable) |

### PasswordCredential (비밀번호 인증 자격증명)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| user | User | 사용자 FK (Unique, 1:1 관계) |
| passwordHash | String | 비밀번호 해시 (BCrypt) |
| status | Enum | ACTIVE, SUSPENDED, WITHDRAWN (기본값: ACTIVE) |
| approvedAt | DateTime | 정회원 승인일 (nullable) |
| approvedBy | Long | 승인 처리자 ID (nullable) |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |
| deleted | Boolean | 삭제 여부 (기본값: false) |
| deletedAt | DateTime | 삭제일 (nullable) |
| deletedBy | Long | 삭제자 ID (nullable) |

### UserSuspension (정지 이력)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| user | User | 사용자 FK |
| reason | String | 정지 사유 (NOT NULL) |
| suspendedAt | DateTime | 정지 시작일 |
| suspendedUntil | DateTime | 정지 종료일 |
| suspendedBy | Long | 정지 처리자 ID |
| liftedAt | DateTime | 해제일 (nullable) |
| liftedBy | Long | 해제 처리자 ID (nullable) |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |

※ 감사 이력 테이블이므로 Soft Delete 미적용

### UserRoleHistory (역할 변경 이력)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| user | User | 사용자 FK |
| previousRole | Enum | 이전 역할 |
| newRole | Enum | 새 역할 |
| reason | String | 변경 사유 (nullable) |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |

※ 감사 이력 테이블이므로 Soft Delete 미적용

### Position (직책)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| name | String(20) | 직책명 (Unique) - 예: 기술부, 기술부장, 회장 |
| imageUrl | String | 직책 이미지 URL (nullable) |
| displayOrder | Integer | 표시 순서 (nullable) |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |
| deleted | Boolean | 삭제 여부 (기본값: false) |
| deletedAt | DateTime | 삭제일 (nullable) |
| deletedBy | Long | 삭제자 ID (nullable) |

### UserPosition (사용자-직책 매핑)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| user | User | 사용자 FK |
| position | Position | 직책 FK |
| assignedAt | DateTime | 직책 부여일 |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |
| createdBy | Long | 생성자 ID |
| updatedBy | Long | 수정자 ID |

※ 한 사용자가 여러 직책을 가질 수 있음 (다대다 관계)
※ UNIQUE 제약: (user_id, position_id)
※ 중간 테이블이므로 Soft Delete 미적용

### Post (게시글)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| board | Enum | notices, general, insight |
| title | String(100) | 제목 |
| content | Text | 내용 |
| authorId | Long | 작성자 FK |
| isAnonymous | Boolean | 익명 여부 |
| isQuestion | Boolean | 질문 태그 |
| isVisibleToAssociate | Boolean | 준회원 공개 여부 (공지사항만) |
| viewCount | Integer | 조회수 |
| isDeleted | Boolean | 삭제 여부 |
| createdAt | DateTime | 작성일 |
| updatedAt | DateTime | 수정일 |

### PostImage (게시글 이미지)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| url | String | 이미지 URL |
| order | Integer | 순서 |

### Comment (댓글)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| parentId | Long | 부모 댓글 FK (nullable) |
| authorId | Long | 작성자 FK |
| content | String(500) | 내용 |
| isAnonymous | Boolean | 익명 여부 |
| isDeleted | Boolean | 삭제 여부 |
| createdAt | DateTime | 작성일 |

### Like (좋아요)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 생성일 |

### Bookmark (북마크)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 생성일 |

### Event (행사)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| title | String | 제목 |
| description | Text | 설명 |
| startDatetime | DateTime | 시작 일시 |
| endDatetime | DateTime | 종료 일시 |
| location | String | 장소 |
| capacity | Integer | 정원 |
| registrationDeadline | DateTime | 신청 마감일 |
| status | Enum | UPCOMING, ONGOING, CLOSED, CANCELED |
| createdAt | DateTime | 생성일 |
| updatedAt | DateTime | 수정일 |

### EventRegistration (행사 신청)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| eventId | Long | 행사 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 신청일 |

### Inquiry (문의)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryNumber | String | 문의 번호 |
| type | Enum | JOIN, EVENT, REPORT, ACCOUNT, OTHER |
| title | String | 제목 |
| content | Text | 내용 |
| email | String | 이메일 |
| userId | Long | 사용자 FK (nullable) |
| status | Enum | PENDING, IN_PROGRESS, COMPLETED |
| createdAt | DateTime | 작성일 |
| updatedAt | DateTime | 수정일 |

### InquiryAttachment (문의 첨부파일)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryId | Long | 문의 FK |
| url | String | 파일 URL |

### InquiryMemo (문의 내부 메모)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryId | Long | 문의 FK |
| adminId | Long | 관리자 FK |
| content | Text | 메모 내용 |
| createdAt | DateTime | 작성일 |

### InquiryReply (문의 답변)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| inquiryId | Long | 문의 FK (Unique) |
| adminId | Long | 답변 작성자 FK |
| content | Text | 답변 내용 |
| emailSent | Boolean | 이메일 발송 여부 |
| emailSentAt | DateTime | 이메일 발송 일시 (nullable) |
| createdAt | DateTime | 작성일 |

### RefreshToken
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| token | String | 토큰 값 |
| userId | Long | 사용자 FK |
| expiresAt | DateTime | 만료일 |
| createdAt | DateTime | 생성일 |

### PrivacyConsent (개인정보 동의 이력)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK |
| consentType | Enum | PRIVACY_POLICY (개인정보 처리방침), TERMS (이용약관) |
| version | String | 동의한 정책 버전 |
| consentedAt | DateTime | 동의 일시 |
| ipAddress | String | 동의 시 IP 주소 |

### LoginHistory (로그인 기록 - 통신비밀보호법)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK |
| loginAt | DateTime | 로그인 일시 |
| ipAddress | String | 접속 IP |
| userAgent | String | 접속 기기 정보 |
| success | Boolean | 성공 여부 |

### EmailVerification (이메일 인증)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| email | String | 인증 대상 이메일 |
| code | String(6) | 인증 코드 |
| signupData | JSON | 임시 저장된 가입 정보 |
| attemptCount | Integer | 인증 시도 횟수 |
| lastSentAt | DateTime | 마지막 발송 시각 |
| expiresAt | DateTime | 만료 시각 |
| verifiedAt | DateTime | 인증 완료 시각 (nullable) |
| createdAt | DateTime | 생성일 |

### EmailLog (이메일 발송 이력)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| toEmail | String | 수신자 이메일 |
| emailType | Enum | VERIFICATION, PASSWORD_RESET, INQUIRY_REPLY |
| status | Enum | PENDING, SENT, FAILED |
| retryCount | Integer | 재시도 횟수 |
| errorMessage | String | 실패 시 에러 메시지 (nullable) |
| sentAt | DateTime | 발송 시각 (nullable) |
| createdAt | DateTime | 생성일 |

### PostView (게시글 조회 기록 - 조회수 중복 방지)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| postId | Long | 게시글 FK |
| userId | Long | 사용자 FK (nullable, 비회원 조회 시 null) |
| ipAddress | String | 접속 IP (비회원 조회 시 사용) |
| viewedAt | DateTime | 조회 일시 |

※ 동일 사용자/IP의 중복 조회수 증가 방지

### CommentLike (댓글 좋아요)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| commentId | Long | 댓글 FK |
| userId | Long | 사용자 FK |
| createdAt | DateTime | 생성일 |

※ UNIQUE 제약: (comment_id, user_id)
※ 본인 댓글에는 좋아요 불가

### CommentReport (댓글 신고)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| commentId | Long | 댓글 FK |
| reporterId | Long | 신고자 FK |
| reason | String | 신고 사유 |
| status | Enum | PENDING, APPROVED, REJECTED |
| processedAt | DateTime | 처리 일시 (nullable) |
| processedBy | Long | 처리자 ID (nullable) |
| createdAt | DateTime | 생성일 |

※ 관리자(OPERATOR 이상) 검토 대기열에 추가

### LoginAttempt (로그인 시도 제한 - Brute Force 방지)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| studentId | String | 학번 |
| attemptCount | Integer | 시도 횟수 |
| lastAttemptAt | DateTime | 마지막 시도 일시 |
| lockedUntil | DateTime | 잠금 해제 시각 (nullable) |
| createdAt | DateTime | 생성일 |

※ 5회 연속 실패 시 계정 잠금 (15분간)
※ 로그인 성공 시 시도 횟수 초기화

### PasswordResetToken (비밀번호 재설정 토큰)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK |
| token | String | 재설정 토큰 (UUID) |
| expiresAt | DateTime | 만료 시각 (기본 1시간) |
| usedAt | DateTime | 사용 일시 (nullable) |
| createdAt | DateTime | 생성일 |

### SemesterMember (학기별 회원 명단)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| userId | Long | 사용자 FK |
| year | Integer | 연도 (예: 2026) |
| semester | Integer | 학기 (1 또는 2) |
| role | Enum | 등록 시점 역할 (ASSOCIATE/MEMBER/OPERATOR/ADMIN) |
| joinedAt | DateTime | 해당 학기 등록일 |
| createdAt | DateTime | 생성일 |
| createdBy | Long | 등록자 ID |

※ UNIQUE 제약: (user_id, year, semester)
※ 탈퇴 회원 포함하여 조회 가능 (soft delete 필터 우회)

---

## 성공 기준

| ID | 기준 | 측정 방법 |
|----|------|----------|
| SC-001 | 사용자가 회원가입을 5분 이내에 완료할 수 있다 | 가입 시작부터 이메일 인증 완료까지 시간 측정 |
| SC-002 | 게시글 작성이 1분 이내에 완료된다 | 작성 화면 진입부터 게시 완료까지 시간 측정 |
| SC-012 | 이메일 인증 코드가 30초 이내에 발송된다 | 요청부터 이메일 수신까지 시간 측정 |
| SC-003 | 게시글 목록이 3초 이내에 표시된다 | 페이지 요청부터 콘텐츠 렌더링까지 시간 측정 |
| SC-004 | 행사 신청/취소가 2번의 클릭으로 완료된다 | UX 플로우 검증 |
| SC-005 | 동시 100명 접속 시 정상 동작한다 | 부하 테스트 |
| SC-006 | 관리자가 회원 상태 변경(승인, 정지 등) 후 즉시(5초 이내) 권한이 반영된다 | 상태 변경 후 권한 검증 시간 측정 |
| SC-007 | 주요 기능 성공률 99% 이상 | 모니터링 대시보드 |
| SC-008 | 준회원 승인 대기 목록이 관리자 대시보드에서 실시간으로 조회 가능하다 | 대시보드 기능 검증 |
| SC-009 | 회원 탈퇴 시 5일 이내에 개인정보가 파기된다 | 파기 로그 검증 |
| SC-010 | 개인정보 처리방침이 웹사이트에서 쉽게 접근 가능하다 | UI 검증 (Footer 링크) |
| SC-011 | 정보주체의 열람/정정/삭제 요청이 법정 기한 내 처리된다 | 요청-처리 시간 측정 |

---

## 가정 및 제약사항

### 가정
- 이메일 발송 서비스는 자체 구현한다 (회원가입 인증, 비밀번호 재설정용)
- 이미지 저장소는 외부 클라우드 스토리지를 사용한다
- 관리자가 준회원 승인을 수동으로 처리한다

### 제약사항
- 학번은 인하대학교 학번 체계(8자리)를 따른다
- 준회원은 관리자 승인 전까지 제한된 기능만 이용 가능
- 행사 신청 시 동시성 처리가 필요함 (Race Condition 방지)
- **개인정보보호법 준수 의무**
  - 회원가입 시 개인정보 수집·이용 동의 필수
  - 회원 탈퇴 시 5일 이내 개인정보 파기 (법령 보존 의무 항목 제외)
  - 개인정보 처리방침 웹사이트 공개 필수
  - 로그인 기록 최소 3개월 보관 (통신비밀보호법)

---

## 범위 외 항목

- 실시간 채팅 기능
- 푸시 알림
- 소셜 로그인 (카카오, 구글 등)
- 모바일 앱
- 결제 기능
- 다국어 지원

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 2026-01-22 | 2.0 | 최초 작성 | - |
| 2026-02-05 | 2.1 | 구현 현황 반영 | - |

### v2.1 변경 상세 (2026-02-05)

**추가된 엔티티:**
- PostView (조회수 중복 방지)
- CommentLike (댓글 좋아요)
- CommentReport (댓글 신고)
- LoginAttempt (로그인 시도 제한, Brute Force 방지)
- PasswordResetToken (비밀번호 재설정 토큰)
- SemesterMember (학기별 회원 명단)

**추가된 기능:**
- 댓글 좋아요/취소 (본인 댓글 불가)
- 댓글 신고 및 관리자 검토
- 댓글 멘션 (@사용자명) 및 알림
- 조회수 중복 방지 (IP/사용자 기반)
- 로그인 시도 횟수 제한 및 계정 잠금 (5회 실패 시 15분간)
- 학기별 회원 등록/제외/조회 (ADMIN/OPERATOR)
- 행사 선착순(AUTO_APPROVE)/선발제(MANUAL_APPROVE) 구분
- 비회원 문의 비밀번호 검증

**API 변경:**
- 모든 API prefix를 `/api/v1/`로 통일
- 인증 API path 변경: `/api/v1/auth/password/*`
- 계정 복구 확인 API 추가: `GET /api/v1/auth/password/account/recovery-check`
- 비밀번호 재설정 토큰 검증 API 추가: `GET /api/v1/auth/password/reset-validate`
- 학기별 회원 API 추가: `/api/v1/semesters/*`, `/api/v1/admin/semesters/*`

**상태 변경:**
- 문서 상태: Draft → In Progress
- 백엔드: 모든 기능 완전 구현
- 프론트엔드: 주요 기능 구현 완료, 일부 관리자 페이지 개발 중

---

## 관련 문서

- [인증 스펙](../auth/auth-spec.md)
- [사용자 엔티티 설계](../auth/user-entity-design.md)
- [게시판 스펙](../community/board-spec.md)
- [게시글 스펙](../community/post-spec.md)
- [댓글 스펙](../community/comment-spec.md)
- [좋아요/북마크 스펙](../community/like-bookmark-spec.md)
- [학기별 회원 명단 스펙](../member-list/member-list-spec.md)
