package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.AdminUserDetails
import com.otoki.internal.common.repository.UserRepository
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AdminUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(employeeId: String): AdminUserDetails {
        val user = userRepository.findByEmployeeId(employeeId)
            .orElseThrow { UsernameNotFoundException("사용자를 찾을 수 없습니다") }

        if (user.status != "재직") {
            throw DisabledException("비활성 계정입니다")
        }

        if (user.appLoginActive != true) {
            throw DisabledException("로그인이 비활성화된 계정입니다")
        }

        val enabled = user.status == "재직" && user.appLoginActive == true

        return AdminUserDetails(
            userId = user.id,
            employeeId = user.employeeId,
            displayName = user.name,
            encodedPassword = user.password,
            role = user.role,
            enabled = enabled
        )
    }
}
