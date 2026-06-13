package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.SObjectRelation
import org.springframework.data.jpa.repository.JpaRepository

interface SObjectRelationRepository : JpaRepository<SObjectRelation, Long> {
    fun findAllByChildSObjectName(childSObjectName: String): List<SObjectRelation>
    fun findAllByParentSObjectName(parentSObjectName: String): List<SObjectRelation>
}
