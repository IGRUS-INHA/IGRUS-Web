-- 낙관적 락을 위한 version 컬럼 추가
ALTER TABLE associate_decisions ADD COLUMN associate_decisions_version BIGINT NOT NULL DEFAULT 0;
