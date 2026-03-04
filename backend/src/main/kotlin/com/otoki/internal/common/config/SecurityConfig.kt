package com.otoki.internal.common.config

import com.otoki.internal.common.security.DomainGuardFilter
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationEntryPoint
import com.otoki.internal.common.security.JwtAuthenticationFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security 설정
 * JWT 인증 필터 통합 (API 전용)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(DeviceBindingProperties::class, DomainProperties::class)
@Order(2)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val gpsConsentFilter: GpsConsentFilter,
    private val domainProperties: DomainProperties
) {

    @Bean
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**", "/h2-console/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/health",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/h2-console/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }  // H2 Console 사용을 위해
            }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(gpsConsentFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun domainGuardFilter(): FilterRegistrationBean<DomainGuardFilter> {
        val registration = FilterRegistrationBean<DomainGuardFilter>()
        registration.filter = DomainGuardFilter(domainProperties.api, domainProperties.admin)
        registration.order = Ordered.HIGHEST_PRECEDENCE + 1
        return registration
    }
}
