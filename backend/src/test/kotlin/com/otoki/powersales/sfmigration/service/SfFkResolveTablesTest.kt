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
        @DisplayName("manager_sfid (sourceTable 없음) → manager_id, ref employee.employee_id (기본값)")
        fun managerAlias() {
            val spec = deriveFkResolveSpec("manager_sfid")!!
            assertThat(spec.idColumn).isEqualTo("manager_id")
            assertThat(spec.refTable).isEqualTo("employee")
            assertThat(spec.refIdColumn).isEqualTo("employee_id")
        }

        @Test
        @DisplayName("employee.manager_sfid → manager_id, ref employee.employee_id (Employee self-reference)")
        fun managerAliasEmployeeScoped() {
            val spec = deriveFkResolveSpec("manager_sfid", "employee")!!
            assertThat(spec.idColumn).isEqualTo("manager_id")
            assertThat(spec.refTable).isEqualTo("employee")
            assertThat(spec.refIdColumn).isEqualTo("employee_id")
        }

        @Test
        @DisplayName("user.manager_sfid → manager_id, ref user.user_id (User self-reference override)")
        fun managerAliasUserScoped() {
            val spec = deriveFkResolveSpec("manager_sfid", "user")!!
            assertThat(spec.idColumn).isEqualTo("manager_id")
            assertThat(spec.refTable).isEqualTo("user")
            assertThat(spec.refIdColumn).isEqualTo("user_id")
        }

        @Test
        @DisplayName("parent_sfid → parent_id, ref account.account_id")
        fun parentAlias() {
            val spec = deriveFkResolveSpec("parent_sfid")!!
            assertThat(spec.refTable).isEqualTo("account")
        }

        @Test
        @DisplayName("category_sfid → category_id, ref account_category_master.account_category_master_id")
        fun categoryAlias() {
            val spec = deriveFkResolveSpec("category_sfid")!!
            assertThat(spec.idColumn).isEqualTo("category_id")
            assertThat(spec.refTable).isEqualTo("account_category_master")
            assertThat(spec.refIdColumn).isEqualTo("account_category_master_id")
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

        @Test
        @DisplayName("theme_sfid → inspection_theme_id (SiteActivity.ThemeId__c alias), ref inspection_theme.inspection_theme_id")
        fun themeAlias() {
            val spec = deriveFkResolveSpec("theme_sfid")!!
            assertThat(spec.sfidColumn).isEqualTo("theme_sfid")
            assertThat(spec.idColumn).isEqualTo("inspection_theme_id")
            assertThat(spec.refTable).isEqualTo("inspection_theme")
            assertThat(spec.refIdColumn).isEqualTo("inspection_theme_id")
        }

        @Test
        @DisplayName("product_code_sfid → product_code_id, ref product.product_id (NewProduct.Product_Code__c = reference/18)")
        fun productCodeAlias() {
            val spec = deriveFkResolveSpec("product_code_sfid")!!
            assertThat(spec.idColumn).isEqualTo("product_code_id")
            assertThat(spec.refTable).isEqualTo("product")
            assertThat(spec.refIdColumn).isEqualTo("product_id")
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
            // record_type 은 SKIP 에서 FK_PREFIX_MAPPING 으로 이동 — record_type_sfid 는 더 이상 null 아님
            // product_code 도 SF reference/18 정상 lookup 임이 확인돼 SKIP 에서 제거 — productCodeAlias() 로 별도 검증
            assertThat(deriveFkResolveSpec("related_sfid")).isNull()
        }

        @Test
        @DisplayName("user_or_group_sfid → null (spec #790 POLYMORPHIC_USER_OR_GROUP_TABLES 가 별도 처리)")
        fun userOrGroupPolymorphic() {
            assertThat(deriveFkResolveSpec("user_or_group_sfid")).isNull()
        }

        @Test
        @DisplayName("target_sfid → null (sharing_rule_target 의 target_id 는 NaturalKey FK Service 가 target_developer_name + target_type 분기 처리)")
        fun targetPolymorphic() {
            assertThat(deriveFkResolveSpec("target_sfid")).isNull()
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

        @Test
        @DisplayName("site_activity 는 POLYMORPHIC_OWNER_TABLES 에 포함 — owner_sfid 의 005/00G 분기 처리")
        fun siteActivityPolymorphicOwner() {
            // SF DKRetail__SiteAcitivity__c.OwnerId.referenceTo = [Group, User].
            // 미등록 시 owner_group_id (00G owner) 가 NULL 로 남는다.
            assertThat(POLYMORPHIC_OWNER_TABLES).contains("site_activity")
        }

        @Test
        @DisplayName("owner_user_id + owner_group_id + XOR 를 가진 16개 entity 가 모두 POLYMORPHIC_OWNER_TABLES 에 등록")
        fun polymorphicOwnerSixteenTables() {
            // 각 전용 SF align 마이그레이션(V112~V146)으로 owner_group_id ADD 확정.
            // 미등록 시 Group(00G) 소유 row 의 owner_user_id/owner_group_id 가 둘 다 NULL.
            // account(OwnerId=User 단독) / group(=[Organization,User]) 은 polymorphic XOR 대상 아님 — 미포함.
            assertThat(POLYMORPHIC_OWNER_TABLES).contains(
                "account_category_master",
                "agreement_history",
                "agreement_word",
                "alternative_holiday",
                "appointment",
                "attend_info",
                "attendance_log",
                "claim",
                "display_work_schedule",
                "employee",
                "employee_input_criteria_master",
                "erp_order_product",
                "monthly_female_employee_integration_schedule",
                "notice",
                "product",
                "team_member_schedule",
            )
            assertThat(POLYMORPHIC_OWNER_TABLES).doesNotContain("account", "group")
        }
    }

    @Nested
    @DisplayName("자연 키 FK 매핑 (spec #790)")
    inner class NaturalKeyFk {

        @Test
        @DisplayName("sharing_rule_condition / sharing_rule_target — NATURAL_KEY_FK_MAPPINGS 에서 제외 (복합 자연 키 전용 method 처리)")
        fun sharingRuleSubtableExcluded() {
            // (s_object_name, developer_name) 복합 키 필요 — 단일 NaturalKeyFkSpec 표현 불가.
            // SfMigrationStage2NaturalKeyFkService.resolveSharingRuleSubtableFk() 전용 method 처리.
            val conditionSpec = NATURAL_KEY_FK_MAPPINGS.find { it.sourceTable == "sharing_rule_condition" }
            val targetSpec = NATURAL_KEY_FK_MAPPINGS.find { it.sourceTable == "sharing_rule_target" }
            assertThat(conditionSpec).isNull()
            assertThat(targetSpec).isNull()
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
        @DisplayName("NATURAL_KEY_FK_MAPPINGS 8 entry — #790 sharing 2 (subtable 2개 제외) + #794 record type 2 + #795 FLS 2 + #798 PSA 1 + permission_set_flags 1")
        fun naturalKeyMappingsCount() {
            // user_role_hierarchy_snapshot + profile_flags +
            // profile_record_type + permission_set_record_type + profile_field_permission + permission_set_field_permission +
            // permission_set_assignment + permission_set_flags = 8
            // (sharing_rule_condition + sharing_rule_target 은 (s_object_name, developer_name) 복합 키라 전용 method 처리)
            assertThat(NATURAL_KEY_FK_MAPPINGS).hasSize(8)
        }

        @Test
        @DisplayName("permission_set_flags.permission_set_name → permission_set.permission_set_id — 본 매핑 부재 시 PermissionSetDetailPage 사용자 추가 버튼 미렌더링 사고 회피")
        fun permissionSetFlagsByName() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "permission_set_flags" && it.sourceColumn == "permission_set_name"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("permission_set")
            assertThat(spec.refColumn).isEqualTo("name")
            assertThat(spec.targetIdColumn).isEqualTo("permission_set_id")
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

    @Nested
    @DisplayName("RecordType FK (Spec #796)")
    inner class RecordTypeFk {

        @Test
        @DisplayName("record_type_sfid → record_type_id, ref record_type.record_type_id")
        fun recordTypeFk() {
            val spec = deriveFkResolveSpec("record_type_sfid")!!
            assertThat(spec.idColumn).isEqualTo("record_type_id")
            assertThat(spec.refTable).isEqualTo("record_type")
            assertThat(spec.refIdColumn).isEqualTo("record_type_id")
        }
    }

    @Nested
    @DisplayName("PermissionSetAssignment FK (Spec #798)")
    inner class PermissionSetAssignmentFk {

        @Test
        @DisplayName("assignee_user_sfid → assignee_user_id, ref user.user_id (FK_PREFIX_MAPPING)")
        fun assigneeUserFk() {
            val spec = deriveFkResolveSpec("assignee_user_sfid")!!
            assertThat(spec.idColumn).isEqualTo("assignee_user_id")
            assertThat(spec.refTable).isEqualTo("user")
            assertThat(spec.refIdColumn).isEqualTo("user_id")
        }

        @Test
        @DisplayName("permission_set_assignment.permission_set_sfid → permission_set_flags_id (NATURAL_KEY_FK_MAPPINGS)")
        fun permissionSetSfidNaturalKey() {
            val spec = NATURAL_KEY_FK_MAPPINGS.find {
                it.sourceTable == "permission_set_assignment" && it.sourceColumn == "permission_set_sfid"
            }
            assertThat(spec).isNotNull
            assertThat(spec!!.refTable).isEqualTo("permission_set_flags")
            assertThat(spec.refColumn).isEqualTo("permission_set_sfid")
            assertThat(spec.targetIdColumn).isEqualTo("permission_set_flags_id")
        }

        @Test
        @DisplayName("NATURAL_KEY_FK_MAPPINGS 8 entry — spec #790 2 (subtable 2개 제외) + #794 2 + #795 2 + #798 1 + permission_set_flags 1")
        fun naturalKeyMappingsCountAfterPSA() {
            assertThat(NATURAL_KEY_FK_MAPPINGS).hasSize(8)
        }
    }
}
