package com.otoki.powersales.common.entity

import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.user.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PrePersist
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * `ownerUser: User?` 필드를 가진 entity 의 INSERT 시점 기본값 자동 set.
 *
 * SF 동등 — SF 의 OwnerId 는 명시 set 이 없으면 record 생성자(`UserInfo.getUserId()`)로 자동 set.
 * Trigger override 케이스 (Account / DisplayWorkSchedule / TeamMemberSchedule) 는 service 단에서
 * 이미 명시 set 하므로 본 listener 의 null 분기를 거치지 않음.
 *
 * - INSERT 시점 (`@PrePersist`) 만. UPDATE 는 SF 와 동일하게 ownerUser 변경 없음.
 * - 미인증 / system context (스케줄러·배치·HC sync) → 기본값 set 미적용 (null 유지)
 *
 * Hibernate 6 + Spring Boot 3.x 는 `ManagedBeanRegistry` 를 통해 EntityListener 를 Spring bean 으로
 * 인스턴스화. 본 클래스를 `@Component` 로 등록하고 entity 에서 `@EntityListeners(OwnerUserDefaultListener::class)`
 * 로 부착하면 EntityManager / SecurityContext 가 정상 주입.
 *
 * ## TEMP: 진단 로그 (제거 예정)
 * 인스턴스화 호출자 stacktrace 를 1회 로깅 — Spring `ManagedBeanRegistryImpl` 경로인지 Hibernate
 * reflection `new` 경로인지 판별. lateinit 미초기화 사고 원인 추적용. 검증 완료 후 제거.
 */
@Component
class OwnerUserDefaultListener {

    init {
        val n = instanceCounter.incrementAndGet()
        log.warn(
            "[DIAG] OwnerUserDefaultListener instance #{} created. caller stack:\n{}",
            n,
            Thread.currentThread().stackTrace.take(15).joinToString("\n  ") { "at $it" },
        )
    }

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @PrePersist
    fun setDefaultOwnerUser(entity: Any) {
        @Suppress("UNCHECKED_CAST")
        val ownerUserProp = entity::class.memberProperties
            .firstOrNull { it.name == "ownerUser" } as? KMutableProperty1<Any, Any?>
            ?: return
        val current = ownerUserProp.get(entity)
        if (current != null) return // 명시 set 된 경우 (Trigger override 동등) 유지

        val userId = currentUserId() ?: return
        ownerUserProp.set(entity, entityManager.getReference(User::class.java, userId))
    }

    private fun currentUserId(): Long? {
        val auth = SecurityContextHolder.getContext()?.authentication ?: return null
        if (!auth.isAuthenticated) return null
        return when (val principal = auth.principal) {
            is WebUserPrincipal -> principal.userId
            is UserPrincipal -> principal.userId
            else -> null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OwnerUserDefaultListener::class.java)
        private val instanceCounter = AtomicInteger(0)
    }
}
