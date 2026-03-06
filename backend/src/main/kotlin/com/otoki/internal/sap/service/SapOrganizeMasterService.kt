package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapOrganizeMasterRequest
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SapOrganizeMasterService(
    private val orgRepository: OrgRepository
) : SapSyncService<SapOrganizeMasterRequest.ReqItem> {

    @Transactional
    override fun sync(items: List<SapOrganizeMasterRequest.ReqItem>): SapSyncResult {
        orgRepository.deleteAllInBatch()

        val entities = items.map { item ->
            Org(
                costCenterLevel2 = item.ccCd2,
                orgCodeLevel2 = item.orgCd2,
                orgNameLevel2 = item.orgNm2,
                costCenterLevel3 = item.ccCd3,
                orgCodeLevel3 = item.orgCd3,
                orgNameLevel3 = item.orgNm3,
                costCenterLevel4 = item.ccCd4,
                orgCodeLevel4 = item.orgCd4,
                orgNameLevel4 = item.orgNm4,
                costCenterLevel5 = item.ccCd5,
                orgCodeLevel5 = item.orgCd5,
                orgNameLevel5 = item.orgNm5
            )
        }

        orgRepository.saveAll(entities)

        return SapSyncResult(
            successCount = items.size,
            failCount = 0
        )
    }
}
