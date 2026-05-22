package com.otoki.powersales.auth.sharing.listener

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.sharing.event.UserRoleChangedEvent
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * UserRole entity JPA lifecycle listener (spec #788).
 *
 * `@PostPersist` / `@PostUpdate` / `@PostRemove` 시점에 [UserRoleChangedEvent] 발행.
 *
 * ## JPA EntityListener 와 Spring bean 주입
 *
 * JPA EntityListener 는 JPA 구현체가 직접 인스턴스화하므로 Spring 의 `@Autowired` 가 동작하지 않는다
 * (Hibernate 가 reflection 으로 no-arg constructor 호출). 본 클래스는 `ApplicationContextAware` 의
 * static holder 패턴으로 우회한다 — Spring 이 본 클래스를 `@Component` 빈으로 인스턴스화할 때
 * [setApplicationContext] 가 호출되어 static field 에 context 박제 → JPA 의 listener 인스턴스가
 * static field 를 통해 publisher 에 접근.
 *
 * ## 트랜잭션 안전
 *
 * 본 listener 자체는 entity flush 시점에 호출 (트랜잭션 commit 전). 발행된 이벤트는
 * [com.otoki.powersales.auth.sharing.service.UserRoleHierarchyEventHandler] 의
 * `@TransactionalEventListener(AFTER_COMMIT)` 이 수신 시점에 트랜잭션 commit 후 처리 →
 * rollback 시 snapshot 변경 0건 보장.
 */
@Component
class UserRoleEntityListener : ApplicationContextAware {

    @PostPersist
    fun postPersist(userRole: UserRole) {
        publish(userRole, UserRoleChangedEvent.ChangeType.CREATED)
    }

    @PostUpdate
    fun postUpdate(userRole: UserRole) {
        publish(userRole, UserRoleChangedEvent.ChangeType.UPDATED)
    }

    @PostRemove
    fun postRemove(userRole: UserRole) {
        publish(userRole, UserRoleChangedEvent.ChangeType.REMOVED)
    }

    private fun publish(userRole: UserRole, changeType: UserRoleChangedEvent.ChangeType) {
        val publisher = eventPublisher
        if (publisher == null) {
            log.warn(
                "[user-role-listener] ApplicationContext 미초기화 — 이벤트 누락 userRoleId={}, changeType={}",
                userRole.id, changeType,
            )
            return
        }
        try {
            publisher.publishEvent(UserRoleChangedEvent(userRole.id, changeType))
            log.debug("[user-role-listener] event published userRoleId={}, changeType={}", userRole.id, changeType)
        } catch (e: Exception) {
            log.warn(
                "[user-role-listener] 이벤트 발행 실패 — userRoleId={}, changeType={}, message={}",
                userRole.id, changeType, e.message,
            )
        }
    }

    @Autowired
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        try {
            eventPublisher = applicationContext.getBean(ApplicationEventPublisher::class.java)
        } catch (e: BeansException) {
            log.error("[user-role-listener] ApplicationEventPublisher 빈 조회 실패", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserRoleEntityListener::class.java)

        @Volatile
        private var eventPublisher: ApplicationEventPublisher? = null
    }
}
