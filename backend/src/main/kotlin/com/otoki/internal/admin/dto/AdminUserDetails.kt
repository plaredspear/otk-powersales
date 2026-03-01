package com.otoki.internal.admin.dto

import com.otoki.internal.common.entity.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AdminUserDetails(
    val userId: Long,
    val employeeId: String,
    val displayName: String,
    private val encodedPassword: String,
    val role: UserRole,
    private val enabled: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String = encodedPassword

    override fun getUsername(): String = employeeId

    override fun isEnabled(): Boolean = enabled

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true
}
