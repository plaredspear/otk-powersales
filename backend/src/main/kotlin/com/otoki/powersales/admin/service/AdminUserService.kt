package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminUserDetailResponse
import com.otoki.powersales.admin.dto.AdminUserListItem
import com.otoki.powersales.admin.dto.AdminUserListResponse
import com.otoki.powersales.admin.dto.AdminUserPasswordResetResponse
import com.otoki.powersales.admin.exception.AdminUserNotFoundException
import com.otoki.powersales.admin.exception.CannotDeactivateSelfException
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * web admin User 관리 화면 서비스.
 *
 * - 목록/상세 조회 (USER_READ)
 * - 비밀번호 임시 리셋 → "1234" BCrypt 해시 + passwordChangeRequired = true
 * - 활성/비활성 토글 — 자기 자신 비활성화 시도는 차단
 *
 * 권한 게이트는 컨트롤러 단의 `@RequiresPermission` 가 처리하며, 본 서비스는 진입 시점에
 * 권한 보유를 가정한다.
 */
@Service
@Transactional(readOnly = true)
class AdminUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(AdminUserService::class.java)

    fun findUsers(keyword: String?, isActive: Boolean?, page: Int, size: Int): AdminUserListResponse {
        val pageable = PageRequest.of(page, size)
        val userPage = userRepository.findUsers(keyword, isActive, pageable)

        return AdminUserListResponse(
            content = userPage.content.map { AdminUserListItem.from(it) },
            page = page,
            size = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    fun findUserDetail(userId: Long): AdminUserDetailResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AdminUserNotFoundException(userId) }
        return AdminUserDetailResponse.from(user)
    }

    @Transactional
    fun resetPassword(userId: Long): AdminUserPasswordResetResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AdminUserNotFoundException(userId) }

        val encoded = passwordEncoder.encode(TEMPORARY_PASSWORD)!!
        user.password = encoded
        user.passwordChangeRequired = true

        logger.info(
            "ADMIN_USER_PASSWORD_RESET target={} username={}",
            user.id,
            user.username
        )

        return AdminUserPasswordResetResponse.from(user)
    }

    @Transactional
    fun updateActiveStatus(targetUserId: Long, requesterUserId: Long, isActive: Boolean) {
        if (!isActive && targetUserId == requesterUserId) {
            throw CannotDeactivateSelfException()
        }

        val user = userRepository.findById(targetUserId)
            .orElseThrow { AdminUserNotFoundException(targetUserId) }

        if (user.isActive == isActive) return

        user.isActive = isActive

        logger.info(
            "ADMIN_USER_ACTIVE_STATUS_CHANGED target={} username={} newStatus={}",
            user.id,
            user.username,
            isActive
        )
    }

    companion object {
        const val TEMPORARY_PASSWORD = "1234"
    }
}
