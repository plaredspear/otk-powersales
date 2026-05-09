package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #616 — UploadFile ↔ Salesforce `UploadFile__c` SF 누락 컬럼 도입 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/업로드파일(UploadFile__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (8개 — 4 기존 + 1 매핑 추가 + 3 신규)
 *   - AC3: PK 미부착 + parent_id (entity-only) 매핑 미부착
 */
@DisplayName("UploadFile SF 어노테이션 검증 (Spec #616)")
class UploadFileSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'UploadFile__c'")
        fun sfObjectValue() {
            val annotation = UploadFile::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("UploadFile__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (8개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)

        @Test
        @DisplayName("매핑 키 수 = 8 (4 기존 + 1 매핑 추가 + 3 신규)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(8)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 1개 신규 부착: Object__c → parent_type")
        fun section62MissingMapping() {
            assertThat(mapping["Object__c"]).isEqualTo("parent_type")
        }

        @Test
        @DisplayName("§6.3 — 신규 3개 필드 매핑")
        fun section63NewFields() {
            assertThat(mapping["Url__c"]).isEqualTo("url")
            assertThat(mapping["UploadKbn__c"]).isEqualTo("upload_kbn")
            assertThat(mapping["FileId__c"]).isEqualTo("file_id")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 4개 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["UniqueKey__c"]).isEqualTo("unique_key")
            assertThat(mapping["RecordId__c"]).isEqualTo("record_id")
            assertThat(mapping["Size__c"]).isEqualTo("size")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / Entity-only 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = UploadFile::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("parent_id (entity-only 다형성 sfid) 에 @SFField 미부착")
        fun parentIdHasNoSfField() {
            val field = UploadFile::class.java.getDeclaredField("parentId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 upload_file_id / parent_id 미등장")
        fun mappingValuesExcludePkAndEntityOnly() {
            val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)
            assertThat(mapping.values).doesNotContain("upload_file_id", "parent_id")
        }
    }
}
