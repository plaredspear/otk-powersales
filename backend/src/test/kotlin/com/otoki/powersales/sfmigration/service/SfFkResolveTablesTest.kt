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

        @Test
        @DisplayName("commute_log_sfid → attendance_log_id (Spec #789 — entity 명 alias), ref attendance_log.attendance_log_id")
        fun commuteLogAlias() {
            val spec = deriveFkResolveSpec("commute_log_sfid")!!
            assertThat(spec.sfidColumn).isEqualTo("commute_log_sfid")
            assertThat(spec.idColumn).isEqualTo("attendance_log_id")
            assertThat(spec.refTable).isEqualTo("attendance_log")
            assertThat(spec.refIdColumn).isEqualTo("attendance_log_id")
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

        @Test
        @DisplayName("user_or_group_sfid → null (spec #790 POLYMORPHIC_USER_OR_GROUP_TABLES 가 별도 처리)")
        fun userOrGroupPolymorphic() {
            assertThat(deriveFkResolveSpec("user_or_group_sfid")).isNull()
        }
    }

    @Nested
    @DisplayName("Polymorphic 화이트리스트 (spec #790)")
    inner class PolymorphicWhitelists {

        @Test
        @DisplayName("POLYMORPHIC_USER_OR_GROUP_TABLES 에 group_member 포함")
        fun userOrGroupTables() {
            assertThat(POLYMORPHIC_USER_OR_GROUP_TABLES).contains("group_member")
        }

        @Test
        @DisplayName("POLYMORPHIC_OWNER_TABLES / POLYMORPHIC_RELATED_TABLES 는 본 spec 변경 없음")
        fun otherPolymorphicTables() {
            // spec #790 은 user_or_group 만 추가 — 기존 owner/related 표는 그대로
            assertThat(POLYMORPHIC_OWNER_TABLES).contains("organization", "order_request")
            assertThat(POLYMORPHIC_RELATED_TABLES).contains("group")
        }
    }

    @Nested
    @DisplayName("자연 키 FK 매핑 (spec #790)")
    inner class NaturalKeyFk {

        @Test
        @DisplayName("sharing_rule_condition.sharing_rule_developer_name → sharing_rule.developer_name → sharing_rule_id")
        fun sharingRuleConditionByDeveloperName() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "sharing_rule_condition" && it.sourceColumn == "sharing_rule_developer_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("sharing_rule")
            assertThat(spec.refColumn).isEqualTo("developer_name")
            assertThat(spec.targetIdColumn).isEqualTo("sharing_rule_id")
        }

        @Test
        @DisplayName("user_role_hierarchy_snapshot.developer_name → user_role.developer_name → user_role_id")
        fun userRoleHierarchyByDeveloperName() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "user_role_hierarchy_snapshot" && it.sourceColumn == "developer_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("user_role")
            assertThat(spec.targetIdColumn).isEqualTo("user_role_id")
        }

        @Test
        @DisplayName("profile_flags.profile_name → profile.name → profile_id")
        fun profileFlagsByName() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "profile_flags" && it.sourceColumn == "profile_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("profile")
            assertThat(spec.refColumn).isEqualTo("name")
            assertThat(spec.targetIdColumn).isEqualTo("profile_id")
        }

        @Test
        @DisplayName("NATURAL_KEY_FK_MAPPINGS 8 entry — #790 sharing 4 + #794 record type 2 + #795 FLS 2")
        fun naturalKeyMappingsCount() {
            // sharing_rule_condition + sharing_rule_target + user_role_hierarchy_snapshot + profile_flags +
            // profile_record_type + permission_set_record_type + profile_field_permission + permission_set_field_permission = 8
            assertThat(NATURAL_KEY_FK_MAPPINGS).hasSize(8)
        }

        @Test
        @DisplayName("spec #795 — profile_field_permission.profile_name → profile.profile_id")
        fun profileFieldPermission() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "profile_field_permission" && it.sourceColumn == "profile_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("profile")
            assertThat(spec.targetIdColumn).isEqualTo("profile_id")
        }

        @Test
        @DisplayName("spec #795 — permission_set_field_permission.permission_set_name → permission_set.permission_set_id")
        fun permissionSetFieldPermission() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "permission_set_field_permission" && it.sourceColumn == "permission_set_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("permission_set")
            assertThat(spec.targetIdColumn).isEqualTo("permission_set_id")
        }

        @Test
        @DisplayName("spec #794 — profile_record_type.profile_name → profile.profile_id")
        fun profileRecordType() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "profile_record_type" && it.sourceColumn == "profile_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("profile")
            assertThat(spec.targetIdColumn).isEqualTo("profile_id")
        }

        @Test
        @DisplayName("spec #794 — permission_set_record_type.permission_set_name → permission_set.permission_set_id")
        fun permissionSetRecordType() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "permission_set_record_type" && it.sourceColumn == "permission_set_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("permission_set")
            assertThat(spec.targetIdColumn).isEqualTo("permission_set_id")
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
