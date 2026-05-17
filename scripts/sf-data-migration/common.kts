/**
 * SF 데이터 마이그레이션 공통 모듈 (Spec #764, v2 — K2 cache 무효화 / PRODUCT_METADATA 정합).
 *
 * Stage 1 / Stage 2 entry script 가 @file:Import 로 로드.
 * - data class + 단순 helper 함수 + 상수 매핑 표만 위치.
 * - lambda 를 사용하는 stage 별 처리 함수는 K2 cross-file 버그 회피 위해
 *   각 entry script (migrate-stage1/2.main.kts) 내부에 유지.
 *
 * JDBC 직접 INSERT 방식 (SQL dump 미사용). DB 연결 정보는 db.properties 에서 로드.
 */

import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

// =============================================================================
// EntityMetadata
// =============================================================================

data class FieldMapping(
    val sfFieldName: String,
    val dbColumnName: String,
    val nullable: Boolean = true,
    val isString: Boolean = true
)

data class EntityMetadata(
    val targetName: String,
    val sObjectName: String,
    val schemaName: String = "powersales",
    val tableName: String,
    val pkColumn: String,
    /**
     * 현재 미사용 (Stage 1 INSERT 는 `ON CONFLICT DO NOTHING` 으로 모든 unique constraint
     * / partial index 충돌을 일괄 skip). 향후 column-specific 충돌 감지가 필요할 때만 사용.
     */
    val conflictKey: String,
    val fields: List<FieldMapping>,
    val rawColumnsAsString: List<String> = emptyList(),
    /**
     * Stage 1 INSERT 시 추가로 채워야 하는 정적 컬럼 (NOT NULL 제약 회피용 placeholder).
     * 예: user.password (NOT NULL) → "" placeholder, Stage 2-C 가 BCrypt 로 덮어씀.
     * 값이 null 이면 SQL NULL 로 처리.
     */
    val extraStaticColumns: Map<String, String?> = emptyMap()
)

/**
 * Stage 2 FK resolve 메타 — sfid 기반 lookup 으로 FK id 컬럼을 채움.
 *
 * @property sfidColumn 본 entity 의 sfid 컬럼 (예: "account_sfid")
 * @property idColumn 본 entity 의 FK id 컬럼 (예: "account_id")
 * @property refTable lookup 대상 테이블명 (예: "account")
 * @property refIdColumn lookup 대상 테이블의 PK 컬럼명 (예: "account_id")
 * @property refSfidColumn lookup 대상 테이블의 sfid 컬럼명 (대부분 "sfid", default)
 */
data class FkResolve(
    val sfidColumn: String,
    val idColumn: String,
    val refTable: String,
    val refIdColumn: String,
    val refSfidColumn: String = "sfid"
)

/**
 * Polymorphic owner entity 목록 (Spec #761 R-2).
 * owner_sfid 의 prefix `005` = User, `00G` = Group 으로 분기되어
 * 각각 owner_user_id / owner_group_id 채움.
 *
 * Stage 2 의 polymorphic resolver 가 본 목록을 순회.
 * (어떤 entity 가 polymorphic 인지는 자동 감지 불가 — owner_user_id / owner_group_id 두
 *  컬럼이 동시에 존재하는 entity 가 대상. Stage 2 의 resolver 가 information_schema 로 확인.)
 */
val POLYMORPHIC_OWNER_TABLES: List<String> = listOf(
    "organization",
    "order_request",
    "order_request_product",
    "promotion",
    "professional_promotion_team_history",
    "professional_promotion_team_master"
)

/**
 * sfid prefix → (refTable, refIdColumn) 매핑.
 *
 * 정책: entity 의 모든 `<prefix>_sfid` 컬럼은 짝으로 `<prefix>_id` FK 컬럼을 가지며,
 *       해당 FK 는 sfid lookup 으로 채워져야 한다.
 *
 * 매핑 방식:
 *   1. 표준 prefix (audit + 명시적 alias) 는 본 표에 직접 명시.
 *   2. 표에 없으면 prefix 를 그대로 테이블명 + `<prefix>_id` 로 자동 추론.
 *   3. SKIP_PREFIXES 에 등록된 prefix 는 FK 처리 대상 외 (코드/메타 lookup).
 */
val FK_PREFIX_MAPPING: Map<String, Pair<String, String>> = mapOf(
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
    // 도메인 FK — prefix 가 곧 table 명인 일반 케이스 (audit 처리 후 자동 추론 fallback 와 동일하지만 명시 권장)
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
    "monthly_female_employee_integration_schedule" to ("monthly_female_employee_integration_schedule" to "monthly_female_employee_integration_schedule_id"),
    "employee_input_criteria_master" to ("employee_input_criteria_master" to "employee_input_criteria_master_id"),
    "category" to ("employee_input_criteria_master" to "employee_input_criteria_master_id")
)

/**
 * FK 처리에서 제외할 prefix.
 *
 *   - product_code : sfid 가 아닌 code 기반 lookup (NewProduct → product.product_code)
 *   - profile      : Profile.Name (한글 문자열) lookup, sfid 아님
 *   - user_role    : UserRole 메타 — 본 프로젝트에서 별도 FK 컬럼 없음
 *   - record_type  : SF RecordType 메타 lookup, 본 프로젝트에서 별도 FK 컬럼 없음
 *   - related      : Group.related (polymorphic User/Group) — 별도 처리 필요
 */
val SKIP_FK_PREFIXES: Set<String> = setOf(
    "product_code",
    "profile",
    "user_role",
    "record_type",
    "related"
)

/**
 * EntityMetadata 의 FieldMapping 을 스캔하여 sfid → id FK 페어 자동 추출.
 *
 * 정책:
 *   - dbColumnName 이 `_sfid` 로 끝나면 FK 후보.
 *   - prefix = sfid 컬럼명에서 `_sfid` 제거.
 *   - SKIP_FK_PREFIXES 에 있으면 처리 안 함.
 *   - FK_PREFIX_MAPPING 우선 조회. 없으면 자동 추론 (`<prefix>` 테이블, `<prefix>_id` PK).
 *   - audit prefix (created_by / last_modified_by / owner) → id 컬럼은 `<prefix>_id` 가 아닌
 *     `<prefix>_id` (created_by → created_by_id, owner → owner_user_id).
 *
 * 반환: 본 entity 의 모든 FK resolve 정의 (audit + 도메인 + polymorphic owner_group 제외 — 별도 처리).
 *       단 `id` 가 없는 sfid (= `sfid` 자체) 는 제외.
 */
fun deriveFkResolves(fields: List<FieldMapping>): List<FkResolve> {
    val result = mutableListOf<FkResolve>()
    for (field in fields) {
        val col = field.dbColumnName
        if (col == "sfid") continue
        if (!col.endsWith("_sfid")) continue
        val prefix = col.removeSuffix("_sfid")
        if (prefix in SKIP_FK_PREFIXES) continue

        // id 컬럼명: audit prefix 는 owner → owner_user_id 처리 (polymorphic).
        val idColumn = when (prefix) {
            "owner" -> "owner_user_id"
            else -> "${prefix}_id"
        }

        // 대상 table 결정.
        val (refTable, refIdColumn) = FK_PREFIX_MAPPING[prefix]
            ?: (prefix to "${prefix}_id")  // fallback 자동 추론

        result.add(FkResolve(col, idColumn, refTable, refIdColumn))
    }
    return result
}

val ORGANIZATION_METADATA = EntityMetadata(
    targetName = "Organization",
    sObjectName = "Org__c",
    tableName = "organization",
    pkColumn = "organization_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("CostCenterLevel2__c", "cc_cd2"),
        FieldMapping("OrgCodeLevel2__c", "org_cd2"),
        FieldMapping("OrgNameLevel2__c", "org_nm2"),
        FieldMapping("CostCenterLevel3__c", "cc_cd3"),
        FieldMapping("OrgCodeLevel3__c", "org_cd3"),
        FieldMapping("OrgNameLevel3__c", "org_nm3"),
        FieldMapping("CostCenterLevel4__c", "cc_cd4"),
        FieldMapping("OrgCodeLevel4__c", "org_cd4"),
        FieldMapping("OrgNameLevel4__c", "org_nm4"),
        FieldMapping("CostCenterLevel5__c", "cc_cd5"),
        FieldMapping("OrgCodeLevel5__c", "org_cd5"),
        FieldMapping("OrgNameLevel5__c", "org_nm5"),
        FieldMapping("ExternalKey__c", "external_key"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val EMPLOYEE_METADATA = EntityMetadata(
    targetName = "Employee",
    sObjectName = "DKRetail__Employee__c",
    tableName = "employee",
    pkColumn = "employee_id",
    conflictKey = "employee_code",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("DKRetail__EmpCode__c", "employee_code", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__Birthdate__c", "birth_date"),
        FieldMapping("DKRetail__Status__c", "status"),
        FieldMapping("DKRetail__APPLoginActive__c", "app_login_active", isString = false),
        FieldMapping("DKRetail__AppAuthority__c", "role"),
        FieldMapping("DKRetail__OrgName__c", "org_name"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("DKRetail__WorkPhone__c", "work_phone"),
        FieldMapping("Phone__c", "phone"),
        FieldMapping("DKRetail__HomePhone__c", "home_phone"),
        FieldMapping("DKRetail__WorkEmail__c", "work_email"),
        FieldMapping("DKRetail__Email__c", "email"),
        FieldMapping("DKRetail__Sex__c", "gender"),
        FieldMapping("DKRetail__StartDate__c", "start_date", isString = false),
        FieldMapping("DKRetail__EndDate__c", "end_date", isString = false),
        FieldMapping("AgreementFlag__c", "agreement_flag", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        // 인사 보강 (직책/직위/직급/직종/근무형태) — SF prod 메타 검증 (2026-05-17)
        FieldMapping("ProfessionalPromotionTeam__c", "professional_promotion_team"),
        FieldMapping("DKRetail__Jikchak__c", "jikchak"),
        FieldMapping("DKRetail__Jikwee__c", "jikwee"),
        FieldMapping("DKRetail__Jikgub__c", "jikgub"),
        FieldMapping("DKRetail__WorkType__c", "work_type"),
        FieldMapping("DKRetail__JobCode__c", "job_code"),
        FieldMapping("DKRetail__WorkArea__c", "work_area"),
        FieldMapping("DKRetail__Jikjong__c", "jikjong"),
        FieldMapping("DKRetail__AppointmentDate__c", "appointment_date", isString = false),
        FieldMapping("OrdDetailNode__c", "ord_detail_node"),
        FieldMapping("DKRetail__CRM_WorkStartDate__c", "crm_work_start_date", isString = false),
        FieldMapping("DKRetail__CostCenterCode__c", "dk_cost_center_code"),
        FieldMapping("DKRetail__LocationCode__c", "location_code"),
        FieldMapping("DKRetail__TotalAnnualLeave__c", "total_annual_leave", isString = false),
        FieldMapping("DKRetail__UsedAnnualLeave__c", "used_annual_leave", isString = false),
        FieldMapping("DKRetail__ManagerId__c", "manager_sfid"),
        FieldMapping("PostponedAppointment__c", "postponed_appointment_sfid"),
        FieldMapping("LockingFlag__c", "locking_flag", isString = false),
        FieldMapping("OfficePhone__c", "office_phone"),
        FieldMapping("DKRetail__CRM_WorkType__c", "crm_work_type"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    ),
    rawColumnsAsString = listOf("role", "professional_promotion_team")
)

val USER_METADATA = EntityMetadata(
    targetName = "User",
    sObjectName = "User",
    tableName = "user",
    pkColumn = "user_id",
    conflictKey = "employee_code",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Username", "username", nullable = false),
        FieldMapping("Email", "email"),
        FieldMapping("IsActive", "is_active", isString = false),
        FieldMapping("DKRetail__EmployeeNumber__c", "employee_code", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("LastName", "last_name"),
        FieldMapping("FirstName", "first_name"),
        FieldMapping("Alias", "alias"),
        FieldMapping("Title", "title"),
        FieldMapping("Department", "department"),
        FieldMapping("Division", "division"),
        FieldMapping("MobilePhone", "mobile_phone"),
        FieldMapping("Phone", "phone"),
        FieldMapping("HR_Code__c", "hr_code"),
        FieldMapping("Branch__c", "branch"),
        FieldMapping("LastLoginDate", "last_login_at"),
        FieldMapping("ManagerId", "manager_sfid"),
        FieldMapping("ProfileId", "profile_sfid"),
        FieldMapping("UserRoleId", "user_role_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("Profile.Name", "profile_type")
    ),
    rawColumnsAsString = listOf("profile_type"),
    extraStaticColumns = mapOf(
        // user.password 가 NOT NULL — Stage 1 placeholder, Stage 2-C 가 BCrypt 로 덮어씀.
        "password" to ""
    )
)

val ACCOUNT_METADATA = EntityMetadata(
    targetName = "Account",
    sObjectName = "Account",
    tableName = "account",
    pkColumn = "account_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Phone", "phone"),
        FieldMapping("MobilePhone__c", "mobile_phone"),
        FieldMapping("Address1__c", "address1"),
        FieldMapping("Address2__c", "address2"),
        FieldMapping("Representative__c", "representative"),
        FieldMapping("ABCType__c", "abc_type"),
        FieldMapping("ABCTypeCode__c", "abc_type_code"),
        FieldMapping("ExternalKey__c", "external_key"),
        FieldMapping("AccountGroup__c", "account_group"),
        FieldMapping("BranchCode__c", "branch_code"),
        FieldMapping("BranchName__c", "branch_name"),
        FieldMapping("Zipcode__c", "zip_code"),
        FieldMapping("Latitude__c", "latitude"),
        FieldMapping("Longitude__c", "longitude"),
        FieldMapping("ClosingTime1__c", "closing_time1"),
        FieldMapping("ClosingTime2__c", "closing_time2"),
        FieldMapping("ClosingTime3__c", "closing_time3"),
        FieldMapping("Industry", "industry"),
        FieldMapping("WERK1_TX__c", "werk1_tx"),
        FieldMapping("WERK2_TX__c", "werk2_tx"),
        FieldMapping("WERK3_TX__c", "werk3_tx"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("Type", "account_type"),
        FieldMapping("AccountStatusName__c", "account_status_name"),
        FieldMapping("EmployeeCode__c", "employee_code"),
        FieldMapping("Distribution__c", "distribution"),
        FieldMapping("AccountStatusCode__c", "account_status_code"),
        FieldMapping("BusinessType__c", "business_type"),
        FieldMapping("BusinessCategory__c", "business_category"),
        FieldMapping("Sic", "business_license_number"),
        FieldMapping("Email__c", "email"),
        FieldMapping("DivisionName__c", "division_name"),
        FieldMapping("SalesDeptName__c", "sales_dept_name"),
        FieldMapping("ConsignmentAcc__c", "consignment_acc"),
        FieldMapping("WERK1__c", "werk1"),
        FieldMapping("WERK2__c", "werk2"),
        FieldMapping("WERK3__c", "werk3"),
        FieldMapping("SalesDeptCostCenter__c", "sales_dept_cost_center"),
        FieldMapping("DivisionCostCenter__c", "division_cost_center"),
        FieldMapping("AccountNumber", "account_number"),
        FieldMapping("Site", "site"),
        FieldMapping("AccountSource", "account_source"),
        FieldMapping("BranchCostCenter__c", "branch_cost_center"),
        FieldMapping("DivisionCode__c", "division_code"),
        FieldMapping("SalesDeptCode__c", "sales_dept_code"),
        FieldMapping("LogisticsName__c", "logistics_name"),
        FieldMapping("LogisticsCode__c", "logistics_code"),
        FieldMapping("FreezerInstalled__c", "freezer_installed", isString = false),
        FieldMapping("FreezerType__c", "freezer_type"),
        FieldMapping("Field1__c", "remaining_credit", isString = false),
        FieldMapping("TotalCredit__c", "total_credit", isString = false),
        FieldMapping("MapCoordinate__c", "map_coordinate"),
        FieldMapping("OrderEndTime__c", "order_end_time"),
        FieldMapping("FirstInstalled__c", "first_installed", isString = false),
        FieldMapping("Description", "description"),
        FieldMapping("Website", "website"),
        FieldMapping("Fax", "fax"),
        FieldMapping("AnnualRevenue", "annual_revenue", isString = false),
        FieldMapping("NumberOfEmployees", "number_of_employees", isString = false),
        FieldMapping("ParentId", "parent_sfid"),
        FieldMapping("Rating", "rating"),
        FieldMapping("Ownership", "ownership"),
        FieldMapping("IsPriorityRecord", "is_priority_record", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val PRODUCT_METADATA = EntityMetadata(
    targetName = "Product",
    sObjectName = "DKRetail__Product__c",
    tableName = "product",
    pkColumn = "product_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__ProductCode__c", "product_code", nullable = false),
        FieldMapping("DKRetail__ProductType__c", "product_type"),
        FieldMapping("DKRetail__ProductStatus__c", "product_status"),
        FieldMapping("DKRetail__StoreCondition__c", "storage_condition"),
        FieldMapping("DKRetail__ShelfLife__c", "shelf_life"),
        FieldMapping("DKRetail__ShelfLifeUnit__c", "shelf_life_unit"),
        FieldMapping("DKRetail__Category1__c", "category1"),
        FieldMapping("DKRetail__Category2__c", "category2"),
        FieldMapping("DKRetail__Category3__c", "category3"),
        FieldMapping("DKRetail__CategoryCode1__c", "category_code1"),
        FieldMapping("DKRetail__CategoryCode2__c", "category_code2"),
        FieldMapping("DKRetail__CategoryCode3__c", "category_code3"),
        FieldMapping("DKRetail__Unit__c", "unit"),
        FieldMapping("DKRetail__OrderingUnit__c", "ordering_unit"),
        FieldMapping("DKRetail__ConversionQuantity__c", "conversion_quantity"),
        FieldMapping("DKRetail__BoxReceivingQuantity__c", "box_receiving_quantity", isString = false),
        FieldMapping("DKRetail__StandardUnitPrice__c", "standard_unit_price", isString = false),
        FieldMapping("SuperTax__c", "super_tax", isString = false),
        FieldMapping("DKRetail__LaunchDate__c", "launch_date", isString = false),
        FieldMapping("DKRetail__LogisticsBarCode__c", "logistics_barcode"),
        FieldMapping("TasteGift__c", "taste_gift"),
        FieldMapping("ProductFeatures__c", "product_features"),
        FieldMapping("SellingPoint__c", "selling_point"),
        FieldMapping("Purpose__c", "purpose"),
        FieldMapping("TargetAccountType__c", "target_account_type"),
        FieldMapping("Allergen__c", "allergen"),
        FieldMapping("CrossContamination__c", "cross_contamination"),
        FieldMapping("ImgRefPath__c", "img_ref_path"),
        FieldMapping("ImgRefPath_front__c", "img_ref_path_front"),
        FieldMapping("ImgRefPath_back__c", "img_ref_path_back"),
        FieldMapping("ImgRefPathTXT__c", "img_ref_path_txt"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("Pallet__c", "pallet", isString = false),
        FieldMapping("DKRetail__Barcode__c", "barcode"),
        FieldMapping("manufacture__c", "manufacture"),
        FieldMapping("manufacture_detail__c", "manufacture_detail"),
        FieldMapping("Claim_Management__c", "claim_management"),
        FieldMapping("New_Product__c", "new_product_sfid"),
        FieldMapping("StoreCondition__c", "store_condition_text"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val PROMOTION_METADATA = EntityMetadata(
    targetName = "Promotion",
    sObjectName = "DKRetail__Promotion__c",
    tableName = "promotion",
    pkColumn = "promotion_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "promotion_number", nullable = false),
        FieldMapping("DKRetail__PromotionType__c", "promotion_type"),
        FieldMapping("AccId__c", "account_sfid"),
        FieldMapping("DKRetail__StartDate__c", "start_date", nullable = false, isString = false),
        FieldMapping("DKRetail__EndDate__c", "end_date", nullable = false, isString = false),
        FieldMapping("DKRetail__PrimaryProductId__c", "primary_product_sfid"),
        FieldMapping("DKRetail__OtherProduct__c", "other_product"),
        FieldMapping("DKRetail__Message__c", "message"),
        FieldMapping("DKRetail__StandLocation__c", "stand_location"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("DKRetail__Remark__c", "remark"),
        FieldMapping("DKRetail__ProductType__c", "product_type"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("DKRetail__ActualAmount__c", "dk_actual_amount", isString = false),
        FieldMapping("DKRetail__TargetAmount__c", "dk_target_amount", isString = false)
    ),
    extraStaticColumns = mapOf(
        // promotion.is_closed NOT NULL — Stage 1 placeholder (SF 매핑 필드 없음)
        "is_closed" to "false"
    )
)

val NOTICE_METADATA = EntityMetadata(
    targetName = "Notice",
    sObjectName = "DKRetail__Notice__c",
    tableName = "notice",
    pkColumn = "notice_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Title__c", "title"),
        FieldMapping("EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__Scope__c", "scope"),
        FieldMapping("DKRetail__Category__c", "category"),
        FieldMapping("DKRetail__Contents__c", "contents"),
        FieldMapping("DKRetail__Jeejum__c", "branch"),
        FieldMapping("DKRetail__JeejumCode__c", "branch_code"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val GROUP_METADATA = EntityMetadata(
    targetName = "Group",
    sObjectName = "Group",
    tableName = "group",
    pkColumn = "group_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name", nullable = false),
        FieldMapping("DeveloperName", "developer_name"),
        FieldMapping("Type", "type", nullable = false),
        FieldMapping("RelatedId", "related_sfid"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("Email", "email"),
        FieldMapping("DoesSendEmailToMembers", "does_send_email_to_members", isString = false),
        FieldMapping("DoesIncludeBosses", "does_include_bosses", isString = false),
        // SF Group 에 Description 컬럼 없음 — backend group.description 은 SF 매핑 외 (수기 입력용)
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val ACCOUNT_CATEGORY_MASTER_METADATA = EntityMetadata(
    targetName = "AccountCategoryMaster",
    sObjectName = "AccountCategoryMaster__c",
    tableName = "account_category_master",
    pkColumn = "account_category_master_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("AccountCode__c", "account_code", nullable = false),
        FieldMapping("Name", "name", nullable = false),
        FieldMapping("useSearch__c", "use_search", nullable = false, isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val AGREEMENT_HISTORY_METADATA = EntityMetadata(
    targetName = "AgreementHistory",
    sObjectName = "AgreementHistory__c",
    tableName = "agreement_history",
    pkColumn = "agreement_history_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("EmployeeId__c", "employee_sfid"),
        FieldMapping("AgreementFlag__c", "agreement_flag", nullable = false, isString = false),
        FieldMapping("AgreementDate__c", "agreement_date", nullable = false, isString = false),
        FieldMapping("AgreementWordId__c", "agreement_word_sfid"),
        FieldMapping("IsDeleted", "is_deleted", nullable = false, isString = false),
        FieldMapping("Name", "name"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val AGREEMENT_WORD_METADATA = EntityMetadata(
    targetName = "AgreementWord",
    sObjectName = "AgreementWord__c",
    tableName = "agreement_word",
    pkColumn = "agreement_word_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name", nullable = false),
        FieldMapping("Contents__c", "contents"),
        FieldMapping("Active__c", "active", nullable = false, isString = false),
        FieldMapping("ActiveDate__c", "active_date", isString = false),
        FieldMapping("AfterActiveDate__c", "after_active_date", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("CreatedDate", "created_at", nullable = false, isString = false),
        FieldMapping("LastModifiedDate", "updated_at", nullable = false, isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val ALTERNATIVE_HOLIDAY_METADATA = EntityMetadata(
    targetName = "AlternativeHoliday",
    sObjectName = "DKRetail__AlternativeHoliday__c",
    tableName = "alternative_holiday",
    pkColumn = "alternative_holiday_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__ActualWorkDate__c", "actual_work_date", nullable = false, isString = false),
        FieldMapping("DKRetail__TargetAltHolidayDate__c", "target_alt_holiday_date", nullable = false, isString = false),
        FieldMapping("DKRetail__ConfirmAltHolidayDate__c", "confirm_alt_holiday_date", isString = false),
        FieldMapping("DKRetail__Status__c", "status", nullable = false),
        FieldMapping("DKRetail__ChangeReason__c", "change_reason"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val APPOINTMENT_METADATA = EntityMetadata(
    targetName = "Appointment",
    sObjectName = "Appointment__c",
    tableName = "appointment",
    pkColumn = "appointment_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("EmployeeCode__c", "employee_code", nullable = false),
        FieldMapping("isEmpCodeExist__c", "emp_code_exist", nullable = false, isString = false),
        FieldMapping("OrgCode__c", "after_org_code"),
        FieldMapping("OrgName__c", "after_org_name"),
        FieldMapping("Jikchak__c", "jikchak"),
        FieldMapping("Jikwee__c", "jikwee"),
        FieldMapping("Jikgub__c", "jikgub"),
        FieldMapping("WorkType__c", "work_type"),
        FieldMapping("ManageType__c", "manage_type"),
        FieldMapping("JobCode__c", "job_code"),
        FieldMapping("WorkArea__c", "work_area"),
        FieldMapping("Jikjong__c", "jikjong"),
        FieldMapping("AppointmentDate__c", "appoint_date", nullable = false, isString = false),
        FieldMapping("JobName__c", "job_name"),
        FieldMapping("OrdDetailCode__c", "ord_detail_code"),
        FieldMapping("OrdDetailNode__c", "ord_detail_node"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("OwnerId", "owner_sfid")
    )
)

val ATTENDANCE_LOG_METADATA = EntityMetadata(
    targetName = "AttendanceLog",
    sObjectName = "DKRetail__CommuteLog__c",
    tableName = "attendance_log",
    pkColumn = "attendance_log_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__CommuteDate__c", "attendance_date", isString = false),
        FieldMapping("DKRetail__AccId__c", "account_sfid"),
        FieldMapping("DKRetail__SecondWorkType__c", "second_work_type"),
        FieldMapping("DKRetail__Reason__c", "reason"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val ATTEND_INFO_METADATA = EntityMetadata(
    targetName = "AttendInfo",
    sObjectName = "AttendInfo__c",
    tableName = "attend_info",
    pkColumn = "attend_info_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("EmployeeCode__c", "employee_code", nullable = false),
        FieldMapping("StartDate__c", "start_date", nullable = false),
        FieldMapping("EndDate__c", "end_date"),
        FieldMapping("AttendType__c", "attend_type"),
        FieldMapping("Status__c", "status"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val BRANCH_REVIEW_METADATA = EntityMetadata(
    targetName = "BranchReview",
    sObjectName = "BranchReview__c",
    tableName = "branch_review",
    pkColumn = "branch_review_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("BranchName__c", "branch_name"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("FirstDayofMonth__c", "first_day_of_month", isString = false),
        FieldMapping("Confirmed__c", "confirmed", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val CLAIM_METADATA = EntityMetadata(
    targetName = "Claim",
    sObjectName = "DKRetail__Claim__c",
    tableName = "claim",
    pkColumn = "claim_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__AccountId__c", "account_sfid"),
        FieldMapping("DKRetail__ClaimDate__c", "date", nullable = false, isString = false),
        FieldMapping("DKRetail__ClaimType1__c", "claim_type1", nullable = false),
        FieldMapping("DKRetail__ClaimType2__c", "claim_type2", nullable = false),
        FieldMapping("DKRetail__Description__c", "defect_description", nullable = false),
        FieldMapping("DKRetail__Quantity__c", "defect_quantity", nullable = false, isString = false),
        FieldMapping("DKRetail__Amount__c", "purchase_amount", isString = false),
        FieldMapping("DKRetail__PurchaseMethod__c", "purchase_method_code"),
        FieldMapping("DKRetail__RequestType__c", "request_type_code"),
        FieldMapping("DKRetail__Status__c", "status", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__counselNumber__c", "counsel_number"),
        FieldMapping("DKRetail__ActionCode__c", "action_code"),
        FieldMapping("DKRetail__ActionStatus__c", "action_status"),
        FieldMapping("ActContent__c", "act_content"),
        FieldMapping("DKRetail__ReasonType__c", "reason_type"),
        FieldMapping("DKRetail__CosmosKey__c", "cosmos_key"),
        FieldMapping("DKRetail__ProductId__c", "product_sfid"),
        FieldMapping("ReturnOrderNumber__c", "customer_delivery_date", isString = false),
        FieldMapping("DKRetail__ReturnOrderNumber__c", "return_order_number"),
        FieldMapping("DKRetail__ExpirationDate__c", "expiration_date", isString = false),
        FieldMapping("DKRetail__InterfaceDate__c", "interface_date", isString = false),
        FieldMapping("DKRetail__ManufacturingDate__c", "manufacturing_date", isString = false),
        FieldMapping("DKRetail__InitialClaim__c", "initial_claim"),
        FieldMapping("DKRetail__LogisticsCenter__c", "logistics_center"),
        FieldMapping("ClaimSequence__c", "claim_sequence"),
        FieldMapping("DKRetail__DetailSNSName__c", "detail_sns_name"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("division__c", "division"),
        FieldMapping("DKRetail__Channel__c", "channel"),
        FieldMapping("DKRetail__SampleCollectionFlag__c", "sample_collection_flag", isString = false),
        FieldMapping("ImageCount__c", "image_count"),
        FieldMapping("DKRetail__ActionDate__c", "action_date", isString = false),
        FieldMapping("IsDeleted", "is_deleted", nullable = false, isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val DISPLAY_WORK_SCHEDULE_METADATA = EntityMetadata(
    targetName = "DisplayWorkSchedule",
    sObjectName = "DisplayWorkScheduleMaster__c",
    tableName = "display_work_schedule",
    pkColumn = "display_work_schedule_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Account__c", "account_sfid"),
        FieldMapping("FullName__c", "employee_sfid"),
        FieldMapping("StartDate__c", "start_date", isString = false),
        FieldMapping("EndDate__c", "end_date", isString = false),
        FieldMapping("Confirmed__c", "confirmed", isString = false),
        FieldMapping("TypeOfWork1__c", "type_of_work1"),
        FieldMapping("TypeOfWork3__c", "type_of_work3"),
        FieldMapping("TypeOfWork4__c", "type_of_work4"),
        FieldMapping("TypeOfWork5__c", "type_of_work5"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("LastMonthRevenue__c", "last_month_revenue", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val EMPLOYEE_INPUT_CRITERIA_MASTER_METADATA = EntityMetadata(
    targetName = "EmployeeInputCriteriaMaster",
    sObjectName = "EmployeeInputCriteriaMaster__c",
    tableName = "employee_input_criteria_master",
    pkColumn = "employee_input_criteria_master_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("BifurcationHalfPersonStandard__c", "bifurcation_half_person_standard", isString = false),
        FieldMapping("Boundary__c", "boundary", isString = false),
        FieldMapping("Category__c", "category_sfid"),
        FieldMapping("Confirmed__c", "confirmed", nullable = false, isString = false),
        FieldMapping("StartDate__c", "start_date", isString = false),
        FieldMapping("EndDate__c", "end_date", isString = false),
        FieldMapping("Fixed1PersonStandardAmount__c", "fixed_1_person_standard_amount", isString = false),
        FieldMapping("TypeOfWork1__c", "type_of_work_1"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val ERP_ORDER_METADATA = EntityMetadata(
    targetName = "ErpOrder",
    sObjectName = "ERP_Order__c",
    tableName = "erp_order",
    pkColumn = "erp_order_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "sap_order_number", nullable = false),
        FieldMapping("SAPAccountCode__c", "sap_account_code"),
        FieldMapping("SAPAccountName__c", "sap_account_name"),
        FieldMapping("DeliveryRequestDate__c", "delivery_request_date", isString = false),
        FieldMapping("OrderDate__c", "order_date", isString = false),
        FieldMapping("EmployeeCode__c", "employee_code"),
        FieldMapping("EmployeeName__c", "employee_name"),
        FieldMapping("TotalOrderAmount__c", "order_sales_amount", isString = false),
        FieldMapping("OrderChannel__c", "order_channel"),
        FieldMapping("OrderChannel_NM__c", "order_channel_nm"),
        FieldMapping("OrderType__c", "order_type"),
        FieldMapping("OrderType_NM__c", "order_type_nm"),
        FieldMapping("AccountId__c", "account_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val ERP_ORDER_PRODUCT_METADATA = EntityMetadata(
    targetName = "ErpOrderProduct",
    sObjectName = "ERP_OrderProduct__c",
    tableName = "erp_order_product",
    pkColumn = "erp_order_product_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("ERPOrderId__c", "erp_order_sfid"),
        FieldMapping("SAPOrderNumber__c", "sap_order_number", nullable = false),
        FieldMapping("LineNumber__c", "line_number", nullable = false),
        FieldMapping("ExternalKey__c", "external_key", nullable = false),
        FieldMapping("ProductCode__c", "product_code"),
        FieldMapping("ProductName__c", "product_name"),
        FieldMapping("OrderQuantity__c", "order_quantity", isString = false),
        FieldMapping("Unit__c", "unit"),
        FieldMapping("ConfirmQuantity_Box__c", "confirm_quantity_box", isString = false),
        FieldMapping("ConfirmQuantity__c", "confirm_quantity", isString = false),
        FieldMapping("Confirm_Unit__c", "confirm_unit"),
        FieldMapping("DefaultReason__c", "default_reason"),
        FieldMapping("LineItemStatus__c", "line_item_status"),
        FieldMapping("OrderStatus__c", "delivery_status"),
        FieldMapping("ShippingDriverName__c", "shipping_driver_name"),
        FieldMapping("ShippingVehicle__c", "shipping_vehicle"),
        FieldMapping("ShippingDriverPhone__c", "shipping_driver_phone"),
        FieldMapping("ShippingScheduleTime__c", "shipping_schedule_time"),
        FieldMapping("ShippingCompleteTime__c", "shipping_complete_time"),
        FieldMapping("ShippingQuantity_Box__c", "shipping_quantity_box", isString = false),
        FieldMapping("ShippingQuantity__c", "shipping_quantity", isString = false),
        FieldMapping("OrderSalesLineAmount__c", "order_sales_line_amount", isString = false),
        FieldMapping("ShippingAmount__c", "shipping_amount", isString = false),
        FieldMapping("Plant__c", "plant"),
        FieldMapping("Plant_NM__c", "plant_nm"),
        FieldMapping("ReleaseQuantity__c", "release_quantity", isString = false),
        FieldMapping("ReleaseAmount__c", "release_amount", isString = false),
        FieldMapping("BoxQuantity__c", "box_quantity", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val HOLIDAY_MASTER_METADATA = EntityMetadata(
    targetName = "HolidayMaster",
    sObjectName = "HolidayMaster__c",
    tableName = "holiday_master",
    pkColumn = "holiday_master_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("HolidayDate__c", "holiday_date", nullable = false, isString = false),
        FieldMapping("Name", "name", nullable = false),
        FieldMapping("Type__c", "type", nullable = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val HQ_REVIEW_METADATA = EntityMetadata(
    targetName = "HqReview",
    sObjectName = "HQReview__c",
    tableName = "hq_review",
    pkColumn = "hq_review_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("BranchCode__c", "branch_code"),
        FieldMapping("BranchName__c", "branch_name"),
        FieldMapping("FirstDayofMonth__c", "first_day_of_month", isString = false),
        FieldMapping("EvaluationyType__c", "evaluation_type"),
        FieldMapping("ABCTypeCode__c", "abc_type_code"),
        FieldMapping("HR_Code_c__c", "hr_code"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val INSPECTION_THEME_METADATA = EntityMetadata(
    targetName = "InspectionTheme",
    sObjectName = "Theme__c",
    tableName = "inspection_theme",
    pkColumn = "inspection_theme_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Title__c", "title"),
        FieldMapping("StartDate__c", "start_date", isString = false),
        FieldMapping("EndDate__c", "end_date", isString = false),
        FieldMapping("Department__c", "department"),
        FieldMapping("BranchCode__c", "branch_code"),
        FieldMapping("PublicFlag__c", "public_flag", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE_METADATA = EntityMetadata(
    targetName = "MonthlyFemaleEmployeeIntegrationSchedule",
    sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c",
    tableName = "monthly_female_employee_integration_schedule",
    pkColumn = "monthly_female_employee_integration_schedule_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("ExternalKey__c", "external_key"),
        FieldMapping("Year__c", "year"),
        FieldMapping("Month__c", "month"),
        FieldMapping("Account__c", "account_sfid"),
        FieldMapping("FullName__c", "employee_sfid"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("WorkingCategory1__c", "working_category1"),
        FieldMapping("WorkingCategory3__c", "working_category3"),
        FieldMapping("WorkingCategory4__c", "working_category4"),
        FieldMapping("WorkingCategory5__c", "working_category5"),
        FieldMapping("EmpBranchName__c", "emp_branch_name"),
        FieldMapping("ProfessionalPromotionTeam__c", "professional_promotion_team"),
        FieldMapping("WorkingDaysMonth__c", "working_days_month", isString = false),
        FieldMapping("NumberOfInputs__c", "number_of_inputs", isString = false),
        FieldMapping("EquivalentNumberOfWorkingDays__c", "equivalent_number_of_working_days", isString = false),
        FieldMapping("ConvertedHeadcount__c", "converted_headcount", isString = false),
        FieldMapping("EDI_POS__c", "edi_pos", isString = false),
        FieldMapping("ThisMonthAmount__c", "this_month_amount", isString = false),
        FieldMapping("AccountConvertedHeadcount__c", "account_converted_headcount", isString = false),
        FieldMapping("EmployeeInputCriteriaMaster__c", "employee_input_criteria_master_sfid"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val MONTHLY_SALES_HISTORY_METADATA = EntityMetadata(
    targetName = "MonthlySalesHistory",
    sObjectName = "MonthlySalesHistory__c",
    tableName = "monthly_sales_history",
    pkColumn = "monthly_sales_history_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("SalesYear__c", "sales_year"),
        FieldMapping("SalesMonth__c", "sales_month"),
        FieldMapping("LastMonthResults__c", "last_month_results", isString = false),
        FieldMapping("ShipClosingAmount__c", "ship_closing_amount", isString = false),
        FieldMapping("ABCClosingAmount1__c", "abc_closing_amount1", isString = false),
        FieldMapping("ABCClosingAmount2__c", "abc_closing_amount2", isString = false),
        FieldMapping("ABCClosingAmount3__c", "abc_closing_amount3", isString = false),
        FieldMapping("AmbientPurpose__c", "ambient_purpose", isString = false),
        FieldMapping("FridgePurpose__c", "fridge_purpose", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("Externalkey__c", "external_key"),
        FieldMapping("TotalLedgerAmount__c", "total_ledger_amount", isString = false),
        FieldMapping("AccountId__c", "account_sfid"),
        FieldMapping("SAPAccountCode__c", "sap_account_code"),
        FieldMapping("SalesDate__c", "sales_date", isString = false),
        FieldMapping("LastMonthlySalesHistory__c", "last_monthly_sales_history_sfid"),
        FieldMapping("Confirm__c", "is_confirmed", isString = false),
        FieldMapping("HQReviews__c", "hq_review_sfid"),
        FieldMapping("Remark__c", "remark"),
        FieldMapping("ShipClosingAmountNH__c", "ship_closing_amount_nh", isString = false),
        FieldMapping("ShipClosingAmount1__c", "ship_closing_amount1", isString = false),
        FieldMapping("ShipClosingAmount2__c", "ship_closing_amount2", isString = false),
        FieldMapping("ShipClosingAmount3__c", "ship_closing_amount3", isString = false),
        FieldMapping("ShipClosingAmount4__c", "ship_closing_amount4", isString = false),
        FieldMapping("ShipClosingSumAmount__c", "ship_closing_sum_amount", isString = false),
        FieldMapping("ABCClosingAmount4__c", "abc_closing_amount4", isString = false),
        FieldMapping("ABCClosingSumAmount__c", "abc_closing_sum_amount", isString = false),
        FieldMapping("LastMonthTargetByHand__c", "last_month_target_by_hand", isString = false),
        FieldMapping("ThisMonthTarget__c", "this_month_target", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val NEW_PRODUCT_METADATA = EntityMetadata(
    targetName = "NewProduct",
    sObjectName = "NewProduct__c",
    tableName = "new_product",
    pkColumn = "new_product_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Customer_Survey__c", "customer_survey", isString = false),
        FieldMapping("Initiator__c", "initiator"),
        FieldMapping("ManagementType__c", "management_type"),
        FieldMapping("Marketability_Review_Report__c", "marketability_review_report", isString = false),
        FieldMapping("Product_Code__c", "product_code_sfid"),
        FieldMapping("Product_Name__c", "product_name", nullable = false),
        FieldMapping("Product_code1__c", "product_code1"),
        FieldMapping("Purpose__c", "purpose", nullable = false),
        FieldMapping("Release_Review_Report__c", "release_review_report", nullable = false, isString = false),
        FieldMapping("Release__c", "release", nullable = false, isString = false),
        FieldMapping("Status__c", "status", nullable = false),
        FieldMapping("firstpropose__c", "firstpropose", nullable = false, isString = false),
        FieldMapping("friday_taste__c", "friday_taste", nullable = false, isString = false),
        FieldMapping("Upload_Description__c", "upload_description"),
        FieldMapping("MarketingTeam__c", "marketing_team"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("RecordTypeId", "record_type_sfid"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val ORDER_REQUEST_METADATA = EntityMetadata(
    targetName = "OrderRequest",
    sObjectName = "DKRetail__OrderRequest__c",
    tableName = "order_request",
    pkColumn = "order_request_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "order_request_number", nullable = false),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__AccountId__c", "account_sfid"),
        FieldMapping("OrderDate__c", "order_date", nullable = false, isString = false),
        FieldMapping("DKRetail__OrderDate__c", "dk_order_date", isString = false),
        FieldMapping("DKRetail__RequestDate__c", "delivery_date", nullable = false, isString = false),
        FieldMapping("TotalOrderAmount__c", "total_amount", nullable = false, isString = false),
        FieldMapping("DKRetail__RequestStatus__c", "order_request_status", nullable = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val ORDER_REQUEST_PRODUCT_METADATA = EntityMetadata(
    targetName = "OrderRequestProduct",
    sObjectName = "DKRetail__OrderRequestProduct__c",
    tableName = "order_request_product",
    pkColumn = "order_request_product_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("DKRetail__RequestNumber__c", "order_request_sfid"),
        FieldMapping("Name", "name"),
        FieldMapping("LineNumber__c", "line_number", nullable = false, isString = false),
        FieldMapping("DKRetail__LineNumber__c", "dk_line_number"),
        FieldMapping("ProductCode__c", "product_code", nullable = false),
        FieldMapping("TotalQuantity_Box__c", "quantity_boxes", nullable = false, isString = false),
        FieldMapping("TotalQuantity_Each__c", "quantity_pieces", nullable = false, isString = false),
        FieldMapping("DKRetail__OrderingUnit__c", "unit", nullable = false),
        FieldMapping("DKRetail__TotalAmount__c", "amount", nullable = false, isString = false),
        FieldMapping("DKRetail__LineChangeType__c", "line_change_type"),
        FieldMapping("Status__c", "status"),
        FieldMapping("DKRetail__ProductId__c", "product_sfid"),
        FieldMapping("DKRetail__Box__c", "box", isString = false),
        FieldMapping("DKRetail__Piece__c", "piece", isString = false),
        FieldMapping("DKRetail__BoxQuantity__c", "box_quantity", isString = false),
        FieldMapping("DKRetail__TotalCount__c", "dk_total_count", isString = false),
        FieldMapping("TotalCount__c", "total_count", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val PRODUCT_BARCODE_METADATA = EntityMetadata(
    targetName = "ProductBarcode",
    sObjectName = "ProductBarcode__c",
    tableName = "product_barcode",
    pkColumn = "product_barcode_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("ProductName__c", "product_name"),
        FieldMapping("ProductBarcode__c", "barcode"),
        FieldMapping("ProductUnit__c", "unit"),
        FieldMapping("ProductSequence__c", "sort_order"),
        FieldMapping("Product__c", "product_sfid"),
        FieldMapping("ProductCode__c", "product_code"),
        FieldMapping("CustomKey__c", "custom_key"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val PROFESSIONAL_PROMOTION_TEAM_HISTORY_METADATA = EntityMetadata(
    targetName = "ProfessionalPromotionTeamHistory",
    sObjectName = "ProfessionalPromotionTeamHistory__c",
    tableName = "professional_promotion_team_history",
    pkColumn = "professional_promotion_team_history_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("EmployeeId__c", "employee_sfid"),
        FieldMapping("oldValue__c", "old_value"),
        FieldMapping("newValue__c", "new_value", nullable = false),
        FieldMapping("updateTime__c", "changed_at", nullable = false, isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val PROFESSIONAL_PROMOTION_TEAM_MASTER_METADATA = EntityMetadata(
    targetName = "ProfessionalPromotionTeamMaster",
    sObjectName = "ProfessionalPromotionTeamMaster__c",
    tableName = "professional_promotion_team_master",
    pkColumn = "professional_promotion_team_master_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Account__c", "account_sfid"),
        FieldMapping("FullName__c", "full_name_sfid"),
        FieldMapping("ProfessionalPromotionTeam__c", "team_type", nullable = false),
        FieldMapping("StartDate__c", "start_date", nullable = false, isString = false),
        FieldMapping("EndDate__c", "end_date", isString = false),
        FieldMapping("Confirmed__c", "is_confirmed", nullable = false, isString = false),
        FieldMapping("CostCenterCode__c", "branch_code"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

// PromotionEmployee SObject 는 master-detail child (부모 Promotion 의 owner 따름) 라
// SF 에 OwnerId 컬럼이 없음 — backend Entity 의 @SFField("OwnerId") 는 잘못된 정의 (별도 정정 필요).
// 본 ETL 에서는 SOQL 호환 위해 OwnerId 매핑 제외 → backend 의 owner_sfid 컬럼은 NULL 로 적재.
val PROMOTION_EMPLOYEE_METADATA = EntityMetadata(
    targetName = "PromotionEmployee",
    sObjectName = "DKRetail__PromotionEmployee__c",
    tableName = "promotion_employee",
    pkColumn = "promotion_employee_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__PromotionId__c", "promotion_sfid"),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__ScheduleDate__c", "schedule_date", isString = false),
        FieldMapping("DKRetail__WorkStatus__c", "work_status"),
        FieldMapping("DKRetail__WorkType1__c", "work_type1"),
        FieldMapping("DKRetail__WorkType3__c", "work_type3"),
        FieldMapping("DKRetail__ScheduleId__c", "team_member_schedule_sfid"),
        FieldMapping("PromoCloseByTm__c", "promo_close_by_tm", nullable = false, isString = false),
        FieldMapping("DKRetail__BasePrice__c", "base_price", isString = false),
        FieldMapping("DKRetail__DailyTargetCount__c", "daily_target_count", isString = false),
        FieldMapping("PrimaryProductAmount__c", "primary_product_amount", isString = false),
        FieldMapping("DKRetail__PrimarySalesQuantity__c", "primary_sales_quantity", isString = false),
        FieldMapping("DKRetail__PrimarySalesPrice__c", "primary_sales_price", isString = false),
        FieldMapping("DKRetail__OtherSalesAmount__c", "other_sales_amount", isString = false),
        FieldMapping("DKRetail__OtherSalesQuantity__c", "other_sales_quantity", isString = false),
        FieldMapping("S3ImageUniqueKey__c", "s3_image_unique_key"),
        FieldMapping("Description__c", "description"),
        FieldMapping("DKRetail__WorkType2__c", "dk_work_type2"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false)
    )
)

val PUSH_MESSAGE_METADATA = EntityMetadata(
    targetName = "PushMessage",
    sObjectName = "PushMessage__c",
    tableName = "push_message",
    pkColumn = "push_message_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("Message__c", "message"),
        FieldMapping("ScheduleDate__c", "schedule_date", isString = false),
        FieldMapping("EmployeeId__c", "employee_sfid"),
        FieldMapping("Branch__c", "branch"),
        FieldMapping("BranchCode__c", "branch_code"),
        FieldMapping("SObjectRecordId__c", "s_object_record_id"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val PUSH_MESSAGE_RECEIVER_METADATA = EntityMetadata(
    targetName = "PushMessageReceiver",
    sObjectName = "PushMessageReceiver__c",
    tableName = "push_message_receiver",
    pkColumn = "push_message_receiver_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("EmployeeId__c", "employee_sfid"),
        FieldMapping("MessageId__c", "push_message_sfid"),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val STAFF_REVIEW_METADATA = EntityMetadata(
    targetName = "StaffReview",
    sObjectName = "StaffReview__c",
    tableName = "staff_review",
    pkColumn = "staff_review_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail_EmployeeId__c", "employee_sfid"),
        FieldMapping("EmployeeName__c", "employee_name"),
        FieldMapping("EmployeeNumber__c", "employee_code"),
        FieldMapping("Branch__c", "branch"),
        FieldMapping("BranchReviews__c", "branch_review_sfid"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("EmployeeTotalScore__c", "employee_total_score", isString = false),
        FieldMapping("Attendance__c", "attendance_score", isString = false),
        FieldMapping("InstructionsDefault__c", "instruction_disobedience_score", isString = false),
        FieldMapping("Priority_EventItemManage__c", "priority_item_event_score", isString = false),
        FieldMapping("DisplayManageEventGoals__c", "display_event_goal_score", isString = false),
        FieldMapping("BusinessPartnerTies__c", "account_partnership_score", isString = false),
        FieldMapping("ClothesSatellite__c", "clothes_hygiene_score", isString = false),
        FieldMapping("ProductManageCallment__c", "product_manage_callment_score", isString = false),
        FieldMapping("EducationalEvaluation__c", "education_evaluation_score", isString = false),
        FieldMapping("DKRetail_WorkingCategory1__c", "working_category1"),
        FieldMapping("DKRetail_WorkingCategory2__c", "working_category2"),
        FieldMapping("DKRetail_WorkingCategory3__c", "working_category3"),
        FieldMapping("JobCode__c", "job_code"),
        FieldMapping("FirstDayofMonth__c", "first_day_of_month", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        FieldMapping("EmployeeType__c", "employee_type"),
        FieldMapping("EntryDate__c", "entry_date", isString = false),
        FieldMapping("Jikwee__c", "jikwee")
    )
)

val TEAM_MEMBER_SCHEDULE_METADATA = EntityMetadata(
    targetName = "TeamMemberSchedule",
    sObjectName = "DKRetail__TeamMemberSchedule__c",
    tableName = "team_member_schedule",
    pkColumn = "team_member_schedule_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
        FieldMapping("DKRetail__WorkingDate__c", "working_date", isString = false),
        FieldMapping("DKRetail__WorkingType__c", "working_type"),
        FieldMapping("DKRetail__WorkingCategory1__c", "working_category1"),
        FieldMapping("DKRetail__WorkingCategory2__c", "working_category2"),
        FieldMapping("DKRetail__WorkingCategory3__c", "working_category3"),
        FieldMapping("WorkingCategory4__c", "working_category4"),
        FieldMapping("AccountId__c", "account_sfid"),
        FieldMapping("teamleadersfid__c", "team_leader_sfid"),
        FieldMapping("DKRetail__AltHolidayId__c", "alt_holiday_sfid"),
        FieldMapping("DKRetail__CommuteLogId__c", "commute_log_sfid"),
        FieldMapping("DKRetail__PromotionEmpId__c", "promotion_employee_sfid"),
        FieldMapping("DisplayWorkScheduleMaster__c", "display_work_schedule_sfid"),
        FieldMapping("CommuteReportDateTime__c", "commute_report_datetime", isString = false),
        FieldMapping("ID__c", "id_field"),
        FieldMapping("TraversalFlag__c", "traversal_flag"),
        FieldMapping("Equipment1__c", "equipment1"),
        FieldMapping("Equipment2__c", "equipment2"),
        FieldMapping("Equipment3__c", "equipment3"),
        FieldMapping("Equipment4__c", "equipment4"),
        FieldMapping("Equipment5__c", "equipment5"),
        FieldMapping("Equipment6__c", "equipment6"),
        FieldMapping("Equipment7__c", "equipment7"),
        FieldMapping("Equipment8__c", "equipment8"),
        FieldMapping("Equipment9__c", "equipment9"),
        FieldMapping("Equipment10__c", "equipment10"),
        FieldMapping("Yes_ChkCnt__c", "yes_chk_cnt", isString = false),
        FieldMapping("No_ChkCnt__c", "no_chk_cnt", isString = false),
        FieldMapping("precaution_chk__c", "precaution_chk", isString = false),
        FieldMapping("precaution__c", "precaution"),
        FieldMapping("StartTime__c", "start_time", isString = false),
        FieldMapping("CompleteTime__c", "complete_time", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("HRCode__c", "hr_code"),
        FieldMapping("DKRetail__PromotionEmpIdExt__c", "promotion_emp_id_ext"),
        FieldMapping("SecondWorkType__c", "second_work_type"),
        FieldMapping("WorkingCategory5__c", "working_category5"),
        FieldMapping("ref_accountName__c", "ref_account_name"),
        FieldMapping("MonthlyFemaleEmployeeIntegrationSchedule__c", "monthly_female_employee_integration_schedule_sfid"),
        FieldMapping("ProfessionalPromotionTeam__c", "professional_promotion_team"),
        FieldMapping("CostCenterCode__c", "cost_center_code"),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

val UPLOAD_FILE_METADATA = EntityMetadata(
    targetName = "UploadFile",
    sObjectName = "UploadFile__c",
    tableName = "upload_file",
    pkColumn = "upload_file_id",
    conflictKey = "sfid",
    fields = listOf(
        FieldMapping("Id", "sfid", nullable = false),
        FieldMapping("Name", "name"),
        FieldMapping("UniqueKey__c", "unique_key"),
        FieldMapping("RecordId__c", "record_id"),
        FieldMapping("Size__c", "size"),
        FieldMapping("Object__c", "parent_type", nullable = false),
        FieldMapping("Url__c", "url"),
        FieldMapping("UploadKbn__c", "upload_kbn"),
        FieldMapping("FileId__c", "file_id"),
        FieldMapping("Date__c", "file_date", isString = false),
        FieldMapping("IsDeleted", "is_deleted", isString = false),
        FieldMapping("CreatedDate", "created_at", nullable = false, isString = false),
        FieldMapping("OwnerId", "owner_sfid"),
        FieldMapping("CreatedById", "created_by_sfid"),
        FieldMapping("LastModifiedById", "last_modified_by_sfid")
    )
)

data class PermissionStagingMetadata(
    val schemaName: String = "powersales",
    val stagingTableName: String = "sf_permission_set_assignment_raw",
    val fields: List<FieldMapping> = listOf(
        FieldMapping("AssigneeId", "assignee_id", nullable = false),
        FieldMapping("Assignee.DKRetail__EmployeeNumber__c", "assignee_employee_code"),
        FieldMapping("PermissionSetId", "permission_set_id", nullable = false),
        FieldMapping("PermissionSet.Name", "permission_set_name"),
        FieldMapping("PermissionSet.Label", "permission_set_label")
    )
)

val PERMISSION_METADATA = PermissionStagingMetadata()

// =============================================================================
// Mapping tables (targets)
// =============================================================================

val APP_AUTHORITY_TO_USER_ROLE: Map<String, String> = mapOf(
    "여사원" to "WOMAN",
    "조장" to "LEADER",
    "지점장" to "BRANCH_MANAGER",
    "영업부장" to "SALES_MANAGER",
    "사업부장" to "BUSINESS_MANAGER",
    "영업본부장" to "HEADQUARTERS_MANAGER",
    "영업지원실" to "SALES_SUPPORT",
    "시스템관리자" to "SYSTEM_ADMIN",
    "AccountViewAll" to "ACCOUNT_VIEW_ALL"
)
val USER_ROLE_FALLBACK = "UNKNOWN"

/**
 * 매핑 — DKRetail__Employee__c.ProfessionalPromotionTeam__c (한글 free text) →
 * ProfessionalPromotionTeamType enum (5종).
 *
 * 출처: backend/.../promotion/enums/ProfessionalPromotionTeamType.kt (displayName 정합).
 * SF picklist 아님 (free text) — 매핑 실패 시 NULL 처리 (fallback 없음).
 */
val PPT_KOREAN_TO_ENUM: Map<String, String> = mapOf(
    "라면세일조" to "RAMEN_SALE",
    "프레시세일조_냉장" to "FRESH_SALE_REFRIGERATED",
    "프레시세일조_냉동" to "FRESH_SALE_FROZEN",
    "프레시세일조_만두" to "FRESH_SALE_DUMPLING",
    "카레행사조" to "CURRY_PROMOTION"
)

val PROFILE_NAME_TO_PROFILE_TYPE: Map<String, String> = mapOf(
    "8. 마케팅" to "MARKETING",
    "마케팅" to "MARKETING",
    "9. Staff" to "STAFF",
    "Staff" to "STAFF",
    "6. 조장" to "TEAM_LEADER",
    "조장" to "TEAM_LEADER",
    "4. 지점장" to "BRANCH_MANAGER",
    "지점장" to "BRANCH_MANAGER",
    "영업부장" to "SALES_MANAGER",
    "사업부장" to "BUSINESS_DIRECTOR",
    "본부장" to "DIVISION_HEAD",
    "5. 영업사원" to "SALES_REP",
    "영업사원" to "SALES_REP",
    "시스템 관리자" to "SYSTEM_ADMIN",
    "System Administrator" to "SYSTEM_ADMIN",
    "システム管理者" to "SYSTEM_ADMIN"
)
val PROFILE_TYPE_FALLBACK = "STAFF"

val PERMISSION_SET_TO_PERMISSIONS: Map<String, List<String>> = mapOf(
    "Employee_View_All" to listOf("EMPLOYEE_READ"),
    "Activity_View_All" to listOf("SCHEDULE_READ"),
    "SalesProgressViewAll" to listOf("ACCOUNT_READ"),
    "View_All" to listOf("AGREEMENT_READ"),
    "View_All_Edit_All" to listOf("SAFETY_CHECK_READ"),
    "View_ALL_EVENT" to listOf("SCHEDULE_READ"),
    "View_All_TeamMemberSchedule" to listOf("SCHEDULE_READ"),
    "Acc_Permission" to listOf("ACCOUNT_READ"),
    "Object_View_All" to listOf("ACCOUNT_READ", "EMPLOYEE_READ"),
    "SalesDiary_View_All" to listOf("SCHEDULE_READ"),
    "Promotion_Master_View_All" to listOf("PROMOTION_READ"),
    "Claim_View_All" to listOf("ACCOUNT_READ"),
    "notification_View_All" to listOf("DASHBOARD_READ"),
    "CVSCLAIMDELETE" to listOf("ACCOUNT_DELETE"),
    "Uploadfile_Create_Delete_Permission" to listOf("PROMOTION_WRITE"),
    "SalesAssistant" to listOf("DASHBOARD_READ", "SCHEDULE_READ"),
    "SalesSupport" to listOf("DASHBOARD_READ", "EMPLOYEE_READ", "ACCOUNT_READ", "SCHEDULE_READ"),
    "notice" to listOf("DASHBOARD_READ"),
    "ProfessionalPromotionTeam" to listOf("PROMOTION_READ", "PROMOTION_WRITE")
)

val INTENTIONALLY_SKIPPED_PERMISSION_SETS: Set<String> = setOf(
    "MFA", "MFA_User", "Einstein_search", "Classic",
    "sfdc_aiplanner_service_permset",
    "sfdcInternalInt__sfdc_a360", "sfdcInternalInt__sfdc_a360_sfcrm_data_extract",
    "sfdcInternalInt__sfdc_chatbot", "sfdcInternalInt__sfdc_nc_constraints_engine_deploy",
    "sfdcInternalInt__sfdc_scrt2",
    "Knowledge_Manager_523278", "mdtViewSet", "permset", "PersetInstead", "Test", "addtionalAppPermission",
    "X1_1", "X1_2", "X1_3", "X1_View", "X2_1", "X2_2", "X2_3", "X4_View",
    "Marketing_ETC", "Marketing_MC", "pptm",
    "OLS_View", "rehabilitation", "Sales_User"
)

// =============================================================================
// Identifier quoting
// =============================================================================

fun quoteTable(name: String): String = if (name == "user") "\"user\"" else name

// =============================================================================
// CSV parsing
// =============================================================================

fun parseCsvFile(file: File): List<Map<String, String?>> {
    if (!file.exists()) error("CSV file not found: ${file.absolutePath}")
    val rows = CSVReaderBuilder(FileReader(file)).build().readAll().map { it.toList() }
    if (rows.isEmpty()) return emptyList()
    val headers = rows[0].map { it.trim() }
    val result = mutableListOf<Map<String, String?>>()
    for (i in 1 until rows.size) {
        val row = rows[i]
        if (row.all { it.isBlank() }) continue
        val m = mutableMapOf<String, String?>()
        for (j in headers.indices) {
            val v = if (j < row.size) row[j].let { if (it.isBlank()) null else it } else null
            m[headers[j]] = v
        }
        result.add(m)
    }
    return result
}

// =============================================================================
// DB Config (db.properties)
// =============================================================================

data class DbConfig(
    val host: String,
    val port: Int,
    val database: String,
    val schema: String,
    val user: String,
    val password: String
) {
    /**
     * stringtype=unspecified — boolean/date/timestamp 컬럼에 String 값을 넣어도 Postgres 가 type 추론.
     * reWriteBatchedInserts=true — addBatch INSERT 를 multi-row INSERT 로 재작성 (속도 향상).
     */
    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$database?stringtype=unspecified&reWriteBatchedInserts=true"
}

fun loadDbConfig(scriptDir: File): DbConfig {
    val configFile = File(scriptDir, "db.properties")
    if (!configFile.exists()) {
        error("db.properties not found: ${configFile.absolutePath}\n" +
              "  → cp db.properties.template db.properties 후 값 채워 넣기")
    }
    val props = Properties()
    configFile.inputStream().use { props.load(it) }
    val host = props.getProperty("host")?.takeIf { it.isNotBlank() }
        ?: error("db.properties: host 누락")
    val port = props.getProperty("port")?.toIntOrNull()
        ?: error("db.properties: port 누락/숫자 아님")
    val database = props.getProperty("database")?.takeIf { it.isNotBlank() }
        ?: error("db.properties: database 누락")
    val schema = props.getProperty("schema") ?: "powersales"
    val user = props.getProperty("user")?.takeIf { it.isNotBlank() }
        ?: error("db.properties: user 누락")
    val password = props.getProperty("password") ?: ""
    if (password.isBlank()) {
        error("db.properties: password 누락 — scripts/db-tunnel.sh -s dev --password 로 조회")
    }
    return DbConfig(host, port, database, schema, user, password)
}

fun openConnection(cfg: DbConfig): Connection {
    Class.forName("org.postgresql.Driver")
    val conn = DriverManager.getConnection(cfg.jdbcUrl, cfg.user, cfg.password)
    conn.autoCommit = false
    return conn
}

// =============================================================================
// Progress bar (ASCII, \r 캐리지 리턴 기반)
// =============================================================================

/**
 * ASCII progress bar — 동일 라인 갱신.
 *
 * 사용: val pb = ProgressBar("Stage 1", total); pb.update(1, "Organization"); ... ; pb.done()
 *
 * 출력 예: [####------] 40% (4/10) Stage 1 - Employee
 *
 * 외부 라이브러리 의존 없음. CI / non-TTY 환경에서도 정상 동작 (단지 줄바꿈 없이 출력될 뿐).
 */
class ProgressBar(
    private val label: String,
    private val total: Int,
    private val width: Int = 30
) {
    private var current = 0

    fun update(step: Int, detail: String = "") {
        current = step.coerceAtMost(total)
        render(detail)
    }

    fun increment(detail: String = "") {
        current = (current + 1).coerceAtMost(total)
        render(detail)
    }

    fun done(finalDetail: String = "done") {
        current = total
        render(finalDetail)
        System.out.print("\n")
        System.out.flush()
    }

    private fun render(detail: String) {
        val pct = if (total == 0) 100 else (current * 100 / total)
        val filled = if (total == 0) width else (current * width / total)
        val bar = "#".repeat(filled) + "-".repeat(width - filled)
        val line = "\r[$bar] %3d%% (%d/%d) %s%s".format(pct, current, total, label, if (detail.isNotEmpty()) " - $detail" else "")
        System.out.print(line.padEnd(120))
        System.out.flush()
    }
}

// =============================================================================
// Report
// =============================================================================

data class TargetReport(
    val targetName: String,
    val sObjectName: String,
    val rawRowsCount: Int = 0,
    val insertedCount: Int = 0,
    val stage1Applied: Boolean = false,
    val stage2MappingApplied: Boolean = false,
    val stage2TransformApplied: Boolean = false,
    val errors: List<String> = emptyList()
)

fun writeReport(
    targetReports: List<TargetReport>,
    stage: String,
    outputFile: File
) {
    outputFile.parentFile?.mkdirs()
    val generatedAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    PrintWriter(outputFile).use { w ->
        w.println("SF 데이터 마이그레이션 리포트 (Stage $stage)")
        w.println("=".repeat(60))
        w.println("Generated at : $generatedAt")
        w.println()
        for (r in targetReports) {
            w.println("[${r.targetName}] (${r.sObjectName})")
            w.println("  raw 행 수             : ${r.rawRowsCount}")
            w.println("  insert/update 행 수   : ${r.insertedCount}")
            w.println("  Stage 1 적용          : ${if (r.stage1Applied) "Y" else "-"}")
            w.println("  Stage 2-B Mapping     : ${if (r.stage2MappingApplied) "Y" else "-"}")
            w.println("  Stage 2-C Transform   : ${if (r.stage2TransformApplied) "Y" else "-"}")
            if (r.errors.isNotEmpty()) {
                w.println("  errors:")
                for (e in r.errors) w.println("    - $e")
            }
            w.println()
        }
    }
}

// =============================================================================
// Target ordering
// =============================================================================

/**
 * target 메타 + csv 파일명 + backend entity 경로.
 *
 * @param meta EntityMetadata | PermissionStagingMetadata
 * @param sObjectName SF SObject API 이름
 * @param csvFileName input dir 안의 추출 CSV 파일명
 * @param entityRelPath backend Kotlin entity 의 상대 경로
 *   (backend/src/main/kotlin/com/otoki/powersales/<rel>.kt). null 이면 verify-metadata 면제.
 */
data class TargetSpec(
    val meta: Any,
    val sObjectName: String,
    val csvFileName: String,
    val entityRelPath: String?
)

val TARGET_SPECS: Map<String, TargetSpec> = mapOf(
    "Organization" to TargetSpec(ORGANIZATION_METADATA, "Org__c", "organizations.csv", "organization/entity/Organization"),
    "Account"      to TargetSpec(ACCOUNT_METADATA, "Account", "accounts.csv", "account/entity/Account"),
    "Product"      to TargetSpec(PRODUCT_METADATA, "DKRetail__Product__c", "products.csv", "product/entity/Product"),
    "Promotion"    to TargetSpec(PROMOTION_METADATA, "DKRetail__Promotion__c", "promotions.csv", "promotion/entity/Promotion"),
    "Group"        to TargetSpec(GROUP_METADATA, "Group", "groups.csv", "employee/entity/Group"),
    "Employee"     to TargetSpec(EMPLOYEE_METADATA, "DKRetail__Employee__c", "employees.csv", "employee/entity/Employee"),
    "User"         to TargetSpec(USER_METADATA, "User", "users.csv", "user/entity/User"),
    "Notice"       to TargetSpec(NOTICE_METADATA, "DKRetail__Notice__c", "notices.csv", "notice/entity/Notice"),
    // PermissionSetAssignment 은 SF SObject 와 backend 의 staging table 1:1 매핑 (자체 entity 없음) → verify 면제
    "AccountCategoryMaster" to TargetSpec(ACCOUNT_CATEGORY_MASTER_METADATA, "AccountCategoryMaster__c", "account_category_masters.csv", "account/entity/AccountCategoryMaster"),
    "AgreementHistory" to TargetSpec(AGREEMENT_HISTORY_METADATA, "AgreementHistory__c", "agreement_historys.csv", "common/entity/AgreementHistory"),
    "AgreementWord" to TargetSpec(AGREEMENT_WORD_METADATA, "AgreementWord__c", "agreement_words.csv", "common/entity/AgreementWord"),
    "AlternativeHoliday" to TargetSpec(ALTERNATIVE_HOLIDAY_METADATA, "DKRetail__AlternativeHoliday__c", "alternative_holidays.csv", "leave/entity/AlternativeHoliday"),
    "Appointment" to TargetSpec(APPOINTMENT_METADATA, "Appointment__c", "appointments.csv", "schedule/entity/Appointment"),
    "AttendanceLog" to TargetSpec(ATTENDANCE_LOG_METADATA, "DKRetail__CommuteLog__c", "attendance_logs.csv", "schedule/entity/AttendanceLog"),
    "AttendInfo" to TargetSpec(ATTEND_INFO_METADATA, "AttendInfo__c", "attend_infos.csv", "schedule/entity/AttendInfo"),
    "BranchReview" to TargetSpec(BRANCH_REVIEW_METADATA, "BranchReview__c", "branch_reviews.csv", "common/entity/BranchReview"),
    "Claim" to TargetSpec(CLAIM_METADATA, "DKRetail__Claim__c", "claims.csv", "claim/entity/Claim"),
    "DisplayWorkSchedule" to TargetSpec(DISPLAY_WORK_SCHEDULE_METADATA, "DisplayWorkScheduleMaster__c", "display_work_schedules.csv", "schedule/entity/DisplayWorkSchedule"),
    "EmployeeInputCriteriaMaster" to TargetSpec(EMPLOYEE_INPUT_CRITERIA_MASTER_METADATA, "EmployeeInputCriteriaMaster__c", "employee_input_criteria_masters.csv", "schedule/entity/EmployeeInputCriteriaMaster"),
    "ErpOrder" to TargetSpec(ERP_ORDER_METADATA, "ERP_Order__c", "erp_orders.csv", "order/entity/ErpOrder"),
    "ErpOrderProduct" to TargetSpec(ERP_ORDER_PRODUCT_METADATA, "ERP_OrderProduct__c", "erp_order_products.csv", "order/entity/ErpOrderProduct"),
    "HolidayMaster" to TargetSpec(HOLIDAY_MASTER_METADATA, "HolidayMaster__c", "holiday_masters.csv", "leave/entity/HolidayMaster"),
    "HqReview" to TargetSpec(HQ_REVIEW_METADATA, "HQReview__c", "hq_reviews.csv", "common/entity/HqReview"),
    "InspectionTheme" to TargetSpec(INSPECTION_THEME_METADATA, "Theme__c", "inspection_themes.csv", "inspection/entity/InspectionTheme"),
    "MonthlyFemaleEmployeeIntegrationSchedule" to TargetSpec(MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE_METADATA, "MonthlyFemaleEmployeeIntegrationSchedule__c", "monthly_female_employee_integration_schedules.csv", "schedule/entity/MonthlyFemaleEmployeeIntegrationSchedule"),
    "MonthlySalesHistory" to TargetSpec(MONTHLY_SALES_HISTORY_METADATA, "MonthlySalesHistory__c", "monthly_sales_historys.csv", "sales/entity/MonthlySalesHistory"),
    "NewProduct" to TargetSpec(NEW_PRODUCT_METADATA, "NewProduct__c", "new_products.csv", "product/entity/NewProduct"),
    "OrderRequest" to TargetSpec(ORDER_REQUEST_METADATA, "DKRetail__OrderRequest__c", "order_requests.csv", "order/entity/OrderRequest"),
    "OrderRequestProduct" to TargetSpec(ORDER_REQUEST_PRODUCT_METADATA, "DKRetail__OrderRequestProduct__c", "order_request_products.csv", "order/entity/OrderRequestProduct"),
    "ProductBarcode" to TargetSpec(PRODUCT_BARCODE_METADATA, "ProductBarcode__c", "product_barcodes.csv", "product/entity/ProductBarcode"),
    "ProfessionalPromotionTeamHistory" to TargetSpec(PROFESSIONAL_PROMOTION_TEAM_HISTORY_METADATA, "ProfessionalPromotionTeamHistory__c", "professional_promotion_team_historys.csv", "promotion/entity/ProfessionalPromotionTeamHistory"),
    "ProfessionalPromotionTeamMaster" to TargetSpec(PROFESSIONAL_PROMOTION_TEAM_MASTER_METADATA, "ProfessionalPromotionTeamMaster__c", "professional_promotion_team_masters.csv", "promotion/entity/ProfessionalPromotionTeamMaster"),
    "PromotionEmployee" to TargetSpec(PROMOTION_EMPLOYEE_METADATA, "DKRetail__PromotionEmployee__c", "promotion_employees.csv", "promotion/entity/PromotionEmployee"),
    "PushMessage" to TargetSpec(PUSH_MESSAGE_METADATA, "PushMessage__c", "push_messages.csv", "common/entity/PushMessage"),
    "PushMessageReceiver" to TargetSpec(PUSH_MESSAGE_RECEIVER_METADATA, "PushMessageReceiver__c", "push_message_receivers.csv", "common/entity/PushMessageReceiver"),
    "StaffReview" to TargetSpec(STAFF_REVIEW_METADATA, "StaffReview__c", "staff_reviews.csv", "common/entity/StaffReview"),
    "TeamMemberSchedule" to TargetSpec(TEAM_MEMBER_SCHEDULE_METADATA, "DKRetail__TeamMemberSchedule__c", "team_member_schedules.csv", "schedule/entity/TeamMemberSchedule"),
    "UploadFile" to TargetSpec(UPLOAD_FILE_METADATA, "UploadFile__c", "upload_files.csv", "common/entity/UploadFile"),
    "Permission"   to TargetSpec(PERMISSION_METADATA, "PermissionSetAssignment", "permission_set_assignments.csv", null)
)

/**
 * target 의존성 순서 — 선행 적재 필수 entity 가 먼저 오도록 정렬.
 * (FK 가 아닌 sfid 보존이라 실제 의존성은 거의 없으나, 운영상 사람-편의 순서)
 */
val TARGET_DEPENDENCY_ORDER = listOf(
    "Organization",
    "Account",
    "Product",
    "Promotion",
    "Group",
    "Employee",
    "User",
    "Notice",
    "AccountCategoryMaster",
    "AgreementHistory",
    "AgreementWord",
    "AlternativeHoliday",
    "Appointment",
    "AttendanceLog",
    "AttendInfo",
    "BranchReview",
    "Claim",
    "DisplayWorkSchedule",
    "EmployeeInputCriteriaMaster",
    "ErpOrder",
    "ErpOrderProduct",
    "HolidayMaster",
    "HqReview",
    "InspectionTheme",
    "MonthlyFemaleEmployeeIntegrationSchedule",
    "MonthlySalesHistory",
    "NewProduct",
    "OrderRequest",
    "OrderRequestProduct",
    "ProductBarcode",
    "ProfessionalPromotionTeamHistory",
    "ProfessionalPromotionTeamMaster",
    "PromotionEmployee",
    "PushMessage",
    "PushMessageReceiver",
    "StaffReview",
    "TeamMemberSchedule",
    "UploadFile",
    "Permission"
)

val SUPPORTED_TARGETS = setOf(
    "Organization", "Account", "Product", "Promotion", "Group",
    "Employee", "User", "Notice", "Permission", "AccountCategoryMaster",
    "AgreementHistory", "AgreementWord", "AlternativeHoliday", "Appointment", "AttendanceLog",
    "AttendInfo", "BranchReview", "Claim", "DisplayWorkSchedule", "EmployeeInputCriteriaMaster",
    "ErpOrder", "ErpOrderProduct", "HolidayMaster", "HqReview", "InspectionTheme",
    "MonthlyFemaleEmployeeIntegrationSchedule", "MonthlySalesHistory", "NewProduct", "OrderRequest", "OrderRequestProduct",
    "ProductBarcode", "ProfessionalPromotionTeamHistory", "ProfessionalPromotionTeamMaster", "PromotionEmployee", "PushMessage",
    "PushMessageReceiver", "StaffReview", "TeamMemberSchedule", "UploadFile"
)

fun sortTargetsByDependency(targets: List<String>): List<String> {
    val result = mutableListOf<String>()
    for (t in TARGET_DEPENDENCY_ORDER) if (t in targets) result.add(t)
    return result
}

// =============================================================================
// CLI helpers
// =============================================================================

fun parseKeyValueArgs(argv: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (a in argv) {
        val eq = a.indexOf('=')
        if (a.startsWith("--") && eq > 2) {
            map[a.substring(2, eq)] = a.substring(eq + 1)
        }
    }
    return map
}

fun parseTargets(targetsStr: String?): List<String> {
    val raw = targetsStr ?: SUPPORTED_TARGETS.joinToString(",")
    val targets = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    for (t in targets) {
        require(t in SUPPORTED_TARGETS) { "Unknown target: $t (allowed: ${SUPPORTED_TARGETS.joinToString(", ")})" }
    }
    return targets
}
