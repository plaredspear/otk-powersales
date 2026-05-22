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
)

/**
 * FK 처리 제외 prefix.
 *
 * - product_code : code 기반 lookup (sfid 아님)
 * - record_type  : SF RecordType 메타 — 별도 FK 컬럼 없음
 * - related      : Group.related polymorphic — 별도 처리
 */
internal val SKIP_FK_PREFIXES: Set<String> = setOf(
    "product_code",
    "record_type",
    "related",
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
        else -> "${prefix}_id"
    }
    val (refTable, refIdColumn) = FK_PREFIX_MAPPING[prefix]
        ?: (prefix to "${prefix}_id")

    return FkResolveSpec(sfidColumn, idColumn, refTable, refIdColumn)
}
