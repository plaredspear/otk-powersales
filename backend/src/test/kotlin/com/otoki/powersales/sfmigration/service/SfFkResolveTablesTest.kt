package com.otoki.powersales.sfmigration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("deriveFkResolveSpec 동작 테스트")
class SfFkResolveTablesTest {

    @Nested
    @DisplayName("기본 자동 추론")
    inner class AutoInference {

        @Test
        @DisplayName("FK_PREFIX_MAPPING 에 명시되지 않은 prefix 는 prefix 자체를 테이블명 + <prefix>_id 로 추론")
        fun autoInferUnmappedPrefix() {
            val spec = deriveFkResolveSpec("unknown_table_sfid")
            assertThat(spec).isNotNull
            assertThat(spec!!.sfidColumn).isEqualTo("unknown_table_sfid")
            assertThat(spec.idColumn).isEqualTo("unknown_table_id")
            assertThat(spec.refTable).isEqualTo("unknown_table")
            assertThat(spec.refIdColumn).isEqualTo("unknown_table_id")
        }
    }

    @Nested
    @DisplayName("Audit FK (User lookup)")
    inner class AuditFk {

        @Test
        @DisplayName("created_by_sfid → created_by_id, ref user.user_id")
        fun createdBy() {
            val spec = deriveFkResolveSpec("created_by_sfid")!!
            assertThat(spec.idColumn).isEqualTo("created_by_id")
            assertThat(spec.refTable).isEqualTo("user")
            assertThat(spec.refIdColumn).isEqualTo("user_id")
        }

        @Test
        @DisplayName("last_modified_by_sfid → last_modified_by_id, ref user.user_id")
        fun lastModifiedBy() {
            val spec = deriveFkResolveSpec("last_modified_by_sfid")!!
            assertThat(spec.idColumn).isEqualTo("last_modified_by_id")
            assertThat(spec.refTable).isEqualTo("user")
        }

        @Test
        @DisplayName("owner_sfid → owner_user_id (polymorphic 대응), ref user.user_id")
        fun ownerPolymorphicDefault() {
            val spec = deriveFkResolveSpec("owner_sfid")!!
            assertThat(spec.idColumn).isEqualTo("owner_user_id")
            assertThat(spec.refTable).isEqualTo("user")
            assertThat(spec.refIdColumn).isEqualTo("user_id")
        }
    }

    @Nested
    @DisplayName("self-reference / alias prefix")
    inner class AliasPrefix {

        @Test
        @DisplayName("manager_sfid → manager_id, ref employee.employee_id")
        fun managerAlias() {
            val spec = deriveFkResolveSpec("manager_sfid")!!
            assertThat(spec.idColumn).isEqualTo("manager_id")
            assertThat(spec.refTable).isEqualTo("employee")
            assertThat(spec.refIdColumn).isEqualTo("employee_id")
        }

        @Test
        @DisplayName("parent_sfid → parent_id, ref account.account_id")
        fun parentAlias() {
            val spec = deriveFkResolveSpec("parent_sfid")!!
            assertThat(spec.refTable).isEqualTo("account")
        }

        @Test
        @DisplayName("category_sfid → category_id, ref employee_input_criteria_master")
        fun categoryAlias() {
            val spec = deriveFkResolveSpec("category_sfid")!!
            assertThat(spec.refTable).isEqualTo("employee_input_criteria_master")
        }
    }

    @Nested
    @DisplayName("Skip / 제외 케이스")
    inner class SkipCases {

        @Test
        @DisplayName("sfid 단독은 null (entity 자기 자신 PK lookup 용)")
        fun standaloneSfid() {
            assertThat(deriveFkResolveSpec("sfid")).isNull()
        }

        @Test
        @DisplayName("_sfid 로 끝나지 않으면 null")
        fun nonSfidColumn() {
            assertThat(deriveFkResolveSpec("employee_code")).isNull()
            assertThat(deriveFkResolveSpec("name")).isNull()
        }

        @Test
        @DisplayName("SKIP_FK_PREFIXES 에 등록된 prefix 는 null")
        fun skipPrefixes() {
            assertThat(deriveFkResolveSpec("product_code_sfid")).isNull()
            assertThat(deriveFkResolveSpec("record_type_sfid")).isNull()
            assertThat(deriveFkResolveSpec("related_sfid")).isNull()
        }
    }

    @Nested
    @DisplayName("Profile / UserRole FK (Spec #780)")
    inner class ProfileAndUserRoleFk {

        @Test
        @DisplayName("profile_sfid → profile_id, ref profile.profile_id")
        fun profileFk() {
            val spec = deriveFkResolveSpec("profile_sfid")!!
            assertThat(spec.idColumn).isEqualTo("profile_id")
            assertThat(spec.refTable).isEqualTo("profile")
            assertThat(spec.refIdColumn).isEqualTo("profile_id")
        }

        @Test
        @DisplayName("user_role_sfid → user_role_id, ref user_role.user_role_id")
        fun userRoleFk() {
            val spec = deriveFkResolveSpec("user_role_sfid")!!
            assertThat(spec.idColumn).isEqualTo("user_role_id")
            assertThat(spec.refTable).isEqualTo("user_role")
            assertThat(spec.refIdColumn).isEqualTo("user_role_id")
        }

        @Test
        @DisplayName("parent_user_role_sfid → parent_user_role_id, ref user_role.user_role_id (자기참조)")
        fun parentUserRoleSelfReference() {
            val spec = deriveFkResolveSpec("parent_user_role_sfid")!!
            assertThat(spec.idColumn).isEqualTo("parent_user_role_id")
            assertThat(spec.refTable).isEqualTo("user_role")
            assertThat(spec.refIdColumn).isEqualTo("user_role_id")
        }
    }
}
