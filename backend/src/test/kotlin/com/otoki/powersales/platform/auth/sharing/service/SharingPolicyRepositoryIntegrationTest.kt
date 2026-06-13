package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.entity.SObjectRelation
import com.otoki.powersales.platform.auth.sharing.entity.SObjectSetting
import com.otoki.powersales.platform.auth.sharing.repository.SObjectRelationRepository
import com.otoki.powersales.platform.auth.sharing.repository.SObjectSettingRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * P4-B 의 Repository 통합 패턴 sObjectName 정합 + OWD/parent 추론 cross-check (spec #782 P4-B, #791).
 *
 * SfSharingConstants const map 에서 `SObjectSettingProvider` (DB-driven) 로 전환. 실 DB 통합
 * (QueryDSL fetch) 은 testcontainers 도입 후 별도 검증하고, 본 테스트는 운영 seed
 * (extract-sharing-meta.sh 산출분) 을 모사한 repository mock 위에서 provider 의 OWD/
 * ControlledByParent/parent 추론 계약을 P4-B 적용 대상 SObject 기준으로 검증하는 안전망이다.
 */
@DisplayName("P4-B SObject 매핑 cross-check — SObjectSettingProvider")
class SharingPolicyRepositoryIntegrationTest {

    private val settingRepository = mockk<SObjectSettingRepository>()
    private val relationRepository = mockk<SObjectRelationRepository>()
    private val provider = SObjectSettingProvider(settingRepository, relationRepository)

    // 운영 seed (CSV 적재분) 을 모사한 고정 데이터. provider 를 직접 생성하므로 @Cacheable 프록시
    // 없이 mock repository 로 바로 위임된다.
    private val settings = listOf(
        SObjectSetting(sObjectName = "Account", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "BranchReview__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "DisplayWorkScheduleMaster__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "HQReview__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "SalesProgressRateMaster__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "DKRetail__Promotion__c", orgWideDefault = "Private"),
        SObjectSetting(sObjectName = "DKRetail__PromotionEmployee__c", orgWideDefault = "ControlledByParent"),
    ).associateBy { it.sObjectName }

    private val relations = listOf(
        SObjectRelation(
            childSObjectName = "DKRetail__PromotionEmployee__c",
            parentSObjectName = "DKRetail__Promotion__c",
            relationFieldName = "DKRetail__Promotion__c",
            isMasterDetail = true,
        ),
    ).groupBy { it.childSObjectName }

    @BeforeEach
    fun setUp() {
        every { settingRepository.findBySObjectName(any()) } answers { settings[firstArg<String>()] }
        every { relationRepository.findAllByChildSObjectName(any()) } answers {
            relations[firstArg<String>()] ?: emptyList()
        }
    }

    @Test
    @DisplayName("P4-B 적용 대상 3 SObject — OWD Private 정합")
    fun appliedSObjectsArePrivate() {
        val targets = listOf(
            "Account",
            "DisplayWorkScheduleMaster__c",
            "MonthlyFemaleEmployeeIntegrationSchedule__c",
        )
        targets.forEach { sObject ->
            assertThat(provider.orgWideDefault(sObject))
                .describedAs("P4-B 적용 대상 %s OWD", sObject)
                .isEqualTo("Private")
            assertThat(provider.isControlledByParent(sObject))
                .describedAs("%s 는 Private (ControlledByParent 아님)", sObject)
                .isFalse
        }
    }

    @Test
    @DisplayName("PromotionEmployee — ControlledByParent + parent = Promotion")
    fun promotionEmployeeControlledByParent() {
        assertThat(provider.isControlledByParent("DKRetail__PromotionEmployee__c")).isTrue
        assertThat(provider.parentSObjectOf("DKRetail__PromotionEmployee__c"))
            .isEqualTo("DKRetail__Promotion__c")
    }

    @Test
    @DisplayName("Promotion 자체 — Private (sharingRule 본문은 빈 파일이지만 OWD 는 Private)")
    fun promotionPrivate() {
        assertThat(provider.orgWideDefault("DKRetail__Promotion__c")).isEqualTo("Private")
        assertThat(provider.isControlledByParent("DKRetail__Promotion__c")).isFalse
    }

    @Test
    @DisplayName("P4-B 비범위 3 SObject — entity 부재로 본 spec 미적용. 도메인 spec 책임")
    fun nonAppliedSObjectsArePrivateButOutOfScope() {
        // BranchReview / HQReview / SalesProgressRateMaster — OWD Private 이지만 entity 부재로
        // P4-B 비범위 (spec.md Q6 옵션 3 / v1.4). 각 도메인 spec 신설 시 entity + sharing policy 동시 부여.
        val nonAppliedTargets = listOf(
            "BranchReview__c",
            "HQReview__c",
            "SalesProgressRateMaster__c",
        )
        nonAppliedTargets.forEach { sObject ->
            assertThat(provider.orgWideDefault(sObject))
                .describedAs("비범위 %s 의 OWD", sObject)
                .isEqualTo("Private")
        }
    }

    @Test
    @DisplayName("미적재 SObject — fallback Private (Q1 옵션 1), findOrNull 은 null")
    fun unmanagedSObject() {
        // const map 시절엔 null 반환이었으나, provider 는 미적재 sObject 를 보수적으로 Private fallback.
        assertThat(provider.findOrNull("NonExistent__c")).isNull()
        assertThat(provider.orgWideDefault("NonExistent__c")).isEqualTo(SObjectSettingProvider.FALLBACK_OWD)
        assertThat(provider.isControlledByParent("NonExistent__c")).isFalse
        assertThat(provider.parentSObjectOf("NonExistent__c")).isNull()
    }

    @Test
    @DisplayName("DataScope.copy 패턴 — backward compat 검증 (기존 branchCodes / isAllBranches 유지)")
    fun dataScopeBackwardCompat() {
        val legacyScope = DataScope(
            branchCodes = listOf("GHA01", "GHA02"),
            isAllBranches = false,
        )
        // 기존 helper 메서드 동작 — 신규 차원 default value 무관.
        assertThat(legacyScope.validateAccess("GHA01")).isTrue
        assertThat(legacyScope.validateAccess("GHB99")).isFalse

        // 신규 차원이 default 인 상태에서도 hasUnrestrictedAccess 평가 — Profile/PermissionSet 모두 NONE
        val evaluator = SharingRulePolicyEvaluator(mockk(relaxed = true))
        assertThat(evaluator.hasUnrestrictedAccess(legacyScope, "Account")).isFalse
    }
}
