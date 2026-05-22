package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.auth.sharing.dto.ProfileFlagsSnapshot
import com.querydsl.core.types.dsl.EntityPathBase
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SharingRulePolicyEvaluator 단위 테스트 (spec #782 P3-B).
 *
 * Repository 통합 (QueryDSL 실행) 은 통합 테스트 단계에서 검증. 본 테스트는 evaluator 의 분기 로직만 검증.
 */
@DisplayName("SharingRulePolicyEvaluator 단위 테스트")
class SharingRulePolicyEvaluatorTest {

    private val evaluator = SharingRulePolicyEvaluator()
    private val entityPath = mockk<EntityPathBase<*>>(relaxed = true)

    @Nested
    @DisplayName("hasUnrestrictedAccess — 우선순위 1")
    inner class UnrestrictedAccess {

        @Test
        @DisplayName("Profile.viewAllData true 면 unrestricted")
        fun profileViewAllData() {
            val scope = DataScope(
                branchCodes = emptyList(),
                isAllBranches = false,
                profileFlags = ProfileFlagsSnapshot(
                    viewAllData = true,
                    modifyAllData = false,
                    viewAllUsers = false,
                    manageUsers = false,
                    apiEnabled = false,
                ),
            )
            assertThat(evaluator.hasUnrestrictedAccess(scope, "Account")).isTrue
        }

        @Test
        @DisplayName("PermissionSet.viewAllRecords[Account] true 면 unrestricted")
        fun permissionSetViewAllRecords() {
            val scope = DataScope(
                branchCodes = emptyList(),
                isAllBranches = false,
                permissionSetFlags = PermissionSetSnapshot(
                    viewAllDataSystem = false,
                    modifyAllDataSystem = false,
                    viewAllRecordsBySObject = mapOf("Account" to true),
                    modifyAllRecordsBySObject = emptyMap(),
                ),
            )
            assertThat(evaluator.hasUnrestrictedAccess(scope, "Account")).isTrue
            // 다른 SObject 는 false
            assertThat(evaluator.hasUnrestrictedAccess(scope, "DKRetail__Promotion__c")).isFalse
        }

        @Test
        @DisplayName("모든 flag false 면 not unrestricted")
        fun noneTrue() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            assertThat(evaluator.hasUnrestrictedAccess(scope, "Account")).isFalse
        }
    }

    @Nested
    @DisplayName("sfApiNameToJpaProperty — SF API name → JPA property 변환")
    inner class SfApiNameMapping {

        @Test
        @DisplayName("CostCenterCode__c → costCenterCode")
        fun costCenterCode() {
            assertThat(evaluator.sfApiNameToJpaProperty("CostCenterCode__c")).isEqualTo("costCenterCode")
        }

        @Test
        @DisplayName("AccountGroup__c → accountGroup")
        fun accountGroup() {
            assertThat(evaluator.sfApiNameToJpaProperty("AccountGroup__c")).isEqualTo("accountGroup")
        }

        @Test
        @DisplayName("CreatedById → createdById (audit 컬럼)")
        fun createdById() {
            assertThat(evaluator.sfApiNameToJpaProperty("CreatedById")).isEqualTo("createdById")
        }

        @Test
        @DisplayName("BranchCode__c → branchCode")
        fun branchCode() {
            assertThat(evaluator.sfApiNameToJpaProperty("BranchCode__c")).isEqualTo("branchCode")
        }

        @Test
        @DisplayName("HR_Code_c__c → hrCode (운영 데이터 정합)")
        fun hrCode() {
            // HR_Code_c__c 의 변형 — `_c` suffix 추가 제거 + 언더스코어 처리
            assertThat(evaluator.sfApiNameToJpaProperty("HR_Code_c__c")).isEqualTo("hRCode")
        }
    }

    @Nested
    @DisplayName("ControlledByParent — 우선순위 6")
    inner class ControlledByParentCheck {

        @Test
        @DisplayName("PromotionEmployee 는 ControlledByParent + parent = Promotion")
        fun promotionEmployee() {
            assertThat(evaluator.isControlledByParent("DKRetail__PromotionEmployee__c")).isTrue
            assertThat(evaluator.parentSObjectOf("DKRetail__PromotionEmployee__c")).isEqualTo("DKRetail__Promotion__c")
        }

        @Test
        @DisplayName("Account 는 Private — ControlledByParent 아님")
        fun account() {
            assertThat(evaluator.isControlledByParent("Account")).isFalse
            assertThat(evaluator.parentSObjectOf("Account")).isNull()
        }

        @Test
        @DisplayName("Promotion 자체는 Private — 부모 없음")
        fun promotion() {
            assertThat(evaluator.isControlledByParent("DKRetail__Promotion__c")).isFalse
        }
    }
}
