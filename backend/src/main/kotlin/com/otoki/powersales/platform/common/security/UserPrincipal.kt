package com.otoki.powersales.platform.common.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * JWT에서 추출한 인증 사용자 정보 (mobile).
 * `@AuthenticationPrincipal UserPrincipal` 로 Controller 에서 접근.
 *
 * `role`: SF DKRetail__AppAuthority__c picklist value (`여사원` / `조장` / `지점장` / `AccountViewAll`) 또는 null.
 * Spring Security ROLE_ authorities 는 SF picklist value 를 그대로 prefix 부여 — `@PreAuthorize` / `hasRole`
 * 사용처가 없어 실제 분기 입력 아님. 분기는 controller / service 단에서 `principal.role == AppAuthority.X` 직접.
 */
data class UserPrincipal(
    val userId: Long,
    val role: String?,
    val agreementFlag: Boolean = false,
    val passwordChangeRequired: Boolean = false
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return if (role != null) listOf(SimpleGrantedAuthority("ROLE_$role")) else emptyList()
    }

    override fun getPassword(): String? = null

    override fun getUsername(): String = userId.toString()

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
