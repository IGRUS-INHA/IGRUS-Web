-- 사용자 익명화 여부 플래그 추가
-- 탈퇴 후 복구 가능 기간(5일) 만료 시 개인정보 영구 삭제 스케줄러에 의해 true로 설정됨
ALTER TABLE users ADD COLUMN users_anonymized BOOLEAN NOT NULL DEFAULT FALSE;
