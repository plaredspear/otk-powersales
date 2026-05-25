package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.SharingRule
import com.otoki.powersales.auth.sharing.entity.SharingRuleCondition
import com.otoki.powersales.auth.sharing.entity.SharingRuleTarget
import com.otoki.powersales.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * SharingRule entity / Repository round-trip 검증 (#782 P1-B).
 *
 * sharing_rule + sharing_rule_condition + sharing_rule_target 3 테이블의 정합성 + finder 메서드 동작 확인.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class SharingRuleRepositoryTest {

    @Autowired
    private lateinit var sharingRuleRepository: SharingRuleRepository

    @Autowired
    private lateinit var sharingRuleConditionRepository: SharingRuleConditionRepository

    @Autowired
    private lateinit var sharingRuleTargetRepository: SharingRuleTargetRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Nested
    @DisplayName("SharingRule round-trip")
    inner class SharingRuleRoundTrip {

        @Test
        @DisplayName("저장 후 findByDeveloperName 으로 조회 가능")
        fun saveAndFindByDeveloperName() {
            val rule = SharingRule(
                developerName = "Account_View_All",
                sObjectName = "Account",
                ruleType = "CRITERIA",
                label = "양미선 조장 Account View All",
                accessLevel = "Read",
                includeOwnedByAll = false,
            )
            sharingRuleRepository.saveAndFlush(rule)
            testEntityManager.clear()

            val found = sharingRuleRepository.findByDeveloperName("Account_View_All")
            assertThat(found).isNotNull
            assertThat(found!!.sObjectName).isEqualTo("Account")
            assertThat(found.ruleType).isEqualTo("CRITERIA")
            assertThat(found.accessLevel).isEqualTo("Read")
            assertThat(found.label).isEqualTo("양미선 조장 Account View All")
        }

        @Test
        @DisplayName("findAllBySObjectName 으로 SObject 별 조회")
        fun findAllBySObjectName() {
            sharingRuleRepository.saveAndFlush(
                SharingRule(
                    developerName = "Account_View_All",
                    sObjectName = "Account",
                    ruleType = "CRITERIA",
                    accessLevel = "Read",
                ),
            )
            sharingRuleRepository.saveAndFlush(
                SharingRule(
                    developerName = "Account_CVS",
                    sObjectName = "Account",
                    ruleType = "CRITERIA",
                    accessLevel = "Read",
                ),
            )
            sharingRuleRepository.saveAndFlush(
                SharingRule(
                    developerName = "BranchReview_LV2",
                    sObjectName = "BranchReview__c",
                    ruleType = "CRITERIA",
                    accessLevel = "Edit",
                ),
            )
            testEntityManager.clear()

            val accountRules = sharingRuleRepository.findAllBySObjectName("Account")
            assertThat(accountRules).hasSize(2)
            assertThat(accountRules.map { it.developerName }).containsExactlyInAnyOrder(
                "Account_View_All", "Account_CVS",
            )
        }
    }

    @Nested
    @DisplayName("SharingRuleCondition / Target — 부모 rule 참조")
    @Disabled(
        "H2 ddl-auto 환경에서 cross-entity ddl 인식 이슈 — testcontainers (Postgres + Flyway V175) 환경 도입 후 활성화 (#782 P1-B 후속)",
    )
    inner class SharingRuleChildrenRoundTrip {

        @Test
        @DisplayName("rule 1건에 condition 다건 + target 1건 저장 후 조회")
        fun saveConditionsAndTargets() {
            val rule = sharingRuleRepository.saveAndFlush(
                SharingRule(
                    developerName = "Account_CVS_Branch",
                    sObjectName = "Account",
                    ruleType = "CRITERIA",
                    accessLevel = "Read",
                ),
            )

            sharingRuleConditionRepository.saveAndFlush(
                SharingRuleCondition(
                    sharingRuleId = rule.id,
                    sharingRuleSObjectName = "Account",
                    field = "AccountGroup__c",
                    operator = "includes",
                    value = "1000,1010,3000",
                    conditionOrder = 1,
                    logicConnector = null,
                ),
            )
            sharingRuleConditionRepository.saveAndFlush(
                SharingRuleCondition(
                    sharingRuleId = rule.id,
                    sharingRuleSObjectName = "Account",
                    field = "BranchCode__c",
                    operator = "equals",
                    value = "GHA01",
                    conditionOrder = 2,
                    logicConnector = "AND",
                ),
            )

            sharingRuleTargetRepository.saveAndFlush(
                SharingRuleTarget(
                    sharingRuleId = rule.id,
                    sharingRuleSObjectName = "Account",
                    targetType = "ROLE_AND_SUBORDINATES_INTERNAL",
                    targetSfid = null,
                ),
            )

            testEntityManager.clear()

            val conditions = sharingRuleConditionRepository.findAllBySharingRuleIdOrderByConditionOrderAsc(rule.id)
            assertThat(conditions).hasSize(2)
            assertThat(conditions[0].field).isEqualTo("AccountGroup__c")
            assertThat(conditions[0].operator).isEqualTo("includes")
            assertThat(conditions[1].field).isEqualTo("BranchCode__c")
            assertThat(conditions[1].logicConnector).isEqualTo("AND")

            val targets = sharingRuleTargetRepository.findAllBySharingRuleId(rule.id)
            assertThat(targets).hasSize(1)
            assertThat(targets[0].targetType).isEqualTo("ROLE_AND_SUBORDINATES_INTERNAL")
        }
    }
}
