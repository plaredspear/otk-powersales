package com.otoki.powersales.admin.dto.request

import com.otoki.powersales.auth.entity.UserRoleEnum
import jakarta.validation.constraints.NotNull

data class UpdateUserPermissionsRequest(
    @field:NotNull(message = "권한 목록은 필수입니다")
    val permissions: List<String>
)

data class UpdateAuthorityRequest(
    @field:NotNull(message = "역할은 필수입니다")
    val role: UserRoleEnum
)

data class UpdateRolePermissionsRequest(
    @field:NotNull(message = "권한 목록은 필수입니다")
    val permissions: List<String>
)
