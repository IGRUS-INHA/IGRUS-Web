# Tasks: 테스트 코드 성능 리팩토링

**목적**: 테스트 실행 속도 개선 및 성능 병목 해소
**예상 개선 효과**: 전체 테스트 실행 시간 30-40% 단축

## 분석 기반

성능 병목 분석 결과 발견된 주요 문제:
- SQL 로깅 활성화로 인한 I/O 오버헤드
- DDL 자동 생성(create-drop)으로 인한 스키마 재생성
- 매 테스트마다 27개 테이블 전체 DELETE
- @MockitoBean 중복 사용으로 컨텍스트 캐시 미스

---

## 리팩토링 전 성능 측정

**측정일**: 2026-01-28
**테스트 개수**: 957개
**측정 환경**: Windows, Gradle 빌드, H2 인메모리 DB

### 10회 실행 결과

| 실행 | 소요 시간 | 초 환산 |
|------|----------|--------|
| Run 1 | 1m 42s | 102초 |
| Run 2 | 2m 44s | 164초 |
| Run 3 | 2m 17s | 137초 |
| Run 4 | 2m 10s | 130초 |
| Run 5 | 2m 5s | 125초 |
| Run 6 | 1m 43s | 103초 |
| Run 7 | 2m 2s | 122초 |
| Run 8 | 2m 5s | 125초 |
| Run 9 | 2m 0s | 120초 |
| Run 10 | 2m 14s | 134초 |

### 통계

| 항목 | 값 |
|------|-----|
| **평균** | **2분 6초 (126.2초)** |
| 최소 | 1분 42초 (102초) |
| 최대 | 2분 44초 (164초) |
| 표준편차 | ~18초 |

### 목표

- **30% 개선 시**: 평균 88초 (1분 28초)
- **50% 개선 시**: 평균 63초 (1분 3초)

---

## Format: `[ID] [P?] [US?] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[US]**: 관련 사용자 스토리 (US1, US2, US3...)

---

## Phase 1: 즉시 개선 (Quick Wins)

**목적**: 설정 변경만으로 즉시 성능 개선 (예상 개선: 15-25%)

- [ ] T001 [P] SQL 로깅 비활성화 in backend/src/test/resources/application.yml
  - `show-sql: true` → `show-sql: false`
  - `format_sql: true` → `format_sql: false`

- [ ] T002 [P] Hibernate statistics 비활성화 확인 in backend/src/test/resources/application.yml
  - `generate_statistics: false` 설정 확인

- [ ] T003 DDL 전략 변경 검토 in backend/src/test/resources/application.yml
  - `ddl-auto: create-drop` → `ddl-auto: create` 또는 `validate` 검토
  - 테스트 격리에 영향 없는지 확인 필요

**Checkpoint**: 설정 변경 후 테스트 실행 시간 측정 및 비교

---

## Phase 2: 테이블 정리 로직 최적화

**목적**: ServiceIntegrationTestBase의 cleanupDatabase() 성능 개선 (예상 개선: 20-30%)

### 분석 및 설계

- [ ] T004 현재 cleanupDatabase() 로직 분석 in backend/src/test/java/igrus/web/common/ServiceIntegrationTestBase.java
  - 27개 테이블 DELETE 순서 및 의존성 파악
  - 실제로 정리가 필요한 테이블 식별

- [ ] T005 테스트별 데이터 사용 패턴 분석
  - 각 테스트가 실제로 사용하는 테이블 목록 파악
  - 불필요한 테이블 정리 식별

### 구현

- [ ] T006 TRUNCATE 기반 정리 방식으로 변경 in backend/src/test/java/igrus/web/common/ServiceIntegrationTestBase.java
  - H2에서 `SET REFERENTIAL_INTEGRITY FALSE` 활용
  - 모든 테이블 TRUNCATE 후 `SET REFERENTIAL_INTEGRITY TRUE`
  - 27번의 DELETE → 단일 트랜잭션 TRUNCATE로 변경

- [ ] T007 [P] 선택적 정리 유틸리티 추가 (선택사항)
  - 테스트에서 사용한 테이블만 정리하는 방식 검토
  - `@DirtiesContext` 대안으로 활용 가능

**Checkpoint**: cleanupDatabase() 개선 후 통합 테스트 시간 측정

---

## Phase 3: 컨텍스트 캐싱 최적화

**목적**: Spring 컨텍스트 재사용률 향상 (예상 개선: 10-15%)

### 분석

- [ ] T008 @MockitoBean 사용 현황 분석
  - 12개 파일에서 46개 @MockitoBean 사용 패턴 파악
  - 중복되는 Mock 설정 식별 (예: AuthEmailService 5회 중복)

- [ ] T009 컨텍스트 캐시 미스 원인 분석
  - 각 테스트 클래스의 Mock 조합 비교
  - 컨텍스트 공유 가능한 클래스 그룹화

### 구현

- [ ] T010 공통 Mock 설정 통합 in backend/src/test/java/igrus/web/common/
  - MockConfiguration 클래스 생성 검토
  - 자주 사용되는 Mock (AuthEmailService 등) 중앙화

- [ ] T011 [P] ControllerIntegrationTestBase Mock 설정 정리 in backend/src/test/java/igrus/web/security/auth/password/controller/ControllerIntegrationTestBase.java
  - 불필요한 @MockitoBean 제거
  - 필수 Mock만 유지

- [ ] T012 [P] ServiceIntegrationTestBase Mock 설정 검토 in backend/src/test/java/igrus/web/common/ServiceIntegrationTestBase.java
  - 상속받는 클래스들의 Mock 일관성 확보

**Checkpoint**: 컨텍스트 초기화 횟수 측정 (테스트 실행 로그 확인)

---

## Phase 4: 테스트 구조 개선

**목적**: 장기적 테스트 성능 및 유지보수성 향상

### 테스트 슬라이스 최적화

- [ ] T013 @SpringBootTest 사용 재검토
  - 현재 4개 클래스에서 전체 컨텍스트 로드
  - @DataJpaTest, @WebMvcTest 등 슬라이스 테스트로 전환 가능 여부 확인

- [ ] T014 [P] Repository 테스트 @DataJpaTest 전환 검토
  - PrivacyConsentRepositoryTest 등 JPA 슬라이스 테스트 적용

### 테스트 병렬 실행 설정

- [ ] T015 JUnit 5 병렬 실행 설정 추가 in backend/src/test/resources/junit-platform.properties
  ```properties
  junit.jupiter.execution.parallel.enabled=true
  junit.jupiter.execution.parallel.mode.default=concurrent
  junit.jupiter.execution.parallel.mode.classes.default=concurrent
  ```

- [ ] T016 병렬 실행 호환성 검증
  - 테스트 격리 문제 발생 여부 확인
  - 필요시 @Isolated 어노테이션 적용

**Checkpoint**: 병렬 실행 후 전체 테스트 시간 측정

---

## Phase 5: 검증 및 문서화

**목적**: 개선 효과 측정 및 가이드라인 수립

- [ ] T017 성능 개선 전후 비교 측정
  - 전체 테스트 실행 시간
  - 개별 테스트 클래스별 시간
  - 컨텍스트 초기화 횟수

- [ ] T018 [P] 테스트 작성 가이드라인 문서화 in backend/docs/test-guidelines.md
  - 성능을 고려한 테스트 작성 규칙
  - @MockitoBean 사용 시 주의사항
  - 테스트 격리 전략

- [ ] T019 [P] CLAUDE.md 테스트 섹션 업데이트 in backend/CLAUDE.md
  - 테스트 성능 관련 규칙 추가

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (즉시 개선) ──┬──> Phase 2 (테이블 정리)
                      │
                      └──> Phase 3 (컨텍스트 캐싱)
                                    │
                                    v
                           Phase 4 (구조 개선)
                                    │
                                    v
                           Phase 5 (검증/문서화)
```

### 우선순위별 실행

1. **즉시 (Phase 1)**: T001-T003 - 설정 변경만으로 빠른 개선
2. **단기 (Phase 2)**: T004-T007 - 가장 큰 병목인 테이블 정리 개선
3. **중기 (Phase 3)**: T008-T012 - 컨텍스트 캐싱 최적화
4. **장기 (Phase 4-5)**: T013-T019 - 구조 개선 및 문서화

### Parallel Opportunities

```bash
# Phase 1 병렬 실행 가능:
T001 (SQL 로깅) + T002 (Hibernate stats)

# Phase 3 병렬 실행 가능:
T011 (Controller Mock) + T012 (Service Mock)

# Phase 5 병렬 실행 가능:
T018 (가이드라인) + T019 (CLAUDE.md)
```

---

## 예상 개선 효과 요약

| Phase | 개선 항목 | 예상 개선율 |
|-------|----------|------------|
| Phase 1 | SQL 로깅 비활성화 | 15-20% |
| Phase 2 | 테이블 정리 최적화 | 20-30% |
| Phase 3 | 컨텍스트 캐싱 | 10-15% |
| Phase 4 | 병렬 실행 | 추가 개선 |
| **총합** | | **30-50%** |

---

## 관련 파일

### 주요 수정 대상
- `backend/src/test/resources/application.yml` - 테스트 설정
- `backend/src/test/java/igrus/web/common/ServiceIntegrationTestBase.java` - 테스트 베이스
- `backend/src/test/java/igrus/web/security/auth/password/controller/ControllerIntegrationTestBase.java` - 컨트롤러 테스트 베이스

### 신규 생성
- `backend/src/test/resources/junit-platform.properties` - JUnit 병렬 실행 설정
- `backend/docs/test-guidelines.md` - 테스트 가이드라인 (선택)

---

## Notes

- 각 Phase 완료 후 테스트 실행하여 기존 테스트가 깨지지 않는지 확인
- 성능 측정은 동일 환경에서 3회 이상 실행하여 평균값 사용
- @Transactional 사용은 프로젝트 규칙상 지양 (CLAUDE.md 참조)
