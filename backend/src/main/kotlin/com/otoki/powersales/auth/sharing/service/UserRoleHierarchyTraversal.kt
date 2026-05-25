package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.repository.UserRoleRepository
import com.otoki.powersales.auth.sharing.entity.UserRoleHierarchySnapshot
import com.otoki.powersales.auth.sharing.repository.UserRoleHierarchySnapshotRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime

/**
 * UserRole 트리 traversal Service (spec #782 P2-B).
 *
 * SF UserRole Hierarchy 의 RoleAndSubordinates 자동화를 backend 에서 정적 스냅샷으로 흡수.
 * 매 평가 시 트리 traverse 회피 — `user_role_hierarchy_snapshot` 테이블의 jsonb lookup 1회.
 *
 * ## 안전 가드 (L2 정정)
 * - **MAX_HIERARCHY_DEPTH = 20** — cycle / max depth 차단 (운영 정책상 5-6 depth 이상 불필요, 안전 마진).
 * - **visited set** — application-level 재귀 시 동일 node 재방문 감지 → IllegalStateException.
 * - **Orphan UserRole** — parent_user_role_id 가 가리키는 row 부재 시 warning 로그 + root 처리.
 */
@Service
class UserRoleHierarchyTraversal(
    private val snapshotRepository: UserRoleHierarchySnapshotRepository,
    private val userRoleRepository: UserRoleRepository,
    private val objectMapper: ObjectMapper,
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val log = LoggerFactory.getLogger(UserRoleHierarchyTraversal::class.java)

    /**
     * 본 UserRole 의 모든 자손 user_role_id (자기 자신 포함).
     *
     * snapshot.allSubordinateIds jsonb 직접 조회 — hot path.
     * snapshot 부재 시 application-level CTE 로 즉시 계산 + snapshot 저장 (lazy init).
     */
    @Cacheable(value = ["hierarchySubordinates"], key = "#userRoleId")
    fun getAllSubordinateUserRoleIds(userRoleId: Long): Set<Long> {
        val snapshot = snapshotRepository.findById(userRoleId).orElse(null)
        if (snapshot != null) {
            return parseLongList(snapshot.allSubordinateIds).toSet()
        }
        // snapshot 부재 — recomputeSubtree 가 lazy 생성. 단 본 메서드는 read-only 분기라 fresh 계산만.
        return computeAllSubordinateIds(userRoleId)
    }

    /**
     * 본 UserRole 부터 root 까지 user_role_id list (depth + 1 개).
     */
    @Cacheable(value = ["hierarchyAncestorPath"], key = "#userRoleId")
    fun getAncestorPath(userRoleId: Long): List<Long> {
        val snapshot = snapshotRepository.findById(userRoleId).orElse(null)
        if (snapshot?.ancestorPath != null) {
            return parseLongList(snapshot.ancestorPath!!)
        }
        return computeAncestorPath(userRoleId)
    }

    /**
     * 본 UserRole + 그 자손 모든 snapshot row 재계산.
     *
     * UserRole.parent_user_role_id 변경 시 호출 (Hierarchy 의 부분 트리 변경).
     *
     * `sharing-rules-for-user:v1` 도 함께 evict — ancestorPath 변경이 sharing_rule_target 의
     * `ROLE_AND_SUBORDINATES` 매칭 결과를 바꿈.
     */
    @Transactional
    @CacheEvict(
        value = ["hierarchySubordinates", "hierarchyAncestorPath", "sharing-rules-for-user:v1"],
        allEntries = true,
    )
    fun recomputeSubtree(userRoleId: Long) {
        val descendants = computeAllSubordinateIds(userRoleId)
        descendants.forEach { id ->
            persistSnapshot(id)
        }
        log.info("[hierarchy] recomputeSubtree({}) — {} snapshots updated", userRoleId, descendants.size)
    }

    /**
     * 전체 snapshot 재계산 batch (운영 트리거 또는 cut-over 1회).
     *
     * `sharing-rules-for-user:v1` 도 함께 evict — recomputeSubtree 와 동일 사유.
     */
    @Transactional
    @CacheEvict(
        value = ["hierarchySubordinates", "hierarchyAncestorPath", "sharing-rules-for-user:v1"],
        allEntries = true,
    )
    fun recomputeAll() {
        val allUserRoleIds = userRoleRepository.findAll().mapNotNull { it.id.takeIf { rid -> rid > 0 } }
        log.info("[hierarchy] recomputeAll — {} UserRole rows", allUserRoleIds.size)
        allUserRoleIds.forEach { id ->
            persistSnapshot(id)
        }
    }

    /**
     * 단일 snapshot row 박제.
     */
    private fun persistSnapshot(userRoleId: Long) {
        val subordinates = computeAllSubordinateIds(userRoleId).toList().sorted()
        val (depth, path) = computeAncestorPathWithDepth(userRoleId)

        val existing = snapshotRepository.findById(userRoleId).orElse(null)
        if (existing != null) {
            existing.allSubordinateIds = objectMapper.writeValueAsString(subordinates)
            existing.depth = depth
            existing.ancestorPath = objectMapper.writeValueAsString(path)
            existing.snapshotAt = LocalDateTime.now()
            snapshotRepository.save(existing)
        } else {
            snapshotRepository.save(
                UserRoleHierarchySnapshot(
                    userRoleId = userRoleId,
                    allSubordinateIds = objectMapper.writeValueAsString(subordinates),
                    depth = depth,
                    ancestorPath = objectMapper.writeValueAsString(path),
                    snapshotAt = LocalDateTime.now(),
                ),
            )
        }
    }

    /**
     * Postgres recursive CTE — 본 UserRole 의 모든 자손 (자기 자신 포함).
     * MAX_HIERARCHY_DEPTH 로 cycle 차단.
     */
    @Suppress("UNCHECKED_CAST")
    private fun computeAllSubordinateIds(userRoleId: Long): Set<Long> {
        val sql = """
            WITH RECURSIVE subtree AS (
                SELECT user_role_id, 0 AS depth
                  FROM powersales.user_role
                 WHERE user_role_id = :targetId
                UNION ALL
                SELECT ur.user_role_id, st.depth + 1
                  FROM powersales.user_role ur
                  JOIN subtree st ON ur.parent_user_role_id = st.user_role_id
                 WHERE st.depth < $MAX_HIERARCHY_DEPTH
            )
            SELECT user_role_id FROM subtree
        """.trimIndent()
        val result = entityManager.createNativeQuery(sql)
            .setParameter("targetId", userRoleId)
            .resultList as List<Number>
        return result.map { it.toLong() }.toSet()
    }

    /**
     * 본 UserRole 부터 root 까지 ancestor user_role_id (자기 자신 포함, root 마지막).
     * application-level 재귀 + visited set 으로 cycle 안전.
     */
    private fun computeAncestorPath(userRoleId: Long): List<Long> = computeAncestorPathWithDepth(userRoleId).second

    private fun computeAncestorPathWithDepth(userRoleId: Long): Pair<Int, List<Long>> {
        val visited = mutableSetOf<Long>()
        val path = mutableListOf<Long>()
        var current: Long? = userRoleId
        var depth = 0
        while (current != null && depth < MAX_HIERARCHY_DEPTH) {
            if (!visited.add(current)) {
                throw IllegalStateException(
                    "UserRole cycle detected: $userRoleId → $path (revisit at $current)",
                )
            }
            path.add(current)
            val parent = userRoleRepository.findById(current).orElse(null)?.parentUserRoleId
            if (parent == null) break
            if (current == parent) {
                throw IllegalStateException("UserRole self-reference: $current")
            }
            current = parent
            depth++
        }
        if (depth >= MAX_HIERARCHY_DEPTH) {
            log.warn("[hierarchy] max depth $MAX_HIERARCHY_DEPTH exceeded — path={}, possible cycle", path)
        }
        return depth to path
    }

    private fun parseLongList(json: String): List<Long> {
        if (json.isBlank() || json == "[]") return emptyList()
        return objectMapper.readValue<List<Long>>(json)
    }

    companion object {
        const val MAX_HIERARCHY_DEPTH = 20
    }
}
