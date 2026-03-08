# Spec Reviewer Memory

## Repeated Patterns

### 1. Missing Dedicated Tests for New Integration Logic
- When existing services are extended (e.g., adding external response integration to statistics), implementers often mock the new dependency to return empty results in existing tests but fail to create dedicated test cases that exercise the new logic with actual data.
- First observed: TASK-024 review (external survey response statistics integration) - all existing tests mock `externalSurveyResponseRepository.findBySurveyId` to return `List.of()`, no tests with actual external response data.

### 2. Test Coverage for Error Handling Paths
- JSON parsing error handling (skip + logging) patterns need dedicated unit tests with malformed JSON input to verify the skip behavior.

## Project-Specific Notes
- `SurveyStatisticsServiceTest.java` is very large (2000+ lines). External response tests should be added as a new `@Nested` class.
- `ExternalSurveyResponse.answers` field is a JSON string containing `List<SubmitAnswerRequest>` structure.
- `SubmitAnswerRequest` has fields: questionId, textValue, selectedOptionIds, numericValue, gridAnswers.
