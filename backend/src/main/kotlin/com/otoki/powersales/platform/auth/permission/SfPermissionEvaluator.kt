package com.otoki.powersales.platform.auth.permission

import org.springframework.stereotype.Component

/**
 * `@RequiresSfPermission` 어노테이션과 user permission set 의 매칭 평가 (spec #801).
 *
 * 본 evaluator 는 stateless — `SfPermissionResolver` 가 산출한 평탄화 key set 을 입력으로 받아
 * 어노테이션의 요구 권한과 매칭 검사. `WebAdminContextFilter` 에서 호출.
 */
@Component
class SfPermissionEvaluator {

    /**
     * user 의 권한 key set 이 어노테이션의 요구를 만족하는지 평가.
     */
    fun isAllowed(annotation: RequiresSfPermission, userPermissions: Set<String>): Boolean {
        return when (annotation.operation) {
            SfPermissionOperation.SYSTEM -> {
                userPermissions.contains(SfPermissionResolver.systemKey(annotation.systemPermission))
            }
            SfPermissionOperation.READ,
            SfPermissionOperation.CREATE,
            SfPermissionOperation.EDIT,
            SfPermissionOperation.DELETE -> {
                if (annotation.entity.isBlank()) return false
                userPermissions.contains(SfPermissionResolver.entityKey(annotation.entity, annotation.operation))
            }
        }
    }
}
