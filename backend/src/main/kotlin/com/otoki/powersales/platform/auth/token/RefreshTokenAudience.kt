package com.otoki.powersales.platform.auth.token

/**
 * Refresh Token 발급 채널 — 기존 Redis 키 prefix 분리(`refresh:` vs `web_refresh:`) 를 컬럼으로 옮긴 것.
 *
 * 단순 네임스페이스 분리가 아니라 `user_id` 의 의미가 채널별로 다르다는 점이 핵심이다.
 * 두 채널의 id 공간이 겹치므로 모든 조회는 반드시 audience 를 함께 조건에 넣어야 한다.
 */
enum class RefreshTokenAudience {
    /** 모바일 앱 — `user_id` = `employee.employee_id`. 발급 경로 [com.otoki.powersales.platform.common.security.JwtTokenProvider]. */
    MOBILE,

    /** 웹(관리자) — `user_id` = `users.users_id`. 발급 경로 [com.otoki.powersales.platform.auth.web.service.WebAuthenticationService]. */
    WEB,
}
