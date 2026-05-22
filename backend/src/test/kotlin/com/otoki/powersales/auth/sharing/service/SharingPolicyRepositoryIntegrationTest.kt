package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.SfSharingConstants
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * P4-B 의 Repository 통합 패턴 sObjectName 정합 + SfSharingConstants cross-check (spec #782 P4-B).
 *
 * 실 DB 통합 (QueryDSL fetch) 은 testcontainers 도입 후 별도 검증. 본 테스트는 P4-B 의 적용 대상
 * 3 SObject + ControlledByParent 매핑이 SfSharingConstants 와 일치하는지 안전망.
 */
@DisplayName("P4-B Repository 통합 패턴 — SObject 매핑 cross-check")
class SharingPolicyRepositoryIntegrationTest {

    @Test
    @DisplayName("P4-B 적용 대상 3 SObject — SF_SHARING_MODEL 의 Private 정합")
    fun appliedSObjectsArePrivate() {
        val targets = listOf(
            "Account",
            "DisplayWorkScheduleMaster__c",
            "MonthlyFemaleEmployeeIntegrationSchedule__c",
        )
        targets.forEach { sObject ->
            assertThat(SfSharingConstants.SF_SHARING_MODEL[sObject])
                .describedAs("P4-B 적용 대상 %s sharingModel", sObject)
                .isEqualTo("Private")
            assertThat(SfSharingConstants.isControlledByParent(sObject))
                .describedAs("%s 는 Private (ControlledByParent 아님)", sObject)
                .isFalse
        }
    }

    @Test
    @DisplayName("PromotionEmployee — ControlledByParent + parent = Promotion")
    fun promotionEmployeeControlledByParent() {
        assertThat(SfSharingConstants.isControlledByParent("DKRetail__PromotionEmployee__c")).isTrue
        assertThat(SfSharingConstants.parentOf("DKRetail__PromotionEmployee__c"))
            .isEqualTo("DKRetail__Promotion__c")
    }

    @Test
    @DisplayName("Promotion 자체 — Private (sharingRule 본문은 빈 파일이지만 sharingModel 는 Private)")
    fun promotionPrivate() {
        assertThat(SfSharingConstants.SF_SHARING_MODEL["DKRetail__Promotion__c"]).isEqualTo("Private")
        assertThat(SfSharingConstants.isControlledByParent("DKRetail__Promotion__c")).isFalse
    }

    @Test
    @DisplayName("P4-B 비범위 3 SObject — entity 부재로 본 spec 미적용. 도메인 spec 책임")
    fun nonAppliedSObjectsArePrivateButOutOfScope() {
        // BranchReview / HQReview / SalesProgressRateMaster — sharingModel Private 이지만 entity 부재로
        // P4-B 비범위 (spec.md Q6 옵션 3 / v1.4). 각 도메인 spec 신설 시 entity + sharing policy 동시 부여.
        val nonAppliedTargets = listOf(
            "BranchReview__c",
            "HQReview__c",
            "SalesProgressRateMaster__c",
        )
        nonAppliedTargets.forEach { sObject ->
            assertThat(SfSharingConstants.SF_SHARING_MODEL[sObject])
                .describedAs("비범위 %s 의 sharingModel", sObject)
                .isEqualTo("Private")
        }
    }

    @Test
    @DisplayName("관리되지 않은 SObject — null 반환 (default Private 가정 아님 — 명시 매핑 우선)")
    fun unmanagedSObject() {
        assertThat(SfSharingConstants.SF_SHARING_MODEL["NonExistent__c"]).isNull()
        assertThat(SfSharingConstants.isControlledByParent("NonExistent__c")).isFalse
        assertThat(SfSharingConstants.parentOf("NonExistent__c")).isNull()
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
