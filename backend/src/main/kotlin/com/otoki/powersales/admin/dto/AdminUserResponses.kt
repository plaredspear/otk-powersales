package com.otoki.powersales.admin.dto

import com.otoki.powersales.user.entity.User
import java.time.LocalDateTime

/**
 * web admin User 관리 화면용 목록 아이템 DTO.
 */
data class AdminUserListItem(
    val id: Long,
    val username: String,
    val employeeCode: String?,
    val name: String?,
    val email: String?,
    val profileName: String? = null,
    val branch: String?,
    val department: String?,
    val isActive: Boolean,
    val lastLoginAt: LocalDateTime?
) {
    companion object {
        fun from(user: User, profileName: String? = null): AdminUserListItem = AdminUserListItem(
            id = user.id,
            username = user.username,
            employeeCode = user.employeeCode,
            name = user.name,
            email = user.email,
            profileName = profileName,
            branch = user.branch,
            department = user.department,
            isActive = user.isActive,
            lastLoginAt = user.lastLoginAt
        )
    }
}

/**
 * web admin User 관리 화면용 목록 응답 DTO (Spring Page 메타 포함).
 */
data class AdminUserListResponse(
    val content: List<AdminUserListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

/**
 * web admin User 상세 응답 DTO. 목록 필드 + SF 메타 + audit 컬럼.
 */
data class AdminUserDetailResponse(
    val id: Long,
    val username: String,
    val employeeCode: String?,
    val name: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val alias: String?,
    val title: String?,
    val department: String?,
    val division: String?,
    val branch: String?,
    val mobilePhone: String?,
    val phone: String?,
    val hrCode: String?,
    val profileName: String? = null,
    val isSalesSupport: Boolean,
    val isActive: Boolean,
    val passwordChangeRequired: Boolean,
    val lastLoginAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val lastModifiedAt: LocalDateTime?
) {
    companion object {
        fun from(user: User, profileName: String? = null): AdminUserDetailResponse = AdminUserDetailResponse(
            id = user.id,
            username = user.username,
            employeeCode = user.employeeCode,
            name = user.name,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            alias = user.alias,
            title = user.title,
            department = user.department,
            division = user.division,
            branch = user.branch,
            mobilePhone = user.mobilePhone,
            phone = user.phone,
            hrCode = user.hrCode,
            // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답에 노출 금지 (정책).
            profileName = profileName,
            isSalesSupport = user.isSalesSupport ?: false,
            isActive = user.isActive,
            passwordChangeRequired = user.passwordChangeRequired ?: true,
            lastLoginAt = user.lastLoginAt,
            createdAt = user.createdAt,
            lastModifiedAt = user.updatedAt
        )
    }
}

/**
 * 임시 비밀번호 리셋 응답 DTO. 임시 비밀번호 평문은 응답에 포함하지 않는다.
 */
data class AdminUserPasswordResetResponse(
    val userId: Long,
    val username: String,
    /** 화면이 임시 비밀번호(`{사번}@pwrs`) 를 재조립해 안내하기 위한 사번. 미매칭 계정은 null. */
    val employeeCode: String?,
    val temporaryPasswordIssued: Boolean,
    val passwordChangeRequired: Boolean,
    val resetAt: LocalDateTime
) {
    companion object {
        fun from(user: User, resetAt: LocalDateTime = LocalDateTime.now()): AdminUserPasswordResetResponse =
            AdminUserPasswordResetResponse(
                userId = user.id,
                username = user.username,
                employeeCode = user.employeeCode,
                temporaryPasswordIssued = true,
                passwordChangeRequired = true,
                resetAt = resetAt
            )
    }
}
