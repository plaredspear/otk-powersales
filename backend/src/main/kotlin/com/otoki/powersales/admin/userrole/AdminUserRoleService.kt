package com.otoki.powersales.admin.userrole

import com.otoki.powersales.admin.userrole.dto.UserRoleNode
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.repository.UserRoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminUserRoleService(
    private val userRoleRepository: UserRoleRepository,
) {

    /**
     * 전체 UserRole row 를 한 번 fetch 한 뒤 parent_user_role_id 기반으로 트리 조립.
     *
     * 운영 size (~수백) 라서 단일 query + 메모리 조립이 충분. 정렬은 name (한글) asc.
     */
    fun getTree(): List<UserRoleNode> {
        val all = userRoleRepository.findAll()
        val byId: Map<Long, UserRole> = all.associateBy { it.id }
        val childrenByParentId: Map<Long?, List<UserRole>> = all.groupBy { it.parentUserRoleId }

        fun build(role: UserRole): UserRoleNode {
            val children = childrenByParentId[role.id].orEmpty()
                .sortedBy { it.name }
                .map(::build)
            return UserRoleNode(
                userRoleId = role.id,
                name = role.name,
                developerName = role.developerName,
                rollupDescription = role.rollupDescription,
                parentUserRoleId = role.parentUserRoleId,
                parentName = role.parentUserRoleId?.let { byId[it]?.name },
                children = children,
            )
        }

        return childrenByParentId[null].orEmpty()
            .sortedBy { it.name }
            .map(::build)
    }
}
