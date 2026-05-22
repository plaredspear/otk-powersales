package com.otoki.powersales.employee.repository

import com.otoki.powersales.employee.entity.Group
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * SF Group sobject 매핑 entity Repository (Spec #755).
 */
interface GroupRepository : JpaRepository<Group, Long> {

    fun findBySfid(sfid: String): Optional<Group>

    fun existsBySfid(sfid: String): Boolean

    fun findByDeveloperName(developerName: String): Group?
}
