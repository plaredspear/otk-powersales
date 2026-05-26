package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

/**
 * PermissionSet 권한 비트 평가 Service (spec #782 P2-B).
 *
 * User 의 활성 PermissionSetAssignment 일람 → PermissionSetFlags 의 objectPermissions JSON 합산.
 *
 * - `viewAllRecords` / `modifyAllRecords` 가 어느 한 PermissionSet 이라도 true 면 합산 true (OR).
 * - 시스템 권한 비트 (`ViewAllData` / `ModifyAllData`) 도 OR 합산.
 *
 * sharing policy evaluator 가 SObject 별 hierarchy / sharingRule 평가 전 본 결과를 우선 분기 —
 * `viewAllRecords[Account] = true` 이면 본 user 의 Account read query 는 전체 row 반환 (predicate FALSE 무관).
 */
@Service
class PermissionSetEvaluator(
    private val assignmentRepository: PermissionSetAssignmentRepository,
    private val flagsRepository: PermissionSetFlagsRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(PermissionSetEvaluator::class.java)

    @Cacheable(value = ["permissionSetFlags:v2"], key = "#userId")
    fun getPermissionSetSnapshot(userId: Long): PermissionSetSnapshot {
        val assignments = assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(userId)
        if (assignments.isEmpty()) return PermissionSetSnapshot.NONE

        var viewAllDataSystem = false
        var modifyAllDataSystem = false
        val viewAllRecordsAccum = mutableMapOf<String, Boolean>()
        val modifyAllRecordsAccum = mutableMapOf<String, Boolean>()
        val permissionSetIdsAccum = mutableSetOf<Long>()

        // spec #798 — Stage1 적재 직후 Stage2 fk substep 미실행 시 permissionSetFlagsId NULL 가능 → skip.
        // assignment 일람 만큼 findById 반복 호출 (N+1) 회피 — findAllById 1회 호출로 일괄 로드.
        val flagsIds = assignments.mapNotNull { it.permissionSetFlagsId }.distinct()
        if (flagsIds.isEmpty()) return PermissionSetSnapshot.NONE
        val flagsList = flagsRepository.findAllById(flagsIds)

        flagsList.forEach { flags ->
            if (flags.permissionsViewAllData) viewAllDataSystem = true
            if (flags.permissionsModifyAllData) modifyAllDataSystem = true

            // spec #796 — permission_set 정규 id 가 Stage2 fk resolve 후 채워진 경우 수집
            flags.permissionSetId?.let { permissionSetIdsAccum.add(it) }

            val objectPerms = parseObjectPermissions(flags.objectPermissions)
            objectPerms.forEach { (sObjectName, perms) ->
                if (perms["viewAllRecords"] == true) viewAllRecordsAccum[sObjectName] = true
                if (perms["modifyAllRecords"] == true) modifyAllRecordsAccum[sObjectName] = true
            }
        }

        return PermissionSetSnapshot(
            viewAllDataSystem = viewAllDataSystem,
            modifyAllDataSystem = modifyAllDataSystem,
            viewAllRecordsBySObject = viewAllRecordsAccum,
            modifyAllRecordsBySObject = modifyAllRecordsAccum,
            permissionSetIds = permissionSetIdsAccum,
        )
    }

    fun hasViewAllRecords(userId: Long, sObjectName: String): Boolean =
        getPermissionSetSnapshot(userId).hasViewAllRecords(sObjectName)

    fun hasModifyAllRecords(userId: Long, sObjectName: String): Boolean =
        getPermissionSetSnapshot(userId).hasModifyAllRecords(sObjectName)

    private fun parseObjectPermissions(json: String?): Map<String, Map<String, Boolean>> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        return try {
            objectMapper.readValue(json, object : TypeReference<Map<String, Map<String, Boolean>>>() {})
        } catch (e: Exception) {
            log.warn("[permission-set] objectPermissions parse failed — {}", e.message)
            emptyMap()
        }
    }
}
