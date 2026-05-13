package com.otoki.powersales.product.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #737 — NewProduct ↔ Salesforce `NewProduct__c` SF Object 정합 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/NewProduct__c.json
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject
 *   - AC2: @SFField 매핑 키셋 (총 24 = Name 1 + Custom 15 + Group A 6 + BaseEntity 2)
 *   - AC3: Custom 15개 매핑
 *   - AC4: Group A 매핑 (IsDeleted / RecordTypeId / OwnerId / CreatedById / LastModifiedById + Reference Product_Code__c)
 *   - AC5: BaseEntity 매핑
 *   - AC6: Picklist enum round-trip (Initiator / NewProductStatus / ManagementType)
 *   - AC7: ManagementType 84 constant 정합 (SF describe picklistValues 와 1:1)
 *   - AC8: PK / FK 미부착
 */
@DisplayName("NewProduct SF 어노테이션 검증 (Spec #737)")
class NewProductSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'NewProduct__c'")
        fun sfObjectValue() {
            val annotation = NewProduct::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("NewProduct__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 수")
    inner class SfFieldMappingSize {

        private val mapping = SFSchemaUtils.getSFMapping(NewProduct::class.java)

        @Test
        @DisplayName("매핑 키 수 = 23 (Name 1 + Custom 15 + Group A 5 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(23)
        }
    }

    @Nested
    @DisplayName("AC3 — Custom 15개 매핑")
    inner class CustomFieldMappings {

        private val mapping = SFSchemaUtils.getSFMapping(NewProduct::class.java)

        @Test
        @DisplayName("Name → name")
        fun nameMapping() {
            assertThat(mapping["Name"]).isEqualTo("name")
        }

        @Test
        @DisplayName("Custom 15개 — API name → column")
        fun customMappings() {
            assertThat(mapping["Customer_Survey__c"]).isEqualTo("customer_survey")
            assertThat(mapping["Initiator__c"]).isEqualTo("initiator")
            assertThat(mapping["ManagementType__c"]).isEqualTo("management_type")
            assertThat(mapping["Marketability_Review_Report__c"]).isEqualTo("marketability_review_report")
            assertThat(mapping["Product_Code__c"]).isEqualTo("product_code_sfid")
            assertThat(mapping["Product_Name__c"]).isEqualTo("product_name")
            assertThat(mapping["Product_code1__c"]).isEqualTo("product_code1")
            assertThat(mapping["Purpose__c"]).isEqualTo("purpose")
            assertThat(mapping["Release_Review_Report__c"]).isEqualTo("release_review_report")
            assertThat(mapping["Release__c"]).isEqualTo("release")
            assertThat(mapping["Status__c"]).isEqualTo("status")
            assertThat(mapping["firstpropose__c"]).isEqualTo("firstpropose")
            assertThat(mapping["friday_taste__c"]).isEqualTo("friday_taste")
            assertThat(mapping["Upload_Description__c"]).isEqualTo("upload_description")
            assertThat(mapping["MarketingTeam__c"]).isEqualTo("marketing_team")
        }
    }

    @Nested
    @DisplayName("AC4 — Group A 매핑 (IsDeleted / RecordTypeId / Owner / CreatedBy / LastModifiedBy)")
    inner class GroupAMappings {

        private val mapping = SFSchemaUtils.getSFMapping(NewProduct::class.java)

        @Test
        @DisplayName("IsDeleted → is_deleted")
        fun isDeletedMapping() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
        }

        @Test
        @DisplayName("RecordTypeId → record_type_sfid (FK 미신설 — Q1)")
        fun recordTypeIdMapping() {
            assertThat(mapping["RecordTypeId"]).isEqualTo("record_type_sfid")
        }

        @Test
        @DisplayName("OwnerId → owner_sfid")
        fun ownerIdMapping() {
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
        }

        @Test
        @DisplayName("CreatedById → created_by_sfid")
        fun createdByIdMapping() {
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
        }

        @Test
        @DisplayName("LastModifiedById → last_modified_by_sfid")
        fun lastModifiedByIdMapping() {
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }
    }

    @Nested
    @DisplayName("AC5 — BaseEntity 매핑 (CreatedDate / LastModifiedDate)")
    inner class BaseEntityMappings {

        private val mapping = SFSchemaUtils.getSFMapping(NewProduct::class.java)

        @Test
        @DisplayName("CreatedDate → created_at (BaseEntity 상속)")
        fun createdDateMapping() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
        }

        @Test
        @DisplayName("LastModifiedDate → updated_at (BaseEntity 상속)")
        fun lastModifiedDateMapping() {
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC6 — Picklist enum round-trip")
    inner class PicklistConverters {

        @Test
        @DisplayName("Initiator — 마케팅발의 / 지시 fromDisplayName 정상 변환")
        fun initiatorConverter() {
            assertThat(Initiator.fromDisplayNameOrNull("마케팅발의")).isEqualTo(Initiator.MARKETING_PROPOSAL)
            assertThat(Initiator.fromDisplayNameOrNull("지시")).isEqualTo(Initiator.INSTRUCTION)
            assertThat(Initiator.fromDisplayNameOrNull(null)).isNull()
            assertThat(Initiator.MARKETING_PROPOSAL.displayName).isEqualTo("마케팅발의")
        }

        @Test
        @DisplayName("NewProductStatus — 진행예정 / 진행중 / 완료 fromDisplayName 정상 변환")
        fun statusConverter() {
            assertThat(NewProductStatus.fromDisplayNameOrNull("진행예정")).isEqualTo(NewProductStatus.PLANNED)
            assertThat(NewProductStatus.fromDisplayNameOrNull("진행중")).isEqualTo(NewProductStatus.IN_PROGRESS)
            assertThat(NewProductStatus.fromDisplayNameOrNull("완료")).isEqualTo(NewProductStatus.COMPLETED)
            assertThat(NewProductStatus.fromDisplayNameOrNull(null)).isNull()
        }

        @Test
        @DisplayName("ManagementType — 샘플 옵션 round-trip 확인")
        fun managementTypeSample() {
            assertThat(ManagementType.fromDisplayNameOrNull("공통")).isEqualTo(ManagementType.COMMON)
            assertThat(ManagementType.fromDisplayNameOrNull("LL면류")).isEqualTo(ManagementType.LL_NOODLES)
            assertThat(ManagementType.fromDisplayNameOrNull("국·찌개류")).isEqualTo(ManagementType.SOUP_AND_STEW)
            assertThat(ManagementType.fromDisplayNameOrNull("선물세트")).isEqualTo(ManagementType.GIFT_SET)
            assertThat(ManagementType.fromDisplayNameOrNull("후추류")).isEqualTo(ManagementType.PEPPER)
        }
    }

    @Nested
    @DisplayName("AC7 — ManagementType 84 constant 정합")
    inner class ManagementTypeSize {

        @Test
        @DisplayName("ManagementType enum entries 수 = 84 (SF picklistValues 와 일치)")
        fun managementTypeEntriesSize() {
            assertThat(ManagementType.entries).hasSize(84)
        }

        @Test
        @DisplayName("ManagementType displayName 모두 UNIQUE")
        fun managementTypeDisplayNameUnique() {
            val displayNames = ManagementType.entries.map { it.displayName }
            assertThat(displayNames).doesNotHaveDuplicates()
        }
    }

    @Nested
    @DisplayName("AC8 — PK / FK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = NewProduct::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 PK / FK 컬럼명 미등장")
        fun mappingValuesExcludePkAndFk() {
            val mapping = SFSchemaUtils.getSFMapping(NewProduct::class.java)
            assertThat(mapping.values).doesNotContain(
                "new_product_id",
                "owner_id",
                "created_by_id",
                "last_modified_by_id",
                "product_code_id"
            )
        }
    }
}
