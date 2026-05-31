package com.otoki.powersales.notice.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #612 — Notice ↔ Salesforce `DKRetail__Notice__c` 어노테이션 정합 검증.
 *
 * 단일 권위: Salesforce Object (`DKRetail__Notice__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경 (DKRetail__Notice__c)
 *   - AC2: `@SFField` 매핑 키셋 (9개 — 7 기존 OK + 1 누락 보강 + 1 신규)
 *   - AC3: PK / FK 미부착
 */
@DisplayName("Notice SF 어노테이션 검증 (Spec #612)")
class NoticeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Notice__c'")
        fun sfObjectValue() {
            val annotation = Notice::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Notice__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (9개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Notice::class.java)

        @Test
        @DisplayName("매핑 키 수 = 15 (Spec #849 EduCategory 부활)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(15)
        }

        @Test
        @DisplayName("Spec #849 — DKRetail__EduCategory__c (Label=\"교육 카테고리(사용안함)\") 부활 매핑 포함")
        fun section849EduCategoryRevived() {
            assertThat(mapping["DKRetail__EduCategory__c"]).isEqualTo("edu_category")
        }

        @Test
        @DisplayName("§6.3 — 신규 1개 필드: Title__c → title")
        fun section63NewField() {
            assertThat(mapping["Title__c"]).isEqualTo("title")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 7개 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__Scope__c"]).isEqualTo("scope")
            assertThat(mapping["DKRetail__Category__c"]).isEqualTo("category")
            assertThat(mapping["DKRetail__Contents__c"]).isEqualTo("contents")
            assertThat(mapping["DKRetail__Jeejum__c"]).isEqualTo("branch")
            assertThat(mapping["DKRetail__JeejumCode__c"]).isEqualTo("branch_code")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / FK 미부착")
    inner class PkFkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Notice::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employee) 필드에 @SFField 미부착")
        fun employeeHasNoSfField() {
            val field = Notice::class.java.getDeclaredField("employee")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 notice_id / employee_id 미등장")
        fun mappingValuesExcludePkFk() {
            val mapping = SFSchemaUtils.getSFMapping(Notice::class.java)
            assertThat(mapping.values).doesNotContain("notice_id", "employee_id")
        }
    }
}
