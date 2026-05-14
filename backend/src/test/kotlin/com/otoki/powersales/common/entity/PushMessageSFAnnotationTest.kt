package com.otoki.powersales.common.entity

import com.otoki.powersales.common.enums.PushMessageBranch
import com.otoki.powersales.common.enums.PushMessageBranchCode
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #709 — PushMessage ↔ Salesforce `PushMessage__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`PushMessage__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject 무변경
 *   - AC2: @SFField 매핑 키셋 (13개 — 기존 7 + Spec #709 신규 4 + BaseEntity 2)
 *   - AC3: PK 미부착
 *   - AC4: Picklist 필드 타입 (Branch__c / BranchCode__c)
 *   - AC5: Reference R-2 sfid 컬럼 (OwnerId / CreatedById / LastModifiedById)
 */
@DisplayName("PushMessage SF 어노테이션 검증 (Spec #709)")
class PushMessageSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'PushMessage__c'")
        fun sfObjectValue() {
            val annotation = PushMessage::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("PushMessage__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (13개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)

        @Test
        @DisplayName("매핑 키 수 = 13 (기존 7 + Spec #709 신규 4 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(13)
        }

        @Test
        @DisplayName("기존 7개 필드 매핑 무변경 (Spec #615)")
        fun existingFields() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Message__c"]).isEqualTo("message")
            assertThat(mapping["ScheduleDate__c"]).isEqualTo("schedule_date")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["Branch__c"]).isEqualTo("branch")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["SObjectRecordId__c"]).isEqualTo("s_object_record_id")
        }

        @Test
        @DisplayName("Spec #709 신규 4개 (Group A IsDeleted + R-2 sfid)")
        fun spec709NewFields() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("BaseEntity 2개 (CreatedDate / LastModifiedDate)")
        fun baseEntityFields() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC3 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = PushMessage::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 push_message_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)
            assertThat(mapping.values).doesNotContain("push_message_id")
        }
    }

    @Nested
    @DisplayName("AC4 — Picklist 필드 타입")
    inner class PicklistFieldTypes {

        @Test
        @DisplayName("Branch__c 필드 타입은 PushMessageBranch")
        fun branchFieldType() {
            val field = PushMessage::class.java.getDeclaredField("branch")
            assertThat(field.type).isEqualTo(PushMessageBranch::class.java)
        }

        @Test
        @DisplayName("BranchCode__c 필드 타입은 PushMessageBranchCode")
        fun branchCodeFieldType() {
            val field = PushMessage::class.java.getDeclaredField("branchCode")
            assertThat(field.type).isEqualTo(PushMessageBranchCode::class.java)
        }
    }

    @Nested
    @DisplayName("AC5 — Reference R-2 sfid 컬럼")
    inner class ReferenceSfidColumns {

        @Test
        @DisplayName("owner_sfid 컬럼 — OwnerId 매핑")
        fun ownerSfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
        }

        @Test
        @DisplayName("created_by_sfid 컬럼 — CreatedById 매핑")
        fun createdBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
        }

        @Test
        @DisplayName("last_modified_by_sfid 컬럼 — LastModifiedById 매핑")
        fun lastModifiedBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }
}
