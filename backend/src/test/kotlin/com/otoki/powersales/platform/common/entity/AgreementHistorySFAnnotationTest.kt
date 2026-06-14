package com.otoki.powersales.platform.common.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.platform.common.entity.AgreementHistory
import com.otoki.powersales.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * AgreementHistory ↔ Salesforce `AgreementHistory__c` 어노테이션 검증.
 *
 * - Spec #629/#706 — 기본 매핑 + Group A audit R-2
 * - sf-meta-diff 후속 — OwnerId polymorphic R-2 (User / Group XOR) + audit FK Employee → User 전환
 *
 * 단일 권위: Salesforce Object 메타 (`AgreementHistory__c`)
 */
@DisplayName("AgreementHistory SF 어노테이션 검증")
class AgreementHistorySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AgreementHistory__c'")
        fun sfObjectValue() {
            val annotation = AgreementHistory::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("AgreementHistory__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 11 (9 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)
            assertThat(mapping).hasSize(11)
        }
    }

    @Nested
    @DisplayName("AC1 — PK·FK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(agreementWordId) 필드에 @SFField 미부착")
        fun agreementWordIdHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("agreementWordId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 agreement_history_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)
            assertThat(mapping.values).doesNotContain("agreement_history_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (9개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)

        @Test
        @DisplayName("기존 4개 SF API Name → 컬럼명 1:1")
        fun existingMappingValues() {
            assertThat(mapping["AgreementFlag__c"]).isEqualTo("agreement_flag")
            assertThat(mapping["AgreementDate__c"]).isEqualTo("agreement_date")
            assertThat(mapping["AgreementWordId__c"]).isEqualTo("agreement_word_sfid")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
        }

        @Test
        @DisplayName("Spec #706 신규 5개: IsDeleted / Name / OwnerId / CreatedById / LastModifiedById")
        fun spec706NewMappingValues() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }

    @Nested
    @DisplayName("AC2 — Spec #706 IsDeleted @SFField 부착")
    inner class IsDeletedAnnotation {

        @Test
        @DisplayName("isDeleted 필드에 @SFField('IsDeleted') 부착")
        fun isDeletedHasSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("isDeleted")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isTrue()
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("IsDeleted")
        }
    }

    @Nested
    @DisplayName("sf-meta-diff 후속 — OwnerId polymorphic + audit FK User")
    inner class PolymorphicOwnerAndUserAudit {

        @Test
        @DisplayName("ownerUser FK 타입 = User (polymorphic User 분기)")
        fun ownerUserIsUserType() {
            val field = AgreementHistory::class.java.getDeclaredField("ownerUser")
            assertThat(field.type).isEqualTo(User::class.java)
        }

        @Test
        @DisplayName("ownerGroup FK 타입 = Group (polymorphic Group 분기)")
        fun ownerGroupIsGroupType() {
            val field = AgreementHistory::class.java.getDeclaredField("ownerGroup")
            assertThat(field.type).isEqualTo(Group::class.java)
        }

        @Test
        @DisplayName("createdBy / lastModifiedBy FK 타입 = User (Employee → User 전환)")
        fun auditFkIsUserType() {
            val createdBy = AgreementHistory::class.java.getDeclaredField("createdBy")
            val lastModifiedBy = AgreementHistory::class.java.getDeclaredField("lastModifiedBy")
            assertThat(createdBy.type).isEqualTo(User::class.java)
            assertThat(lastModifiedBy.type).isEqualTo(User::class.java)
        }

        @Test
        @DisplayName("기존 owner: Employee? FK 제거 — 필드 부재")
        fun legacyOwnerFieldRemoved() {
            val declared = AgreementHistory::class.java.declaredFields.map { it.name }
            assertThat(declared).doesNotContain("owner")
        }
    }
}
