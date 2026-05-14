package com.otoki.powersales.employee.entity

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import com.otoki.powersales.employee.entity.converter.CrmWorkTypeConverter
import com.otoki.powersales.employee.entity.converter.GenderConverter
import com.otoki.powersales.employee.enums.CrmWorkType
import com.otoki.powersales.employee.enums.Gender
import jakarta.persistence.Convert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #607 / #713 — Employee ↔ Salesforce `DKRetail__Employee__c` 어노테이션 부착 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`DKRetail__Employee__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (44개 — 38 기존 + 6 신규 (#713))
 *   - AC3: PK 미부착
 *   - AC5: Picklist 정합 (Gender ↔ 남/여, UserRole ↔ 조장/여사원/지점장)
 *   - AC10 (#713): Group A 신규 어노테이션 + Reference FK 검증
 *   - AC11 (#713): Gender GenderConverter + CrmWorkType enum + CrmWorkTypeConverter 검증
 */
@DisplayName("Employee SF 어노테이션 검증 (Spec #607 / #713)")
class EmployeeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Employee__c'")
        fun sfObjectValue() {
            val annotation = Employee::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Employee__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Employee::class.java)

        @Test
        @DisplayName("매핑 키 수 = 43 (prnflag__c prod 부재 제거 후)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(43)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 3개 신규 부착")
        fun section62MissingMappings() {
            assertThat(mapping["DKRetail__WorkEmail__c"]).isEqualTo("work_email")
            assertThat(mapping["DKRetail__Email__c"]).isEqualTo("email")
            assertThat(mapping["ProfessionalPromotionTeam__c"]).isEqualTo("professional_promotion_team")
        }

        @Test
        @DisplayName("§6.3 — 신규 7개 필드 매핑 (Q2 옵션 1: dkCostCenterCode 포함)")
        fun section63NewFields() {
            assertThat(mapping["DKRetail__CostCenterCode__c"]).isEqualTo("dk_cost_center_code")
            assertThat(mapping["DKRetail__LocationCode__c"]).isEqualTo("location_code")
            assertThat(mapping["DKRetail__TotalAnnualLeave__c"]).isEqualTo("total_annual_leave")
            assertThat(mapping["DKRetail__UsedAnnualLeave__c"]).isEqualTo("used_annual_leave")
            assertThat(mapping["DKRetail__ManagerId__c"]).isEqualTo("manager_sfid")
            assertThat(mapping["PostponedAppointment__c"]).isEqualTo("postponed_appointment_sfid")
            assertThat(mapping["LockingFlag__c"]).isEqualTo("locking_flag")
        }

        @Test
        @DisplayName("기존 OK 25개 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["DKRetail__EmpCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__AppAuthority__c"]).isEqualTo("role")
            assertThat(mapping["DKRetail__Sex__c"]).isEqualTo("gender")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Employee::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례 — HCColumn 만)")
        fun sfidHasNoSfField() {
            val field = Employee::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 employee_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Employee::class.java)
            assertThat(mapping.values).doesNotContain("employee_id")
        }
    }

    @Nested
    @DisplayName("AC5 — Picklist 정합 검증 (Q3 옵션 1: 정합 단언만)")
    inner class PicklistConsistency {

        @Test
        @DisplayName("Gender displayName ↔ SF Sex picklist 한국어 (남/여) 1:1")
        fun genderSfMapping() {
            assertThat(Gender.MALE.displayName).isEqualTo("남")
            assertThat(Gender.FEMALE.displayName).isEqualTo("여")
        }

        @Test
        @DisplayName("UserRole.fromKorean 이 SF AppAuthority 4값 전체를 enum 매핑 (조장/여사원/지점장/AccountViewAll)")
        fun userRoleSfMappingAll() {
            assertThat(UserRole.fromKorean("조장")).isEqualTo(UserRole.LEADER)
            assertThat(UserRole.fromKorean("여사원")).isEqualTo(UserRole.WOMAN)
            assertThat(UserRole.fromKorean("지점장")).isEqualTo(UserRole.BRANCH_MANAGER)
            assertThat(UserRole.fromKorean("AccountViewAll")).isEqualTo(UserRole.ACCOUNT_VIEW_ALL)
        }
    }

    @Nested
    @DisplayName("AC10 (#713) — Group A 신규 어노테이션 + Reference FK 검증")
    inner class Spec713GroupAAndReference {

        @Test
        @DisplayName("is_deleted 필드에 @SFField(\"IsDeleted\") 신규 부착")
        fun isDeletedHasSfField() {
            val field = Employee::class.java.getDeclaredField("isDeleted")
            val annotation = field.getAnnotation(SFField::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("IsDeleted")
        }

        @Test
        @DisplayName("owner_sfid 필드 + @SFField(\"OwnerId\") + @HCColumn(\"ownerid\") + length 18")
        fun ownerSfidField() {
            val field = Employee::class.java.getDeclaredField("ownerSfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("owner_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("OwnerId")
            assertThat(field.getAnnotation(com.otoki.powersales.common.salesforce.HCColumn::class.java).value).isEqualTo("ownerid")
        }

        @Test
        @DisplayName("created_by_sfid 필드 + @SFField(\"CreatedById\") + @HCColumn(\"createdbyid\") + length 18")
        fun createdBySfidField() {
            val field = Employee::class.java.getDeclaredField("createdBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("created_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("CreatedById")
            assertThat(field.getAnnotation(com.otoki.powersales.common.salesforce.HCColumn::class.java).value).isEqualTo("createdbyid")
        }

        @Test
        @DisplayName("last_modified_by_sfid 필드 + @SFField(\"LastModifiedById\") + @HCColumn(\"lastmodifiedbyid\") + length 18")
        fun lastModifiedBySfidField() {
            val field = Employee::class.java.getDeclaredField("lastModifiedBySfid")
            assertThat(field.type).isEqualTo(String::class.java)
            val column = field.getAnnotation(jakarta.persistence.Column::class.java)
            assertThat(column.name).isEqualTo("last_modified_by_sfid")
            assertThat(column.length).isEqualTo(18)
            assertThat(field.getAnnotation(SFField::class.java).value).isEqualTo("LastModifiedById")
            assertThat(field.getAnnotation(com.otoki.powersales.common.salesforce.HCColumn::class.java).value).isEqualTo("lastmodifiedbyid")
        }

        @Test
        @DisplayName("ownerUser FK (@ManyToOne + @JoinColumn(\"owner_user_id\") → User, polymorphic 분기, @SFField 미부착)")
        fun ownerUserFk() {
            val field = Employee::class.java.getDeclaredField("ownerUser")
            assertThat(field.type).isEqualTo(com.otoki.powersales.user.entity.User::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("owner_user_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("ownerGroup FK (@ManyToOne + @JoinColumn(\"owner_group_id\") → Group, polymorphic 분기, @SFField 미부착)")
        fun ownerGroupFk() {
            val field = Employee::class.java.getDeclaredField("ownerGroup")
            assertThat(field.type).isEqualTo(Group::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("owner_group_id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("createdBy FK (@ManyToOne + @JoinColumn(\"created_by_id\") → User)")
        fun createdByFk() {
            val field = Employee::class.java.getDeclaredField("createdBy")
            assertThat(field.type).isEqualTo(com.otoki.powersales.user.entity.User::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("created_by_id")
        }

        @Test
        @DisplayName("lastModifiedBy FK (@ManyToOne + @JoinColumn(\"last_modified_by_id\") → User)")
        fun lastModifiedByFk() {
            val field = Employee::class.java.getDeclaredField("lastModifiedBy")
            assertThat(field.type).isEqualTo(com.otoki.powersales.user.entity.User::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("last_modified_by_id")
        }

        @Test
        @DisplayName("manager FK (@ManyToOne + @JoinColumn(\"manager_id\") → Employee self)")
        fun managerFk() {
            val field = Employee::class.java.getDeclaredField("manager")
            assertThat(field.type).isEqualTo(Employee::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.ManyToOne::class.java)).isTrue()
            assertThat(field.getAnnotation(jakarta.persistence.JoinColumn::class.java).name).isEqualTo("manager_id")
        }
    }

    @Nested
    @DisplayName("AC11 (#713) — Gender GenderConverter + CrmWorkType enum + CrmWorkTypeConverter")
    inner class Spec713PicklistEnum {

        @Test
        @DisplayName("GenderConverter — SF 원본값(`남`/`여`) 양방향 변환 + backward compat(`MALE`/`FEMALE`)")
        fun genderConverter() {
            val converter = GenderConverter()
            assertThat(converter.convertToDatabaseColumn(Gender.MALE)).isEqualTo("남")
            assertThat(converter.convertToDatabaseColumn(Gender.FEMALE)).isEqualTo("여")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("남")).isEqualTo(Gender.MALE)
            assertThat(converter.convertToEntityAttribute("여")).isEqualTo(Gender.FEMALE)
            assertThat(converter.convertToEntityAttribute("MALE")).isEqualTo(Gender.MALE)
            assertThat(converter.convertToEntityAttribute("FEMALE")).isEqualTo(Gender.FEMALE)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("")).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("CrmWorkType enum 1개 멤버 + SF 원본 displayName(`-`)")
        fun crmWorkTypeEnum() {
            assertThat(CrmWorkType.entries).hasSize(1)
            assertThat(CrmWorkType.HYPHEN.displayName).isEqualTo("-")
        }

        @Test
        @DisplayName("CrmWorkTypeConverter — enum ↔ SF 원본값('-') 양방향 변환")
        fun crmWorkTypeConverter() {
            val converter = CrmWorkTypeConverter()
            assertThat(converter.convertToDatabaseColumn(CrmWorkType.HYPHEN)).isEqualTo("-")
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
            assertThat(converter.convertToEntityAttribute("-")).isEqualTo(CrmWorkType.HYPHEN)
            assertThat(converter.convertToEntityAttribute(null)).isNull()
            assertThat(converter.convertToEntityAttribute("")).isNull()
            assertThat(converter.convertToEntityAttribute("UNKNOWN")).isNull()
        }

        @Test
        @DisplayName("Employee.gender 필드에 @Convert(GenderConverter) 부착 + @Enumerated 미부착")
        fun genderFieldConverterAnnotation() {
            val field = Employee::class.java.getDeclaredField("gender")
            assertThat(field.type).isEqualTo(Gender::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(GenderConverter::class.java)
            assertThat(field.isAnnotationPresent(jakarta.persistence.Enumerated::class.java)).isFalse()
        }

        @Test
        @DisplayName("Employee.crmWorkType 필드에 @Convert(CrmWorkTypeConverter) 부착 + 타입 CrmWorkType")
        fun crmWorkTypeFieldConverterAnnotation() {
            val field = Employee::class.java.getDeclaredField("crmWorkType")
            assertThat(field.type).isEqualTo(CrmWorkType::class.java)
            val convert = field.getAnnotation(Convert::class.java)
            assertThat(convert).isNotNull
            assertThat(convert.converter.java).isEqualTo(CrmWorkTypeConverter::class.java)
        }
    }
}
