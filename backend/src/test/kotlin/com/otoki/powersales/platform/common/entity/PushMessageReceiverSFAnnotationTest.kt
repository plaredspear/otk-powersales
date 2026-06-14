package com.otoki.powersales.platform.common.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.push.entity.PushMessageReceiver
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #710 — PushMessageReceiver ↔ Salesforce `PushMessageReceiver__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`PushMessageReceiver__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject 무변경
 *   - AC2: @SFField 매핑 키셋 (6개 — 기존 3 + Spec #710 신규 3)
 *   - AC3: PK·내부 FK 미부착
 *   - AC4: Reference R-2 sfid 컬럼 (CreatedById / LastModifiedById)
 */
@DisplayName("PushMessageReceiver SF 어노테이션 검증 (Spec #710)")
class PushMessageReceiverSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'PushMessageReceiver__c'")
        fun sfObjectValue() {
            val annotation = PushMessageReceiver::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("PushMessageReceiver__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (6개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)

        @Test
        @DisplayName("매핑 키 수 = 8 (기존 3 + Spec #710 신규 3 + CreatedDate/LastModifiedDate)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(8)
        }

        @Test
        @DisplayName("기존 3개 필드 매핑 무변경")
        fun existingFields() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["MessageId__c"]).isEqualTo("push_message_sfid")
        }

        @Test
        @DisplayName("Spec #710 신규 3개 (Group A IsDeleted + R-2 sfid)")
        fun spec710NewFields() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }

    @Nested
    @DisplayName("AC3 — PK·내부 FK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = PushMessageReceiver::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("내부 FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = PushMessageReceiver::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("내부 FK(pushMessageId) 필드에 @SFField 미부착")
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

    @Nested
    @DisplayName("AC4 — Reference R-2 sfid 컬럼")
    inner class ReferenceSfidColumns {

        @Test
        @DisplayName("created_by_sfid 컬럼 — CreatedById 매핑")
        fun createdBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
        }

        @Test
        @DisplayName("last_modified_by_sfid 컬럼 — LastModifiedById 매핑")
        fun lastModifiedBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(PushMessageReceiver::class.java)
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }
}
