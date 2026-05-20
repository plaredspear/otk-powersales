package com.otoki.powersales.user.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #757 — User ↔ Salesforce `User` Object 어노테이션 검증.
 *
 * 단일 권위: Salesforce Object (`User`)
 */
@DisplayName("User SF 어노테이션 검증 (Spec #757)")
class UserEntityTest {

    @Nested
    @DisplayName("AC1 — 클래스 어노테이션 (@Entity / @Table / @SFObject)")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'User'")
        fun sfObjectValue() {
            val annotation = User::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("User")
        }

        @Test
        @DisplayName("@Table.name 은 \"user\" — PostgreSQL reserved keyword double-quote (Q5)")
        fun tableNameQuoted() {
            val annotation = User::class.java.getAnnotation(Table::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.name).isEqualTo("\"user\"")
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = User::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
        }

        @Test
        @DisplayName("PK(id) 컬럼명은 user_id")
        fun idColumnName() {
            val field = User::class.java.getDeclaredField("id")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("user_id")
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (sync buffer)")
        fun sfidHasNoSfField() {
            val field = User::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (도메인 19개 + Group A 3개 + BaseEntity 2개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(User::class.java)

        @Test
        @DisplayName("매핑 키 수 = 24 (도메인 19 + Group A audit 3 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(24)
        }

        @Test
        @DisplayName("도메인 19개 SF API Name → 컬럼명 1:1")
        fun domainMappingValues() {
            assertThat(mapping["Username"]).isEqualTo("username")
            assertThat(mapping["Email"]).isEqualTo("email")
            assertThat(mapping["IsActive"]).isEqualTo("is_active")
            assertThat(mapping["DKRetail__EmployeeNumber__c"]).isEqualTo("employee_code")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["LastName"]).isEqualTo("last_name")
            assertThat(mapping["FirstName"]).isEqualTo("first_name")
            assertThat(mapping["Alias"]).isEqualTo("alias")
            assertThat(mapping["Title"]).isEqualTo("title")
            assertThat(mapping["Department"]).isEqualTo("department")
            assertThat(mapping["Division"]).isEqualTo("division")
            assertThat(mapping["MobilePhone"]).isEqualTo("mobile_phone")
            assertThat(mapping["Phone"]).isEqualTo("phone")
            assertThat(mapping["HR_Code__c"]).isEqualTo("hr_code")
            assertThat(mapping["Branch__c"]).isEqualTo("branch")
            assertThat(mapping["LastLoginDate"]).isEqualTo("last_login_at")
            assertThat(mapping["ManagerId"]).isEqualTo("manager_sfid")
            assertThat(mapping["ProfileId"]).isEqualTo("profile_sfid")
            assertThat(mapping["UserRoleId"]).isEqualTo("user_role_sfid")
        }

        @Test
        @DisplayName("Group A audit 3개 + BaseEntity 2개")
        fun auditMappingValues() {
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }

        @Test
        @DisplayName("매핑 values 에 user_id 미등장 (PK 제외)")
        fun mappingValuesExcludePk() {
            assertThat(mapping.values).doesNotContain("user_id")
        }

        @Test
        @DisplayName("매핑 values 에 sfid / password / password_change_required 미등장 (sync buffer + 인증 정보)")
        fun mappingValuesExcludeNonSfFields() {
            assertThat(mapping.values).doesNotContain("sfid", "password", "password_change_required")
        }
    }

    @Nested
    @DisplayName("AC2 — Unique 제약 (Q1)")
    inner class UniqueConstraint {

        @Test
        @DisplayName("username 컬럼은 unique=true + nullable=false (Q1)")
        fun usernameUnique() {
            val field = User::class.java.getDeclaredField("username")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("username")
            assertThat(column.unique).isTrue()
            assertThat(column.nullable).isFalse()
            assertThat(column.length).isEqualTo(80)
        }

        @Test
        @DisplayName("employee_code 컬럼은 unique=true + nullable=true (V167 부서 공용 계정 적재 허용)")
        fun employeeCodeUnique() {
            val field = User::class.java.getDeclaredField("employeeCode")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("employee_code")
            assertThat(column.unique).isTrue()
            assertThat(column.nullable).isTrue()
            assertThat(column.length).isEqualTo(20)
        }

        @Test
        @DisplayName("sfid 컬럼은 unique=true (sync buffer)")
        fun sfidUnique() {
            val field = User::class.java.getDeclaredField("sfid")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("sfid")
            assertThat(column.unique).isTrue()
            assertThat(column.length).isEqualTo(18)
        }
    }

    @Nested
    @DisplayName("AC3 — 인증 정보 컬럼 (Q4)")
    inner class AuthenticationColumns {

        @Test
        @DisplayName("password 컬럼은 NOT NULL, VARCHAR(255)")
        fun passwordColumn() {
            val field = User::class.java.getDeclaredField("password")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("password")
            assertThat(column.nullable).isFalse()
            assertThat(column.length).isEqualTo(255)
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("password_change_required 컬럼 (V157 에서 nullable 화)")
        fun passwordChangeRequiredColumn() {
            val field = User::class.java.getDeclaredField("passwordChangeRequired")
            val column = field.getAnnotation(Column::class.java)
            assertThat(column.name).isEqualTo("password_change_required")
            // V157: SF Stage 1 raw INSERT 정합 위해 NOT NULL 제거. 기본값은 application 레벨 (true).
            assertThat(column.nullable).isTrue()
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

    @Nested
    @DisplayName("AC4 — Self-reference FK (R-2)")
    inner class SelfReference {

        @Test
        @DisplayName("manager 필드는 User 타입 self-reference")
        fun managerSelfReference() {
            val field = User::class.java.getDeclaredField("manager")
            assertThat(field.type).isEqualTo(User::class.java)
        }

        @Test
        @DisplayName("createdBy 필드는 User 타입 self-reference")
        fun createdBySelfReference() {
            val field = User::class.java.getDeclaredField("createdBy")
            assertThat(field.type).isEqualTo(User::class.java)
        }

        @Test
        @DisplayName("lastModifiedBy 필드는 User 타입 self-reference")
        fun lastModifiedBySelfReference() {
            val field = User::class.java.getDeclaredField("lastModifiedBy")
            assertThat(field.type).isEqualTo(User::class.java)
        }
    }
}
