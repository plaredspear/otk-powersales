package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.entity.SObjectSetting
import com.otoki.powersales.auth.sharing.repository.SObjectRelationRepository
import com.otoki.powersales.auth.sharing.repository.SObjectSettingRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * SF OWD + role hierarchy 옵트인 + master-detail parent 추론 도우미 (spec #791).
 *
 * `SfSharingConstants` 의 const map 을 DB-driven 으로 전환. 본 provider 가 단일 진입점.
 *
 * ## 결정 사항 정합
 * - Q1 (옵션 1): row 미적재 sObject 의 fallback = Private (가장 보수적)
 * - Q2 (옵션 1): parent SObject 추론은 `sobject_relation` 테이블에서 조회
 * - Q3 (옵션 1): SF retrieve XML 의 OWD 값 그대로 박제 (extract-sharing-meta.sh 산출 CSV 적재)
 * - Q4 (옵션 1): externalSharingModel 미적재 — internal 만
 */
@Service
class SObjectSettingProvider(
    private val sObjectSettingRepository: SObjectSettingRepository,
    private val sObjectRelationRepository: SObjectRelationRepository,
) {

    /**
     * sObject 의 OWD 값.
     *
     * 미적재 sObject 는 `Private` fallback (Q1 옵션 1). DB row 갱신 시 cache evict 는 #792 의
     * sharing recalc endpoint 가 일괄 처리.
     */
    @Cacheable(cacheNames = ["sobject-setting:v2"], key = "#sObjectName")
    fun orgWideDefault(sObjectName: String): String =
        sObjectSettingRepository.findBySObjectName(sObjectName)?.orgWideDefault ?: FALLBACK_OWD

    /**
     * sObject 의 hierarchy 옵트인 여부.
     *
     * 미적재 sObject 는 SF 기본값 true (대부분 sObject) — User / UserRole 같은 일부만 false 인데
     * 본 프로젝트 운영에서 hierarchy 비활성 sObject 가 운영 의도가 있으면 명시적 false 박제 필요.
     */
    @Cacheable(cacheNames = ["sobject-setting:v2"], key = "'hier:' + #sObjectName")
    fun allowHierarchyGrant(sObjectName: String): Boolean =
        sObjectSettingRepository.findBySObjectName(sObjectName)?.allowHierarchyGrant ?: true

    /**
     * ControlledByParent sObject 의 parent sObject 추론 (Q2 옵션 1).
     *
     * sobject_relation 테이블에서 master-detail relationship 1건 조회. 다중 parent 보유는 SF 운영 부재 가정.
     */
    @Cacheable(cacheNames = ["sobject-setting:v2"], key = "'parent:' + #childSObjectName")
    fun parentSObjectOf(childSObjectName: String): String? =
        sObjectRelationRepository.findAllByChildSObjectName(childSObjectName)
            .firstOrNull { it.isMasterDetail }
            ?.parentSObjectName

    fun isControlledByParent(sObjectName: String): Boolean =
        orgWideDefault(sObjectName) == OWD_CONTROLLED_BY_PARENT

    fun findOrNull(sObjectName: String): SObjectSetting? =
        sObjectSettingRepository.findBySObjectName(sObjectName)

    fun findAll(): List<SObjectSetting> = sObjectSettingRepository.findAll()

    companion object {
        const val FALLBACK_OWD = "Private"
        const val OWD_PRIVATE = "Private"
        const val OWD_PUBLIC_READ_ONLY = "PublicReadOnly"
        const val OWD_PUBLIC_READ_WRITE = "PublicReadWrite"
        const val OWD_CONTROLLED_BY_PARENT = "ControlledByParent"

        /** Custom SObject 운영의 추가 값 — 인벤토리 §2.6 발견 */
        const val OWD_READ = "Read"
        const val OWD_READ_WRITE = "ReadWrite"
    }
}
