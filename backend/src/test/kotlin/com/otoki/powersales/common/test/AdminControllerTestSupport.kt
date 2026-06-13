package com.otoki.powersales.common.test

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.sharing.service.FlsService
import com.otoki.powersales.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc

/**
 * admin 도메인 controller WebMvcTest 의 공통 보일러플레이트를 흡수하는 base.
 *
 * 흡수 대상:
 * - `MockMvc` autowire
 * - 필터 5종 `@MockkBean` (`JwtAuthenticationFilter` / `GpsConsentFilter` /
 *   `JwtTokenProvider` / `SapInboundAuditService`)
 * - `@BeforeEach` 에서 `WebUserPrincipal` (userId=100L, role=BRANCH_MANAGER,
 *   employeeCode=S001, employeeId=1L, profileName=9. Staff, permissions/granted=empty)
 *   SecurityContext stub
 *
 * 권한 변형이 필요한 테스트는 `authenticateAsAdmin(role, ...)` 를 호출해 override.
 * 도메인 service mock 은 상속 파일에서 `@MockkBean` 으로 개별 선언.
 */
abstract class AdminControllerTestSupport {

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
    fun setUpAdminSecurityContext() {
        authenticateAsAdmin(role = AppAuthority.BRANCH_MANAGER)
    }

    protected fun authenticateAsAdmin(
        userId: Long = 100L,
        role: String?,
        employeeCode: String = "S001",
        employeeId: Long = 1L,
        costCenterCode: String? = null,
        profileName: String? = "9. Staff",
        isSalesSupport: Boolean = false,
        permissions: Set<String> = emptySet(),
    ) {
        val principal = WebUserPrincipal(
            userId = userId,
            usernameValue = "test@otokims.co.kr",
            employeeCode = employeeCode,
            employeeId = employeeId,
            role = role,
            costCenterCode = costCenterCode,
            profileName = profileName,
            isSalesSupport = isSalesSupport,
            passwordChangeRequired = false,
            permissions = permissions,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }
}
