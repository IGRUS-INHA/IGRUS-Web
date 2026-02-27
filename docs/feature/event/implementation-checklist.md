# 행사 Visibility 구현 체크리스트

## 메타데이터
| 항목 | 값 |
|------|---|
| 생성일 | 2026-02-27 |
| 최종 업데이트 | 2026-02-28 |
| 검증 기준 문서 | `docs/criteria/event/event-verification-criteria.md` |
| 작업 계획 문서 | `docs/feature/event/task-plan.md` |
| 전체 상태 | COMPLETED |

## 작업 진행 현황

### 그룹 1: 기반 구조 + 도메인/DTO
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-001 | Flyway 마이그레이션 | DONE | - | `backend/src/main/resources/db/migration/V46__add_event_visibility_column.sql` | event_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' |
| TASK-002 | EventVisibility enum | DONE | - | `backend/src/main/java/igrus/web/event/domain/EventVisibility.java` | SurveyVisibility 패턴 준수 |
| TASK-004 | EventChangeType 확장 | DONE | - | `backend/src/main/java/igrus/web/event/domain/EventChangeType.java` | EVENT_PUBLISHED, EVENT_UNPUBLISHED 추가 |
| TASK-012 | SecurityConfig 경로 추가 | DONE | - | `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java` | OPERATOR+ 블록에 /api/v1/admin/events/** 추가 |
| TASK-003 | Event 엔티티 visibility | DONE | - | `backend/src/main/java/igrus/web/event/domain/Event.java`, `backend/src/main/java/igrus/web/event/exception/InvalidEventStateTransitionException.java` | visibility 필드, publish(), unpublish() 추가. 3축 모델로 Javadoc 업데이트 |
| TASK-005 | EventDetailResponse visibility | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventDetailResponse.java` | visibility 필드 추가, from() 메서드 수정 |
| TASK-006 | EventListResponse visibility | DONE | - | `backend/src/main/java/igrus/web/event/dto/response/EventListResponse.java` | visibility 필드 추가, from() 메서드 수정 |

**그룹 상태**: DONE
**리뷰 이력**: -

---

### 그룹 2: Repository/Service
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-007 | EventRepository visibility 쿼리 | DONE | FIXED | `backend/src/main/java/igrus/web/event/repository/EventRepository.java` | 공개 API용 findByVisibilityAndFilters(복합 필터 JPQL) + findByIdAndVisibility + 관리자용 findAllByAdminFilters 쿼리. 리뷰 피드백 반영: 개별 쿼리 3개를 복합 필터 쿼리 1개로 통합 |
| TASK-009 | EventService publish/unpublish | DONE | PASS | `backend/src/main/java/igrus/web/event/service/EventService.java` | publishEvent(), unpublishEvent() 추가. 감사 이력 reason=null |
| TASK-008 | EventService 공개 API 필터 | DONE | FIXED | `backend/src/main/java/igrus/web/event/service/EventService.java` | 리뷰 피드백 반영: getEventList()에서 if-else if-else 구조를 단일 findByVisibilityAndFilters 호출로 개선. eventStatus+registrationStatus 동시 필터링 지원 |
| TASK-010 | EventService 관리자 조회 | DONE | FIXED | `backend/src/main/java/igrus/web/event/service/EventService.java` | 리뷰 피드백 반영: getAdminEventList(), getAdminEvent()에서 @Transactional(readOnly=true) 제거. 클래스 레벨 @Transactional 상속하여 Lazy Evaluation DB 반영 보장 |
| TASK-020 | EventRegistrationService visibility 차단 | DONE | PASS | `backend/src/main/java/igrus/web/event/service/EventRegistrationService.java` | registerEvent()에 UNPUBLISHED 차단 추가 |

**그룹 상태**: DONE
**리뷰 이력**: 리뷰 R1 피드백 2건 반영 완료 (2026-02-28)
**참고**: visibility 도입으로 기존 테스트 수정 필요 - EventServiceTest(mock 변경), EventRegistrationServiceTest(visibility mock 추가), 통합 테스트(event.publish() 호출 추가). 전체 이벤트 테스트 266개 통과.

---

### 그룹 3: Controller + SecurityConfig 테스트
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-011 | AdminEventController | DONE | PASS | `backend/src/main/java/igrus/web/event/controller/AdminEventController.java` | GET 목록/상세, POST publish/unpublish 4개 엔드포인트. SurveyController 패턴 준수 |
| TASK-018 | SecurityConfig 통합 테스트 | DONE | PASS | `backend/src/test/java/igrus/web/event/integration/AdminEventControllerSecurityTest.java` | 비인증(401), ASSOCIATE/MEMBER(403), OPERATOR/ADMIN(200) 총 16개 테스트 |

**그룹 상태**: DONE
**리뷰 이력**: spec-reviewer PASS, code-reviewer PASS (R1, 2026-02-28)

---

### 그룹 4: 단위 테스트
| TASK-ID | 작업명 | 구현 상태 | 리뷰 상태 | 파일 | 비고 |
|---------|--------|----------|----------|------|------|
| TASK-013 | EventVisibility 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/domain/EventVisibilityTest.java` | canTransitionTo 4케이스 + displayName/description 2케이스 = 6개 |
| TASK-014 | Event 엔티티 테스트 | DONE | FIXED | `backend/src/test/java/igrus/web/event/domain/EventTest.java` | VisibilityTransitionTest 중첩 클래스 추가. 14개 테스트. code-reviewer R2 반영: Javadoc 3축 상태 모델로 업데이트 |
| TASK-015 | publish/unpublish 서비스 테스트 | DONE | FIXED | `backend/src/test/java/igrus/web/event/service/EventServiceTest.java` | PublishEvent(5개) + UnpublishEvent(6개). code-reviewer R2 반영: ArgumentCaptor FQCN을 import로 변경 |
| TASK-016 | 공개 API 필터 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/service/EventServiceTest.java` | GetEventVisibilityFilter(2개) + GetEventListVisibilityFilter(3개) 중첩 클래스 추가. PUBLISHED 조회 성공, UNPUBLISHED 404, Repository 호출 인자 검증 |
| TASK-017 | 관리자 API 조회 테스트 | DONE | FIXED | `backend/src/test/java/igrus/web/event/service/EventServiceTest.java` | GetAdminEventList(7개) + GetAdminEvent(5개). code-reviewer R2 반영: UNPUBLISHED 필터 테스트에서 mockEvent 재사용으로 간소화 |
| TASK-019 | DTO 테스트 | DONE | - | `backend/src/test/java/igrus/web/event/dto/EventResponseVisibilityTest.java` | EventDetailResponse(3개) + EventListResponse(2개) visibility 매핑 검증 |
| TASK-021 | 신청 차단 테스트 | DONE | FIXED | `backend/src/test/java/igrus/web/event/service/EventRegistrationServiceTest.java` | RegisterEventVisibilityTest(4개). code-reviewer R2 반영: cancelRegistration mock 상태 체이닝으로 모순 해결 |

**그룹 상태**: DONE
**리뷰 이력**: spec-reviewer R1 피드백 5건 반영, code-reviewer R2 피드백 4건(필수1 + 권장3) 반영 완료 (2026-02-28)
**테스트 결과**: 전체 이벤트 테스트 343개 모두 통과

---

## 검증 기준 충족 현황
| ID | 설명 | 관련 TASK | 상태 |
|----|------|----------|:----:|
| EVT-INV-05 | 초기 상태 (visibility=UNPUBLISHED) | TASK-003, TASK-014 | ✅ |
| EVT-INV-14 | 감사 이력 (EVENT_PUBLISHED/UNPUBLISHED) | TASK-004, TASK-009, TASK-015 | ✅ |
| EVT-INV-16 | Visibility 축 독립성 | TASK-003, TASK-014 | ✅ |
| EVT-INV-17 | Visibility 양방향 전이 | TASK-002, TASK-003, TASK-013, TASK-014 | ✅ |
| EVT-INV-18 | 공개 API UNPUBLISHED 차단 | TASK-008, TASK-020, TASK-016, TASK-021 | ✅ |
| EVT-INV-19 | Publish/Unpublish 사유 불필요 | TASK-009, TASK-011, TASK-015 | ✅ |
| EVT-INV-20 | Unpublish 시 등록 마감 연동 | TASK-003, TASK-009, TASK-014 | ✅ |
| EVT-INV-21 | 관리자 API visibility 무관 접근 | TASK-010, TASK-011, TASK-017 | ✅ |
| EVT-INV-22 | DTO visibility 필드 포함 | TASK-005, TASK-006, TASK-019 | ✅ |
| EVT-INV-23 | DB 마이그레이션 기존 데이터 정합성 | TASK-001 | ✅ |

## 이슈 로그
| # | 라운드 | 그룹 | 심각도 | 리뷰어 | 설명 | 해결 |
|---|--------|------|--------|--------|------|------|
| 1 | R1 | 그룹2 | 필수 | code-reviewer | getAdminEventList/getAdminEvent의 @Transactional(readOnly=true)가 Lazy Evaluation DB 반영을 억제함 | FIXED - @Transactional(readOnly=true) 제거, 클래스 레벨 @Transactional 상속 |
| 2 | R1 | 그룹2 | 필수 | code-reviewer | getEventList()에서 eventStatus/registrationStatus 동시 필터링 시 registrationStatus 무시됨 (if-else if-else 구조) | FIXED - Repository에 findByVisibilityAndFilters JPQL 복합 쿼리 추가, 단일 호출로 변경 |
| 3 | R1 | 그룹4 | 필수 | spec-reviewer | publishEvent/unpublishEvent 이미 동일 상태에서 호출 시 예외 테스트 누락 | FIXED - PublishEvent/UnpublishEvent에 InvalidEventStateTransitionException 테스트 추가 |
| 4 | R1 | 그룹4 | 필수 | spec-reviewer | cancelRegistration 시 visibility 무관 정상 동작 테스트 누락 | FIXED - RegisterEventVisibilityTest에 UNPUBLISHED 행사 취소 정상 동작 테스트 추가 |
| 5 | R1 | 그룹4 | 권장 | spec-reviewer | publishEvent/unpublishEvent 감사 이력 newValue 미검증 | FIXED - newValue 검증 테스트 추가 |
| 6 | R1 | 그룹4 | 권장 | spec-reviewer | unpublishEvent + OPEN 상태 등록 자동 마감 서비스 테스트 부재 | FIXED - 서비스 레벨 도메인 unpublish() 호출 검증 테스트 추가 |
| 7 | R1 | 그룹4 | 권장 | spec-reviewer | getAdminEventList registrationStatus 단독 필터 테스트 부재 | FIXED - registrationStatus 단독 필터 테스트 추가 |
| 8 | R2 | 그룹4 | 필수 | code-reviewer | cancelRegistration_UnpublishedEvent mock 상태 모순 (getStatus=CANCELED + isCanceled=false) | FIXED - getStatus() 체이닝으로 cancel 전(REGISTERED)/후(CANCELED) 구분 |
| 9 | R2 | 그룹4 | 권장 | code-reviewer | EventServiceTest ArgumentCaptor FQCN 사용 | FIXED - import 추가 후 단순명 사용 (11곳) |
| 10 | R2 | 그룹4 | 권장 | code-reviewer | EventTest Javadoc "2축 상태 모델" → "3축 상태 모델" | FIXED - 3축 상태 모델로 업데이트 |
| 11 | R2 | 그룹4 | 권장 | code-reviewer | getAdminEventList UNPUBLISHED 필터 테스트 별도 mock 생성 | FIXED - mockEvent 재사용으로 간소화 |
