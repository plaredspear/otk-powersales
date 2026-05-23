package com.otoki.powersales.inspection.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #633 / #714 — InspectionTheme ↔ Salesforce `Theme__c` 어노테이션 검증.
 *
 * 단일 권위: Salesforce Object (`Theme__c`)
 *
 * 검증 분류:
 *   - AC1: @SFObject + 매핑 키셋
 *   - AC2: @SFField 매핑 키셋 (13개 — 7 도메인 + 1 IsDeleted + 3 Reference + 2 BaseEntity)
 *   - AC3: PK 미부착
 *   - AC10 (#714): Group A 신규 어노테이션 + Reference FK 검증
 */
@DisplayName("InspectionTheme SF 어노테이션 검증 (Spec #633 / #714)")
class InspectionThemeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'Theme__c'")
        fun sfObjectValue() {
            val annotation = InspectionTheme::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Theme__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(InspectionTheme::class.java)

        @Test
        @DisplayName("매핑 키 수 = 13 (7 도메인 + 1 IsDeleted + 3 Reference + 2 BaseEntity)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(13)
        }

        @Test
        @DisplayName("도메인 7개 SF API Name → 컬럼명 1:1")
        fun domainMappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Title__c"]).isEqualTo("title")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Department__c"]).isEqualTo("department")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["PublicFlag__c"]).isEqualTo("public_flag")
        }

        @Test
        @DisplayName("Group A + Reference 매핑 (IsDeleted / OwnerId / CreatedById / LastModifiedById)")
        fun groupAAndReferenceMappings() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("BaseEntity CreatedDate / LastModifiedDate 매핑이 결과에 포함")
        fun baseEntityMappingIncluded() {
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
            val field = InspectionTheme::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

    @Nested
    @DisplayName("AC10 (#714) — Group A 신규 어노테이션 + Reference FK")
    inner class Spec714GroupAAndReference {

        @Test
        @DisplayName("is_deleted 필드에 @SFField(\"IsDeleted\") 신규 부착")
        fun isDeletedHasSfField() {
            val field = InspectionTheme::class.java.getDeclaredField("isDeleted")
            val annotation = field.getAnnotation(SFField::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("IsDeleted")
        }

        @Test
        @DisplayName("owner_sfid 필드 + @SFField(\"OwnerId\") + length 18")
        fun ownerSfidField() {
            val field = InspectionTheme::class.java.getDeclaredField("ownerSfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("owner_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("OwnerId")
        }

        @Test
        @DisplayName("created_by_sfid 필드 + @SFField(\"CreatedById\") + length 18")
        fun createdBySfidField() {
            val field = InspectionTheme::class.java.getDeclaredField("createdBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("created_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("CreatedById")
        }

        @Test
        @DisplayName("last_modified_by_sfid 필드 + @SFField(\"LastModifiedById\") + length 18")
        fun lastModifiedBySfidField() {
            val field = InspectionTheme::class.java.getDeclaredField("lastModifiedBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("last_modified_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("LastModifiedById")
        }

        @Test
        @DisplayName("owner FK (@ManyToOne + @JoinColumn(\"owner_id\") → Employee)")
        fun ownerFk() {
            val field = InspectionTheme::class.java.getDeclaredField("owner")
            assertThat(field.type).isEqualTo(com.otoki.powersales.employee.entity.Employee::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("owner_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("createdBy FK (@ManyToOne + @JoinColumn(\"created_by_id\") → Employee)")
        fun createdByFk() {
            val field = InspectionTheme::class.java.getDeclaredField("createdBy")
            assertThat(field.type).isEqualTo(com.otoki.powersales.employee.entity.Employee::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("created_by_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("lastModifiedBy FK (@ManyToOne + @JoinColumn(\"last_modified_by_id\") → Employee)")
        fun lastModifiedByFk() {
            val field = InspectionTheme::class.java.getDeclaredField("lastModifiedBy")
            assertThat(field.type).isEqualTo(com.otoki.powersales.employee.entity.Employee::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("last_modified_by_id")
        }
    }
}
