package com.otoki.powersales.auth.sharing.service

// import removed (spec #801)
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.sharing.annotation.FlsField
import com.otoki.powersales.auth.sharing.annotation.FlsFiltered
import com.otoki.powersales.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.util.Optional

/**
 * FlsResponseBodyAdvice 단위 테스트 (spec #797).
 *
 * Q1~Q6 옵션 1 정합 — SecurityContextHolder principal 추출 + Jackson Map 변환 + readable=false omit.
 */
@DisplayName("FlsResponseBodyAdvice — spec #797")
class FlsResponseBodyAdviceTest {

    private val flsService = mockk<FlsService>()
    private val permissionSetEvaluator = mockk<PermissionSetEvaluator>()
    private val userRepository = mockk<UserRepository>()
    private val objectMapper: ObjectMapper = JsonMapper.builder().build()

    private val advice = FlsResponseBodyAdvice(
        flsService = flsService,
        permissionSetEvaluator = permissionSetEvaluator,
        userRepository = userRepository,
        objectMapper = objectMapper,
    )

    @FlsFiltered(sObject = "Account")
    class FilteredController {
        @FlsFiltered(sObject = "Account")
        fun getAccount(): AccountDto = AccountDto()
    }

    class PlainController {
        fun getPlain(): AccountDto = AccountDto()
    }

    data class AccountDto(
        val id: Long = 1L,
        val name: String = "Acme",
        @FlsField(sObjectField = "Account.AnnualRevenue")
        val annualRevenue: Long = 1_000_000L,
        @FlsField(sObjectField = "Account.OwnerSecret")
        val ownerSecret: String = "secret",
    )

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("Q3 — @FlsFiltered 미부착 endpoint 는 supports() false")
    fun supports_notFlsFiltered_returnsFalse() {
        val method = PlainController::class.java.getMethod("getPlain")
        val handler = HandlerMethod(PlainController(), method)
        val param = handler.returnType
        assertThat(advice.supports(param, org.springframework.http.converter.json.MappingJackson2HttpMessageConverter::class.java))
            .isFalse
    }

    @Test
    @DisplayName("Q3 — @FlsFiltered 부착 endpoint 는 supports() true")
    fun supports_flsFiltered_returnsTrue() {
        val method = FilteredController::class.java.getMethod("getAccount")
        val handler = HandlerMethod(FilteredController(), method)
        val param = handler.returnType
        assertThat(advice.supports(param, org.springframework.http.converter.json.MappingJackson2HttpMessageConverter::class.java))
            .isTrue
    }

    @Test
    @DisplayName("Q5 — 미인증 SecurityContext 시 body 그대로 통과")
    fun beforeBodyWrite_noAuth_returnsBodyAsIs() {
        // SecurityContext 비어있음
        val body = AccountDto()
        val method = FilteredController::class.java.getMethod("getAccount")
        val handler = HandlerMethod(FilteredController(), method)

        val result = advice.beforeBodyWrite(
            body, handler.returnType, MediaType.APPLICATION_JSON,
            org.springframework.http.converter.json.MappingJackson2HttpMessageConverter::class.java,
            mockk<org.springframework.http.server.ServerHttpRequest>(relaxed = true),
            mockk<org.springframework.http.server.ServerHttpResponse>(relaxed = true),
        )

        assertThat(result).isSameAs(body)
    }

    @Test
    @DisplayName("Q2 — readable=false field 는 응답 Map 에서 omit, readable=true field 는 포함")
    fun beforeBodyWrite_masksReadableFalse() {
        val principal = buildPrincipal(userId = 100L)
        setSecurityContext(principal)
        every { userRepository.findById(100L) } returns Optional.of(
            User(id = 100L, username = "u", employeeCode = "EMP1", password = "x").apply { profileId = 1L },
        )
        every { permissionSetEvaluator.getPermissionSetSnapshot(100L) } returns PermissionSetSnapshot(
            viewAllDataSystem = false,
            modifyAllDataSystem = false,
            viewAllRecordsBySObject = emptyMap(),
            modifyAllRecordsBySObject = emptyMap(),
            permissionSetIds = setOf(10L),
        )
        // AnnualRevenue 만 readable=true, OwnerSecret 은 readable=false
        every { flsService.readableFields(100L, "Account", 1L, setOf(10L)) } returns setOf("AnnualRevenue")

        val body = AccountDto()
        val method = FilteredController::class.java.getMethod("getAccount")
        val handler = HandlerMethod(FilteredController(), method)

        val result = advice.beforeBodyWrite(
            body, handler.returnType, MediaType.APPLICATION_JSON,
            org.springframework.http.converter.json.MappingJackson2HttpMessageConverter::class.java,
            mockk(relaxed = true), mockk(relaxed = true),
        )

        @Suppress("UNCHECKED_CAST")
        val resultMap = result as Map<String, Any?>
        // id, name, annualRevenue 는 포함 (id/name 은 @FlsField 미부착, annualRevenue 는 readable=true)
        assertThat(resultMap).containsKeys("id", "name", "annualRevenue")
        // ownerSecret 은 readable=false → omit
        assertThat(resultMap).doesNotContainKey("ownerSecret")
    }

    @Test
    @DisplayName("Q3 — @FlsField 미부착 field 는 readable 평가 자체 skip (항상 포함)")
    fun beforeBodyWrite_nonAnnotatedFieldAlwaysIncluded() {
        val principal = buildPrincipal(userId = 200L)
        setSecurityContext(principal)
        every { userRepository.findById(200L) } returns Optional.of(
            User(id = 200L, username = "u", employeeCode = "EMP2", password = "x").apply { profileId = null },
        )
        every { permissionSetEvaluator.getPermissionSetSnapshot(200L) } returns PermissionSetSnapshot.NONE
        // 어떤 field 도 readable 아님
        every { flsService.readableFields(200L, "Account", null, emptySet()) } returns emptySet()

        val body = AccountDto()
        val method = FilteredController::class.java.getMethod("getAccount")
        val handler = HandlerMethod(FilteredController(), method)

        val result = advice.beforeBodyWrite(
            body, handler.returnType, MediaType.APPLICATION_JSON,
            org.springframework.http.converter.json.MappingJackson2HttpMessageConverter::class.java,
            mockk(relaxed = true), mockk(relaxed = true),
        )

        @Suppress("UNCHECKED_CAST")
        val resultMap = result as Map<String, Any?>
        // @FlsField 미부착 id / name 은 readable 0건 상황에서도 통과
        assertThat(resultMap).containsKeys("id", "name")
        // @FlsField 부착 field 는 모두 omit (readable 0건)
        assertThat(resultMap).doesNotContainKeys("annualRevenue", "ownerSecret")
    }

    private fun buildPrincipal(userId: Long) = WebUserPrincipal(
        userId = userId,
        usernameValue = "user$userId",
        employeeCode = null,
        employeeId = null,
        role = null,
        costCenterCode = null,
        profileName = "9. Staff",
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet<String>(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true,
    )

    private fun setSecurityContext(principal: WebUserPrincipal) {
        val auth = UsernamePasswordAuthenticationToken(principal, "x", principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }
}
