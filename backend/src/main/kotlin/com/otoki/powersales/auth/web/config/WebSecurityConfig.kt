package com.otoki.powersales.auth.web.config

import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.admin.scope.AdminPermissionHolder
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.WebAdminContextFilter
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.auth.web.WebJwtAuthenticationFilter
import com.otoki.powersales.auth.web.WebJwtService
import com.otoki.powersales.auth.web.WebRefreshTokenStore
import com.otoki.powersales.common.security.JwtAuthenticationEntryPoint
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.ObjectMapper

/**
 * Web (관리자 대시보드) SecurityFilterChain (Spec #760).
 *
 * `/api/v1/admin/` 하위 경로 한정. User entity 기반 인증 (Mobile 의 Employee 기반과 분리).
 * - 인증 entry: [WebJwtAuthenticationFilter] — audience="web" JWT 만 수용
 * - 인증 실패: 기존 [JwtAuthenticationEntryPoint] 재사용 (401 JSON 응답)
 * - STATELESS 세션 (JWT)
 *
 * Order=1 — Mobile [com.otoki.powersales.common.config.SecurityConfig] (Order=2) 보다 먼저 매칭.
 * 본 chain 의 securityMatcher 가 admin 경로로 한정되어 있어 Mobile chain 과 경로 충돌 없음.
 *
 * Web 인증 인프라(`WebJwtService` / `WebJwtAuthenticationFilter` / `WebRefreshTokenStore`) 는 본 config
 * 의 @Bean 으로만 등록 — Spring Boot 의 servlet filter 자동 스캔(@Component) 회피 → 컨트롤러 테스트
 * (`@WebMvcTest`) 에서 의존 누락 에러 미발생.
 */
@Configuration
class WebSecurityConfig(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration}") private val jwtAccessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val jwtRefreshExpiration: Long,
) {

    @Bean
    fun webJwtService(): WebJwtService =
        WebJwtService(jwtSecret, jwtAccessExpiration, jwtRefreshExpiration)

    @Bean
    fun webRefreshTokenStore(
        redisTemplate: RedisTemplate<String, String>,
        objectMapper: ObjectMapper,
    ): WebRefreshTokenStore = WebRefreshTokenStore(redisTemplate, objectMapper)

    @Bean
    fun webJwtAuthenticationFilter(webJwtService: WebJwtService): WebJwtAuthenticationFilter =
        WebJwtAuthenticationFilter(webJwtService)

    @Bean
    fun webAdminContextFilter(
        employeeRepository: EmployeeRepository,
        adminDataScopeService: AdminDataScopeService,
        adminPermissionResolver: AdminPermissionResolver,
        adminEmployeeHolder: AdminEmployeeHolder,
        dataScopeHolder: DataScopeHolder,
        adminPermissionHolder: AdminPermissionHolder,
        requestMappingHandlerMapping: RequestMappingHandlerMapping,
        objectMapper: ObjectMapper,
    ): WebAdminContextFilter = WebAdminContextFilter(
        employeeRepository = employeeRepository,
        adminDataScopeService = adminDataScopeService,
        adminPermissionResolver = adminPermissionResolver,
        adminEmployeeHolder = adminEmployeeHolder,
        dataScopeHolder = dataScopeHolder,
        adminPermissionHolder = adminPermissionHolder,
        requestMappingHandlerMapping = requestMappingHandlerMapping,
        objectMapper = objectMapper,
    )

    @Bean
    @Order(1)
    fun webSecurityFilterChain(
        http: HttpSecurity,
        webJwtAuthenticationFilter: WebJwtAuthenticationFilter,
        webAdminContextFilter: WebAdminContextFilter,
        jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    ): SecurityFilterChain {
        http
            .securityMatcher("/api/v1/admin/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/refresh"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .addFilterBefore(webJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(webAdminContextFilter, WebJwtAuthenticationFilter::class.java)

        return http.build()
    }
}
