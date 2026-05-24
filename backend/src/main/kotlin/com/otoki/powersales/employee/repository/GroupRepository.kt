package com.otoki.powersales.employee.repository

import com.otoki.powersales.employee.entity.Group
import org.springframework.data.jpa.repository.JpaRepository

/**
 * SF Group sobject 매핑 entity Repository (Spec #755).
 */
interface GroupRepository : JpaRepository<Group, Long> {

    fun findByDeveloperName(developerName: String): Group?

    // spec #782 P2-B — typed FK 직접 사용 (Group.RelatedId polymorphic 의 User/UserRole 분기).
    fun findAllByRelatedUserId(relatedUserId: Long): List<Group>

    fun findAllByRelatedUserRoleId(relatedUserRoleId: Long): List<Group>
}
