package com.otoki.internal.admin.config

import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@Order(1)
class AdminApiSecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val adminAuthorityFilter: AdminAuthorityFilter
) {

    @Bean
    fun adminApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/v1/admin/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(adminAuthorityFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }
}
