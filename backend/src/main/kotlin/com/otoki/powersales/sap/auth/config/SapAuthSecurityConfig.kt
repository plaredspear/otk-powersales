package com.otoki.powersales.sap.auth.config

import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.filter.SapAuthErrorWriter
import com.otoki.powersales.sap.auth.filter.SapBearerTokenFilter
import com.otoki.powersales.sap.auth.filter.SapIpAllowlistFilter
import com.otoki.powersales.sap.auth.service.SapJwtCodec
import com.otoki.powersales.sap.auth.util.IpAllowlistMatcher
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

// /api/v1/sap/** 전용 SecurityFilterChain. mobile / admin 보다 먼저 매칭되도록 @Order(0).
@Configuration
@Order(0)
@EnableConfigurationProperties(SapAuthProperties::class)
class SapAuthSecurityConfig(
    private val properties: SapAuthProperties,
    private val sapJwtCodec: SapJwtCodec,
    private val auditService: SapInboundAuditService,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun sapApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val ipMatcher = IpAllowlistMatcher(properties.allowedIps)
        val ipFilter = SapIpAllowlistFilter(ipMatcher, auditService, objectMapper)
        val bearerFilter = SapBearerTokenFilter(sapJwtCodec, auditService, objectMapper)

        http
            .securityMatcher("/api/v1/sap/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/sap/oauth/token").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(sapAuthenticationEntryPoint())
                ex.accessDeniedHandler(sapAccessDeniedHandler())
            }
            .addFilterBefore(ipFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(bearerFilter, SapIpAllowlistFilter::class.java)

        return http.build()
    }

    @Bean
    fun sapBearerTokenFilterRegistration(): FilterRegistrationBean<SapBearerTokenFilter> {
        val registration = FilterRegistrationBean(
            SapBearerTokenFilter(sapJwtCodec, auditService, objectMapper)
        )
        registration.isEnabled = false
        return registration
    }

    @Bean
    fun sapIpAllowlistFilterRegistration(): FilterRegistrationBean<SapIpAllowlistFilter> {
        val ipMatcher = IpAllowlistMatcher(properties.allowedIps)
        val registration = FilterRegistrationBean(
            SapIpAllowlistFilter(ipMatcher, auditService, objectMapper)
        )
        registration.isEnabled = false
        return registration
    }

    private fun sapAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
            SapAuthErrorWriter.write(response, objectMapper, 401, "INVALID_TOKEN", "토큰이 필요합니다")
        }
    }

    private fun sapAccessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { _: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            SapAuthErrorWriter.write(response, objectMapper, 403, "INSUFFICIENT_SCOPE", "권한 없음")
        }
    }
}
