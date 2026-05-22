package com.otoki.powersales.sfmigration.stage1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Stage1Targets — 신규 SF Sharing 메타 7 entity 등록 검증 (spec #790).
 *
 * - ALL 맵에 7 entity 등록 — XML 메타 출처 6 + SOQL 출처 1 (GroupMember)
 * - DEPENDENCY_ORDER 에 7 entity 등록 (UserRole / Profile / Group 적재 후)
 * - sObjectName — XML 메타 출처는 null (Q1 옵션 1), SOQL 출처는 SF SObject API 명
 * - FieldMapping sfFieldName ↔ extract-sharing-meta.main.kts CSV 헤더 정합
 */
@DisplayName("Stage1Targets — spec #790 신규 7 entity")
class Stage1TargetsTest {

    @Nested
    @DisplayName("ALL 맵 등록")
    inner class AllMapRegistration {

        @Test
        @DisplayName("SharingRule / Condition / Target — XML 메타 출처 (sObjectName null)")
        fun sharingMetaEntities() {
            listOf("SharingRule", "SharingRuleCondition", "SharingRuleTarget").forEach { target ->
                val meta = Stage1Targets.get(target)
                assertThat(meta).withFailMessage("$target 미등록").isNotNull
                assertThat(meta!!.sObjectName).withFailMessage("$target sObjectName 은 XML 메타 출처라 null 이어야 함").isNull()
            }
        }

        @Test
        @DisplayName("UserRoleHierarchySnapshot / ProfileFlags / PermissionSetFlags — XML 메타 출처")
        fun xmlMetaEntities() {
            listOf("UserRoleHierarchySnapshot", "ProfileFlags", "PermissionSetFlags").forEach { target ->
                val meta = Stage1Targets.get(target)
                assertThat(meta).withFailMessage("$target 미등록").isNotNull
                assertThat(meta!!.sObjectName).isNull()
            }
        }

        @Test
        @DisplayName("GroupMember — SOQL 출처 (sObjectName = 'GroupMember')")
        fun groupMember() {
            val meta = Stage1Targets.get("GroupMember")
            assertThat(meta).isNotNull
            assertThat(meta!!.sObjectName).isEqualTo("GroupMember")
            assertThat(meta.tableName).isEqualTo("group_member")
            assertThat(meta.csvFileName).isEqualTo("group_members.csv")
        }
    }

    @Nested
    @DisplayName("FieldMapping sfFieldName ↔ CSV 헤더 정합")
    inner class FieldMappingHeaders {

        @Test
        @DisplayName("SharingRule — extract-sharing-meta.main.kts 의 sharing-rule.csv 헤더 정합")
        fun sharingRuleHeaders() {
            val meta = Stage1Targets.get("SharingRule")!!
            val headers = meta.fields.map { it.sfFieldName }
            // extract-sharing-meta.main.kts 의 CSV 출력 컬럼 정합
            assertThat(headers).containsExactly(
                "developerName", "sObjectName", "ruleType", "label", "accessLevel", "includeOwnedByAll",
            )
        }

        @Test
        @DisplayName("SharingRuleCondition — parentDeveloperName 매핑 to sharing_rule_developer_name")
        fun sharingRuleConditionParentMapping() {
            val meta = Stage1Targets.get("SharingRuleCondition")!!
            val parentMapping = meta.fields.find { it.sfFieldName == "parentDeveloperName" }
            assertThat(parentMapping).isNotNull
            assertThat(parentMapping!!.dbColumnName).isEqualTo("sharing_rule_developer_name")
        }

        @Test
        @DisplayName("SharingRuleCondition — value 매핑 to condition_value (reserved keyword 회피)")
        fun sharingRuleConditionValueMapping() {
            val meta = Stage1Targets.get("SharingRuleCondition")!!
            val valueMapping = meta.fields.find { it.sfFieldName == "value" }
            assertThat(valueMapping!!.dbColumnName).isEqualTo("condition_value")
        }

        @Test
        @DisplayName("UserRoleHierarchySnapshot — developerName + parentDeveloperName 보유")
        fun userRoleHierarchyHeaders() {
            val meta = Stage1Targets.get("UserRoleHierarchySnapshot")!!
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).contains("developerName", "parentDeveloperName")
        }

        @Test
        @DisplayName("ProfileFlags — profileName 자연 키 + 5 boolean flag")
        fun profileFlagsHeaders() {
            val meta = Stage1Targets.get("ProfileFlags")!!
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "profileName",
                "permissionsViewAllData",
                "permissionsModifyAllData",
                "permissionsViewAllUsers",
                "permissionsManageUsers",
                "permissionsApiEnabled",
            )
        }

        @Test
        @DisplayName("PermissionSetFlags — permissionSetName + objectPermissionsJson")
        fun permissionSetFlagsHeaders() {
            val meta = Stage1Targets.get("PermissionSetFlags")!!
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "permissionSetName",
                "permissionsViewAllData",
                "permissionsModifyAllData",
                "objectPermissionsJson",
            )
        }

        @Test
        @DisplayName("GroupMember — Id / GroupId / UserOrGroupId (SOQL 출처)")
        fun groupMemberHeaders() {
            val meta = Stage1Targets.get("GroupMember")!!
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly("Id", "GroupId", "UserOrGroupId")
            assertThat(meta.fields.find { it.sfFieldName == "Id" }!!.dbColumnName).isEqualTo("sfid")
            assertThat(meta.fields.find { it.sfFieldName == "UserOrGroupId" }!!.dbColumnName).isEqualTo("user_or_group_sfid")
        }
    }

    @Nested
    @DisplayName("DEPENDENCY_ORDER 등록")
    inner class DependencyOrder {

        @Test
        @DisplayName("7 entity 모두 DEPENDENCY_ORDER 에 등록")
        fun allRegistered() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            listOf(
                "SharingRule", "SharingRuleCondition", "SharingRuleTarget",
                "UserRoleHierarchySnapshot", "ProfileFlags", "PermissionSetFlags",
                "GroupMember",
            ).forEach { target ->
                assertThat(order).withFailMessage("$target 가 DEPENDENCY_ORDER 미등록").contains(target)
            }
        }

        @Test
        @DisplayName("UserRoleHierarchySnapshot 은 UserRole 뒤")
        fun userRoleHierarchyAfterUserRole() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            val userRoleIdx = order.indexOf("UserRole")
            val snapshotIdx = order.indexOf("UserRoleHierarchySnapshot")
            assertThat(userRoleIdx).isGreaterThanOrEqualTo(0)
            assertThat(snapshotIdx).isGreaterThan(userRoleIdx)
        }

        @Test
        @DisplayName("ProfileFlags 는 Profile 뒤")
        fun profileFlagsAfterProfile() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            assertThat(order.indexOf("ProfileFlags")).isGreaterThan(order.indexOf("Profile"))
        }

        @Test
        @DisplayName("GroupMember 는 Group 뒤")
        fun groupMemberAfterGroup() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            assertThat(order.indexOf("GroupMember")).isGreaterThan(order.indexOf("Group"))
        }
    }

    @Nested
    @DisplayName("spec #791 — SObjectSetting / SObjectRelation 등록")
    inner class SObjectSettingRegistration {

        @Test
        @DisplayName("SObjectSetting — sObjectName null (XML 메타 출처) + 4 fields")
        fun sObjectSetting() {
            val meta = Stage1Targets.get("SObjectSetting")
            assertThat(meta).withFailMessage("SObjectSetting 미등록").isNotNull
            assertThat(meta!!.sObjectName).isNull()
            assertThat(meta.tableName).isEqualTo("sobject_setting")
            assertThat(meta.csvFileName).isEqualTo("sobject-setting.csv")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "sObjectName", "orgWideDefault", "allowHierarchyGrant", "parentSObjectName",
            )
        }

        @Test
        @DisplayName("SObjectRelation — sObjectName null (XML 메타 출처) + 4 fields")
        fun sObjectRelation() {
            val meta = Stage1Targets.get("SObjectRelation")
            assertThat(meta).withFailMessage("SObjectRelation 미등록").isNotNull
            assertThat(meta!!.sObjectName).isNull()
            assertThat(meta.tableName).isEqualTo("sobject_relation")
            assertThat(meta.csvFileName).isEqualTo("sobject-relation.csv")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "childSObjectName", "parentSObjectName", "relationFieldName", "isMasterDetail",
            )
        }

        @Test
        @DisplayName("DEPENDENCY_ORDER 에 SObjectSetting / SObjectRelation 등록")
        fun dependencyOrder() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            assertThat(order).contains("SObjectSetting", "SObjectRelation")
        }
    }

    @Nested
    @DisplayName("spec #794 — RecordType / ProfileRecordType / PermissionSetRecordType 등록")
    inner class RecordTypeRegistration {

        @Test
        @DisplayName("RecordType — sObjectName null + 5 fields")
        fun recordType() {
            val meta = Stage1Targets.get("RecordType")
            assertThat(meta).isNotNull
            assertThat(meta!!.sObjectName).isNull()
            assertThat(meta.tableName).isEqualTo("record_type")
            assertThat(meta.csvFileName).isEqualTo("record-type.csv")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "sObjectName", "developerName", "label", "description", "isActive",
            )
        }

        @Test
        @DisplayName("ProfileRecordType — 5 fields + sObjectName 자연 키")
        fun profileRecordType() {
            val meta = Stage1Targets.get("ProfileRecordType")
            assertThat(meta).isNotNull
            assertThat(meta!!.tableName).isEqualTo("profile_record_type")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "profileName", "sObjectName", "recordTypeDeveloperName", "visible", "isDefault",
            )
        }

        @Test
        @DisplayName("PermissionSetRecordType — 5 fields")
        fun permissionSetRecordType() {
            val meta = Stage1Targets.get("PermissionSetRecordType")
            assertThat(meta).isNotNull
            assertThat(meta!!.tableName).isEqualTo("permission_set_record_type")
        }

        @Test
        @DisplayName("DEPENDENCY_ORDER 등록")
        fun dependencyOrder() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            assertThat(order).contains("RecordType", "ProfileRecordType", "PermissionSetRecordType")
        }
    }

    @Nested
    @DisplayName("spec #795 — ProfileFieldPermission / PermissionSetFieldPermission 등록")
    inner class FieldPermissionRegistration {

        @Test
        @DisplayName("ProfileFieldPermission — 5 fields + sObjectName 자연 키")
        fun profileFieldPermission() {
            val meta = Stage1Targets.get("ProfileFieldPermission")
            assertThat(meta).isNotNull
            assertThat(meta!!.tableName).isEqualTo("profile_field_permission")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "profileName", "sObjectName", "fieldName", "readable", "editable",
            )
        }

        @Test
        @DisplayName("PermissionSetFieldPermission — 5 fields")
        fun permissionSetFieldPermission() {
            val meta = Stage1Targets.get("PermissionSetFieldPermission")
            assertThat(meta).isNotNull
            assertThat(meta!!.tableName).isEqualTo("permission_set_field_permission")
        }

        @Test
        @DisplayName("DEPENDENCY_ORDER 등록")
        fun dependencyOrder() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            assertThat(order).contains("ProfileFieldPermission", "PermissionSetFieldPermission")
        }
    }
}
