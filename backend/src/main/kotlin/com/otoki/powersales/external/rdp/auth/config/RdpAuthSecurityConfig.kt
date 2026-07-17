package com.otoki.powersales.external.rdp.auth.config

import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.auth.filter.RdpAuthErrorWriter
import com.otoki.powersales.external.rdp.auth.filter.RdpBearerTokenFilter
import com.otoki.powersales.external.rdp.auth.service.RdpJwtCodec
import com.otoki.powersales.external.rdp.inbound.config.RdpInboundProperties
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

// /api/v1/rdp/** 전용 SecurityFilterChain. SAP / SF 전용 chain 과 격리된 별도 chain.
// @Order(0) 으로 글로벌 chain 보다 먼저 매칭된다. SAP / SF chain 도 @Order(0) 이지만
// securityMatcher 가 서로 다르므로 충돌 없음.
// SF 와 동일 구조 (IP allowlist filter 없음, RFC 6749 표준 OAuth 에러 형식 사용).
@Configuration
@EnableConfigurationProperties(RdpAuthProperties::class, RdpInboundProperties::class)
class RdpAuthSecurityConfig(
    private val properties: RdpAuthProperties,
    private val rdpJwtCodec: RdpJwtCodec,
    private val auditService: RdpInboundAuditService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    @Order(0)
    fun rdpApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val bearerFilter = RdpBearerTokenFilter(rdpJwtCodec, auditService, objectMapper)

        http
            .securityMatcher("/api/v1/rdp/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/rdp/oauth/token").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(rdpAuthenticationEntryPoint())
                ex.accessDeniedHandler(rdpAccessDeniedHandler())
            }
            .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * 본 빈은 [RdpBearerTokenFilter] 가 servlet container 의 글로벌 filter chain 으로도 등록되는 것을
     * 차단한다. Spring Boot 가 [OncePerRequestFilter] 를 자동 등록하므로 명시적으로 disable.
     */
    @Bean
    fun rdpBearerTokenFilterRegistration(): FilterRegistrationBean<RdpBearerTokenFilter> {
        val registration = FilterRegistrationBean(
            RdpBearerTokenFilter(rdpJwtCodec, auditService, objectMapper)
        )
        registration.isEnabled = false
        return registration
    }

    private fun rdpAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
            RdpAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
        }
    }

    private fun rdpAccessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            RdpAuthErrorWriter.write(response, objectMapper, 403, "insufficient_scope", "권한 없음")
        }
    }
}
