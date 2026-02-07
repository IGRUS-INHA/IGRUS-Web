-- 감사 이력 테이블의 FK 제약조건 제거
-- 사유: 감사 이력은 비정규화된 studentId를 이미 저장하므로 FK 불필요
-- AFTER_COMMIT 이벤트 리스너가 별도 트랜잭션에서 동작하므로 FK 참조 무결성 문제 방지
-- 향후 탈퇴 사용자 hard-delete 시 FK 충돌 방지
ALTER TABLE account_status_change_histories DROP FOREIGN KEY fk_asch_user;
ALTER TABLE account_status_change_histories DROP FOREIGN KEY fk_asch_changed_by;
