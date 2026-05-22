package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.entity.SObjectRelation
import com.otoki.powersales.auth.sharing.entity.SObjectSetting
import com.otoki.powersales.auth.sharing.repository.SObjectRelationRepository
import com.otoki.powersales.auth.sharing.repository.SObjectSettingRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SObjectSettingProvider 단위 테스트 (spec #791).
 *
 * - Q1 옵션 1 — 미적재 sObject 의 fallback = Private
 * - Q2 옵션 1 — parent 추론은 sobject_relation 테이블 조회
 * - Q3 옵션 1 — orgWideDefault 가 DB 값 그대로 반환 (PublicReadOnly / ReadWrite 등 모두 통과)
 */
@DisplayName("SObjectSettingProvider — spec #791")
class SObjectSettingProviderTest {

    private val settingRepo = mockk<SObjectSettingRepository>()
    private val relationRepo = mockk<SObjectRelationRepository>()
    private val provider = SObjectSettingProvider(settingRepo, relationRepo)

    @Nested
    @DisplayName("orgWideDefault — Q1 / Q3 옵션 1")
    inner class OrgWideDefault {

        @Test
        @DisplayName("적재된 sObject — DB 값 그대로 반환 (SF XML 박제)")
        fun loaded() {
            every { settingRepo.findBySObjectName("Account") } returns SObjectSetting(
                sObjectName = "Account",
                orgWideDefault = "PublicReadOnly",
                allowHierarchyGrant = true,
            )
            assertThat(provider.orgWideDefault("Account")).isEqualTo("PublicReadOnly")
        }

        @Test
        @DisplayName("미적재 sObject — fallback Private (Q1 옵션 1)")
        fun fallback() {
            every { settingRepo.findBySObjectName("UnknownSObject__c") } returns null
            assertThat(provider.orgWideDefault("UnknownSObject__c")).isEqualTo("Private")
        }

        @Test
        @DisplayName("Custom 운영의 Read / ReadWrite 도 그대로 반환")
        fun customOwd() {
            every { settingRepo.findBySObjectName("Promotion__c") } returns SObjectSetting(
                sObjectName = "Promotion__c",
                orgWideDefault = "Read",
                allowHierarchyGrant = true,
            )
            assertThat(provider.orgWideDefault("Promotion__c")).isEqualTo("Read")
        }
    }

    @Nested
    @DisplayName("allowHierarchyGrant")
    inner class HierarchyGrant {

        @Test
        @DisplayName("적재된 sObject — DB 값 그대로 반환")
        fun loaded() {
            every { settingRepo.findBySObjectName("User") } returns SObjectSetting(
                sObjectName = "User",
                orgWideDefault = "Private",
                allowHierarchyGrant = false,
            )
            assertThat(provider.allowHierarchyGrant("User")).isFalse
        }

        @Test
        @DisplayName("미적재 — SF 기본값 true")
        fun fallback() {
            every { settingRepo.findBySObjectName("Account") } returns null
            assertThat(provider.allowHierarchyGrant("Account")).isTrue
        }
    }

    @Nested
    @DisplayName("parentSObjectOf — Q2 옵션 1")
    inner class ParentLookup {

        @Test
        @DisplayName("master-detail relation 1건 — parent 반환")
        fun masterDetailFound() {
            every { relationRepo.findAllByChildSObjectName("DKRetail__PromotionEmployee__c") } returns listOf(
                SObjectRelation(
                    childSObjectName = "DKRetail__PromotionEmployee__c",
                    parentSObjectName = "DKRetail__Promotion__c",
                    relationFieldName = "Promotion__c",
                    isMasterDetail = true,
                ),
            )
            assertThat(provider.parentSObjectOf("DKRetail__PromotionEmployee__c"))
                .isEqualTo("DKRetail__Promotion__c")
        }

        @Test
        @DisplayName("relation 부재 — null")
        fun noRelation() {
            every { relationRepo.findAllByChildSObjectName("Account") } returns emptyList()
            assertThat(provider.parentSObjectOf("Account")).isNull()
        }

        @Test
        @DisplayName("master-detail 아닌 lookup 만 있는 경우 — null")
        fun lookupOnly() {
            every { relationRepo.findAllByChildSObjectName("ChildLookup__c") } returns listOf(
                SObjectRelation(
                    childSObjectName = "ChildLookup__c",
                    parentSObjectName = "ParentLookup__c",
                    relationFieldName = "Parent__c",
                    isMasterDetail = false,
                ),
            )
            assertThat(provider.parentSObjectOf("ChildLookup__c")).isNull()
        }
    }

    @Nested
    @DisplayName("isControlledByParent")
    inner class ControlledByParent {

        @Test
        @DisplayName("ControlledByParent OWD — true")
        fun controlled() {
            every { settingRepo.findBySObjectName("DKRetail__PromotionEmployee__c") } returns SObjectSetting(
                sObjectName = "DKRetail__PromotionEmployee__c",
                orgWideDefault = "ControlledByParent",
                allowHierarchyGrant = true,
            )
            assertThat(provider.isControlledByParent("DKRetail__PromotionEmployee__c")).isTrue
        }

        @Test
        @DisplayName("Private OWD — false")
        fun privateMode() {
            every { settingRepo.findBySObjectName("Account") } returns SObjectSetting(
                sObjectName = "Account",
                orgWideDefault = "Private",
                allowHierarchyGrant = true,
            )
            assertThat(provider.isControlledByParent("Account")).isFalse
        }
    }
}
