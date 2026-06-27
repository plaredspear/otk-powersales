package com.otoki.powersales.domain.foundation.account.entity

import com.otoki.powersales.domain.foundation.account.entity.converter.AccountSourceConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.FreezerTypeConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.IndustryConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.OwnershipConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.RatingConverter
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import com.otoki.powersales.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #602 / #703 — Account ↔ Salesforce `Account` 어노테이션 부착 검증.
 *
 * 단일 권위:
 *   - Salesforce Object (`Account`) (#602)
 *   - SF Object 정합 정책 §6 (#703 정합 정책)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (68개 — 22 기존 + 17 SAP 보존 + 23 신규 + 1 Spec #644 OwnerId
 *          + 3 Spec #703 Group A (IsDeleted/CreatedById/LastModifiedById) + 2 BaseEntity (CreatedDate/LastModifiedDate))
 *   - AC3: PK / FK 미부착
 *   - AC8: parent_sfid `@SFField("ParentId")` 부착
 *   - AC9: AccountType / FreezerType enum + Converter 검증
 *   - AC10 (#703): Group A 신규 어노테이션 + Reference FK 검증
 *   - AC11 (#703 §3-2): Industry / Ownership / Rating / AccountSource enum + Converter 검증
 */
@DisplayName("Account SF 어노테이션 검증 (Spec #602 / #703)")
class AccountSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'Account'")
        fun sfObjectValue() {
            val annotation = Account::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Account")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Account::class.java)

        @Test
        @DisplayName("매핑 키 수 = 69 (22 기존 + 17 SAP 보존 + 23 신규 + 1 Spec #644 OwnerId + 3 Spec #703 Group A + 2 BaseEntity + 1 SF prod 정합 IsPriorityRecord)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(69)
        }

        @Test
        @DisplayName("§6.1 — 기존 22개 매핑 무변경")
        fun section61Existing() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Phone"]).isEqualTo("phone")
            assertThat(mapping["MobilePhone__c"]).isEqualTo("mobile_phone")
            assertThat(mapping["Address1__c"]).isEqualTo("address1")
            assertThat(mapping["Address2__c"]).isEqualTo("address2")
            assertThat(mapping["Representative__c"]).isEqualTo("representative")
            assertThat(mapping["ABCType__c"]).isEqualTo("abc_type")
            assertThat(mapping["ABCTypeCode__c"]).isEqualTo("abc_type_code")
            assertThat(mapping["ExternalKey__c"]).isEqualTo("external_key")
            assertThat(mapping["AccountGroup__c"]).isEqualTo("account_group")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["BranchName__c"]).isEqualTo("branch_name")
            assertThat(mapping["Zipcode__c"]).isEqualTo("zip_code")
            assertThat(mapping["Latitude__c"]).isEqualTo("latitude")
            assertThat(mapping["Longitude__c"]).isEqualTo("longitude")
            assertThat(mapping["ClosingTime1__c"]).isEqualTo("closing_time1")
            assertThat(mapping["ClosingTime2__c"]).isEqualTo("closing_time2")
            assertThat(mapping["ClosingTime3__c"]).isEqualTo("closing_time3")
            assertThat(mapping["Industry"]).isEqualTo("industry")
            assertThat(mapping["WERK1_TX__c"]).isEqualTo("werk1_tx")
            assertThat(mapping["WERK2_TX__c"]).isEqualTo("werk2_tx")
            assertThat(mapping["WERK3_TX__c"]).isEqualTo("werk3_tx")
        }

        @Test
        @DisplayName("§6.2 — SAP 보존 17개 필드 신규 부착")
        fun section62SapPreserved() {
            assertThat(mapping["Type"]).isEqualTo("account_type")
            assertThat(mapping["AccountStatusName__c"]).isEqualTo("account_status_name")
            assertThat(mapping["EmployeeCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["Distribution__c"]).isEqualTo("distribution")
            assertThat(mapping["AccountStatusCode__c"]).isEqualTo("account_status_code")
            assertThat(mapping["BusinessType__c"]).isEqualTo("business_type")
            assertThat(mapping["BusinessCategory__c"]).isEqualTo("business_category")
            assertThat(mapping["Sic"]).isEqualTo("business_license_number")
            assertThat(mapping["Email__c"]).isEqualTo("email")
            assertThat(mapping["DivisionName__c"]).isEqualTo("division_name")
            assertThat(mapping["SalesDeptName__c"]).isEqualTo("sales_dept_name")
            assertThat(mapping["ConsignmentAcc__c"]).isEqualTo("consignment_acc")
            assertThat(mapping["WERK1__c"]).isEqualTo("werk1")
            assertThat(mapping["WERK2__c"]).isEqualTo("werk2")
            assertThat(mapping["WERK3__c"]).isEqualTo("werk3")
            assertThat(mapping["SalesDeptCostCenter__c"]).isEqualTo("sales_dept_cost_center")
            assertThat(mapping["DivisionCostCenter__c"]).isEqualTo("division_cost_center")
        }

        @Test
        @DisplayName("§6.3 — 신규 23개 필드 매핑 (Q1 옵션 1 + Q4 추가)")
        fun section63NewFields() {
            assertThat(mapping["AccountNumber"]).isEqualTo("account_number")
            assertThat(mapping["Site"]).isEqualTo("site")
            assertThat(mapping["AccountSource"]).isEqualTo("account_source")
            assertThat(mapping["BranchCostCenter__c"]).isEqualTo("branch_cost_center")
            assertThat(mapping["DivisionCode__c"]).isEqualTo("division_code")
            assertThat(mapping["SalesDeptCode__c"]).isEqualTo("sales_dept_code")
            assertThat(mapping["LogisticsName__c"]).isEqualTo("logistics_name")
            assertThat(mapping["LogisticsCode__c"]).isEqualTo("logistics_code")
            assertThat(mapping["FreezerInstalled__c"]).isEqualTo("freezer_installed")
            assertThat(mapping["FreezerType__c"]).isEqualTo("freezer_type")
            assertThat(mapping["Field1__c"]).isEqualTo("remaining_credit")
            assertThat(mapping["TotalCredit__c"]).isEqualTo("total_credit")
            assertThat(mapping["MapCoordinate__c"]).isEqualTo("map_coordinate")
            assertThat(mapping["OrderEndTime__c"]).isEqualTo("order_end_time")
            assertThat(mapping["FirstInstalled__c"]).isEqualTo("first_installed")
            assertThat(mapping["Description"]).isEqualTo("description")
            assertThat(mapping["Website"]).isEqualTo("website")
            assertThat(mapping["Fax"]).isEqualTo("fax")
            assertThat(mapping["AnnualRevenue"]).isEqualTo("annual_revenue")
            assertThat(mapping["NumberOfEmployees"]).isEqualTo("number_of_employees")
            assertThat(mapping["ParentId"]).isEqualTo("parent_sfid")
            assertThat(mapping["Rating"]).isEqualTo("rating")
            assertThat(mapping["Ownership"]).isEqualTo("ownership")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val idField = Account::class.java.getDeclaredField("id")
            assertThat(idField.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 account_id 미등장")
        fun mappingValuesExcludeAccountId() {
            val mapping = SFSchemaUtils.getSFMapping(Account::class.java)
            assertThat(mapping.values).doesNotContain("account_id")
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (SOQL 자동 포함 정책)")
        fun sfidHasNoSfField() {
            val sfidField = Account::class.java.getDeclaredField("sfid")
            assertThat(sfidField.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

    @Nested
    @DisplayName("AC8 — parent_sfid lookup 매핑")
    inner class LookupSfid {

        @Test
        @DisplayName("parent_sfid 필드에 @SFField(\"ParentId\") 부착")
        fun parentSfidHasSfField() {
            val field = Account::class.java.getDeclaredField("parentSfid")
            val annotation = field.getAnnotation(SFField::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ParentId")
        }

        @Test
        @DisplayName("getSFMapping()[\"ParentId\"] == \"parent_sfid\"")
        fun parentIdMapping() {
            val mapping = SFSchemaUtils.getSFMapping(Account::class.java)
            assertThat(mapping["ParentId"]).isEqualTo("parent_sfid")
        }
    }

    @Nested
    @DisplayName("AC10 (#703) — Group A 신규 어노테이션 + Reference FK")
    inner class Spec703GroupAAndReference {

        @Test
        @DisplayName("is_deleted 필드에 @SFField(\"IsDeleted\") 신규 부착")
        fun isDeletedHasSfField() {
            val field = Account::class.java.getDeclaredField("isDeleted")
            val annotation = field.getAnnotation(SFField::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("IsDeleted")
        }

        @Test
        @DisplayName("created_by_sfid 필드 + @SFField(\"CreatedById\") + length 18")
        fun createdBySfidField() {
            val field = Account::class.java.getDeclaredField("createdBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("created_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("CreatedById")
        }

        @Test
        @DisplayName("last_modified_by_sfid 필드 + @SFField(\"LastModifiedById\") + length 18")
        fun lastModifiedBySfidField() {
            val field = Account::class.java.getDeclaredField("lastModifiedBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("last_modified_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("LastModifiedById")
        }

        @Test
        @DisplayName("createdBy FK (@ManyToOne + @JoinColumn(\"created_by_id\") → User) — Spec #758 Account 단독 전환")
        fun createdByFk() {
            val field = Account::class.java.getDeclaredField("createdBy")
            assertThat(field.type).isEqualTo(User::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("created_by_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("lastModifiedBy FK (@ManyToOne + @JoinColumn(\"last_modified_by_id\") → User) — Spec #758 Account 단독 전환")
        fun lastModifiedByFk() {
            val field = Account::class.java.getDeclaredField("lastModifiedBy")
            assertThat(field.type).isEqualTo(User::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("last_modified_by_id")
        }

        @Test
        @DisplayName("parent FK (@ManyToOne + @JoinColumn(\"parent_id\") → Account self)")
        fun parentFk() {
            val field = Account::class.java.getDeclaredField("parent")
            assertThat(field.type).isEqualTo(Account::class.java)
            assertThat(field.isAnnotationPresent(ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(JoinColumn::class.java).name).isEqualTo("parent_id")
        }

        @Test
        @DisplayName("BaseEntity CreatedDate / LastModifiedDate 매핑이 Account 매핑 결과에 포함")
        fun baseEntityMappingIncluded() {
            val mapping = SFSchemaUtils.getSFMapping(Account::class.java)
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC9 — Picklist enum + Converter (Q5 옵션 1 + Q5-1 옵션 2)")
    inner class PicklistEnum {

        @Test
        @DisplayName("FreezerType enum 2개 멤버 + 한국어 displayName")
        fun freezerTypeEnumMembers() {
            assertThat(FreezerType.entries).hasSize(2)
            assertThat(FreezerType.LARGE.displayName).isEqualTo("대")
            assertThat(FreezerType.MEDIUM.displayName).isEqualTo("중")
        }

        @Test
        @DisplayName("FreezerTypeConverter — enum ↔ 한국어 String 양방향 변환")
        fun freezerTypeConverter() {
            val converter = FreezerTypeConverter()
            assertThat(converter.convertToDatabaseColumn(FreezerType.LARGE)).isEqualTo("대")
            assertThat(converter.convertToDatabaseColumn(FreezerType.MEDIUM)).isEqualTo("중")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("대")).isEqualTo(FreezerType.LARGE)
            assertThat(converter.convertToEntityAttribute("중")).isEqualTo(FreezerType.MEDIUM)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
        }

        @Test
        @DisplayName("Account.accountType 필드는 String 타입 (거래처유형마스터 Name raw 값, @Convert 미부착)")
        fun accountTypeFieldIsRawString() {
            // enum + AccountTypeConverter 제거 — 운영 마스터(AccountCategoryMaster) 에서 유형이 추가/변경돼도
            // DB read 시 null 로 소실되지 않도록 raw String 으로 보관한다.
            val field = Account::class.java.getDeclaredField("accountType")
            assertThat(field.type).isEqualTo(String::class.java)
            assertThat(field.getAnnotation(Convert::class.java)).isNull()
        }

        @Test
        @DisplayName("Account.freezerType 필드에 @Convert(FreezerTypeConverter) 부착")
        fun freezerTypeFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("freezerType")
            assertThat(field.type).isEqualTo(FreezerType::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(FreezerTypeConverter::class.java)
        }
    }

    @Nested
    @DisplayName("AC11 (#703 §3-2) — Industry / Ownership / Rating / AccountSource enum + Converter")
    inner class PicklistEnumSpec703 {

        @Test
        @DisplayName("Industry enum 32개 멤버 + 영어 displayName 보존")
        fun industryEnumMembers() {
            assertThat(Industry.entries).hasSize(32)
            assertThat(Industry.AGRICULTURE.displayName).isEqualTo("Agriculture")
            assertThat(Industry.FOOD_AND_BEVERAGE.displayName).isEqualTo("Food & Beverage")
            assertThat(Industry.NOT_FOR_PROFIT.displayName).isEqualTo("Not For Profit")
            assertThat(Industry.TELECOMMUNICATIONS.displayName).isEqualTo("Telecommunications")
            assertThat(Industry.OTHER.displayName).isEqualTo("Other")
        }

        @Test
        @DisplayName("Ownership enum 4개 멤버 + 영어 displayName")
        fun ownershipEnumMembers() {
            assertThat(Ownership.entries).hasSize(4)
            assertThat(Ownership.PUBLIC.displayName).isEqualTo("Public")
            assertThat(Ownership.PRIVATE.displayName).isEqualTo("Private")
            assertThat(Ownership.SUBSIDIARY.displayName).isEqualTo("Subsidiary")
            assertThat(Ownership.OTHER.displayName).isEqualTo("Other")
        }

        @Test
        @DisplayName("Rating enum 3개 멤버 + 영어 displayName")
        fun ratingEnumMembers() {
            assertThat(Rating.entries).hasSize(3)
            assertThat(Rating.HOT.displayName).isEqualTo("Hot")
            assertThat(Rating.WARM.displayName).isEqualTo("Warm")
            assertThat(Rating.COLD.displayName).isEqualTo("Cold")
        }

        @Test
        @DisplayName("AccountSource enum 10개 멤버 + 영어 displayName")
        fun accountSourceEnumMembers() {
            assertThat(AccountSource.entries).hasSize(10)
            assertThat(AccountSource.ADVERTISEMENT.displayName).isEqualTo("Advertisement")
            assertThat(AccountSource.CUSTOMER_EVENT.displayName).isEqualTo("Customer Event")
            assertThat(AccountSource.EMPLOYEE_REFERRAL.displayName).isEqualTo("Employee Referral")
            assertThat(AccountSource.GOOGLE_ADWORDS.displayName).isEqualTo("Google AdWords")
            assertThat(AccountSource.PURCHASED_LIST.displayName).isEqualTo("Purchased List")
            assertThat(AccountSource.TRADE_SHOW.displayName).isEqualTo("Trade Show")
        }

        @Test
        @DisplayName("IndustryConverter — enum ↔ 영어 String 양방향 변환")
        fun industryConverter() {
            val converter = IndustryConverter()
            assertThat(converter.convertToDatabaseColumn(Industry.FOOD_AND_BEVERAGE)).isEqualTo("Food & Beverage")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("Food & Beverage")).isEqualTo(Industry.FOOD_AND_BEVERAGE)
            assertThat(converter.convertToEntityAttribute("Not For Profit")).isEqualTo(Industry.NOT_FOR_PROFIT)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("")).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN_VALUE")).isNull()
        }

        @Test
        @DisplayName("OwnershipConverter — enum ↔ 영어 String 양방향 변환")
        fun ownershipConverter() {
            val converter = OwnershipConverter()
            assertThat(converter.convertToDatabaseColumn(Ownership.SUBSIDIARY)).isEqualTo("Subsidiary")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("Public")).isEqualTo(Ownership.PUBLIC)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("RatingConverter — enum ↔ 영어 String 양방향 변환")
        fun ratingConverter() {
            val converter = RatingConverter()
            assertThat(converter.convertToDatabaseColumn(Rating.HOT)).isEqualTo("Hot")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("Cold")).isEqualTo(Rating.COLD)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("AccountSourceConverter — enum ↔ 영어 String 양방향 변환")
        fun accountSourceConverter() {
            val converter = AccountSourceConverter()
            assertThat(converter.convertToDatabaseColumn(AccountSource.EMPLOYEE_REFERRAL)).isEqualTo("Employee Referral")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("Google AdWords")).isEqualTo(AccountSource.GOOGLE_ADWORDS)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("Account.industry 필드에 @Convert(IndustryConverter) 부착 + 타입 Industry")
        fun industryFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("industry")
            assertThat(field.type).isEqualTo(Industry::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(IndustryConverter::class.java)
        }

        @Test
        @DisplayName("Account.ownership 필드에 @Convert(OwnershipConverter) 부착 + 타입 Ownership")
        fun ownershipFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("ownership")
            assertThat(field.type).isEqualTo(Ownership::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(OwnershipConverter::class.java)
        }

        @Test
        @DisplayName("Account.rating 필드에 @Convert(RatingConverter) 부착 + 타입 Rating")
        fun ratingFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("rating")
            assertThat(field.type).isEqualTo(Rating::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(RatingConverter::class.java)
        }

        @Test
        @DisplayName("Account.accountSource 필드에 @Convert(AccountSourceConverter) 부착 + 타입 AccountSource")
        fun accountSourceFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("accountSource")
            assertThat(field.type).isEqualTo(AccountSource::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(AccountSourceConverter::class.java)
        }
    }
}
