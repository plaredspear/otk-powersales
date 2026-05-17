package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.RolePermissionMatrix
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * 부팅 시 [RolePermissionMatrix] SoT 를 `role_permission` 테이블과 INSERT-only 동기화.
 *
 * 정책:
 * - SoT 에 있고 DB 에 없는 (role, permission) → INSERT
 * - DB 에만 있는 row → **보존** (web admin 의 권한 매트릭스 수정 UI 변경분 / 운영자 임시 부여 보호)
 * - 매트릭스 본문 누락 (예: SoT 에 없는 권한이 DB 에 있다) 은 별도 처리 없이 그대로 둔다
 *
 * 운영 가시성:
 * - INSERT 발생 시 INFO 로 추가된 (role, permission) 와 건수 출력
 * - 변경 없을 시 단일 라인 INFO
 *
 * 실행 순서: [Order] 0 — [com.otoki.powersales.common.config.LocalDataInitializer] (default Order)
 * 보다 먼저 돌아야 시드 SYSTEM_ADMIN 계정이 즉시 권한을 보유한다.
 */
@Component
@Order(0)
class RolePermissionSyncRunner(
    private val rolePermissionRepository: RolePermissionRepository,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            transactionTemplate.executeWithoutResult { sync() }
        } catch (e: Exception) {
            log.error("role_permission sync 실패 — 매트릭스 동기화 미적용: {}", e.message, e)
        }
    }

    private fun sync() {
        val existing: Set<Pair<String, String>> = rolePermissionRepository.findAll()
            .map { it.role to it.permission }
            .toSet()

        val desired: List<Pair<String, String>> = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> role.name to perm.name }

        val toInsert: List<Pair<String, String>> = desired.filter { it !in existing }

        if (toInsert.isEmpty()) {
            log.info("role_permission sync: 변경 없음 (existing={}, desired={})", existing.size, desired.size)
            return
        }

        val newRows = toInsert.map { (role, permission) ->
            RolePermission(role = role, permission = permission)
        }
        rolePermissionRepository.saveAll(newRows)

        toInsert.forEach { (role, permission) ->
            log.info("role_permission sync INSERT: role={} permission={}", role, permission)
        }
        log.info("role_permission sync 완료: inserted={} (existing={}, desired={})", toInsert.size, existing.size, desired.size)
    }
}
