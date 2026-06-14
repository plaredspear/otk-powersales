package com.otoki.powersales.domain.org.organization.service

import com.otoki.powersales.platform.common.config.CacheConfig
import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceCommand
import com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceResult
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 조직 마스터 REPLACE_ALL 도메인 서비스 (파괴적 전체 교체).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapOrganizeMasterService]
 * - origin spec: #556 (SAP 조직 마스터 인바운드) — 어댑터/도메인 분리: #635 P3-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<OrganizationReplaceCommand>` — 전량 교체 입력 (UPSERT 가 아님).
 * 2. 트랜잭션 advisory lock 획득 ([acquireOrganizationLock]) — 동일 인터페이스 동시 호출 직렬화.
 * 3. 외부 호출:
 *    - [OrganizationRepository.deleteAllInBatch] — 기존 행 전체 삭제.
 *    - [OrganizationRepository.flush] — DELETE flush.
 *    - [OrganizationRepository.saveAll] — 신규 행 일괄 INSERT.
 * 4. ConstraintViolation 등 적재 도중 throw → `@Transactional` 롤백으로 기존 데이터 복원, 호출자(어댑터) 가 catch.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * 페이로드 형식 검증 (`item.isAllNull()` 행 거부) 은 어댑터 책임으로 잔류한다.
 * `@SapDestructiveEndpoint(threshold=20)` AOP 도 어댑터 메서드 위치 보존.
 * `sap.*` 패키지 의존 0건.
 */
@Service
class OrganizationReplaceService(
    private val organizationRepository: OrganizationRepository,
    private val entityManager: EntityManager
) {

    /**
     * Organization 전체 교체 + 캐시 무효화.
     *
     * `@CacheEvict` 가 SAP daily sync 직후 [CacheConfig.CACHE_ORGANIZATION_CASCADE] /
     * [CacheConfig.CACHE_TEAM_SCHEDULE_BRANCHES] 의 모든 entry 를 즉시 무효화한다.
     * Organization row 가 전량 교체되었으므로 어떤 cascade lookup / branch 옵션 결과도
     * stale 가능성이 있어 `allEntries = true`.
     *
     * `beforeInvocation = false` (기본) — 트랜잭션 커밋 후 evict. 적재 실패 시 evict 도 안 일어나
     * 기존 캐시 보존 (정합 우선). 단 트랜잭션 advisory lock 안에서 동작하므로 동시 호출은 직렬화됨.
     *
     * NoOp profile (test / local) 에서는 evict 가 무동작 — 캐시 자체가 없으므로 시맨틱 동등.
     */
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.CACHE_ORGANIZATION_CASCADE], allEntries = true),
            CacheEvict(value = [CacheConfig.CACHE_TEAM_SCHEDULE_BRANCHES], allEntries = true),
        ]
    )
    @Transactional
    fun replaceAll(commands: List<OrganizationReplaceCommand>): OrganizationReplaceResult {
        acquireOrganizationLock()
        organizationRepository.deleteAllInBatch()
        organizationRepository.flush()
        val saved = organizationRepository.saveAll(commands.map { it.toEntity() })
        return OrganizationReplaceResult(replacedCount = saved.count())
    }

    private fun acquireOrganizationLock() {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
            .setParameter("key", ORGANIZATION_LOCK_KEY)
            .singleResult
    }

    private fun OrganizationReplaceCommand.toEntity(): Organization = Organization(
        costCenterLevel2 = ccCd2,
        orgCodeLevel2 = orgCd2,
        orgNameLevel2 = orgNm2,
        costCenterLevel3 = ccCd3,
        orgCodeLevel3 = orgCd3,
        orgNameLevel3 = orgNm3,
        costCenterLevel4 = ccCd4,
        orgCodeLevel4 = orgCd4,
        orgNameLevel4 = orgNm4,
        costCenterLevel5 = ccCd5,
        orgCodeLevel5 = orgCd5,
        orgNameLevel5 = orgNm5
    )

    companion object {
        // pg_advisory_xact_lock 키. 스펙 #556 에서 결정 (D2): Organization 테이블 식별자 1개로 고정.
        private const val ORGANIZATION_LOCK_KEY: Long = 5560001L
    }
}
