package com.otoki.powersales.sfmigration.service

/**
 * SF 데이터 마이그레이션 FK Resolve 정책 표 (Stage 2-A).
 *
 * 정책: `<prefix>_sfid` 컬럼 → 짝의 `<prefix>_id` FK 컬럼 채움 (sfid lookup UPDATE).
 * source: scripts/sf-data-migration/common.kts (런칭 후 폐기).
 */

internal data class FkResolveSpec(
    val sfidColumn: String,
    val idColumn: String,
    val refTable: String,
    val refIdColumn: String,
    val refSfidColumn: String = "sfid",
)

/**
 * Polymorphic owner entity 화이트리스트 — owner_sfid prefix `005` = User, `00G` = Group 분기.
 * owner_user_id / owner_group_id 두 컬럼이 동시 존재하는 entity 가 대상.
 */
internal val POLYMORPHIC_OWNER_TABLES: Set<String> = setOf(
    "organization",
    "order_request",
    "order_request_product",
    "promotion",
    "professional_promotion_team_history",
    "professional_promotion_team_master",
)

/**
 * Polymorphic related entity 화이트리스트 — related_sfid prefix `005` = User, `00E` = UserRole 분기 (spec #782 P2-B).
 * SF describe `Group.RelatedId.referenceTo = [User, UserRole]` 정합.
 * related_user_id / related_user_role_id 두 컬럼이 동시 존재하는 entity 가 대상 — 현재 `group` 1건.
 */
internal val POLYMORPHIC_RELATED_TABLES: Set<String> = setOf(
    "group",
)

/**
 * Polymorphic user-or-group entity 화이트리스트 — user_or_group_sfid prefix `005` = User, `00G` = Group 분기 (spec #790).
 * SF describe `GroupMember.UserOrGroupId.referenceTo = [Group, User]` 정합.
 * group_member 1건 — Stage 2 fk substep 이 user_or_group_id + user_or_group_type 채움.
 */
internal val POLYMORPHIC_USER_OR_GROUP_TABLES: Set<String> = setOf(
    "group_member",
)

/**
 * 자연 키 lookup FK 화이트리스트 — sfid 가 아닌 developerName / profileName / permissionSetName 등으로 lookup (spec #790).
 *
 * 본 표는 SfMigrationStage2FkService 가 sharing 메타 entity 의 fk resolve 시 참조.
 * 키: (sourceTable, sourceColumn) → 값: (refTable, refColumn, idColumn)
 *
 * 예: ("sharing_rule_condition", "sharing_rule_developer_name") → ("sharing_rule", "developer_name", "sharing_rule_id")
 *   → UPDATE sharing_rule_condition c SET sharing_rule_id = sr.sharing_rule_id
 *     FROM sharing_rule sr WHERE sr.developer_name = c.sharing_rule_developer_name
 */
internal data class NaturalKeyFkSpec(
    val sourceTable: String,
    val sourceColumn: String,
    val refTable: String,
    val refColumn: String,
    val targetIdColumn: String,
)

internal val NATURAL_KEY_FK_MAPPINGS: List<NaturalKeyFkSpec> = listOf(
    // sharing_rule_condition.sharing_rule_developer_name → sharing_rule.sharing_rule_id
    NaturalKeyFkSpec(
        sourceTable = "sharing_rule_condition",
        sourceColumn = "sharing_rule_developer_name",
        refTable = "sharing_rule",
        refColumn = "developer_name",
        targetIdColumn = "sharing_rule_id",
    ),
    // sharing_rule_target.sharing_rule_developer_name → sharing_rule.sharing_rule_id
    NaturalKeyFkSpec(
        sourceTable = "sharing_rule_target",
        sourceColumn = "sharing_rule_developer_name",
        refTable = "sharing_rule",
        refColumn = "developer_name",
        targetIdColumn = "sharing_rule_id",
    ),
    // user_role_hierarchy_snapshot.developer_name → user_role.user_role_id
    NaturalKeyFkSpec(
        sourceTable = "user_role_hierarchy_snapshot",
        sourceColumn = "developer_name",
        refTable = "user_role",
        refColumn = "developer_name",
        targetIdColumn = "user_role_id",
    ),
    // profile_flags.profile_name → profile.profile_id
    NaturalKeyFkSpec(
        sourceTable = "profile_flags",
        sourceColumn = "profile_name",
        refTable = "profile",
        refColumn = "name",
        targetIdColumn = "profile_id",
    ),
    // ──────────────────────────────────────────────────────────────────────
    // spec #794 — Record Type 권한 FK 해소 (3 NATURAL_KEY_FK_MAPPINGS)
    // ──────────────────────────────────────────────────────────────────────
    // profile_record_type.profile_name → profile.profile_id
    NaturalKeyFkSpec(
        sourceTable = "profile_record_type",
        sourceColumn = "profile_name",
        refTable = "profile",
        refColumn = "name",
        targetIdColumn = "profile_id",
    ),
    // permission_set_record_type.permission_set_name → permission_set.permission_set_id
    // (permission_set entity 가 없을 수 있어 fk substep 이 skip 처리됨)
    NaturalKeyFkSpec(
        sourceTable = "permission_set_record_type",
        sourceColumn = "permission_set_name",
        refTable = "permission_set",
        refColumn = "name",
        targetIdColumn = "permission_set_id",
    ),
    // ──────────────────────────────────────────────────────────────────────
    // spec #795 — FLS FK 해소 (2 NATURAL_KEY_FK_MAPPINGS)
    // ──────────────────────────────────────────────────────────────────────
    NaturalKeyFkSpec(
        sourceTable = "profile_field_permission",
        sourceColumn = "profile_name",
        refTable = "profile",
        refColumn = "name",
        targetIdColumn = "profile_id",
    ),
    NaturalKeyFkSpec(
        sourceTable = "permission_set_field_permission",
        sourceColumn = "permission_set_name",
        refTable = "permission_set",
        refColumn = "name",
        targetIdColumn = "permission_set_id",
    ),
)

/**
 * sfid prefix → (refTable, refIdColumn) 명시 매핑. 본 표에 없는 prefix 는 자동 추론.
 */
internal val FK_PREFIX_MAPPING: Map<String, Pair<String, String>> = mapOf(
    // audit (User lookup)
    "created_by" to ("user" to "user_id"),
    "last_modified_by" to ("user" to "user_id"),
    "owner" to ("user" to "user_id"),
    // self-reference 또는 별칭
    "manager" to ("employee" to "employee_id"),
    "parent" to ("account" to "account_id"),
    "full_name" to ("employee" to "employee_id"),
    "team_leader" to ("employee" to "employee_id"),
    "primary_product" to ("product" to "product_id"),
    "alt_holiday" to ("alternative_holiday" to "alternative_holiday_id"),
    "postponed_appointment" to ("appointment" to "appointment_id"),
    "last_monthly_sales_history" to ("monthly_sales_history" to "monthly_sales_history_id"),
    "commute_log" to ("attendance_log" to "attendance_log_id"),
    // 도메인 FK — prefix 가 곧 table 명인 케이스
    "account" to ("account" to "account_id"),
    "employee" to ("employee" to "employee_id"),
    "product" to ("product" to "product_id"),
    "promotion" to ("promotion" to "promotion_id"),
    "promotion_employee" to ("promotion_employee" to "promotion_employee_id"),
    "team_member_schedule" to ("team_member_schedule" to "team_member_schedule_id"),
    "new_product" to ("new_product" to "new_product_id"),
    "agreement_word" to ("agreement_word" to "agreement_word_id"),
    "order_request" to ("order_request" to "order_request_id"),
    "erp_order" to ("erp_order" to "erp_order_id"),
    "push_message" to ("push_message" to "push_message_id"),
    "display_work_schedule" to ("display_work_schedule" to "display_work_schedule_id"),
    "hq_review" to ("hq_review" to "hq_review_id"),
    "branch_review" to ("branch_review" to "branch_review_id"),
    "monthly_female_employee_integration_schedule" to (
        "monthly_female_employee_integration_schedule" to "monthly_female_employee_integration_schedule_id"
    ),
    "employee_input_criteria_master" to (
        "employee_input_criteria_master" to "employee_input_criteria_master_id"
    ),
    "category" to ("employee_input_criteria_master" to "employee_input_criteria_master_id"),
    // Spec #780 — Profile / UserRole entity 신규 시스템 편입.
    "profile" to ("profile" to "profile_id"),
    "user_role" to ("user_role" to "user_role_id"),
    "parent_user_role" to ("user_role" to "user_role_id"),
    // Spec #796 — RecordType FK 활성화. SObject row 의 RecordTypeId(sfid) → record_type.record_type_id 변환.
    "record_type" to ("record_type" to "record_type_id"),
)

/**
 * FK 처리 제외 prefix.
 *
 * - product_code : code 기반 lookup (sfid 아님)
 * - related      : Group.related polymorphic — 별도 처리
 *
 * (record_type 은 spec #796 부터 FK_PREFIX_MAPPING 으로 활성화 — 본 set 에서 제거됨)
 */
internal val SKIP_FK_PREFIXES: Set<String> = setOf(
    "product_code",
    "related",
    // spec #790 — group_member.user_or_group_sfid 는 POLYMORPHIC_USER_OR_GROUP_TABLES 가 별도 처리
    "user_or_group",
)

/**
 * sfid 컬럼명에서 FK resolve spec derive.
 *
 *   - `*_sfid` 컬럼만 대상 (단독 `sfid` 는 entity 자신의 PK lookup 용이라 제외)
 *   - prefix = sfid 컬럼명 - `_sfid`
 *   - SKIP_FK_PREFIXES 에 있으면 null
 *   - id 컬럼명: `owner` prefix → `owner_user_id` (polymorphic), 그 외 → `<prefix>_id`
 *   - 대상 table: FK_PREFIX_MAPPING 우선, 없으면 prefix 자체를 테이블명 + `<prefix>_id` 로 추론
 */
internal fun deriveFkResolveSpec(sfidColumn: String): FkResolveSpec? {
    if (sfidColumn == "sfid") return null
    if (!sfidColumn.endsWith("_sfid")) return null
    val prefix = sfidColumn.removeSuffix("_sfid")
    if (prefix in SKIP_FK_PREFIXES) return null

    val idColumn = when (prefix) {
        "owner" -> "owner_user_id"
        "commute_log" -> "attendance_log_id" // Spec #789 — legacy DKRetail__CommuteLogId__c lookup 의 신규 entity 명 = AttendanceLog
        else -> "${prefix}_id"
    }
    val (refTable, refIdColumn) = FK_PREFIX_MAPPING[prefix]
        ?: (prefix to "${prefix}_id")

    return FkResolveSpec(sfidColumn, idColumn, refTable, refIdColumn)
}
