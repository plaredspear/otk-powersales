package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapClaimRequest
import com.otoki.internal.sap.dto.SapClaimResponse
import com.otoki.internal.sap.repository.TmpClaimRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapClaimService(
    private val tmpClaimRepository: TmpClaimRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun syncClaim(item: SapClaimRequest.ClaimItem): SapClaimResponse {
        val name = item.name
            ?: return SapClaimResponse.error("name is required")

        val existing = tmpClaimRepository.findByClaimName(name)
            ?: return SapClaimResponse.error("해당 클레임을 찾을 수 없습니다: $name")

        existing.claimSequence = item.claimSequence
        existing.actionCode = item.actionCode
        existing.claimStatus = item.claimStatus
        existing.claimContent = item.content
        existing.reasonType = item.reasonType
        existing.cosmosKey = item.cosmosKey
        existing.updDate = LocalDateTime.now()

        tmpClaimRepository.save(existing)
        log.info("클레임 동기화 성공: claimName={}", name)

        return SapClaimResponse.success()
    }
}
