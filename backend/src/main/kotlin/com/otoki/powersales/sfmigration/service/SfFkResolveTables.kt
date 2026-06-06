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
    "suggestion",
    // V199 — 7개 entity 의 owner_user_id + owner_group_id polymorphic 추가
    "holiday_master",
    "inspection_theme",
    "new_product",
    "product_barcode",
    "push_message",
    "upload_file",
    // SF DKRetail__SiteAcitivity__c.OwnerId.referenceTo = [Group, User] polymorphic.
    // site_activity.owner_sfid → owner_user_id (005) / owner_group_id (00G) 분기.
    "site_activity",
    // owner_user_id + owner_group_id + XOR CHECK 를 가졌으나 누락돼 있던 17개 entity.
    // 각 전용 SF align 마이그레이션(V112~V146)으로 owner_group_id ADD 확정. 미등록 시
    // Group(00G) 소유 row 의 owner_user_id/owner_group_id 가 둘 다 NULL 로 남던 문제.
    // (account / group 은 OwnerId.referenceTo 가 User 단독 / [Organization,User] 라 polymorphic XOR 대상 아님 — 제외)
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
    // V145 — owner polymorphic R-2 (referenceTo = [Group, User]). MonthlySalesHistory 복원
    // (db841eb9) 시 owner_sfid/owner_user_id/owner_group_id/XOR 는 갖췄으나 화이트리스트 등록 누락.
    "monthly_sales_history",
    // SalesProgressRateMaster__c.OwnerId.referenceTo = [Group, User] polymorphic.
    // owner_sfid/owner_user_id/owner_group_id/XOR 는 entity 에 있으나 화이트리스트 등록 누락
    // (monthly_sales_history 와 동일 함정). 미등록 시 Group(00G) 소유 row 의 owner_*_id 가 둘 다 NULL.
    "sales_progress_rate_master",
    // DailySalesHistory__c.OwnerId.referenceTo = [Group, User] polymorphic. owner_sfid → owner_user_id (005) /
    // owner_group_id (00G) 분기. SF 정합 누락 필드 추가 (MonthlySalesHistory / SalesProgressRateMaster 동일 패턴).
    "daily_sales_history",
    "notice",
    "product",
    "team_member_schedule",
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
    // sharing_rule_condition / sharing_rule_target 의 sharing_rule_id resolve 는
    // (s_object_name, developer_name) 복합 자연 키 필요 — 단일 NaturalKeyFkSpec 표현 불가.
    // SfMigrationStage2NaturalKeyFkService.resolveSharingRuleSubtableFk() 전용 method 처리.
    //
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
    // permission_set_flags.permission_set_name → permission_set.permission_set_id
    // 본 매핑이 없으면 PermissionSetDetailPage 의 flags / objectPermissions / assignedUsers
    // 가 모두 빈 응답으로 떨어지고 "사용자 추가" 버튼도 렌더링되지 않는다 (frontend 가드
    // `data.flags?.permissionSetFlagsId` 가 undefined 가 되기 때문).
    NaturalKeyFkSpec(
        sourceTable = "permission_set_flags",
        sourceColumn = "permission_set_name",
        refTable = "permission_set",
        refColumn = "name",
        targetIdColumn = "permission_set_id",
    ),
    // ──────────────────────────────────────────────────────────────────────
    // spec #798 — PermissionSetAssignment FK 해소
    // permission_set_sfid → permission_set_flags.permission_set_sfid → permission_set_flags_id
    // ──────────────────────────────────────────────────────────────────────
    NaturalKeyFkSpec(
        sourceTable = "permission_set_assignment",
        sourceColumn = "permission_set_sfid",
        refTable = "permission_set_flags",
        refColumn = "permission_set_sfid",
        targetIdColumn = "permission_set_flags_id",
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
    "theme" to ("inspection_theme" to "inspection_theme_id"),
    // 도메인 FK — prefix 가 곧 table 명인 케이스
    "account" to ("account" to "account_id"),
    // Spec #849 — deprecated SF lookup 부활 (TeamMemberSchedule.DKRetail__AccountId__c / Promotion.DKRetail__AccId__c).
    // dk_account_sfid → dk_account_id (자동추론), ref account.account_id.
    "dk_account" to ("account" to "account_id"),
    "employee" to ("employee" to "employee_id"),
    "product" to ("product" to "product_id"),
    // NewProduct__c.Product_Code__c → DKRetail__Product__c lookup (product_code_sfid → product_code_id).
    // idColumn 은 자동추론 ${prefix}_id = product_code_id 로 정확.
    "product_code" to ("product" to "product_id"),
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
    // EmployeeInputCriteriaMaster.Category__c → account_category_master 참조 (category_sfid → category_id).
    // 짝 FK 컬럼은 category_id, 참조 대상은 employee_input_criteria_master 가 아니라 account_category_master.
    // Flyway backfill(V202605262256__backfill_..._category_id) 과 동일한 lookup — 이중 안전망(idempotent).
    "category" to ("account_category_master" to "account_category_master_id"),
    // Spec #780 — Profile / UserRole entity 신규 시스템 편입.
    "profile" to ("profile" to "profile_id"),
    "user_role" to ("user_role" to "user_role_id"),
    "parent_user_role" to ("user_role" to "user_role_id"),
    // Spec #796 — RecordType FK 활성화. SObject row 의 RecordTypeId(sfid) → record_type.record_type_id 변환.
    "record_type" to ("record_type" to "record_type_id"),
    // Spec #798 — PermissionSetAssignment.AssigneeId → user.user_id 해소 (assignee_user_sfid → assignee_user_id).
    "assignee_user" to ("user" to "user_id"),
)

/**
 * FK 처리 제외 prefix.
 *
 * - related      : Group.related polymorphic — 별도 처리
 * - user_or_group : group_member 의 polymorphic — POLYMORPHIC_USER_OR_GROUP_TABLES 가 별도 처리
 * - target       : sharing_rule_target.target_sfid 는 SF retrieve XML 출처에서 부재 (SF
 *                  자체가 sharedTo 본문 element 의 DeveloperName 으로만 식별).
 *                  target_id 채움은 NaturalKey FK Service 의 sharing_rule_target 전용 분기
 *                  (target_developer_name + target_type) 가 처리. sfid prefix 경로 skip.
 * - record       : upload_file.record_sfid 는 parent_type 분기로 claim/notice/proposal/site_activity
 *                  를 가리키는 polymorphic. 단일 (refTable, refIdColumn) 로 표현 불가 →
 *                  일반 FK substep 에서 제외하고, SfMigrationStage2Service.runUploadFilePolymorphicParent()
 *                  가 (parent_type, record_sfid) → parent_id 로 전용 처리.
 *
 * 비고: product_code 는 과거 "code 기반 lookup(sfid 아님)" 으로 오분류돼 여기 있었으나,
 *      SF NewProduct__c.Product_Code__c describe = reference/18 → DKRetail__Product__c
 *      (relationshipName Product_Code__r) 인 정상 sfid lookup 이라 제외에서 풀고
 *      FK_PREFIX_MAPPING 에 ("product","product_id") 로 등록.
 */
internal val SKIP_FK_PREFIXES: Set<String> = setOf(
    "related",
    "user_or_group",
    "target",
    "record",
)

/**
 * 같은 prefix 라도 source 테이블에 따라 ref 대상이 갈리는 override 표.
 *
 * `manager_sfid` 는 두 테이블이 서로 다른 entity 를 self/cross reference:
 *   - employee.manager_sfid  → employee.employee_id (Employee self-reference, FK_PREFIX_MAPPING 기본값)
 *   - user.manager_sfid      → user.user_id          (User self-reference, SF User.ManagerId)
 * 단일 prefix 매핑으로는 동시 표현 불가 → (sourceTable, prefix) → (refTable, refIdColumn) override.
 */
internal val TABLE_SCOPED_FK_OVERRIDE: Map<Pair<String, String>, Pair<String, String>> = mapOf(
    ("user" to "manager") to ("user" to "user_id"),
)

/**
 * sfid 컬럼명에서 FK resolve spec derive.
 *
 *   - `*_sfid` 컬럼만 대상 (단독 `sfid` 는 entity 자신의 PK lookup 용이라 제외)
 *   - prefix = sfid 컬럼명 - `_sfid`
 *   - SKIP_FK_PREFIXES 에 있으면 null
 *   - id 컬럼명: `owner` prefix → `owner_user_id` (polymorphic), 그 외 → `<prefix>_id`
 *   - 대상 table: TABLE_SCOPED_FK_OVERRIDE (sourceTable 별) → FK_PREFIX_MAPPING → prefix 자체 추론 순
 *
 * @param sourceTable sfid 컬럼이 속한 테이블명. 같은 prefix 가 테이블별로 다른 entity 를
 *   참조하는 케이스 (manager 등) override 에 사용. null 이면 prefix 전역 매핑만 적용.
 */
/**
 * sfid 컬럼이 어느 규칙으로 ref table 을 결정하는지 분류 (deriveFkResolveSpec 와 동일 우선순위).
 *
 * - SKIP          : 처리 제외 (`sfid` 단독 / `*_sfid` 아님 / SKIP_FK_PREFIXES)
 * - TABLE_SCOPED  : (sourceTable, prefix) override 로 해소
 * - MAPPED        : FK_PREFIX_MAPPING 명시 매핑으로 해소 (audit / alias / 도메인 FK 포함)
 * - AUTO_INFERRED : 어느 명시 표에도 없어 `(prefix, prefix_id)` 자동 추론 (prefix == table 가정).
 *   → 새 `*_sfid` 컬럼이 매핑 없이 추가됐을 때의 silent-miss 위험 신호. 회귀 테스트가 감시.
 *
 * 비고: AUTO_INFERRED 자체가 곧 오류는 아니다 (prefix == table 명인 도메인 FK 는 추론이 정확).
 * 회귀 테스트 [SfFkResolveInventoryTest] 가 "알려진 컬럼 인벤토리의 분류/ref 가 고정값과 일치" 를
 * 검증하여, 신규 컬럼 추가 / 매핑 변경 시 인벤토리 갱신을 강제한다.
 */
enum class FkResolutionKind { SKIP, TABLE_SCOPED, MAPPED, AUTO_INFERRED }

fun classifyFkResolution(sfidColumn: String, sourceTable: String? = null): FkResolutionKind {
    if (sfidColumn == "sfid") return FkResolutionKind.SKIP
    if (!sfidColumn.endsWith("_sfid")) return FkResolutionKind.SKIP
    val prefix = sfidColumn.removeSuffix("_sfid")
    if (prefix in SKIP_FK_PREFIXES) return FkResolutionKind.SKIP
    if (sourceTable != null && TABLE_SCOPED_FK_OVERRIDE.containsKey(sourceTable to prefix)) {
        return FkResolutionKind.TABLE_SCOPED
    }
    if (FK_PREFIX_MAPPING.containsKey(prefix)) return FkResolutionKind.MAPPED
    return FkResolutionKind.AUTO_INFERRED
}

internal fun deriveFkResolveSpec(sfidColumn: String, sourceTable: String? = null): FkResolveSpec? {
    if (sfidColumn == "sfid") return null
    if (!sfidColumn.endsWith("_sfid")) return null
    val prefix = sfidColumn.removeSuffix("_sfid")
    if (prefix in SKIP_FK_PREFIXES) return null

    val idColumn = when (prefix) {
        "owner" -> "owner_user_id"
        "commute_log" -> "attendance_log_id" // Spec #789 — legacy DKRetail__CommuteLogId__c lookup 의 신규 entity 명 = AttendanceLog
        "full_name" -> "employee_id" // ProfessionalPromotionTeamMaster.FullName__c → employee_id 단일 컬럼으로 통합 (full_name_id 폐기)
        "theme" -> "inspection_theme_id" // SiteActivity.ThemeId__c → inspection_theme 참조 (site_activity.theme_sfid → inspection_theme_id)
        else -> "${prefix}_id"
    }
    val (refTable, refIdColumn) = sourceTable?.let { TABLE_SCOPED_FK_OVERRIDE[it to prefix] }
        ?: FK_PREFIX_MAPPING[prefix]
        ?: (prefix to "${prefix}_id")

    return FkResolveSpec(sfidColumn, idColumn, refTable, refIdColumn)
}
