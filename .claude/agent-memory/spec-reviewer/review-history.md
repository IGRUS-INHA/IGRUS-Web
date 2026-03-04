# Detailed Review History

## Storage Domain Reviews

### Service Layer R1: TASK-014/008/009/010/011/012 - PASS (2026-02-26)
- All 6 tasks met criteria. Recommended: PresignedUrlService exception masking, Content-Length null bypass, no bulk pagination, COMPLETED vs not-found same 404.

### API Layer R1: TASK-013/015 - FAIL (2026-02-26)
- Critical: DELETE endpoint can't handle slash-containing Object Keys. Fix: @RequestParam.

### API Layer R2: TASK-013/015 - PASS (2026-02-26)
- DELETE changed to @RequestParam. authenticated() = ASSOCIATE+ (no SUSPENDED role).

### Test Layer R1: TASK-016~021 - PASS (2026-02-27)
- 34 TCs covered. Key: ArgumentCaptor for SDK params, OutputCaptureExtension for logs, native SQL for @CreatedDate overrides.

## OpenAPI Validation Reviews

### Phase C R1: TASK-202/221 - FAIL (2026-03-02)
- Critical: LevelResolver + Validator creation logic duplicated between test util and main config.

### Phase C R2: TASK-202/221 - PASS (2026-03-02)
- Resolved via OpenApiValidatorFactory. Graceful degradation added.

### Phase D R1: TASK-210/211/222 - FAIL (2026-03-02)
- Critical: TC-221-03 missing, TASK-222 violates ControllerIntegrationTestBase inheritance rule.

## Contract-First Migration Reviews

### Group 7 R1: TASK-020/060/070 - PASS (2026-03-02)
- 41 endpoints migrated. ServletContextUtil for auth, operationId collision suffixes.

## Survey-Event Registration Reviews

### Group 1 R1: TASK-001/003/006/011 - FAIL (2026-03-02)
- Critical: V47 FK references wrong column name (survey_id vs surveys_id).

### Group 2 R1: TASK-002/004/005/007 - PASS (2026-03-02)
- Entity/Service changes correct. validateSurveyExists + validateSurveyState properly implemented.
- Recommended: findByIdAndDeletedFalseAndTrashedAtIsNull, createEvent log missing surveyId.

### Group 3 R1: TASK-008/009/010/015 - PASS (2026-03-02)
- 8-branch matrix fully implemented in registerEventWithSurvey()
- Branching: event.hasSurvey() routes to private method (same @Transactional)
- Re-registration: checks survey state + response existence, no surveyAnswers processing (by design)
- Verification order matches SEVT-INV-12 exactly
- All 6 log events correct levels/formats
- Recommended: handleReRegistration() no surveyAnswers, controller hardcodes null (TASK-012 pending),
  branch #1 throws SurveyResponseDuplicateException vs criteria "ignore", DECISION-05 comment missing
