package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemCodeMasterRepository : JpaRepository<SystemCodeMaster, Long> {

    fun findByExternalKey(externalKey: String): SystemCodeMaster?

    fun findByGroupCodeIn(groupCodes: List<String>): List<SystemCodeMaster>
}
