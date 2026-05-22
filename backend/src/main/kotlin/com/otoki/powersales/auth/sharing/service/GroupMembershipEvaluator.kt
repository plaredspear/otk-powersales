package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.employee.repository.GroupRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Group 멤버십 평가 Service (spec #782 P2-B).
 *
 * SF Group 의 RelatedId 가 본 User 또는 그 UserRole/상위 RoleAndSubordinatesInternal 을 가리키는 경우
 * 본 User 가 해당 Group 의 멤버로 평가.
 *
 * ## typed FK 활용 (v1.4)
 * SF describe `Group.RelatedId.referenceTo = [User, UserRole]` 정합으로 `Group.related_user_id` /
 * `Group.related_user_role_id` 2 nullable FK 로 분리되어 있어, sfid prefix 파싱 / lookup query 회피.
 *
 * ## 평가 알고리즘 (v1.4 — Group.RelatedId 만)
 * 1. **User 직접 매칭**: `Group.relatedUserId = userId` 인 group 일람.
 * 2. **UserRole 매칭**: `Group.relatedUserRoleId = userRoleId` 인 group 일람.
 * 3. **RoleAndSubordinatesInternal 매칭**: ancestorPath(userRoleId) 의 어느 user_role_id 라도
 *    `Group.relatedUserRoleId` 인 group 일람. UserRoleHierarchyTraversal.getAncestorPath 사용.
 *
 * **Nested Group** (Group → Group RelatedId) — SF describe referenceTo 부재 (Group 자기참조 부재) 로
 * 본 spec 비범위. Type=Regular (PublicGroup) 의 멤버십은 별도 `GroupMember` 테이블 lookup —
 * 본 프로젝트 운영 0건 (sharingRule `<group>` 인용 1건만, GroupMember 자체 미적재) 로 본 Service 미흡수.
 */
@Service
class GroupMembershipEvaluator(
    private val groupRepository: GroupRepository,
    private val userRoleHierarchyTraversal: UserRoleHierarchyTraversal,
) {
    private val log = LoggerFactory.getLogger(GroupMembershipEvaluator::class.java)

    /**
     * 본 User 가 직접 또는 간접 (UserRole / RoleAndSubordinatesInternal 경유) 멤버인 모든 Group.group_id set.
     *
     * @param userId User entity 의 PK (sfid 아님)
     * @param userRoleId User.userRoleId (null 가능 — 미할당 user)
     */
    @Cacheable(value = ["memberGroupIds"], key = "{#userId, #userRoleId ?: 0L}")
    fun getMemberGroupIds(userId: Long, userRoleId: Long?): Set<Long> {
        val result = mutableSetOf<Long>()

        // 1) User 직접 매칭 — Group.relatedUserId = userId
        groupRepository.findAllByRelatedUserId(userId).forEach { result.add(it.id) }

        if (userRoleId != null) {
            // 2) UserRole 매칭 + 3) RoleAndSubordinatesInternal (ancestor path)
            val ancestorPath = try {
                userRoleHierarchyTraversal.getAncestorPath(userRoleId)
            } catch (e: IllegalStateException) {
                log.warn("[group-membership] cycle/depth issue for userRoleId={} — fallback to self only", userRoleId, e)
                listOf(userRoleId)
            }
            ancestorPath.forEach { ancestorRoleId ->
                groupRepository.findAllByRelatedUserRoleId(ancestorRoleId).forEach { result.add(it.id) }
            }
        }
        return result
    }
}
