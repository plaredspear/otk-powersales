package com.otoki.powersales.common.config

import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.user.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class JpaAuditingConfig {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * SecurityContext 인증 principal 에서 현재 user.id (Long PK) 추출 후
     * `EntityManager.getReference` 로 User proxy 반환.
     *
     * - `@CreatedBy createdBy: User?`, `@LastModifiedBy lastModifiedBy: User?` 필드 자동 채움
     * - getReference 는 SELECT 발생시키지 않음 (proxy 만 발급) → INSERT/UPDATE 시 FK 값만 set
     * - 미인증 / system context (스케줄러·배치·HC sync) → Optional.empty() → 필드 미변경 (기존 값 유지)
     * - WebUserPrincipal (web 관리자) → User proxy
     * - UserPrincipal (mobile 영업사원) → User proxy
     *
     * SF 동등 — CreatedById/LastModifiedById 가 SF 플랫폼에 의해 자동 set 되는 동작을 application 단에서 모사.
     */
    @Bean
    fun auditorAware(): AuditorAware<User> = AuditorAware {
        val authentication = SecurityContextHolder.getContext()?.authentication
        if (authentication == null || !authentication.isAuthenticated) {
            return@AuditorAware Optional.empty()
        }
        val userId: Long? = when (val principal = authentication.principal) {
            is WebUserPrincipal -> principal.userId
            is UserPrincipal -> principal.userId
            else -> null
        }
        if (userId == null) {
            Optional.empty()
        } else {
            Optional.of(entityManager.getReference(User::class.java, userId))
        }
    }
}
