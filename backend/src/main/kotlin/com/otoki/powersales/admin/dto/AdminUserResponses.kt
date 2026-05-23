package com.otoki.powersales.admin.dto

import com.otoki.powersales.user.entity.ProfileType
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
    val profileType: String,
    val profileTypeLabel: String,
    /** Spec #805 — Profile.name SoT. spec #806 destructive 시 profileType/profileTypeLabel 제거 + 본 필드 유지. */
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
            profileType = user.profileType.name,
            profileTypeLabel = user.profileType.toKoreanLabel(),
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
    val sfid: String?,
    val profileType: String,
    val profileTypeLabel: String,
    /** Spec #805 — Profile.name SoT. spec #806 destructive 시 profileType/profileTypeLabel 제거 + 본 필드 유지. */
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
            sfid = user.sfid,
            profileType = user.profileType.name,
            profileTypeLabel = user.profileType.toKoreanLabel(),
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
    val temporaryPasswordIssued: Boolean,
    val passwordChangeRequired: Boolean,
    val resetAt: LocalDateTime
) {
    companion object {
        fun from(user: User, resetAt: LocalDateTime = LocalDateTime.now()): AdminUserPasswordResetResponse =
            AdminUserPasswordResetResponse(
                userId = user.id,
                username = user.username,
                temporaryPasswordIssued = true,
                passwordChangeRequired = true,
                resetAt = resetAt
            )
    }
}

/**
 * ProfileType 한글 라벨. 화면 표시용. SF Profile.Name 의 한글(첫 항목 또는 정합 라벨) 기준.
 */
private fun ProfileType.toKoreanLabel(): String = when (this) {
    ProfileType.MARKETING -> "마케팅"
    ProfileType.STAFF -> "Staff"
    ProfileType.TEAM_LEADER -> "조장"
    ProfileType.BRANCH_MANAGER -> "지점장"
    ProfileType.SALES_MANAGER -> "영업부장"
    ProfileType.BUSINESS_DIRECTOR -> "사업부장"
    ProfileType.DIVISION_HEAD -> "본부장"
    ProfileType.SALES_REP -> "영업사원"
    ProfileType.SALES_REP_LEADER -> "영업사원+조장"
    ProfileType.FACTORY_STAFF -> "공장관계자"
    ProfileType.OLS -> "OLS"
    ProfileType.SYSTEM_ADMIN -> "시스템관리자"
}
