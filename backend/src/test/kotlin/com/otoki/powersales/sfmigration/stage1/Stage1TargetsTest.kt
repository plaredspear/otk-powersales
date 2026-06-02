package com.otoki.powersales.sfmigration.stage1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

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
        @DisplayName("ProfileFlags — profileName 자연 키 + 5 boolean flag + objectPermissionsJson")
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
                "objectPermissionsJson",
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
        @DisplayName("RecordType — SOQL 출처 (sObjectName = 'RecordType') + 6 fields (Id=sfid 포함)")
        fun recordType() {
            val meta = Stage1Targets.get("RecordType")
            assertThat(meta).isNotNull
            // SOQL 출처로 전환 — XML 메타엔 18자리 RecordTypeId 가 없어 record_type.sfid 가 NULL →
            // record_type_sfid → record_type_id FK resolve(sfid 매칭) 전량 미해소였던 결손 해소.
            assertThat(meta!!.sObjectName).isEqualTo("RecordType")
            assertThat(meta.tableName).isEqualTo("record_type")
            assertThat(meta.csvFileName).isEqualTo("record-type.csv")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly(
                "Id", "SobjectType", "DeveloperName", "Name", "Description", "IsActive",
            )
            // Id → sfid 매핑이 존재해야 record_type.sfid 가 채워진다.
            assertThat(meta.fields.find { it.sfFieldName == "Id" }!!.dbColumnName).isEqualTo("sfid")
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

    @Nested
    @DisplayName("spec #796 — PermissionSet 정규 entity 등록")
    inner class PermissionSetRegistration {

        @Test
        @DisplayName("PermissionSet — SOQL 출처 + 3 fields (Id / Name / Label)")
        fun permissionSet() {
            val meta = Stage1Targets.get("PermissionSet")
            assertThat(meta).withFailMessage("PermissionSet 미등록").isNotNull
            assertThat(meta!!.sObjectName).isEqualTo("PermissionSet")
            assertThat(meta.tableName).isEqualTo("permission_set")
            assertThat(meta.csvFileName).isEqualTo("permission_sets.csv")
            val headers = meta.fields.map { it.sfFieldName }
            assertThat(headers).containsExactly("Id", "Name", "Label")
        }

        @Test
        @DisplayName("PermissionSet — Id → sfid, Name → name, Label → label")
        fun fieldColumnMapping() {
            val meta = Stage1Targets.get("PermissionSet")!!
            assertThat(meta.fields.find { it.sfFieldName == "Id" }!!.dbColumnName).isEqualTo("sfid")
            assertThat(meta.fields.find { it.sfFieldName == "Name" }!!.dbColumnName).isEqualTo("name")
            assertThat(meta.fields.find { it.sfFieldName == "Label" }!!.dbColumnName).isEqualTo("label")
        }

        @Test
        @DisplayName("DEPENDENCY_ORDER — PermissionSet 는 PermissionSetFlags 보다 선행 (자연 키 ref side)")
        fun dependencyOrderBeforeFlags() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            val permissionSetIdx = order.indexOf("PermissionSet")
            val flagsIdx = order.indexOf("PermissionSetFlags")
            assertThat(permissionSetIdx).isGreaterThanOrEqualTo(0)
            assertThat(flagsIdx).isGreaterThan(permissionSetIdx)
        }
    }

    @Nested
    @DisplayName("spec #798 — PermissionSetAssignment 등록")
    inner class PermissionSetAssignmentRegistration {

        @Test
        @DisplayName("PermissionSetAssignment — SOQL 출처 + 5 fields")
        fun permissionSetAssignment() {
            val meta = Stage1Targets.get("PermissionSetAssignment")
            assertThat(meta).withFailMessage("PermissionSetAssignment 미등록").isNotNull
            assertThat(meta!!.sObjectName).isEqualTo("PermissionSetAssignment")
            assertThat(meta.tableName).isEqualTo("permission_set_assignment")
            assertThat(meta.csvFileName).isEqualTo("permission_set_assignments.csv")
            val headers = meta.fields.map { it.sfFieldName }
            // SystemModstamp — PSA 는 SF 표준 CreatedDate 미보유 (prod describe 2026-05-24)
            assertThat(headers).containsExactly("Id", "AssigneeId", "PermissionSetId", "IsActive", "SystemModstamp")
        }

        @Test
        @DisplayName("PermissionSetAssignment — Id → sfid, AssigneeId → assignee_user_sfid, PermissionSetId → permission_set_sfid")
        fun fieldColumnMapping() {
            val meta = Stage1Targets.get("PermissionSetAssignment")!!
            assertThat(meta.fields.find { it.sfFieldName == "Id" }!!.dbColumnName).isEqualTo("sfid")
            assertThat(meta.fields.find { it.sfFieldName == "AssigneeId" }!!.dbColumnName).isEqualTo("assignee_user_sfid")
            assertThat(meta.fields.find { it.sfFieldName == "PermissionSetId" }!!.dbColumnName).isEqualTo("permission_set_sfid")
        }

        @Test
        @DisplayName("DEPENDENCY_ORDER — PermissionSetAssignment 는 User / PermissionSetFlags 뒤")
        fun dependencyOrderAfterUserAndFlags() {
            val order = Stage1Targets.DEPENDENCY_ORDER
            val psaIdx = order.indexOf("PermissionSetAssignment")
            assertThat(psaIdx).isGreaterThanOrEqualTo(0)
            assertThat(psaIdx).isGreaterThan(order.indexOf("User"))
            assertThat(psaIdx).isGreaterThan(order.indexOf("PermissionSetFlags"))
        }
    }

    @Nested
    @DisplayName("csvFileName drift 가드 — scripts/extract-* ↔ Stage1Targets 양방향 정합")
    inner class CsvFileNameDriftGuard {

        /**
         * scripts/extract-csv.sh 의 `$OUT_DIR/<file>.csv` + scripts/extract-sharing-meta.main.kts 의
         * `"<file>.csv"` 모두 grep 으로 수집. backend Stage1Targets 의 csvFileName 과 양방향
         * 정합 확인. 한쪽만 변경하면 dev/prod 적재 흐름이 끊어지므로 CI 에서 drift 차단.
         *
         * 본 테스트는 scripts/ 디렉토리가 backend cwd 의 부모에 있다는 worktree 구조를 전제.
         * Gradle 실행 cwd 가 backend/ 이므로 ../scripts/sf-data-migration/ 로 접근.
         *
         * Docker 빌드 컨텍스트는 backend/ 만 COPY 하므로 scripts/ 가 없다.
         * 이 경우 Assumptions 로 skip — 로컬 worktree 에서는 drift guard 동작, Docker 에서는 skip.
         */
        private val scriptsDir = File(System.getProperty("user.dir"))
            .parentFile
            .resolve("scripts/sf-data-migration")

        private val extractCsvSh = scriptsDir.resolve("extract-csv.sh")
        private val extractSharingMetaKts = scriptsDir.resolve("extract-sharing-meta.main.kts")

        /**
         * Stage1 적재 대상이 아닌 중간 산출물 CSV — extract-csv.sh 가 SOQL 로 추출하지만 직접 DB 적재되지
         * 않고 extract-sharing-meta.main.kts 가 읽어 다른 CSV (profile-flags.csv) 로 가공/병합하는 입력.
         *   - profile_object_permissions.csv: ObjectPermissions SOQL (Parent.IsOwnedByProfile=TRUE) 결과.
         *     Profile 별로 묶여 profile-flags.csv 의 objectPermissionsJson 컬럼이 됨.
         */
        private val INTERMEDIATE_CSV_NAMES = setOf("profile_object_permissions.csv")

        /**
         * extract-csv.sh + extract-sharing-meta.main.kts 가 출력하는 모든 CSV 파일명 수집.
         *
         * - extract-csv.sh: `$OUT_DIR/<file>.csv` 패턴
         * - extract-sharing-meta.main.kts: `"<file>.csv"` 리터럴
         *
         * 두 스크립트 모두 hardcode 방식 — backend Stage1Targets 와 별도 SoT 가 없다.
         */
        private fun collectExtractedCsvNames(): Set<String> {
            assumeTrue(extractCsvSh.isFile, "extract-csv.sh 미존재 (Docker 빌드 컨텍스트) — skip: ${extractCsvSh.absolutePath}")
            assumeTrue(
                extractSharingMetaKts.isFile,
                "extract-sharing-meta.main.kts 미존재 (Docker 빌드 컨텍스트) — skip: ${extractSharingMetaKts.absolutePath}",
            )
            val result = mutableSetOf<String>()
            // record-type.csv 처럼 하이픈을 쓰는 산출물(SOQL 출처로 이관)도 매칭하도록 `-` 포함.
            val outDirPattern = Regex("""\${'$'}OUT_DIR/([a-z][a-z_-]*\.csv)""")
            val litPattern = Regex(""""([a-z][a-z-]*\.csv)"""")
            extractCsvSh.useLines { lines ->
                lines.forEach { line ->
                    outDirPattern.findAll(line).forEach { result += it.groupValues[1] }
                }
            }
            extractSharingMetaKts.useLines { lines ->
                lines.forEach { line ->
                    litPattern.findAll(line).forEach { result += it.groupValues[1] }
                }
            }
            // 중간 산출물 (profile-flags.csv 로 병합되는 SOQL 입력) 은 Stage1 적재 대상이 아니므로 제외.
            return result - INTERMEDIATE_CSV_NAMES
        }

        private fun collectRegisteredCsvNames(): Set<String> =
            Stage1Targets.DEPENDENCY_ORDER.mapNotNull { Stage1Targets.get(it)?.csvFileName }.toSet()

        @Test
        @DisplayName("Stage1Targets.csvFileName 은 모두 scripts 의 extract 산출물 파일명에 등장")
        fun allTargetsHaveExtractedCsv() {
            val extracted = collectExtractedCsvNames()
            val registered = collectRegisteredCsvNames()

            val orphanTargets = registered - extracted
            assertThat(orphanTargets)
                .withFailMessage(
                    "Stage1Targets 에 등록되었지만 scripts/extract-* 가 생성하지 않는 CSV: %s",
                    orphanTargets,
                )
                .isEmpty()
        }

        @Test
        @DisplayName("scripts 의 extract 산출물 파일명은 모두 Stage1Targets.csvFileName 에 등록")
        fun allExtractedCsvHaveTargets() {
            val extracted = collectExtractedCsvNames()
            val registered = collectRegisteredCsvNames()

            val orphanExtracted = extracted - registered
            assertThat(orphanExtracted)
                .withFailMessage(
                    "scripts/extract-* 는 생성하지만 Stage1Targets 에 미등록 CSV: %s",
                    orphanExtracted,
                )
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("preClear 멱등성 — DB 자연 키 NULL entity 만 true")
    inner class PreClear {

        /**
         * Stage1 시점 DB 자연 키 (UNIQUE 제약 컬럼) 가 NULL 이라 INSERT ... ON CONFLICT
         * DO NOTHING 의 충돌 매칭이 일어나지 않는 entity 목록. 재실행 시 row 누적되므로
         * preClear = true 강제. dev 운영 중 PSF/PSF_flags/PSRT 의 재실행 누적 사건 (#800
         * 후속) 원인 분석으로 도출.
         */
        private val PRE_CLEAR_REQUIRED = setOf(
            "SharingRule",
            "SharingRuleCondition",
            "SharingRuleTarget",
            "PermissionSetFlags",
            "ProfileRecordType",
            "PermissionSetRecordType",
            "ProfileFieldPermission",
            "PermissionSetFieldPermission",
        )

        @Test
        @DisplayName("7 entity 모두 preClear = true")
        fun preClearRequiredEntitiesAllTrue() {
            PRE_CLEAR_REQUIRED.forEach { targetName ->
                val meta = Stage1Targets.get(targetName)
                    ?: error("$targetName 가 Stage1Targets 에 미등록")
                assertThat(meta.preClear)
                    .withFailMessage("$targetName 은 preClear = true 필요")
                    .isTrue
            }
        }

        @Test
        @DisplayName("sfid UNIQUE 가 NOT NULL 로 채워지는 entity 는 preClear = false (멱등성 ON CONFLICT 보장)")
        fun sfidPopulatedEntitiesPreClearFalse() {
            listOf("User", "Account", "Product", "Employee", "PermissionSet", "GroupMember", "PermissionSetAssignment")
                .forEach { targetName ->
                    val meta = Stage1Targets.get(targetName)
                        ?: error("$targetName 가 Stage1Targets 에 미등록")
                    assertThat(meta.preClear)
                        .withFailMessage("$targetName 은 sfid 자연 키 보유 — preClear = false 필요")
                        .isFalse
                }
        }
    }

    @Nested
    @DisplayName("Profile conflictUpdate — name 충돌 시 SF 원본 sfid 보강")
    inner class ProfileConflictUpdate {

        /**
         * LocalDataInitializer (제거 전) 가 local→dev DB 로 name-only (sfid=NULL) profile row 를
         * 선 INSERT 했던 사고의 재발 방지. profile 은 name UNIQUE 충돌 시 DO NOTHING 이 아니라
         * DO UPDATE 로 SF 원본 sfid / 메타를 보강해야 Stage2 FK Resolve 의 profile_sfid 매칭이 산다.
         */
        @Test
        @DisplayName("Profile 은 ON CONFLICT (name) DO UPDATE — sfid 포함 보강 컬럼 지정")
        fun profileConflictUpdateOnName() {
            val meta = Stage1Targets.get("Profile") ?: error("Profile 미등록")
            val cu = meta.conflictUpdate
            assertThat(cu).withFailMessage("Profile 은 conflictUpdate 필요 (seed sfid=NULL row 보강)").isNotNull
            assertThat(cu!!.conflictColumn).isEqualTo("name")
            assertThat(cu.updateColumns)
                .withFailMessage("sfid 가 보강 대상에 반드시 포함되어야 FK Resolve 가 산다")
                .contains("sfid")
        }

        @Test
        @DisplayName("conflictUpdate 의 보강 컬럼은 모두 Profile 의 실제 dbColumn 이어야 함")
        fun profileConflictUpdateColumnsExist() {
            val meta = Stage1Targets.get("Profile")!!
            val dbColumns = meta.fields.map { it.dbColumnName }.toSet()
            meta.conflictUpdate!!.updateColumns.forEach { col ->
                assertThat(dbColumns)
                    .withFailMessage("conflictUpdate 보강 컬럼 '%s' 가 Profile fields 에 없음", col)
                    .contains(col)
            }
        }

        @Test
        @DisplayName("conflictColumn 'name' 은 보강 대상에서 제외 (충돌 키 자신은 UPDATE 불요)")
        fun conflictColumnNotInUpdateColumns() {
            val cu = Stage1Targets.get("Profile")!!.conflictUpdate!!
            assertThat(cu.updateColumns).doesNotContain(cu.conflictColumn)
        }
    }
}
