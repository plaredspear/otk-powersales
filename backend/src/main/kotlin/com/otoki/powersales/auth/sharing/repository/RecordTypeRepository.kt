package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.RecordType
import org.springframework.data.jpa.repository.JpaRepository

interface RecordTypeRepository : JpaRepository<RecordType, Long> {
    fun findBySObjectNameAndDeveloperName(sObjectName: String, developerName: String): RecordType?
    fun findAllBySObjectName(sObjectName: String): List<RecordType>
}
