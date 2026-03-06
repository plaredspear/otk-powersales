package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.SystemCodeMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemCodeMasterRepository : JpaRepository<SystemCodeMaster, Long> {

    fun findByExternalKey(externalKey: String): SystemCodeMaster?
}
