package com.otoki.powersales.account.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #605 — AccountCategoryMaster ↔ Salesforce `AccountCategoryMaster__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/거래처유형마스터(AccountCategoryMaster__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject("AccountCategoryMaster__c")` 신규 부착
 *   - AC2: `@SFField` 매핑 키셋 (3개 — Name / AccountCode__c / useSearch__c)
 *   - AC3: PK 컨벤션 정합 (`@Column(name = "account_category_master_id")`)
 *   - AC4: sfid + use_search 필드 존재
 *   - AC10 (#704): Group A 신규 어노테이션 + Reference FK 검증 (Owner / CreatedBy / LastModifiedBy)
 */
@DisplayName("AccountCategoryMaster SF 어노테이션 검증 (Spec #605 + #704)")
class AccountCategoryMasterSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AccountCategoryMaster__c'")
        fun sfObjectValue() {
            val annotation = AccountCategoryMaster::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("AccountCategoryMaster__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (8개: 3개 + BaseEntity 2 + Spec #704 Group A/Reference 3)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AccountCategoryMaster::class.java)

        @Test
        @DisplayName("매핑 키 수 = 8 (3 + BaseEntity 2 + Spec #704 3)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(8)
        }

        @Test
        @DisplayName("§6.2 + §6.3 — 매칭/신규 3개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["AccountCode__c"]).isEqualTo("account_code")
            assertThat(mapping["useSearch__c"]).isEqualTo("use_search")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Spec #703 BaseEntity + Spec #704 Group A/Reference 포함)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "Name",
                    "AccountCode__c",
                    "useSearch__c",
                    "CreatedDate",
                    "LastModifiedDate",
                    "OwnerId",
                    "CreatedById",
                    "LastModifiedById"
                )
        }
    }

    @Nested
    @DisplayName("AC3 — PK 컨벤션 정합")
    inner class PkConvention {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"account_category_master_id\") 명시")
        fun pkColumnNameConvention() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("account_category_master_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 account_category_master_id / sfid 미등장")
        fun mappingValuesExcludePkAndSfid() {
            val mapping = SFSchemaUtils.getSFMapping(AccountCategoryMaster::class.java)
            assertThat(mapping.values).doesNotContain("account_category_master_id", "sfid")
        }
    }

    @Nested
    @DisplayName("AC4 — sfid / use_search 신규 필드")
    inner class NewFields {

        @Test
        @DisplayName("sfid 필드 + @Column(name = \"sfid\", length = 18)")
        fun sfidFieldExists() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("sfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("sfid")
            assertThat(column.length).isEqualTo(18)
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례)")
        fun sfidHasNoSfField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("useSearch 필드 + @Column(name = \"use_search\", nullable = false)")
        fun useSearchFieldExists() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("useSearch")
            assertThat(field.type).isEqualTo(Boolean::class.javaPrimitiveType)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("use_search")
            assertThat(column.nullable).isFalse()
        }

        @Test
        @DisplayName("useSearch 필드에 @SFField(\"useSearch__c\") 부착")
        fun useSearchHasSfField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("useSearch")
            val annotation = field.getAnnotation(SFField::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("useSearch__c")
        }
    }

    @Nested
    @DisplayName("AC10 (#704) — Group A 신규 어노테이션 + Reference FK")
    inner class Spec704GroupAAndReference {

        @Test
        @DisplayName("owner_sfid 필드 + @SFField(\"OwnerId\") + @HCColumn(\"ownerid\") + length 18")
        fun ownerSfidField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("ownerSfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("owner_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("OwnerId")
            assertThat(field.getAnnotation(HCColumn::class.java).value).isEqualTo("ownerid")
        }

        @Test
        @DisplayName("created_by_sfid 필드 + @SFField(\"CreatedById\") + @HCColumn(\"createdbyid\") + length 18")
        fun createdBySfidField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("createdBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("created_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("CreatedById")
            assertThat(field.getAnnotation(HCColumn::class.java).value).isEqualTo("createdbyid")
        }

        @Test
        @DisplayName("last_modified_by_sfid 필드 + @SFField(\"LastModifiedById\") + @HCColumn(\"lastmodifiedbyid\") + length 18")
        fun lastModifiedBySfidField() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("lastModifiedBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("last_modified_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("LastModifiedById")
            assertThat(field.getAnnotation(HCColumn::class.java).value).isEqualTo("lastmodifiedbyid")
        }

        @Test
        @DisplayName("owner FK (@ManyToOne + @JoinColumn(\"owner_id\") → Employee)")
        fun ownerFk() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("owner")
            assertThat(field.type).isEqualTo(Employee::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("owner_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("createdBy FK (@ManyToOne + @JoinColumn(\"created_by_id\") → Employee)")
        fun createdByFk() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("createdBy")
            assertThat(field.type).isEqualTo(Employee::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("created_by_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("lastModifiedBy FK (@ManyToOne + @JoinColumn(\"last_modified_by_id\") → Employee)")
        fun lastModifiedByFk() {
            val field = AccountCategoryMaster::class.java.getDeclaredField("lastModifiedBy")
            assertThat(field.type).isEqualTo(Employee::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("last_modified_by_id")
        }

        @Test
        @DisplayName("BaseEntity CreatedDate / LastModifiedDate 매핑이 AccountCategoryMaster 매핑 결과에 포함")
        fun baseEntityMappingIncluded() {
            val mapping = SFSchemaUtils.getSFMapping(AccountCategoryMaster::class.java)
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }
}
