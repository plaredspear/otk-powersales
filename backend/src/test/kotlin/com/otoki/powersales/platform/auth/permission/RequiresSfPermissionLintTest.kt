package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.platform.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.RequiresSfPermissionLint
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("RequiresSfPermissionLint")
class RequiresSfPermissionLintTest {

    @Test
    @DisplayName("등록된 entity 만 사용 — 부팅 통과 (spec #808)")
    fun knownEntityPasses() {
        val lint = lintOf(
            registryKnownEntities = setOf("employee", "dashboard"),
            handlers = mapOf(
                handlerMethodWith(annotationOfEntity("employee")) to "GET /employees",
                handlerMethodWith(annotationOfEntity("dashboard")) to "GET /dashboard",
            ),
        )

        assertThatNoException().isThrownBy { lint.afterSingletonsInstantiated() }
    }

    @Test
    @DisplayName("미등록 entity (오타) — 부팅 실패 + 메시지에 식별자 포함")
    fun unknownEntityFails() {
        val lint = lintOf(
            registryKnownEntities = setOf("employee"),
            handlers = mapOf(
                handlerMethodWith(annotationOfEntity("emplyee")) to "GET /typo",
            ),
        )

        assertThatThrownBy { lint.afterSingletonsInstantiated() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("emplyee")
    }

    @Test
    @DisplayName("SYSTEM operation — entity 식별자 검사 대상 아님 (entity 미사용)")
    fun systemOperationSkipped() {
        val lint = lintOf(
            registryKnownEntities = setOf("employee"),
            handlers = mapOf(
                handlerMethodWith(annotationOfSystem()) to "GET /system",
            ),
        )

        assertThatNoException().isThrownBy { lint.afterSingletonsInstantiated() }
    }

    private fun lintOf(
        registryKnownEntities: Set<String>,
        handlers: Map<HandlerMethod, String>,
    ): RequiresSfPermissionLint {
        val mapping = mockk<RequestMappingHandlerMapping>()
        val mappingMap = handlers.entries
            .associate { (handler, _) -> mockk<RequestMappingInfo>() to handler }
        every { mapping.handlerMethods } returns mappingMap

        val registry = mockk<EntitySfNameRegistry>()
        every { registry.contains(any()) } answers { firstArg<String>() in registryKnownEntities }

        return RequiresSfPermissionLint(mapping, registry)
    }

    private fun annotationOfEntity(entity: String): RequiresSfPermission {
        val mock = mockk<RequiresSfPermission>()
        every { mock.entity } returns entity
        every { mock.operation } returns SfPermissionOperation.READ
        every { mock.systemPermission } returns SfSystemPermission.VIEW_ALL_DATA
        return mock
    }

    private fun annotationOfSystem(): RequiresSfPermission {
        val mock = mockk<RequiresSfPermission>()
        every { mock.entity } returns ""
        every { mock.operation } returns SfPermissionOperation.SYSTEM
        every { mock.systemPermission } returns SfSystemPermission.VIEW_ALL_DATA
        return mock
    }

    private fun handlerMethodWith(annotation: RequiresSfPermission): HandlerMethod {
        val hm = mockk<HandlerMethod>()
        every { hm.getMethodAnnotation(RequiresSfPermission::class.java) } returns annotation
        every { hm.beanType } returns DummyController::class.java
        every { hm.method } returns DummyController::class.java.declaredMethods.first()
        return hm
    }

    class DummyController {
        fun execute() {}
    }
}
