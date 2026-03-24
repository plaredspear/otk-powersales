-- education_post_attachment.education_post_id: NOT NULL → nullable
-- HerokuMigrationTool이 INSERT 후 edu_id 매칭으로 UPDATE하는 패턴 지원
ALTER TABLE salesforce2.education_post_attachment
    ALTER COLUMN education_post_id DROP NOT NULL;
