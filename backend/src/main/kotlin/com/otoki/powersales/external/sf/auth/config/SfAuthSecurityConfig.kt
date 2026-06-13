package com.otoki.powersales.external.sf.auth.config

import com.otoki.powersales.external.sf.auth.audit.SfInboundAuditService
import com.otoki.powersales.external.sf.auth.filter.SfAuthErrorWriter
import com.otoki.powersales.external.sf.auth.filter.SfBearerTokenFilter
import com.otoki.powersales.external.sf.auth.service.SfJwtCodec
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

// /api/v1/sf/** 전용 SecurityFilterChain. SAP 측 SapAuthSecurityConfig 와 격리된 별도 chain.
// @Order(0) 으로 글로벌 chain 보다 먼저 매칭된다. SAP sapApiSecurityFilterChain 도 @Order(0)
// 이지만 securityMatcher 가 다르므로 충돌 없음.
// SAP 와 차이점:
//   - IP allowlist filter 없음 (Spec #774 Q5)
//   - SfAuthErrorWriter 는 RFC 6749 표준 OAuth 에러 형식 사용 (SAP 는 SapResultWrapper)
@Configuration
@EnableConfigurationProperties(SfAuthProperties::class)
class SfAuthSecurityConfig(
    private val properties: SfAuthProperties,
    private val sfJwtCodec: SfJwtCodec,
    private val auditService: SfInboundAuditService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    @Order(0)
    fun sfApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val bearerFilter = SfBearerTokenFilter(sfJwtCodec, auditService, objectMapper)

        http
            .securityMatcher("/api/v1/sf/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/sf/oauth/token").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(sfAuthenticationEntryPoint())
                ex.accessDeniedHandler(sfAccessDeniedHandler())
            }
            .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * 본 빈은 [SfBearerTokenFilter] 가 servlet container 의 글로벌 filter chain 으로도 등록되는 것을
     * 차단한다. Spring Boot 가 [OncePerRequestFilter] 를 자동 등록하므로 명시적으로 disable.
     */
    @Bean
    fun sfBearerTokenFilterRegistration(): FilterRegistrationBean<SfBearerTokenFilter> {
        val registration = FilterRegistrationBean(
            SfBearerTokenFilter(sfJwtCodec, auditService, objectMapper)
        )
        registration.isEnabled = false
        return registration
    }

    private fun sfAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
            SfAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
        }
    }

    private fun sfAccessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            SfAuthErrorWriter.write(response, objectMapper, 403, "insufficient_scope", "권한 없음")
        }
    }
}
