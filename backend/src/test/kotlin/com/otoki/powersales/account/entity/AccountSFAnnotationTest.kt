package com.otoki.powersales.account.entity

import com.otoki.powersales.account.entity.converter.AccountTypeConverter
import com.otoki.powersales.account.entity.converter.FreezerTypeConverter
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Convert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #602 — Account ↔ Salesforce `Account` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/거래처(Account).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (63개 — 22 기존 + 17 SAP 보존 + 23 신규 + 1 Spec #644 OwnerId)
 *   - AC3: PK / FK 미부착
 *   - AC5: 기존 `@HCColumn` 매핑 보존
 *   - AC8: parent_sfid `@SFField("ParentId")` 부착
 *   - AC9: AccountType / FreezerType enum + Converter 검증
 */
@DisplayName("Account SF 어노테이션 검증 (Spec #602)")
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
        @DisplayName("매핑 키 수 = 63 (22 기존 + 17 SAP 보존 + 23 신규 + 1 Spec #644 OwnerId)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(63)
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
    @DisplayName("AC5 — @HCColumn 매핑 보존")
    inner class HcColumnPreservation {

        private val hcMapping = SFSchemaUtils.getHCMapping(Account::class.java)

        @Test
        @DisplayName("기존 @HCColumn 매핑 무변경 (sfid + 22개 매핑 + isdeleted + Spec #644 ownerid = 25개)")
        fun hcMappingUnchanged() {
            assertThat(hcMapping["sfid"]).isEqualTo("sfid")
            assertThat(hcMapping["name"]).isEqualTo("name")
            assertThat(hcMapping["phone"]).isEqualTo("phone")
            assertThat(hcMapping["externalkey__c"]).isEqualTo("external_key")
            assertThat(hcMapping["industry"]).isEqualTo("industry")
            assertThat(hcMapping["werk1_tx__c"]).isEqualTo("werk1_tx")
            assertThat(hcMapping["isdeleted"]).isEqualTo("is_deleted")
            assertThat(hcMapping["ownerid"]).isEqualTo("owner_sfid")
            assertThat(hcMapping).hasSize(25)
        }

        @Test
        @DisplayName("신규 23개 컬럼 + SAP 보존 17개 컬럼에 @HCColumn 미부착")
        fun newColumnsHaveNoHcColumn() {
            assertThat(hcMapping.values).doesNotContain(
                // SAP 보존 17개
                "account_status_name",
                "employee_code",
                "distribution",
                "account_status_code",
                "business_type",
                "business_category",
                "business_license_number",
                "email",
                "division_name",
                "sales_dept_name",
                "consignment_acc",
                "werk1",
                "werk2",
                "werk3",
                "sales_dept_cost_center",
                "division_cost_center",
                // account_type 은 SAP 보존 (Spec #142 도입 시 @HCColumn 미부착)
                // 신규 23개
                "account_number",
                "site",
                "account_source",
                "branch_cost_center",
                "division_code",
                "sales_dept_code",
                "logistics_name",
                "logistics_code",
                "freezer_installed",
                "freezer_type",
                "remaining_credit",
                "total_credit",
                "map_coordinate",
                "order_end_time",
                "first_installed",
                "description",
                "website",
                "fax",
                "annual_revenue",
                "number_of_employees",
                "parent_sfid",
                "rating",
                "ownership",
            )
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
    @DisplayName("AC9 — Picklist enum + Converter (Q5 옵션 1 + Q5-1 옵션 2)")
    inner class PicklistEnum {

        @Test
        @DisplayName("AccountType enum 14개 멤버 + 한국어 displayName")
        fun accountTypeEnumMembers() {
            assertThat(AccountType.entries).hasSize(14)
            assertThat(AccountType.DISCOUNT_STORE.displayName).isEqualTo("할인점")
            assertThat(AccountType.CHAIN.displayName).isEqualTo("체인")
            assertThat(AccountType.NONGHYUP.displayName).isEqualTo("농협")
            assertThat(AccountType.SUPER.displayName).isEqualTo("수퍼")
            assertThat(AccountType.FOOD_MATERIAL.displayName).isEqualTo("식자재")
            assertThat(AccountType.GROUP_FEEDING.displayName).isEqualTo("단체급식")
            assertThat(AccountType.OIL_CONFECTIONERY.displayName).isEqualTo("유지제과")
            assertThat(AccountType.RESTAURANT.displayName).isEqualTo("외식")
            assertThat(AccountType.DEPARTMENT_STORE.displayName).isEqualTo("백화점")
            assertThat(AccountType.CVS.displayName).isEqualTo("C.V.S")
            assertThat(AccountType.AGENCY.displayName).isEqualTo("대리점")
            assertThat(AccountType.MANUFACTURING.displayName).isEqualTo("제조")
            assertThat(AccountType.MILITARY.displayName).isEqualTo("군납")
            assertThat(AccountType.OTHER.displayName).isEqualTo("기타")
        }

        @Test
        @DisplayName("FreezerType enum 2개 멤버 + 한국어 displayName")
        fun freezerTypeEnumMembers() {
            assertThat(FreezerType.entries).hasSize(2)
            assertThat(FreezerType.LARGE.displayName).isEqualTo("대")
            assertThat(FreezerType.MEDIUM.displayName).isEqualTo("중")
        }

        @Test
        @DisplayName("AccountTypeConverter — enum ↔ 한국어 String 양방향 변환")
        fun accountTypeConverter() {
            val converter = AccountTypeConverter()
            // enum → DB 한국어
            assertThat(converter.convertToDatabaseColumn(AccountType.DISCOUNT_STORE)).isEqualTo("할인점")
            assertThat(converter.convertToDatabaseColumn(AccountType.CVS)).isEqualTo("C.V.S")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            // DB 한국어 → enum
            assertThat(converter.convertToEntityAttribute("할인점")).isEqualTo(AccountType.DISCOUNT_STORE)
            assertThat(converter.convertToEntityAttribute("C.V.S")).isEqualTo(AccountType.CVS)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("")).isNull()
            // 매칭 안되는 값 → null (운영 호환)
            assertThat(converter.convertToEntityAttribute("UNKNOWN_VALUE")).isNull()
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
        @DisplayName("Account.accountType 필드에 @Convert(AccountTypeConverter) 부착")
        fun accountTypeFieldConverterAnnotation() {
            val field = Account::class.java.getDeclaredField("accountType")
            assertThat(field.type).isEqualTo(AccountType::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(AccountTypeConverter::class.java)
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
}
