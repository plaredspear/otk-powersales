package com.otoki.internal.sap.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.filter.SapApiKeyFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@Order(0)
@EnableConfigurationProperties(SapAuthProperties::class)
class SapSecurityConfig(
    private val sapAuthProperties: SapAuthProperties,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun sapSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/v1/sap/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(
                SapApiKeyFilter(sapAuthProperties, objectMapper),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
