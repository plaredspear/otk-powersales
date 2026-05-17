package com.otoki.powersales.admin.security

/**
 * [WebAdminContextFilter] 산출값을 [jakarta.servlet.http.HttpServletRequest] attribute 로
 * 전달하기 위한 키 모음.
 *
 * holder (`@RequestScope` 빈) 패턴을 대체. 동일 요청 안에서 Filter → ArgumentResolver →
 * (필요 시) interceptor 모두가 같은 키로 산출값에 접근.
 *
 * 키 prefix `powersales.admin.` 는 다른 attribute 와의 충돌 방지.
 */
object AdminContextAttributes {
    const val DATA_SCOPE = "powersales.admin.currentDataScope"
    const val PERMISSIONS = "powersales.admin.currentPermissions"
}
