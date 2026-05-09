package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #631 — PushMessageReceiver ↔ Salesforce `PushMessageReceiver__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/메시지수신자(PushMessageReceiver__c).md
 */
@DisplayName("PushMessageReceiver SF 어노테이션 검증 (Spec #631)")
class PushMessageReceiverSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'PushMessageReceiver__c'")
        fun sfObjectValue() {
            val annotation = PushMessageReceiver::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("PushMessageReceiver__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 3")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)
            assertThat(mapping).hasSize(3)
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (3개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)

        @Test
        @DisplayName("3개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["MessageId__c"]).isEqualTo("push_message_sfid")
        }
    }

    @Nested
    @DisplayName("AC1 — PK·FK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = PushMessageReceiver::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = PushMessageReceiver::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(pushMessageId) 필드에 @SFField 미부착")
        fun pushMessageIdHasNoSfField() {
            val field = PushMessageReceiver::class.java.getDeclaredField("pushMessageId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 push_message_receiver_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)
            assertThat(mapping.values).doesNotContain("push_message_receiver_id")
        }
    }
}
