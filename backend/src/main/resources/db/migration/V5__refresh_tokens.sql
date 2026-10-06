-- =============================================================================
-- V5: refresh tokens.
--
-- An access token used to be the only credential: 24 hours, no way to renew it, so a
-- reader who stayed signed in was thrown out the moment it lapsed with nothing to
-- trade for a new one. This table holds the credential that renews it.
--
-- The value is opaque rather than signed, and it is spent in place (used = 1) on
-- every exchange, so a copy of a token that has already been traded in answers the
-- same as one that never existed.
--
-- Passwords changing is what `deleteByUserId` in the repository is for; nothing here
-- runs on that schedule — the rows are removed by the application when the password
-- changes, and they expire on their own regardless.
-- =============================================================================

create table refresh_tokens (
    used       bit          not null,
    created_at datetime(6),
    expires_at datetime(6)  not null,
    id         binary(16)   not null,
    user_id    binary(16)   not null,
    token      varchar(100) not null,
    primary key (id)
) engine = InnoDB;

alter table refresh_tokens
    add constraint fk_refresh_tokens_user foreign key (user_id) references users (id);

alter table refresh_tokens
    add constraint uk_refresh_tokens_token unique (token);
