package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Org
import java.time.LocalDateTime

data class OrgResponse(
    val id: Long,
    val costCenterLevel2: String?,
    val orgCodeLevel2: String?,
    val orgNameLevel2: String?,
    val costCenterLevel3: String?,
    val orgCodeLevel3: String?,
    val orgNameLevel3: String?,
    val costCenterLevel4: String?,
    val orgCodeLevel4: String?,
    val orgNameLevel4: String?,
    val costCenterLevel5: String?,
    val orgCodeLevel5: String?,
    val orgNameLevel5: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: Org): OrgResponse = OrgResponse(
            id = entity.id,
            costCenterLevel2 = entity.costCenterLevel2,
            orgCodeLevel2 = entity.orgCodeLevel2,
            orgNameLevel2 = entity.orgNameLevel2,
            costCenterLevel3 = entity.costCenterLevel3,
            orgCodeLevel3 = entity.orgCodeLevel3,
            orgNameLevel3 = entity.orgNameLevel3,
            costCenterLevel4 = entity.costCenterLevel4,
            orgCodeLevel4 = entity.orgCodeLevel4,
            orgNameLevel4 = entity.orgNameLevel4,
            costCenterLevel5 = entity.costCenterLevel5,
            orgCodeLevel5 = entity.orgCodeLevel5,
            orgNameLevel5 = entity.orgNameLevel5,
            createdAt = entity.createdAt
        )
    }
}
