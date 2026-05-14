package com.otoki.powersales.employee.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import com.otoki.powersales.employee.entity.converter.GroupTypeConverter
import jakarta.persistence.Convert
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #755 — Group ↔ Salesforce `Group` 표준 sobject 어노테이션 검증.
 *
 * 단일 권위: SF prod raw JSON (`docs/plan/old_source_260408/sf-object-meta/prod/_raw/Group.json`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject / @HCTable / @Table 어노테이션
 *   - AC2: @SFField 매핑 키셋 (SF Group 의 11 필드 + BaseEntity 2 = 13)
 *   - AC3: @HCColumn 매핑 키셋
 *   - AC4: Type 컬럼 GroupTypeConverter 부착
 */
@DisplayName("Group SF 어노테이션 검증 (Spec #755)")
class GroupTest {

    @Nested
    @DisplayName("AC1 — 클래스 어노테이션")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'Group' (SF 표준 sobject)")
        fun sfObjectValue() {
            val annotation = Group::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Group")
        }

        @Test
        @DisplayName("@HCTable 값은 'group'")
        fun hcTableValue() {
            val annotation = Group::class.java.getAnnotation(HCTable::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("group")
        }

        @Test
        @DisplayName("@Table.name 은 reserved keyword 회피 위해 double-quoted")
        fun tableNameQuoted() {
            val annotation = Group::class.java.getAnnotation(Table::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.name).isEqualTo("\"group\"")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (SF Group 11 필드 + BaseEntity 2)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Group::class.java)

        @Test
        @DisplayName("매핑 키 수 = 13 (Name/DeveloperName/Type/RelatedId/OwnerId/Email/" +
            "DoesSendEmailToMembers/DoesIncludeBosses/Description/CreatedById/LastModifiedById + " +
            "CreatedDate/LastModifiedDate)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(13)
        }

        @Test
        @DisplayName("SF Group 표준 필드 매핑 — Name/DeveloperName/Type")
        fun standardFields() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DeveloperName"]).isEqualTo("developer_name")
            assertThat(mapping["Type"]).isEqualTo("type")
        }

        @Test
        @DisplayName("polymorphic reference sfid only — RelatedId/OwnerId")
        fun polymorphicSfid() {
            assertThat(mapping["RelatedId"]).isEqualTo("related_sfid")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
        }

        @Test
        @DisplayName("boolean / textarea 필드 매핑")
        fun extraFields() {
            assertThat(mapping["Email"]).isEqualTo("email")
            assertThat(mapping["DoesSendEmailToMembers"]).isEqualTo("does_send_email_to_members")
            assertThat(mapping["DoesIncludeBosses"]).isEqualTo("does_include_bosses")
            assertThat(mapping["Description"]).isEqualTo("description")
        }

        @Test
        @DisplayName("Group A R-2 audit — CreatedById/LastModifiedById sfid 컬럼")
        fun groupAR2Audit() {
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("BaseEntity 상속 — CreatedDate/LastModifiedDate")
        fun baseEntityFields() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC3 — @HCColumn 매핑")
    inner class HcColumnMapping {

        private val mapping = SFSchemaUtils.getHCMapping(Group::class.java)

        @Test
        @DisplayName("sfid + 11 SF 필드 + BaseEntity 2 = 14")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(14)
        }

        @Test
        @DisplayName("sfid → sfid")
        fun sfidMapping() {
            assertThat(mapping["sfid"]).isEqualTo("sfid")
        }

        @Test
        @DisplayName("SF API name 소문자 형 — relatedid/ownerid/createdbyid/lastmodifiedbyid")
        fun hcLowercaseMapping() {
            assertThat(mapping["relatedid"]).isEqualTo("related_sfid")
            assertThat(mapping["ownerid"]).isEqualTo("owner_sfid")
            assertThat(mapping["createdbyid"]).isEqualTo("created_by_sfid")
            assertThat(mapping["lastmodifiedbyid"]).isEqualTo("last_modified_by_sfid")
        }
    }

    @Nested
    @DisplayName("AC4 — Type 컬럼 Converter")
    inner class TypeConverter {

        @Test
        @DisplayName("type 필드에 @Convert(GroupTypeConverter) 부착")
        fun typeConvert() {
            val field = Group::class.java.declaredFields.first { it.name == "type" }
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(GroupTypeConverter::class.java)
        }

        @Test
        @DisplayName("type 필드 @SFField('Type') 부착")
        fun typeSfField() {
            val field = Group::class.java.declaredFields.first { it.name == "type" }
            val sfField = field.getAnnotation(SFField::class.java)
            assertThat(sfField).isNotNull
            assertThat(sfField.value).isEqualTo("Type")
        }

        @Test
        @DisplayName("type 필드 @HCColumn('type') 부착")
        fun typeHcColumn() {
            val field = Group::class.java.declaredFields.first { it.name == "type" }
            val hc = field.getAnnotation(HCColumn::class.java)
            assertThat(hc).isNotNull
            assertThat(hc.value).isEqualTo("type")
        }
    }
}
