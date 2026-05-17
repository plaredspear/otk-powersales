package com.otoki.powersales.auth.web

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.user.entity.ProfileType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Web 인증 사용자 principal (Spec #760).
 *
 * Spring Security `UserDetails` 구현. JWT subject = `User.username`.
 * Mobile 의 `UserPrincipal` (`Employee.id` 기반) 과 분리.
 *
 * - `userId`: `User.id` (DB PK)
 * - `usernameValue`: `User.username` (인증 ID, SF Username = Email)
 * - `encodedPassword`: BCrypt 해시
 * - `profileType` / `isSalesSupport`: 권한 산출 입력 (§2.3 매핑 정책)
 * - `passwordChangeRequired`: 임시 비밀번호 상태 — frontend 강제 변경 화면 분기 입력
 * - `costCenterCode`: 로그인 시점 Employee.costCenterCode snapshot — admin service 가 Employee 엔티티
 *                    재조회 없이 데이터 스코프(지점/조직) 분기를 수행하기 위한 캐시. SAP 인사 발령으로
 *                    Employee 측이 갱신되어도 본 값은 토큰 lifetime 동안 stale — 재로그인 시 자연 보정.
 * - `permissions`: 로그인 시점 effective permission snapshot — role_permission + user_permission 합집합.
 *                  WebAdminContextFilter 가 매 요청 DB 조회 없이 @RequiresPermission 가드를 수행한다.
 *                  권한 변경 후 즉시 반영이 필요하면 사용자 재로그인 안내 (token TTL = max stale window).
 */
data class WebUserPrincipal(
    val userId: Long,
    val usernameValue: String,
    val employeeCode: String,
    val employeeId: Long?,
    val role: UserRole?,
    val costCenterCode: String?,
    val profileType: ProfileType,
    val isSalesSupport: Boolean,
    val passwordChangeRequired: Boolean,
    val permissions: Set<AdminPermission>,
    private val encodedPassword: String,
    private val grantedAuthorities: Collection<GrantedAuthority>,
    private val active: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = grantedAuthorities

    override fun getPassword(): String = encodedPassword

    override fun getUsername(): String = usernameValue

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = active

    /**
     * Employee.id 강제 추출. ADMIN- 부트스트랩 등 Employee 미존재 사용자는 예외.
     *
     * admin 패키지 controller 들이 service 에 employeeId 를 넘기는 표준 경로.
     */
    fun requireEmployeeId(): Long = employeeId
        ?: throw IllegalStateException("Web Admin 사용자에 매칭되는 Employee 가 없습니다 (employeeCode=$employeeCode)")
}
