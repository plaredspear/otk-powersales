package com.otoki.powersales.common.entity

import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.user.entity.User
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PrePersist
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
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
 * ## 정적 holder 의존성 주입 패턴
 *
 * Hibernate 7 + Spring Boot 4 의 EntityListener 인스턴스화 모델은 `@Component` listener 에
 * DI 를 주입하지 못한다 — 실측 확인 결과 (instance #2 stacktrace):
 * ```
 *   FallbackBeanInstanceProducer.produceBeanInstance
 *   SpringBeanContainer.createBean
 *   ManagedBeanRegistryImpl.createBean
 *   ListenerCallback$Definition.createCallback
 * ```
 * 즉 `SpringBeanContainer` 가 등록되어도 Hibernate 는 listener 콜백 build 시점에
 * `beanFactory.createBean(beanType)` 으로 **새 prototype 인스턴스를 생성**하려 시도하고
 * (`@Component` 빈 lookup 이 아님), EMF 가 부팅 중이라 EntityManager 주입에 실패해
 * `FallbackBeanInstanceProducer` 로 reflection `new` fallback → `lateinit` 미초기화.
 *
 * 따라서 listener 클래스 자체에는 DI 를 두지 않고, 별도 `Dependencies` `@Component` 가
 * 부팅 완료 후 정적 companion 필드에 EntityManager 를 저장한다. listener 는 `@PrePersist`
 * 호출 시점에 정적 lookup 으로 EntityManager 를 획득 — Hibernate 가 reflection `new` 로
 * 인스턴스화해도 정상 동작.
 *
 * `MainJpaRepositoriesConfig` 의 `SpringBeanContainer` 등록은 보완 인프라로 유지 — 향후
 * `useJpaCompliantCreation()=false` 인 다른 callback 이 추가되면 활용 가능.
 */
class OwnerUserDefaultListener {

    @PrePersist
    fun setDefaultOwnerUser(entity: Any) {
        @Suppress("UNCHECKED_CAST")
        val ownerUserProp = entity::class.memberProperties
            .firstOrNull { it.name == "ownerUser" } as? KMutableProperty1<Any, Any?>
            ?: return
        val current = ownerUserProp.get(entity)
        if (current != null) return // 명시 set 된 경우 (Trigger override 동등) 유지

        val userId = currentUserId() ?: return
        val em = Dependencies.entityManager ?: return
        ownerUserProp.set(entity, em.getReference(User::class.java, userId))
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

    /**
     * 부팅 시 Spring 이 1회 인스턴스화. `@PersistenceContext` 로 받은 EntityManager
     * (실제로는 `SharedEntityManagerCreator` proxy — thread-safe) 를 companion 필드에
     * 저장해 listener 가 정적 lookup 으로 사용하도록 한다.
     */
    @Component
    class Dependencies(
        @PersistenceContext private val em: EntityManager,
    ) : InitializingBean {
        override fun afterPropertiesSet() {
            entityManager = em
        }

        companion object {
            @Volatile
            var entityManager: EntityManager? = null
                private set
        }
    }
}
