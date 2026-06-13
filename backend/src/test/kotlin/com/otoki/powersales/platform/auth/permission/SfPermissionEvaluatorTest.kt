package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionEvaluator
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SfPermissionEvaluator")
class SfPermissionEvaluatorTest {

    private val evaluator = SfPermissionEvaluator()

    @Test
    @DisplayName("entity READ — permission set 에 키 존재 시 통과")
    fun entityReadAllowed() {
        val annotation = annotation(entity = "employee", operation = SfPermissionOperation.READ)
        val perms = setOf("employee:R")
        assertThat(evaluator.isAllowed(annotation, perms)).isTrue()
    }

    @Test
    @DisplayName("entity EDIT — permission set 에 키 부재 시 차단")
    fun entityEditBlocked() {
        val annotation = annotation(entity = "employee", operation = SfPermissionOperation.EDIT)
        val perms = setOf("employee:R")
        assertThat(evaluator.isAllowed(annotation, perms)).isFalse()
    }

    @Test
    @DisplayName("SYSTEM operation — systemPermission 매칭")
    fun systemPermissionAllowed() {
        val annotation = annotation(
            operation = SfPermissionOperation.SYSTEM,
            systemPermission = SfSystemPermission.MODIFY_ALL_DATA,
        )
        val perms = setOf("SYSTEM:MODIFY_ALL_DATA")
        assertThat(evaluator.isAllowed(annotation, perms)).isTrue()
    }

    @Test
    @DisplayName("SYSTEM operation — systemPermission 미매칭 시 차단")
    fun systemPermissionBlocked() {
        val annotation = annotation(
            operation = SfPermissionOperation.SYSTEM,
            systemPermission = SfSystemPermission.MODIFY_ALL_DATA,
        )
        val perms = setOf("SYSTEM:VIEW_ALL_DATA")
        assertThat(evaluator.isAllowed(annotation, perms)).isFalse()
    }

    @Test
    @DisplayName("entity 미지정 + CRUD operation — 차단")
    fun missingEntityBlocked() {
        val annotation = annotation(entity = "", operation = SfPermissionOperation.READ)
        val perms = setOf("employee:R")
        assertThat(evaluator.isAllowed(annotation, perms)).isFalse()
    }

    private fun annotation(
        entity: String = "",
        operation: SfPermissionOperation,
        systemPermission: SfSystemPermission = SfSystemPermission.VIEW_ALL_DATA,
    ): RequiresSfPermission {
        val mock = mockk<RequiresSfPermission>()
        every { mock.entity } returns entity
        every { mock.operation } returns operation
        every { mock.systemPermission } returns systemPermission
        return mock
    }
}
