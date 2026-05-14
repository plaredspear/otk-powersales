package com.otoki.powersales.auth.web

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
 */
data class WebUserPrincipal(
    val userId: Long,
    val usernameValue: String,
    val employeeNumber: String,
    val profileType: ProfileType,
    val isSalesSupport: Boolean,
    val passwordChangeRequired: Boolean,
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
}
