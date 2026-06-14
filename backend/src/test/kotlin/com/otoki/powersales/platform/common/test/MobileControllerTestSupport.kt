package com.otoki.powersales.platform.common.test

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.platform.common.security.GpsConsentFilter
import com.otoki.powersales.platform.common.security.JwtAuthenticationFilter
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc

/**
 * mobile 도메인 controller WebMvcTest 의 공통 보일러플레이트를 흡수하는 base.
 *
 * 흡수 대상:
 * - `MockMvc` autowire
 * - 필터 5종 `@MockkBean` (`JwtAuthenticationFilter` / `GpsConsentFilter` /
 *   `JwtTokenProvider` / `SapInboundAuditService`)
 * - `@BeforeEach` 에서 `UserPrincipal` (userId=1L, role=WOMAN) SecurityContext stub
 *
 * 권한 변형이 필요한 테스트는 `authenticateAs(userId, role)` 를 호출해 override.
 * 도메인 service mock 은 상속 파일에서 `@MockkBean` 으로 개별 선언.
 */
abstract class MobileControllerTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @MockkBean
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    protected lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    protected lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    protected lateinit var gpsConsentFilter: GpsConsentFilter

    // FlsResponseBodyAdvice (@RestControllerAdvice) 가 global 로 로드되므로 의존성 3종 mock 필요.
    // @FlsFiltered 미부착 endpoint 는 advice 의 supports() 가 false 반환 → 동작 무영향.
    @MockkBean
    protected lateinit var flsService: FlsService

    @MockkBean
    protected lateinit var permissionSetEvaluator: PermissionSetEvaluator

    @MockkBean
    protected lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUpMobileSecurityContext() {
        authenticateAs(userId = 1L, role = AppAuthority.WOMAN)
    }

    protected fun authenticateAs(userId: Long, role: String?) {
        val principal = UserPrincipal(userId = userId, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }
}
