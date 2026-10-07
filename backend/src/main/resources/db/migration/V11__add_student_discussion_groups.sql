CREATE TABLE student_discussion_groups (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    campus VARCHAR(160) NOT NULL,
    created_by BINARY(16) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_student_group_campus_name UNIQUE (campus, name),
    CONSTRAINT fk_student_group_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE student_discussion_group_members (
    id BINARY(16) NOT NULL PRIMARY KEY,
    group_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    joined_at DATETIME NOT NULL,
    CONSTRAINT uk_student_group_member UNIQUE (group_id, user_id),
    CONSTRAINT fk_student_group_member_group FOREIGN KEY (group_id)
        REFERENCES student_discussion_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_group_member_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE bulletin_posts
    ADD COLUMN student_group_id BINARY(16) NULL,
    ADD CONSTRAINT fk_bulletin_post_student_group FOREIGN KEY (student_group_id)
        REFERENCES student_discussion_groups(id) ON DELETE CASCADE;
