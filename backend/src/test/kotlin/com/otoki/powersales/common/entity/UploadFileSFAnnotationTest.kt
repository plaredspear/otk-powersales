package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #712 — UploadFile ↔ Salesforce `UploadFile__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce Object 메타 (`UploadFile__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject 무변경
 *   - AC2: @SFField 매핑 키셋 (13개 — 기존 8 + Spec #712 신규 5)
 *   - AC3: PK 미부착 + parent_id (entity-only) 매핑 미부착
 *   - AC4: Reference R-2 sfid 컬럼 (OwnerId / CreatedById / LastModifiedById)
 */
@DisplayName("UploadFile SF 어노테이션 검증 (Spec #712)")
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
    @DisplayName("AC2 — @SFField 매핑 키셋 (13개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)

        @Test
        @DisplayName("매핑 키 수 = 15 (기존 8 + Spec #712 신규 5 + audit CreatedDate + LastModifiedDate)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(15)
        }

        @Test
        @DisplayName("기존 8개 필드 매핑 무변경 (Spec #616)")
        fun existingFields() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["UniqueKey__c"]).isEqualTo("unique_key")
            assertThat(mapping["RecordId__c"]).isEqualTo("record_sfid")
            assertThat(mapping["Size__c"]).isEqualTo("size")
            // Object__c → object_type (SF 원본 보존). parent_type 은 Stage2 가 object_type 기준 파생 (@SFField 미부착).
            assertThat(mapping["Object__c"]).isEqualTo("object_type")
            assertThat(mapping["Url__c"]).isEqualTo("url")
            assertThat(mapping["UploadKbn__c"]).isEqualTo("upload_kbn")
            assertThat(mapping["FileId__c"]).isEqualTo("file_id")
        }

        @Test
        @DisplayName("Spec #712 신규 5개 (Group A IsDeleted + CreatedDate + R-2 sfid)")
        fun spec712NewFields() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
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
        @DisplayName("parent_type (Stage2 파생, 신규 시스템 값) 에 @SFField 미부착")
        fun parentTypeHasNoSfField() {
            val field = UploadFile::class.java.getDeclaredField("parentType")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 upload_file_id / parent_id 미등장")
        fun mappingValuesExcludePkAndEntityOnly() {
            val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)
            assertThat(mapping.values).doesNotContain("upload_file_id", "parent_id")
        }
    }

    @Nested
    @DisplayName("AC4 — Reference R-2 sfid 컬럼")
    inner class ReferenceSfidColumns {

        @Test
        @DisplayName("owner_sfid 컬럼 — OwnerId 매핑")
        fun ownerSfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
        }

        @Test
        @DisplayName("created_by_sfid 컬럼 — CreatedById 매핑")
        fun createdBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
        }

        @Test
        @DisplayName("last_modified_by_sfid 컬럼 — LastModifiedById 매핑")
        fun lastModifiedBySfidMapping() {
            val mapping = SFSchemaUtils.getSFMapping(UploadFile::class.java)
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }
}
