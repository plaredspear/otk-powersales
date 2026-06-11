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
     * - WebUserPrincipal (web 관리자) → User proxy (web 로그인 PK 는 실제 user.id)
     * - UserPrincipal (mobile 영업사원) → **Optional.empty()** (아래 주의 참조)
     *
     * SF 동등 — CreatedById/LastModifiedById 가 SF 플랫폼에 의해 자동 set 되는 동작을 application 단에서 모사.
     *
     * ## 주의 — mobile 등록 레코드는 영업사원으로 감사(created_by/owner) 기록하지 않는다
     * mobile `UserPrincipal.userId` 는 **employee.id (영업사원 PK)** 이며 `user.id` 가 아니다
     * (`AuthService.login` 이 토큰 subject 로 `employee.id` 를 발급). 이를 `getReference(User, id)` 로
     * User FK 컬럼(created_by_id / last_modified_by_id)에 그대로 쓰면, `user` 에 같은 id 가 없는 한
     * FK 위반(예: `fk_suggestion_created_by`)으로 INSERT 가 실패한다.
     *
     * 레거시(Heroku → SF Apex `IF_REST_MOBILE_ProposalRegist` / `ClaimRegist`)는 Proposal/Claim insert 시
     * OwnerId/CreatedById 를 명시 set 하지 않아 SF 플랫폼이 **통합 유저(interface@otg.com)** 로 자동 기록하고,
     * 영업사원은 owner 가 아니라 `EmployeeId__c`(= 우리 `employee_id`) lookup 으로만 추적한다. 즉 mobile 등록
     * 레코드의 소유자/생성자는 영업사원이 아니다. 따라서 mobile 컨텍스트에서는 감사 주체를 비워(`empty`) 두어
     * created_by/last_modified_by 를 null 로 남기고, 영업사원 추적은 `employee_id` 컬럼이 담당한다.
     */
    @Bean
    fun auditorAware(): AuditorAware<User> = AuditorAware {
        val authentication = SecurityContextHolder.getContext()?.authentication
        if (authentication == null || !authentication.isAuthenticated) {
            return@AuditorAware Optional.empty()
        }
        val userId: Long? = when (val principal = authentication.principal) {
            is WebUserPrincipal -> principal.userId
            // mobile 영업사원: userId 는 employee.id (user.id 아님) → 감사 주체 미기록 (레거시 정합)
            is UserPrincipal -> null
            else -> null
        }
        if (userId == null) {
            Optional.empty()
        } else {
            Optional.of(entityManager.getReference(User::class.java, userId))
        }
    }
}
