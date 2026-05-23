package com.otoki.powersales.auth.permission

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * `@RequiresSfPermission(entity = X)` 의 X 가 [EntitySfNameRegistry] 카탈로그에 등록되어 있는지
 * 부팅 시 1회 검증 (spec #808).
 *
 * 미스매치 (오타 / 폐기된 entity / 등록되지 않은 가상 자원) 시 부팅 실패.
 *
 * ## 검증 의의
 *
 * 가드의 entity 식별자 오타 (`entity = "emplyee"`) 는 가드 평가 시점에 항상 false 가 되어 정상적인 사용자도
 * 차단되거나 (운영 장애), 운영자가 권한을 부여할 수 없는 dead entity 가 PermissionSet 데이터에 남게 된다.
 * 본 lint 가 부팅 시점에 잡아내어 운영 환경 도달 전 발견 보장.
 */
@Component
class RequiresSfPermissionLint(
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val entitySfNameRegistry: EntitySfNameRegistry,
) : SmartInitializingSingleton {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterSingletonsInstantiated() {
        val unknownEntities = mutableSetOf<String>()
        var checkedCount = 0

        for ((_, handlerMethod) in requestMappingHandlerMapping.handlerMethods) {
            val annotation = handlerMethod.findAnnotation() ?: continue
            checkedCount++
            if (annotation.operation == SfPermissionOperation.SYSTEM) continue
            val entity = annotation.entity
            if (entity.isBlank()) continue
            if (!entitySfNameRegistry.contains(entity)) {
                unknownEntities.add("$entity (${handlerMethod.beanType.simpleName}.${handlerMethod.method.name})")
            }
        }

        if (unknownEntities.isNotEmpty()) {
            val message = buildString {
                appendLine("[RequiresSfPermissionLint] @RequiresSfPermission(entity = ...) 의 entity 식별자가 카탈로그에 미등록 — ${unknownEntities.size} 건:")
                for (entry in unknownEntities.sorted()) {
                    appendLine("  - $entry")
                }
                appendLine("해결: (a) entity 이름 오타 정정, (b) JPA @Table(name) 매칭 확인, 또는 (c) JPA entity 가 없는 가상 자원이면 @PermissionResource 어노테이션 부착")
            }
            log.error(message)
            error(message)
        }

        log.info(
            "[RequiresSfPermissionLint] 가드 lint 통과 — endpoint {} 종 검사, 모든 entity 식별자가 카탈로그에 등록됨",
            checkedCount,
        )
    }

    private fun HandlerMethod.findAnnotation(): RequiresSfPermission? {
        // method 우선, 그 다음 declaring class
        return getMethodAnnotation(RequiresSfPermission::class.java)
            ?: beanType.getAnnotation(RequiresSfPermission::class.java)
    }
}
