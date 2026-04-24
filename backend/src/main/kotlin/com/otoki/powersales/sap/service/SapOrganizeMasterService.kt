package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.dto.SapOrganizeMasterRequest
import com.otoki.powersales.sap.dto.SapSyncResult
import com.otoki.powersales.sap.entity.Organization
import com.otoki.powersales.sap.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SapOrganizeMasterService(
    private val organizationRepository: OrganizationRepository
) : SapSyncService<SapOrganizeMasterRequest.ReqItem> {

    @Transactional
    override fun sync(items: List<SapOrganizeMasterRequest.ReqItem>): SapSyncResult {
        organizationRepository.deleteAllInBatch()

        val entities = items.map { item ->
            Organization(
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

        organizationRepository.saveAll(entities)

        return SapSyncResult(
            successCount = items.size,
            failCount = 0
        )
    }
}
