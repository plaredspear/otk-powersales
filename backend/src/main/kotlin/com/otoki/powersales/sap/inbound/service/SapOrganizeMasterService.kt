package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.sanity.SapDestructiveEndpoint
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterDetail
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequestItem
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.organization.repository.OrganizationRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SapOrganizeMasterService(
    private val organizationRepository: OrganizationRepository,
    private val entityManager: EntityManager
) {

    /**
     * SAP 조직 마스터 전량 교체.
     *
     * 1. 트랜잭션 advisory lock 획득 (동일 인터페이스 동시 호출 직렬화)
     * 2. 행 전체 null 검증 (실패 시 INVALID_PAYLOAD)
     * 3. 기존 Organization 전체 삭제
     * 4. 신규 Organization 일괄 INSERT
     *
     * sanity check (받은 건수 0 / ±20% 변동) 는 [SapDestructiveEndpoint] AOP 가 트랜잭션 진입 전에 처리.
     */
    @Transactional
    @SapDestructiveEndpoint(threshold = 20, countArgName = "items")
    fun replaceAll(items: List<OrganizeMasterRequestItem>): OrganizeMasterDetail {
        acquireOrganizationLock()
        validateItems(items)
        organizationRepository.deleteAllInBatch()
        organizationRepository.flush()
        val saved = organizationRepository.saveAll(items.map { it.toEntity() })
        return OrganizeMasterDetail(
            successCount = saved.count(),
            failureCount = 0,
            failures = emptyList()
        )
    }

    private fun validateItems(items: List<OrganizeMasterRequestItem>) {
        items.forEachIndexed { index, item ->
            if (item.isAllNull()) {
                throw SapInvalidPayloadException("필수 필드 누락 (line ${index + 1})")
            }
        }
    }

    private fun acquireOrganizationLock() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
            .setParameter("key", ORGANIZATION_LOCK_KEY)
            .singleResult
    }

    companion object {
        // pg_advisory_xact_lock 키. 스펙 #556 에서 결정 (D2): Organization 테이블 식별자 1개로 고정.
        private const val ORGANIZATION_LOCK_KEY: Long = 5560001L
    }
}
