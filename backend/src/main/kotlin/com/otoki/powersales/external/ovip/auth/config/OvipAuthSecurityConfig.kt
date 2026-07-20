package com.otoki.powersales.external.ovip.auth.config

import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.auth.filter.OvipAuthErrorWriter
import com.otoki.powersales.external.ovip.auth.filter.OvipBearerTokenFilter
import com.otoki.powersales.external.ovip.auth.service.OvipJwtCodec
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

// /api/v1/ovip/** 전용 SecurityFilterChain. SAP / SF 전용 chain 과 격리된 별도 chain.
// @Order(0) 으로 글로벌 chain 보다 먼저 매칭된다. SAP / SF chain 도 @Order(0) 이지만
// securityMatcher 가 서로 다르므로 충돌 없음.
// SF 와 동일 구조 (IP allowlist filter 없음, RFC 6749 표준 OAuth 에러 형식 사용).
@Configuration
@EnableConfigurationProperties(OvipAuthProperties::class, OvipInboundProperties::class)
class OvipAuthSecurityConfig(
    private val properties: OvipAuthProperties,
    private val ovipJwtCodec: OvipJwtCodec,
    private val auditService: OvipInboundAuditService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    @Order(0)
    fun ovipApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val bearerFilter = OvipBearerTokenFilter(ovipJwtCodec, auditService, objectMapper)

        http
            .securityMatcher("/api/v1/ovip/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/ovip/oauth/token").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(ovipAuthenticationEntryPoint())
                ex.accessDeniedHandler(ovipAccessDeniedHandler())
            }
            .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * 본 빈은 [OvipBearerTokenFilter] 가 servlet container 의 글로벌 filter chain 으로도 등록되는 것을
     * 차단한다. Spring Boot 가 [OncePerRequestFilter] 를 자동 등록하므로 명시적으로 disable.
     */
    @Bean
    fun ovipBearerTokenFilterRegistration(): FilterRegistrationBean<OvipBearerTokenFilter> {
        val registration = FilterRegistrationBean(
            OvipBearerTokenFilter(ovipJwtCodec, auditService, objectMapper)
        )
        registration.isEnabled = false
        return registration
    }

    private fun ovipAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
            OvipAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
        }
    }

    private fun ovipAccessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            OvipAuthErrorWriter.write(response, objectMapper, 403, "insufficient_scope", "권한 없음")
        }
    }
}
