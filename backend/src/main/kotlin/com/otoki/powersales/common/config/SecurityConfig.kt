package com.otoki.powersales.common.config

import com.otoki.powersales.common.security.DomainGuardFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationEntryPoint
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.PasswordChangeRequiredFilter
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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher

/**
 * Spring Security 설정
 * JWT 인증 필터 통합 (API 전용)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(UuidCheckProperties::class, DomainProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val gpsConsentFilter: GpsConsentFilter,
    private val passwordChangeRequiredFilter: PasswordChangeRequiredFilter,
    private val domainProperties: DomainProperties
) {

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // securityMatcher 좌합 회피: admin/sap/sf 전용 chain 과 path 충돌이 발생하지 않도록 명시 제외.
        // 과거 SecurityFilterChain 빈 @Order 가 인식되지 못해 글로벌 chain 이 먼저 매칭되는 회귀가 있었기에,
        // ordering 의존을 줄이고 matcher 자체로 disjoint 를 보장.
        val mvcMatcher = PathPatternRequestMatcher.withDefaults()
        val apiBaseMatcher = OrRequestMatcher(
            mvcMatcher.matcher("/api/**"),
            mvcMatcher.matcher("/h2-console/**"),
            mvcMatcher.matcher("/swagger-ui/**"),
            mvcMatcher.matcher("/swagger-ui.html"),
            mvcMatcher.matcher("/v3/api-docs/**"),
        )
        val excludeAdminSapSf = NegatedRequestMatcher(
            OrRequestMatcher(
                mvcMatcher.matcher("/api/v1/admin/**"),
                mvcMatcher.matcher("/api/v1/sap/**"),
                mvcMatcher.matcher("/api/v1/sf/**"),
            )
        )
        http
            .securityMatcher(AndRequestMatcher(apiBaseMatcher, excludeAdminSapSf))
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/health",
                        "/api/v1/mobile/auth/login",
                        "/api/v1/mobile/auth/refresh",
                        // 앱 패키지 버전 체크/다운로드는 강제 업데이트 게이트가 로그인 전에 동작해야 하므로 무인증 허용.
                        "/api/v1/mobile/app-package/**",
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
            .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter::class.java)
            .addFilterAfter(gpsConsentFilter, PasswordChangeRequiredFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun domainGuardFilter(): FilterRegistrationBean<DomainGuardFilter> {
        val registration = FilterRegistrationBean(
            DomainGuardFilter(domainProperties.api, domainProperties.admin)
        )
        registration.order = Ordered.HIGHEST_PRECEDENCE + 1
        return registration
    }
}
