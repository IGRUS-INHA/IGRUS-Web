-- 공개 프로필: 닉네임(표시 이름), 자기소개, 외부 링크(JSON 배열 [{"label","url"}])
ALTER TABLE users
    ADD COLUMN users_nickname VARCHAR(50) NULL,
    ADD COLUMN users_introduction TEXT NULL,
    ADD COLUMN users_links JSON NULL;
