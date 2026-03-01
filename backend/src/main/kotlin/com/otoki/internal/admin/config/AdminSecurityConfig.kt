package com.otoki.internal.admin.config

import com.otoki.internal.admin.service.AdminUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Order(1)
class AdminSecurityConfig(
    private val adminUserDetailsService: AdminUserDetailsService,
    private val passwordEncoder: PasswordEncoder
) {

    @Bean
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**")
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .csrf { /* enabled by default */ }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/admin/login", "/admin/css/**").permitAll()
                    .anyRequest().hasAnyRole("LEADER", "ADMIN")
            }
            .formLogin { form ->
                form
                    .loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .defaultSuccessUrl("/admin/dashboard", true)
                    .failureUrl("/admin/login?error")
                    .usernameParameter("employeeId")
                    .passwordParameter("password")
            }
            .logout { logout ->
                logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
            }
            .exceptionHandling { ex ->
                ex
                    .accessDeniedHandler { _, response, _ ->
                        response.sendRedirect("/admin/login?denied")
                    }
                    .authenticationEntryPoint { _, response, _ ->
                        response.sendRedirect("/admin/login")
                    }
            }
            .authenticationProvider(adminAuthenticationProvider())

        return http.build()
    }

    @Bean
    fun adminAuthenticationProvider(): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider(passwordEncoder)
        provider.setUserDetailsService(adminUserDetailsService)
        return provider
    }
}
