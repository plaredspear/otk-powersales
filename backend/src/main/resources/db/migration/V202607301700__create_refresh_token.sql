-- Refresh Token 저장소 — Redis SoT → DB SoT 전환.
--
-- 전환 배경: refresh token 메타데이터가 Redis 에만 존재해 Redis 장애 시 로그인/토큰갱신이
-- 전면 불가했다 (WebRefreshTokenStore.store / JwtTokenProvider.storeRefreshToken 이 곧바로
-- RedisConnectionFailureException). access token 블랙리스트는 매 요청 조회라 Redis 에 남기고,
-- 사용자당 시간당 1회 수준인 refresh 계열만 DB 를 SoT 로 삼는다.
--
-- audience 분리(MOBILE/WEB): 기존 Redis 키 prefix 분리(`refresh:` vs `web_refresh:`) 를 컬럼으로
-- 옮긴 것. 단순한 네임스페이스 분리가 아니라 **user_id 의 의미 자체가 다르다** —
--   MOBILE = employee.employee_id (JwtTokenProvider 발급 경로)
--   WEB    = users.users_id      (WebAuthenticationService 발급 경로)
-- 따라서 employee/users 어느 쪽으로도 FK 를 걸지 않는다. audience 없이 user_id 를 단독 조회하면
-- 두 id 공간이 충돌한다.
--
-- expires_at: Redis TTL 의 대체물. 조회 시 반드시 `expires_at > now()` 를 함께 판정해야 하며
-- (만료행이 배치 실행 전까지 남아 있으므로), 물리 삭제는 RefreshTokenCleanupBatch 가 담당한다.

CREATE TABLE powersales.refresh_token (
    refresh_token_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audience         VARCHAR(10) NOT NULL,
    token_id         VARCHAR(36) NOT NULL,
    user_id          BIGINT NOT NULL,
    family_id        VARCHAR(36) NOT NULL,
    issued_at        TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    -- BaseEntity (V165 timestamptz 전환 이후 신규 테이블 표준)
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_token_audience_token UNIQUE (audience, token_id)
);

-- 로그아웃 / 단말 초기화 시 사용자별 전량 회수 경로.
CREATE INDEX idx_refresh_token_audience_user
    ON powersales.refresh_token (audience, user_id);

-- 만료행 정리 배치 경로.
CREATE INDEX idx_refresh_token_expires_at
    ON powersales.refresh_token (expires_at);

-- Token Family 무효화 기록 (재사용=탈취 감지 시 family 전체 차단).
-- Redis `refresh_family:<familyId>` = "revoked" + TTL 의 대체물. expires_at 경과 시 무효화가
-- 자연 해제되는 것도 Redis TTL 동작과 동일하다.
CREATE TABLE powersales.refresh_token_family_revocation (
    refresh_token_family_revocation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audience   VARCHAR(10) NOT NULL,
    family_id  VARCHAR(36) NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_token_family_revocation_audience_family UNIQUE (audience, family_id)
);

CREATE INDEX idx_refresh_token_family_revocation_expires_at
    ON powersales.refresh_token_family_revocation (expires_at);
