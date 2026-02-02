-- SemesterMember에 유저 정보 스냅샷 필드 추가
-- 학기 등록 시점의 유저 정보를 보존하기 위해 snapshot 컬럼 추가

ALTER TABLE semester_members ADD COLUMN semester_members_name VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE semester_members ADD COLUMN semester_members_student_id VARCHAR(20) NOT NULL DEFAULT '';
ALTER TABLE semester_members ADD COLUMN semester_members_department VARCHAR(50) NULL;
ALTER TABLE semester_members ADD COLUMN semester_members_grade INT NOT NULL DEFAULT 1;
ALTER TABLE semester_members ADD COLUMN semester_members_motivation TEXT NULL;

-- 기존 데이터 백필: users 테이블에서 현재 값을 복사
UPDATE semester_members sm
    JOIN users u ON sm.semester_members_user_id = u.users_id
SET sm.semester_members_name = u.users_name,
    sm.semester_members_student_id = u.users_student_id,
    sm.semester_members_department = u.users_department,
    sm.semester_members_grade = u.users_grade,
    sm.semester_members_motivation = u.users_motivation;

-- 백필 완료 후 기본값 제거
ALTER TABLE semester_members ALTER COLUMN semester_members_name DROP DEFAULT;
ALTER TABLE semester_members ALTER COLUMN semester_members_student_id DROP DEFAULT;
ALTER TABLE semester_members ALTER COLUMN semester_members_grade DROP DEFAULT;
