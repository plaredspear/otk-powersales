package com.otoki.powersales.domain.org.leave.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import com.otoki.powersales.domain.org.leave.enums.HolidayType
import jakarta.persistence.Column
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #604 — HolidayMaster ↔ Salesforce `HolidayMaster__c` 어노테이션 부착 검증.
 *
 * 단일 권위: Salesforce Object (`HolidayMaster__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject("HolidayMaster__c")` 신규 부착
 *   - AC2: `@SFField` 매핑 키셋 (3개 — Name / HolidayDate__c / Type__c)
 *   - AC3: sfid 필드 존재
 *   - AC4: PK 컨벤션 정합 (`@Column(name = "holiday_master_id")`) + 자체 컬럼 미부착
 *   - AC5: VALID_TYPES 갱신 (Q1 옵션 2 — SF picklist 값 그대로)
 */
@DisplayName("HolidayMaster SF 어노테이션 검증 (Spec #604)")
class HolidayMasterSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'HolidayMaster__c'")
        fun sfObjectValue() {
            val annotation = HolidayMaster::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("HolidayMaster__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (8개: 3 도메인 + BaseEntity 2 + Group A R-2 sfid 3)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(HolidayMaster::class.java)

        @Test
        @DisplayName("매핑 키 수 = 9 (도메인 3 + BaseEntity 2 + Group A R-2 sfid 3 + IsDeleted)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(9)
        }

        @Test
        @DisplayName("§6.2 — 매칭 필드 3개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["HolidayDate__c"]).isEqualTo("holiday_date")
            assertThat(mapping["Type__c"]).isEqualTo("type")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Spec #716 — Group A R-2 sfid 3개 + BaseEntity 2개 포함)")
        fun mappingKeysExact() {
            assertThat(mapping.keys).containsExactlyInAnyOrder(
                "Name", "HolidayDate__c", "Type__c",
                "CreatedDate", "LastModifiedDate",
                "OwnerId", "CreatedById", "LastModifiedById",
                "IsDeleted"
            )
        }
    }

    @Nested
    @DisplayName("AC3 — sfid 필드 존재")
    inner class SfidField {

        @Test
        @DisplayName("sfid 필드 + @Column(name = \"sfid\", length = 18)")
        fun sfidFieldExists() {
            val field = HolidayMaster::class.java.getDeclaredField("sfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("sfid")
            assertThat(column.length).isEqualTo(18)
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례)")
        fun sfidHasNoSfField() {
            val field = HolidayMaster::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

    @Nested
    @DisplayName("AC4 — PK / 자체 컬럼 정책")
    inner class PkAndSelfColumns {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"holiday_master_id\") 명시 (backend-conventions.md PK 컨벤션)")
        fun pkColumnNameConvention() {
            val field = HolidayMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("holiday_master_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = HolidayMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("year 자체 파생 컬럼에 @SFField 미부착")
        fun yearHasNoSfField() {
            val field = HolidayMaster::class.java.getDeclaredField("year")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 holiday_master_id / year 미등장")
        fun mappingValuesExcludePkAndSelfColumns() {
            val mapping = SFSchemaUtils.getSFMapping(HolidayMaster::class.java)
            assertThat(mapping.values).doesNotContain("holiday_master_id", "year", "sfid")
        }
    }

    @Nested
    @DisplayName("AC5 — HolidayType enum (Spec #716: SF picklist 정합)")
    inner class HolidayTypeEnum {

        @Test
        @DisplayName("HolidayType enum = [공휴일, 주말, 기타]")
        fun holidayTypeValues() {
            assertThat(HolidayType.entries.map { it.displayName })
                .containsExactly("공휴일", "주말", "기타")
        }

        @Test
        @DisplayName("기존 값(법정공휴일/대체공휴일/임시공휴일) 부재 단언")
        fun oldValuesAbsent() {
            assertThat(HolidayType.entries.map { it.displayName })
                .doesNotContain("법정공휴일", "대체공휴일", "임시공휴일")
        }
    }
}
