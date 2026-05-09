package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #615 — PushMessage ↔ Salesforce `PushMessage__c` SF 누락 컬럼 도입 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/메시지(PushMessage__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (7개 — 3 기존 + 4 신규)
 *   - AC3: PK 미부착
 */
@DisplayName("PushMessage SF 어노테이션 검증 (Spec #615)")
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
    @DisplayName("AC2 — @SFField 매핑 키셋 (7개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PushMessage::class.java)

        @Test
        @DisplayName("매핑 키 수 = 7 (3 기존 + 4 신규)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(7)
        }

        @Test
        @DisplayName("§6.2 — 신규 4개 필드 매핑")
        fun section62NewFields() {
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["Branch__c"]).isEqualTo("branch")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["SObjectRecordId__c"]).isEqualTo("s_object_record_id")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 3개 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Message__c"]).isEqualTo("message")
            assertThat(mapping["ScheduleDate__c"]).isEqualTo("schedule_date")
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
}
