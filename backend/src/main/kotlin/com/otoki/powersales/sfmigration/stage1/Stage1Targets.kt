package com.otoki.powersales.sfmigration.stage1

/**
 * Stage 1 적재 target 메타 카탈로그.
 *
 * scripts/sf-data-migration/common.kts 의 EntityMetadata 카탈로그를 backend 로 이관
 * (적재에 의미 없는 isString/rawColumnsAsString/useCopyStrategy/copyChunkSize/pkColumn/
 *  conflictKey 는 제외 — backend service 는 항상 staging COPY → INSERT-SELECT 패턴이라
 *  COPY 전략 선택지 자체가 없음).
 *
 * Permission (PermissionStagingMetadata) 은 staging table 구조가 정규 entity 와 달라 본
 * 카탈로그에서 제외 — 별도 처리가 필요한 경우 전용 service 로 분리.
 *
 * 등록되지 않은 target 이름은 controller 에서 404 반환.
 *
 * 일괄 실행 순서는 [DEPENDENCY_ORDER] — scripts/common.kts 의 TARGET_DEPENDENCY_ORDER 1:1 이관.
 * sfid 보존 적재라 실제 FK 의존성은 거의 없으나 운영상 사람-편의 순서 유지.
 */
object Stage1Targets {

    private val ORGANIZATION = EntityMetadata(
        targetName = "Organization",
        sObjectName = "Org__c",
        tableName = "organization",
        csvFileName = "organizations.csv",
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
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val EMPLOYEE = EntityMetadata(
        targetName = "Employee",
        sObjectName = "DKRetail__Employee__c",
        tableName = "employee",
        csvFileName = "employees.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("DKRetail__EmpCode__c", "employee_code", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__Birthdate__c", "birth_date"),
            FieldMapping("DKRetail__Status__c", "status"),
            FieldMapping("DKRetail__APPLoginActive__c", "app_login_active"),
            FieldMapping("DKRetail__AppAuthority__c", "role"),
            FieldMapping("DKRetail__OrgName__c", "org_name"),
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("DKRetail__WorkPhone__c", "work_phone"),
            FieldMapping("Phone__c", "phone"),
            FieldMapping("DKRetail__HomePhone__c", "home_phone"),
            FieldMapping("DKRetail__WorkEmail__c", "work_email"),
            FieldMapping("DKRetail__Email__c", "email"),
            FieldMapping("DKRetail__Sex__c", "gender"),
            FieldMapping("DKRetail__StartDate__c", "start_date"),
            FieldMapping("DKRetail__EndDate__c", "end_date"),
            FieldMapping("AgreementFlag__c", "agreement_flag"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("ProfessionalPromotionTeam__c", "professional_promotion_team"),
            FieldMapping("DKRetail__Jikchak__c", "jikchak"),
            FieldMapping("DKRetail__Jikwee__c", "jikwee"),
            FieldMapping("DKRetail__Jikgub__c", "jikgub"),
            FieldMapping("DKRetail__WorkType__c", "work_type"),
            FieldMapping("DKRetail__JobCode__c", "job_code"),
            FieldMapping("DKRetail__WorkArea__c", "work_area"),
            FieldMapping("DKRetail__Jikjong__c", "jikjong"),
            FieldMapping("DKRetail__AppointmentDate__c", "appointment_date"),
            FieldMapping("OrdDetailNode__c", "ord_detail_node"),
            FieldMapping("DKRetail__CRM_WorkStartDate__c", "crm_work_start_date"),
            FieldMapping("DKRetail__CostCenterCode__c", "dk_cost_center_code"),
            FieldMapping("DKRetail__LocationCode__c", "location_code"),
            FieldMapping("DKRetail__TotalAnnualLeave__c", "total_annual_leave"),
            FieldMapping("DKRetail__UsedAnnualLeave__c", "used_annual_leave"),
            FieldMapping("DKRetail__ManagerId__c", "manager_sfid"),
            FieldMapping("PostponedAppointment__c", "postponed_appointment_sfid"),
            FieldMapping("LockingFlag__c", "locking_flag"),
            FieldMapping("OfficePhone__c", "office_phone"),
            FieldMapping("DKRetail__CRM_WorkType__c", "crm_work_type"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val USER = EntityMetadata(
        targetName = "User",
        sObjectName = "User",
        tableName = "user",
        csvFileName = "users.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Username", "username", nullable = false),
            FieldMapping("Email", "email"),
            FieldMapping("IsActive", "is_active"),
            FieldMapping("DKRetail__EmployeeNumber__c", "employee_code"),
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
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
        extraStaticColumns = mapOf(
            // user.password NOT NULL — Stage 1 placeholder, backend admin API stage2/password 가 BCrypt 로 덮어씀.
            "password" to "",
        ),
    )

    private val ACCOUNT = EntityMetadata(
        targetName = "Account",
        sObjectName = "Account",
        tableName = "account",
        csvFileName = "accounts.csv",
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
            FieldMapping("IsDeleted", "is_deleted"),
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
            FieldMapping("FreezerInstalled__c", "freezer_installed"),
            FieldMapping("FreezerType__c", "freezer_type"),
            FieldMapping("Field1__c", "remaining_credit"),
            FieldMapping("TotalCredit__c", "total_credit"),
            FieldMapping("MapCoordinate__c", "map_coordinate"),
            FieldMapping("OrderEndTime__c", "order_end_time"),
            FieldMapping("FirstInstalled__c", "first_installed"),
            FieldMapping("Description", "description"),
            FieldMapping("Website", "website"),
            FieldMapping("Fax", "fax"),
            FieldMapping("AnnualRevenue", "annual_revenue"),
            FieldMapping("NumberOfEmployees", "number_of_employees"),
            FieldMapping("ParentId", "parent_sfid"),
            FieldMapping("Rating", "rating"),
            FieldMapping("Ownership", "ownership"),
            FieldMapping("IsPriorityRecord", "is_priority_record"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val PRODUCT = EntityMetadata(
        targetName = "Product",
        sObjectName = "DKRetail__Product__c",
        tableName = "product",
        csvFileName = "products.csv",
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
            FieldMapping("DKRetail__BoxReceivingQuantity__c", "box_receiving_quantity"),
            FieldMapping("DKRetail__StandardUnitPrice__c", "standard_unit_price"),
            FieldMapping("SuperTax__c", "super_tax"),
            FieldMapping("DKRetail__LaunchDate__c", "launch_date"),
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
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("Pallet__c", "pallet"),
            FieldMapping("DKRetail__Barcode__c", "barcode"),
            FieldMapping("manufacture__c", "manufacture"),
            FieldMapping("manufacture_detail__c", "manufacture_detail"),
            FieldMapping("Claim_Management__c", "claim_management"),
            FieldMapping("New_Product__c", "new_product_sfid"),
            FieldMapping("StoreCondition__c", "store_condition_text"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val PROMOTION = EntityMetadata(
        targetName = "Promotion",
        sObjectName = "DKRetail__Promotion__c",
        tableName = "promotion",
        csvFileName = "promotions.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "promotion_number", nullable = false),
            FieldMapping("DKRetail__PromotionType__c", "promotion_type"),
            FieldMapping("AccId__c", "account_sfid"),
            FieldMapping("DKRetail__StartDate__c", "start_date", nullable = false),
            FieldMapping("DKRetail__EndDate__c", "end_date", nullable = false),
            FieldMapping("DKRetail__PrimaryProductId__c", "primary_product_sfid"),
            FieldMapping("DKRetail__OtherProduct__c", "other_product"),
            FieldMapping("DKRetail__Message__c", "message"),
            FieldMapping("DKRetail__StandLocation__c", "stand_location"),
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("DKRetail__Remark__c", "remark"),
            FieldMapping("DKRetail__ProductType__c", "product_type"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("DKRetail__ActualAmount__c", "dk_actual_amount"),
            FieldMapping("DKRetail__TargetAmount__c", "dk_target_amount"),
        ),
        extraStaticColumns = mapOf(
            // promotion.is_closed NOT NULL — SF 매핑 필드 없어 placeholder.
            "is_closed" to "false",
        ),
    )

    private val NOTICE = EntityMetadata(
        targetName = "Notice",
        sObjectName = "DKRetail__Notice__c",
        tableName = "notice",
        csvFileName = "notices.csv",
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
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val GROUP = EntityMetadata(
        targetName = "Group",
        sObjectName = "Group",
        tableName = "group",
        csvFileName = "groups.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("DeveloperName", "developer_name"),
            FieldMapping("Type", "type", nullable = false),
            FieldMapping("RelatedId", "related_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("Email", "email"),
            FieldMapping("DoesSendEmailToMembers", "does_send_email_to_members"),
            FieldMapping("DoesIncludeBosses", "does_include_bosses"),
            FieldMapping("Description", "description"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val ACCOUNT_CATEGORY_MASTER = EntityMetadata(
        targetName = "AccountCategoryMaster",
        sObjectName = "AccountCategoryMaster__c",
        tableName = "account_category_master",
        csvFileName = "account_category_masters.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("AccountCode__c", "account_code", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("useSearch__c", "use_search", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val AGREEMENT_HISTORY = EntityMetadata(
        targetName = "AgreementHistory",
        sObjectName = "AgreementHistory__c",
        tableName = "agreement_history",
        csvFileName = "agreement_historys.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("EmployeeId__c", "employee_sfid"),
            FieldMapping("AgreementFlag__c", "agreement_flag", nullable = false),
            FieldMapping("AgreementDate__c", "agreement_date", nullable = false),
            FieldMapping("AgreementWordId__c", "agreement_word_sfid"),
            FieldMapping("IsDeleted", "is_deleted", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val AGREEMENT_WORD = EntityMetadata(
        targetName = "AgreementWord",
        sObjectName = "AgreementWord__c",
        tableName = "agreement_word",
        csvFileName = "agreement_words.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("Contents__c", "contents"),
            FieldMapping("Active__c", "active", nullable = false),
            FieldMapping("ActiveDate__c", "active_date"),
            FieldMapping("AfterActiveDate__c", "after_active_date"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val ALTERNATIVE_HOLIDAY = EntityMetadata(
        targetName = "AlternativeHoliday",
        sObjectName = "DKRetail__AlternativeHoliday__c",
        tableName = "alternative_holiday",
        csvFileName = "alternative_holidays.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__ActualWorkDate__c", "actual_work_date", nullable = false),
            FieldMapping("DKRetail__TargetAltHolidayDate__c", "target_alt_holiday_date", nullable = false),
            FieldMapping("DKRetail__ConfirmAltHolidayDate__c", "confirm_alt_holiday_date"),
            FieldMapping("DKRetail__Status__c", "status", nullable = false),
            FieldMapping("DKRetail__ChangeReason__c", "change_reason"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val APPOINTMENT = EntityMetadata(
        targetName = "Appointment",
        sObjectName = "Appointment__c",
        tableName = "appointment",
        csvFileName = "appointments.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("EmployeeCode__c", "employee_code", nullable = false),
            FieldMapping("isEmpCodeExist__c", "emp_code_exist", nullable = false),
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
            FieldMapping("AppointmentDate__c", "appoint_date", nullable = false),
            FieldMapping("JobName__c", "job_name"),
            FieldMapping("OrdDetailCode__c", "ord_detail_code"),
            FieldMapping("OrdDetailNode__c", "ord_detail_node"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
        ),
    )

    private val ATTENDANCE_LOG = EntityMetadata(
        targetName = "AttendanceLog",
        sObjectName = "DKRetail__CommuteLog__c",
        tableName = "attendance_log",
        csvFileName = "attendance_logs.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__CommuteDate__c", "attendance_date"),
            FieldMapping("DKRetail__AccId__c", "account_sfid"),
            FieldMapping("DKRetail__SecondWorkType__c", "second_work_type"),
            FieldMapping("DKRetail__Reason__c", "reason"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val ATTEND_INFO = EntityMetadata(
        targetName = "AttendInfo",
        sObjectName = "AttendInfo__c",
        tableName = "attend_info",
        csvFileName = "attend_infos.csv",
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
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val CLAIM = EntityMetadata(
        targetName = "Claim",
        sObjectName = "DKRetail__Claim__c",
        tableName = "claim",
        csvFileName = "claims.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__AccountId__c", "account_sfid"),
            FieldMapping("DKRetail__ClaimDate__c", "date", nullable = false),
            FieldMapping("DKRetail__ClaimType1__c", "claim_type1", nullable = false),
            FieldMapping("DKRetail__ClaimType2__c", "claim_type2", nullable = false),
            FieldMapping("DKRetail__Description__c", "defect_description", nullable = false),
            FieldMapping("DKRetail__Quantity__c", "defect_quantity", nullable = false),
            FieldMapping("DKRetail__Amount__c", "purchase_amount"),
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
            FieldMapping("ReturnOrderNumber__c", "customer_delivery_date"),
            FieldMapping("DKRetail__ReturnOrderNumber__c", "return_order_number"),
            FieldMapping("DKRetail__ExpirationDate__c", "expiration_date"),
            FieldMapping("DKRetail__InterfaceDate__c", "interface_date"),
            FieldMapping("DKRetail__ManufacturingDate__c", "manufacturing_date"),
            FieldMapping("DKRetail__InitialClaim__c", "initial_claim"),
            FieldMapping("DKRetail__LogisticsCenter__c", "logistics_center"),
            FieldMapping("ClaimSequence__c", "claim_sequence"),
            FieldMapping("DKRetail__DetailSNSName__c", "detail_sns_name"),
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("division__c", "division"),
            FieldMapping("DKRetail__Channel__c", "channel"),
            FieldMapping("DKRetail__SampleCollectionFlag__c", "sample_collection_flag"),
            FieldMapping("ImageCount__c", "image_count"),
            FieldMapping("DKRetail__ActionDate__c", "action_date"),
            FieldMapping("IsDeleted", "is_deleted", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val DISPLAY_WORK_SCHEDULE = EntityMetadata(
        targetName = "DisplayWorkSchedule",
        sObjectName = "DisplayWorkScheduleMaster__c",
        tableName = "display_work_schedule",
        csvFileName = "display_work_schedules.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("Account__c", "account_sfid"),
            FieldMapping("FullName__c", "employee_sfid"),
            FieldMapping("StartDate__c", "start_date"),
            FieldMapping("EndDate__c", "end_date"),
            FieldMapping("Confirmed__c", "confirmed"),
            FieldMapping("TypeOfWork1__c", "type_of_work1"),
            FieldMapping("TypeOfWork3__c", "type_of_work3"),
            FieldMapping("TypeOfWork4__c", "type_of_work4"),
            FieldMapping("TypeOfWork5__c", "type_of_work5"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("LastMonthRevenue__c", "last_month_revenue"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val EMPLOYEE_INPUT_CRITERIA_MASTER = EntityMetadata(
        targetName = "EmployeeInputCriteriaMaster",
        sObjectName = "EmployeeInputCriteriaMaster__c",
        tableName = "employee_input_criteria_master",
        csvFileName = "employee_input_criteria_masters.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("BifurcationHalfPersonStandard__c", "bifurcation_half_person_standard"),
            FieldMapping("Boundary__c", "boundary"),
            FieldMapping("Category__c", "category_sfid"),
            FieldMapping("Confirmed__c", "confirmed", nullable = false),
            FieldMapping("StartDate__c", "start_date"),
            FieldMapping("EndDate__c", "end_date"),
            FieldMapping("Fixed1PersonStandardAmount__c", "fixed_1_person_standard_amount"),
            FieldMapping("TypeOfWork1__c", "type_of_work_1"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val ERP_ORDER = EntityMetadata(
        targetName = "ErpOrder",
        sObjectName = "ERP_Order__c",
        tableName = "erp_order",
        csvFileName = "erp_orders.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "sap_order_number", nullable = false),
            FieldMapping("SAPAccountCode__c", "sap_account_code"),
            FieldMapping("SAPAccountName__c", "sap_account_name"),
            FieldMapping("DeliveryRequestDate__c", "delivery_request_date"),
            FieldMapping("OrderDate__c", "order_date"),
            FieldMapping("EmployeeCode__c", "employee_code"),
            FieldMapping("EmployeeName__c", "employee_name"),
            FieldMapping("TotalOrderAmount__c", "order_sales_amount"),
            FieldMapping("OrderChannel__c", "order_channel"),
            FieldMapping("OrderChannel_NM__c", "order_channel_nm"),
            FieldMapping("OrderType__c", "order_type"),
            FieldMapping("OrderType_NM__c", "order_type_nm"),
            FieldMapping("AccountId__c", "account_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val ERP_ORDER_PRODUCT = EntityMetadata(
        targetName = "ErpOrderProduct",
        sObjectName = "ERP_OrderProduct__c",
        tableName = "erp_order_product",
        csvFileName = "erp_order_products.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("ERPOrderId__c", "erp_order_sfid"),
            FieldMapping("SAPOrderNumber__c", "sap_order_number", nullable = false),
            FieldMapping("LineNumber__c", "line_number", nullable = false),
            FieldMapping("ExternalKey__c", "external_key", nullable = false),
            FieldMapping("ProductCode__c", "product_code"),
            FieldMapping("ProductName__c", "product_name"),
            FieldMapping("OrderQuantity__c", "order_quantity"),
            FieldMapping("Unit__c", "unit"),
            FieldMapping("ConfirmQuantity_Box__c", "confirm_quantity_box"),
            FieldMapping("ConfirmQuantity__c", "confirm_quantity"),
            FieldMapping("Confirm_Unit__c", "confirm_unit"),
            FieldMapping("DefaultReason__c", "default_reason"),
            FieldMapping("LineItemStatus__c", "line_item_status"),
            FieldMapping("OrderStatus__c", "delivery_status"),
            FieldMapping("ShippingDriverName__c", "shipping_driver_name"),
            FieldMapping("ShippingVehicle__c", "shipping_vehicle"),
            FieldMapping("ShippingDriverPhone__c", "shipping_driver_phone"),
            FieldMapping("ShippingScheduleTime__c", "shipping_schedule_time"),
            FieldMapping("ShippingCompleteTime__c", "shipping_complete_time"),
            FieldMapping("ShippingQuantity_Box__c", "shipping_quantity_box"),
            FieldMapping("ShippingQuantity__c", "shipping_quantity"),
            FieldMapping("OrderSalesLineAmount__c", "order_sales_line_amount"),
            FieldMapping("ShippingAmount__c", "shipping_amount"),
            FieldMapping("Plant__c", "plant"),
            FieldMapping("Plant_NM__c", "plant_nm"),
            FieldMapping("ReleaseQuantity__c", "release_quantity"),
            FieldMapping("ReleaseAmount__c", "release_amount"),
            FieldMapping("BoxQuantity__c", "box_quantity"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val HOLIDAY_MASTER = EntityMetadata(
        targetName = "HolidayMaster",
        sObjectName = "HolidayMaster__c",
        tableName = "holiday_master",
        csvFileName = "holiday_masters.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("HolidayDate__c", "holiday_date", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("Type__c", "type", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val INSPECTION_THEME = EntityMetadata(
        targetName = "InspectionTheme",
        sObjectName = "Theme__c",
        tableName = "inspection_theme",
        csvFileName = "inspection_themes.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("Title__c", "title"),
            FieldMapping("StartDate__c", "start_date"),
            FieldMapping("EndDate__c", "end_date"),
            FieldMapping("Department__c", "department"),
            FieldMapping("BranchCode__c", "branch_code"),
            FieldMapping("PublicFlag__c", "public_flag"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val SITE_ACTIVITY = EntityMetadata(
        targetName = "SiteActivity",
        sObjectName = "DKRetail__SiteAcitivity__c",
        tableName = "site_activity",
        csvFileName = "site_activities.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__ActivityDate__c", "activity_date"),
            FieldMapping("DKRetail__Category__c", "category"),
            FieldMapping("DKRetail__ProductType__c", "product_type"),
            FieldMapping("DKRetail__Description__c", "description"),
            FieldMapping("DKRetail__Title__c", "title"),
            FieldMapping("DKRetail__SAPAccountCode__c", "sap_account_code"),
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("DKRetail__CompetitorName__c", "competitor_name"),
            FieldMapping("DKRetail__CompetitorProductName__c", "competitor_product_name"),
            FieldMapping("DKRetail__CompetitorActivityDescription__c", "competitor_activity_description"),
            FieldMapping("DKRetail__CompetitorProudctPrice__c", "competitor_proudct_price"),
            FieldMapping("DKRetail__SampleTastFlag__c", "sample_tast_flag"),
            FieldMapping("DKRetail__SalesQuantity__c", "sales_quantity"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("DKRetail__AccountId__c", "account_sfid"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__ProductId__c", "product_sfid"),
            FieldMapping("ThemeId__c", "theme_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE = EntityMetadata(
        targetName = "MonthlyFemaleEmployeeIntegrationSchedule",
        sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c",
        tableName = "monthly_female_employee_integration_schedule",
        csvFileName = "monthly_female_employee_integration_schedules.csv",
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
            FieldMapping("WorkingDaysMonth__c", "working_days_month"),
            FieldMapping("NumberOfInputs__c", "number_of_inputs"),
            FieldMapping("EquivalentNumberOfWorkingDays__c", "equivalent_number_of_working_days"),
            FieldMapping("ConvertedHeadcount__c", "converted_headcount"),
            FieldMapping("EDI_POS__c", "edi_pos"),
            FieldMapping("ThisMonthAmount__c", "this_month_amount"),
            FieldMapping("AccountConvertedHeadcount__c", "account_converted_headcount"),
            FieldMapping("EmployeeInputCriteriaMaster__c", "employee_input_criteria_master_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val MONTHLY_SALES_HISTORY = EntityMetadata(
        targetName = "MonthlySalesHistory",
        sObjectName = "MonthlySalesHistory__c",
        tableName = "monthly_sales_history",
        csvFileName = "monthly_sales_historys.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("SalesYear__c", "sales_year"),
            FieldMapping("SalesMonth__c", "sales_month"),
            FieldMapping("LastMonthResults__c", "last_month_results"),
            FieldMapping("ShipClosingAmount__c", "ship_closing_amount"),
            FieldMapping("ABCClosingAmount1__c", "abc_closing_amount1"),
            FieldMapping("ABCClosingAmount2__c", "abc_closing_amount2"),
            FieldMapping("ABCClosingAmount3__c", "abc_closing_amount3"),
            FieldMapping("AmbientPurpose__c", "ambient_purpose"),
            FieldMapping("FridgePurpose__c", "fridge_purpose"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("Externalkey__c", "external_key"),
            FieldMapping("TotalLedgerAmount__c", "total_ledger_amount"),
            FieldMapping("AccountId__c", "account_sfid"),
            FieldMapping("SAPAccountCode__c", "sap_account_code"),
            FieldMapping("SalesDate__c", "sales_date"),
            FieldMapping("LastMonthlySalesHistory__c", "last_monthly_sales_history_sfid"),
            FieldMapping("Confirm__c", "is_confirmed"),
            FieldMapping("Remark__c", "remark"),
            FieldMapping("ShipClosingAmountNH__c", "ship_closing_amount_nh"),
            FieldMapping("ShipClosingAmount1__c", "ship_closing_amount1"),
            FieldMapping("ShipClosingAmount2__c", "ship_closing_amount2"),
            FieldMapping("ShipClosingAmount3__c", "ship_closing_amount3"),
            FieldMapping("ShipClosingAmount4__c", "ship_closing_amount4"),
            FieldMapping("ShipClosingSumAmount__c", "ship_closing_sum_amount"),
            FieldMapping("ABCClosingAmount4__c", "abc_closing_amount4"),
            FieldMapping("ABCClosingSumAmount__c", "abc_closing_sum_amount"),
            FieldMapping("LastMonthTargetByHand__c", "last_month_target_by_hand"),
            FieldMapping("ThisMonthTarget__c", "this_month_target"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val DAILY_SALES_HISTORY = EntityMetadata(
        targetName = "DailySalesHistory",
        sObjectName = "DailySalesHistory__c",
        tableName = "daily_sales_history",
        csvFileName = "daily_sales_historys.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("SAPAccountCode__c", "sap_account_code"),
            // SF Date (yyyy-MM-dd) → DB varchar(8) yyyyMMdd. external_key 는 이미 yyyyMMdd 로 export 되어 변환 불요.
            FieldMapping("SalesDate__c", "sales_date", dateToYyyymmdd = true),
            FieldMapping("Externalkey__c", "external_key"),
            FieldMapping("AccountId__c", "account_sfid"),
            FieldMapping("ERPSalesAmount1__c", "erp_sales_amount1"),
            FieldMapping("ERPSalesAmount2__c", "erp_sales_amount2"),
            FieldMapping("ERPSalesAmount3__c", "erp_sales_amount3"),
            FieldMapping("ERPDistributionAmount1__c", "erp_distribution_amount1"),
            FieldMapping("ERPDistributionAmount2__c", "erp_distribution_amount2"),
            FieldMapping("ERPDistributionAmount3__c", "erp_distribution_amount3"),
            FieldMapping("ERPSalesAmount__c", "erp_sales_amount"),
            FieldMapping("ERPDistributionAmount__c", "erp_distribution_amount"),
            FieldMapping("LedgerAmount__c", "ledger_amount"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
        ),
    )

    private val SALES_PROGRESS_RATE_MASTER = EntityMetadata(
        targetName = "SalesProgressRateMaster",
        sObjectName = "SalesProgressRateMaster__c",
        tableName = "sales_progress_rate_master",
        csvFileName = "sales_progress_rate_masters.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("AccountCDUpl__c", "account_cd_upl"),
            FieldMapping("BusinessRate__c", "business_rate"),
            FieldMapping("CurrentMonthSalesAmount__c", "current_month_sales_amount"),
            FieldMapping("ExternalKey__c", "external_key"),
            FieldMapping("FOTartgetAmount__c", "fo_target_amount"),
            FieldMapping("FRTargetAmount__c", "fr_target_amount"),
            FieldMapping("PreviousMonthSalesAmount__c", "previous_month_sales_amount"),
            FieldMapping("RMTartgetAmount__c", "rm_target_amount"),
            FieldMapping("RTTargetAmount__c", "rt_target_amount"),
            FieldMapping("TargetMonth__c", "target_month"),
            FieldMapping("TargetSumAmount__c", "target_sum_amount"),
            FieldMapping("TargetYear__c", "target_year"),
            FieldMapping("accountbranchView__c", "account_branch_view"),
            FieldMapping("AccountBranchCode__c", "account_branch_code"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("Account__c", "account_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val NEW_PRODUCT = EntityMetadata(
        targetName = "NewProduct",
        sObjectName = "NewProduct__c",
        tableName = "new_product",
        csvFileName = "new_products.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("Customer_Survey__c", "customer_survey"),
            FieldMapping("Initiator__c", "initiator"),
            FieldMapping("ManagementType__c", "management_type"),
            FieldMapping("Marketability_Review_Report__c", "marketability_review_report"),
            FieldMapping("Product_Code__c", "product_code_sfid"),
            FieldMapping("Product_Name__c", "product_name", nullable = false),
            FieldMapping("Product_code1__c", "product_code1"),
            FieldMapping("Purpose__c", "purpose", nullable = false),
            FieldMapping("Release_Review_Report__c", "release_review_report", nullable = false),
            FieldMapping("Release__c", "release", nullable = false),
            FieldMapping("Status__c", "status", nullable = false),
            FieldMapping("firstpropose__c", "firstpropose", nullable = false),
            FieldMapping("friday_taste__c", "friday_taste", nullable = false),
            FieldMapping("Upload_Description__c", "upload_description"),
            FieldMapping("MarketingTeam__c", "marketing_team"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("RecordTypeId", "record_type_sfid"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val ORDER_REQUEST = EntityMetadata(
        targetName = "OrderRequest",
        sObjectName = "DKRetail__OrderRequest__c",
        tableName = "order_request",
        csvFileName = "order_requests.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "order_request_number", nullable = false),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__AccountId__c", "account_sfid"),
            FieldMapping("OrderDate__c", "order_date", nullable = false),
            FieldMapping("DKRetail__OrderDate__c", "dk_order_date"),
            FieldMapping("DKRetail__RequestDate__c", "delivery_date", nullable = false),
            FieldMapping("TotalOrderAmount__c", "total_amount", nullable = false),
            FieldMapping("DKRetail__RequestStatus__c", "order_request_status", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val ORDER_REQUEST_PRODUCT = EntityMetadata(
        targetName = "OrderRequestProduct",
        sObjectName = "DKRetail__OrderRequestProduct__c",
        tableName = "order_request_product",
        csvFileName = "order_request_products.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("DKRetail__RequestNumber__c", "order_request_sfid"),
            FieldMapping("Name", "name"),
            FieldMapping("LineNumber__c", "line_number", nullable = false),
            FieldMapping("DKRetail__LineNumber__c", "dk_line_number"),
            FieldMapping("ProductCode__c", "product_code", nullable = false),
            FieldMapping("TotalQuantity_Box__c", "quantity_boxes", nullable = false),
            FieldMapping("TotalQuantity_Each__c", "quantity_pieces", nullable = false),
            FieldMapping("DKRetail__OrderingUnit__c", "unit", nullable = false),
            FieldMapping("DKRetail__TotalAmount__c", "amount", nullable = false),
            FieldMapping("DKRetail__LineChangeType__c", "line_change_type"),
            FieldMapping("Status__c", "status"),
            FieldMapping("DKRetail__ProductId__c", "product_sfid"),
            FieldMapping("DKRetail__Box__c", "box"),
            FieldMapping("DKRetail__Piece__c", "piece"),
            FieldMapping("DKRetail__BoxQuantity__c", "box_quantity"),
            FieldMapping("DKRetail__TotalCount__c", "dk_total_count"),
            FieldMapping("TotalCount__c", "total_count"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val PRODUCT_BARCODE = EntityMetadata(
        targetName = "ProductBarcode",
        sObjectName = "ProductBarcode__c",
        tableName = "product_barcode",
        csvFileName = "product_barcodes.csv",
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
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val PROFESSIONAL_PROMOTION_TEAM_HISTORY = EntityMetadata(
        targetName = "ProfessionalPromotionTeamHistory",
        sObjectName = "ProfessionalPromotionTeamHistory__c",
        tableName = "professional_promotion_team_history",
        csvFileName = "professional_promotion_team_historys.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("EmployeeId__c", "employee_sfid"),
            FieldMapping("oldValue__c", "old_value"),
            FieldMapping("newValue__c", "new_value", nullable = false),
            FieldMapping("updateTime__c", "changed_at", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val PROFESSIONAL_PROMOTION_TEAM_MASTER = EntityMetadata(
        targetName = "ProfessionalPromotionTeamMaster",
        sObjectName = "ProfessionalPromotionTeamMaster__c",
        tableName = "professional_promotion_team_master",
        csvFileName = "professional_promotion_team_masters.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("Account__c", "account_sfid"),
            FieldMapping("FullName__c", "full_name_sfid"),
            FieldMapping("ProfessionalPromotionTeam__c", "team_type", nullable = false),
            FieldMapping("StartDate__c", "start_date", nullable = false),
            FieldMapping("EndDate__c", "end_date"),
            FieldMapping("Confirmed__c", "is_confirmed", nullable = false),
            FieldMapping("CostCenterCode__c", "branch_code"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    // PromotionProduct 는 master-detail child 라 SF 에 OwnerId 컬럼이 없음 (V193, V154 PromotionEmployee 동일 패턴) → owner_sfid 제거.
    private val PROMOTION_PRODUCT = EntityMetadata(
        targetName = "PromotionProduct",
        sObjectName = "DKRetail__PromotionProduct__c",
        tableName = "promotion_product",
        csvFileName = "promotion_products.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__PromotionId__c", "promotion_sfid"),
            FieldMapping("DKRetail__ProductId__c", "product_sfid"),
            FieldMapping("DKRetail__Price__c", "price"),
            FieldMapping("PromotionIdExt__c", "promotion_id_ext"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted", nullable = false),
        ),
    )

    // PromotionEmployee 는 master-detail child 라 SF 에 OwnerId 컬럼이 없음 → owner_sfid NULL 적재.
    private val PROMOTION_EMPLOYEE = EntityMetadata(
        targetName = "PromotionEmployee",
        sObjectName = "DKRetail__PromotionEmployee__c",
        tableName = "promotion_employee",
        csvFileName = "promotion_employees.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__PromotionId__c", "promotion_sfid"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__ScheduleDate__c", "schedule_date"),
            FieldMapping("DKRetail__WorkStatus__c", "work_status"),
            FieldMapping("DKRetail__WorkType1__c", "work_type1"),
            FieldMapping("DKRetail__WorkType3__c", "work_type3"),
            FieldMapping("DKRetail__ScheduleId__c", "team_member_schedule_sfid"),
            FieldMapping("PromoCloseByTm__c", "promo_close_by_tm", nullable = false),
            FieldMapping("DKRetail__BasePrice__c", "base_price"),
            FieldMapping("DKRetail__DailyTargetCount__c", "daily_target_count"),
            FieldMapping("PrimaryProductAmount__c", "primary_product_amount"),
            FieldMapping("DKRetail__PrimarySalesQuantity__c", "primary_sales_quantity"),
            FieldMapping("DKRetail__PrimarySalesPrice__c", "primary_sales_price"),
            FieldMapping("DKRetail__OtherSalesAmount__c", "other_sales_amount"),
            FieldMapping("DKRetail__OtherSalesQuantity__c", "other_sales_quantity"),
            FieldMapping("S3ImageUniqueKey__c", "s3_image_unique_key"),
            FieldMapping("Description__c", "description"),
            FieldMapping("DKRetail__WorkType2__c", "dk_work_type2"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val PUSH_MESSAGE = EntityMetadata(
        targetName = "PushMessage",
        sObjectName = "PushMessage__c",
        tableName = "push_message",
        csvFileName = "push_messages.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("Message__c", "message"),
            FieldMapping("ScheduleDate__c", "schedule_date"),
            FieldMapping("EmployeeId__c", "employee_sfid"),
            FieldMapping("Branch__c", "branch"),
            FieldMapping("BranchCode__c", "branch_code"),
            FieldMapping("SObjectRecordId__c", "s_object_record_id"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val PUSH_MESSAGE_RECEIVER = EntityMetadata(
        targetName = "PushMessageReceiver",
        sObjectName = "PushMessageReceiver__c",
        tableName = "push_message_receiver",
        csvFileName = "push_message_receivers.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("EmployeeId__c", "employee_sfid"),
            FieldMapping("MessageId__c", "push_message_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val TEAM_MEMBER_SCHEDULE = EntityMetadata(
        targetName = "TeamMemberSchedule",
        sObjectName = "DKRetail__TeamMemberSchedule__c",
        tableName = "team_member_schedule",
        csvFileName = "team_member_schedules.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__WorkingDate__c", "working_date"),
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
            FieldMapping("CommuteReportDateTime__c", "commute_report_datetime"),
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
            FieldMapping("Yes_ChkCnt__c", "yes_chk_cnt"),
            FieldMapping("No_ChkCnt__c", "no_chk_cnt"),
            FieldMapping("precaution_chk__c", "precaution_chk"),
            FieldMapping("precaution__c", "precaution"),
            FieldMapping("StartTime__c", "start_time"),
            FieldMapping("CompleteTime__c", "complete_time"),
            FieldMapping("IsDeleted", "is_deleted"),
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
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val UPLOAD_FILE = EntityMetadata(
        targetName = "UploadFile",
        sObjectName = "UploadFile__c",
        tableName = "upload_file",
        csvFileName = "upload_files.csv",
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
            FieldMapping("Date__c", "file_date"),
            FieldMapping("IsDeleted", "is_deleted"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val SUGGESTION = EntityMetadata(
        targetName = "Suggestion",
        sObjectName = "DKRetail__Proposal__c",
        tableName = "suggestion",
        csvFileName = "suggestions.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "proposal_number", nullable = false),
            FieldMapping("DKRetail__Title__c", "title", nullable = false),
            FieldMapping("DKRetail__Description__c", "content", nullable = false),
            FieldMapping("Category__c", "category", nullable = false),
            FieldMapping("Category1__c", "category1"),
            FieldMapping("Category2__c", "category2"),
            FieldMapping("Category3__c", "category3"),
            FieldMapping("AccountId__c", "account_sfid"),
            FieldMapping("DKRetail__SAPAccountCode__c", "sap_account_code"),
            FieldMapping("DKRetail__EmployeeId__c", "employee_sfid"),
            FieldMapping("DKRetail__ProductId__c", "product_sfid"),
            FieldMapping("ProductCode__c", "product_code"),
            FieldMapping("OrgCostCenterCode__c", "org_cost_center_code"),
            // SF CostCenterCode__c (라벨 "조직유형") — 사원 소속 코스트센터코드 원본. 데이터 보존용.
            FieldMapping("CostCenterCode__c", "cost_center_code"),
            FieldMapping("CarNumber__c", "car_number"),
            FieldMapping("ClaimDate__c", "claim_date"),
            FieldMapping("ClaimType__c", "claim_type"),
            FieldMapping("ClaimTypeMeasures__c", "claim_type_measures"),
            FieldMapping("LogisticsResponsibility__c", "logistics_responsibility"),
            FieldMapping("WERK1_TEXT2__c", "reception_logistics_center"),
            FieldMapping("WERK3_TEXT2__c", "responsible_logistics_center"),
            FieldMapping("ActionStatus__c", "action_status"),
            FieldMapping("DuplicateProposalNum__c", "duplicate_proposal_num"),
            // Spec #833 sf-align-suggestion-action-fields — SF describe 정합 도입. common.kts 정합.
            FieldMapping("ActionContent__c", "action_content"),
            FieldMapping("ActionManager__c", "action_manager"),
            FieldMapping("ActionNum__c", "action_num", nullable = false),
            FieldMapping("IsDeleted", "is_deleted", nullable = false),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
        ),
    )

    // Spec #780 — SF UserRole / Profile entity 신규 시스템 편입.
    // SF describe 실측 결과 (UserRole 16→7 필드, Profile 573→8 필드). read-only audit lookup.

    private val USER_ROLE = EntityMetadata(
        targetName = "UserRole",
        sObjectName = "UserRole",
        tableName = "user_role",
        csvFileName = "user_roles.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("DeveloperName", "developer_name"),
            FieldMapping("RollupDescription", "rollup_description"),
            FieldMapping("ParentRoleId", "parent_user_role_sfid"),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
    )

    private val PROFILE = EntityMetadata(
        targetName = "Profile",
        sObjectName = "Profile",
        tableName = "profile",
        csvFileName = "profiles.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("UserType", "user_type"),
            FieldMapping("Description", "description"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
        ),
        // LocalDataInitializer.seedProfiles() 가 먼저 만든 name-only (sfid=NULL) row 와
        // name UNIQUE (V185 profile_name_unique) 충돌 시, DO NOTHING 이면 SF 원본 sfid 가
        // 영영 묵살된다 (→ Stage2 FK Resolve 의 profile_sfid ↔ profile.sfid JOIN 전건 실패).
        // ON CONFLICT (name) DO UPDATE 로 seed row 의 sfid / 메타 컬럼을 SF 원본값으로 보강.
        conflictUpdate = ConflictUpdate(
            conflictColumn = "name",
            updateColumns = listOf(
                "sfid", "user_type", "description",
                "created_at", "updated_at", "created_by_sfid", "last_modified_by_sfid",
            ),
        ),
    )

    // ─────────────────────────────────────────────────────────
    // spec #790 — SF Sharing 메타 7 entity 적재
    // (XML 메타 출처 6 entity + SOQL 출처 GroupMember 1 entity)
    // ─────────────────────────────────────────────────────────

    private val SHARING_RULE = EntityMetadata(
        targetName = "SharingRule",
        sObjectName = null, // XML 메타 출처 — Q1 옵션 1 (nullable + 자동 skip)
        tableName = "sharing_rule",
        csvFileName = "sharing-rule.csv",
        fields = listOf(
            FieldMapping("developerName", "developer_name", nullable = false),
            FieldMapping("sObjectName", "s_object_name", nullable = false),
            FieldMapping("ruleType", "rule_type", nullable = false),
            FieldMapping("label", "label"),
            FieldMapping("accessLevel", "access_level", nullable = false, nullPlaceholder = "Read"),
            FieldMapping("includeOwnedByAll", "include_owned_by_all", nullable = false, nullPlaceholder = "false"),
        ),
        // V206 — 단일 unique (developer_name) 에서 복합 unique (s_object_name, developer_name) 전환.
        // 직전 적재본은 단일 unique 충돌로 같은 developer_name 의 2번째 이후 sObject row 가 drop 된
        // 상태이므로 preClear 로 전량 재적재해야 정합 회복 (운영 dev: CSV 335 → DB 293, 42 row 누락).
        preClear = true,
    )

    private val SHARING_RULE_CONDITION = EntityMetadata(
        targetName = "SharingRuleCondition",
        sObjectName = null,
        tableName = "sharing_rule_condition",
        csvFileName = "sharing-rule-condition.csv",
        fields = listOf(
            FieldMapping("sObjectName", "sharing_rule_s_object_name", nullable = false),
            FieldMapping("parentDeveloperName", "sharing_rule_developer_name", nullable = false),
            FieldMapping("field", "field", nullable = false),
            FieldMapping("operator", "operator", nullable = false),
            FieldMapping("value", "condition_value"),
            FieldMapping("conditionOrder", "condition_order", nullable = false),
            FieldMapping("logicConnector", "logic_connector"),
        ),
        // UNIQUE (sharing_rule_id, condition_order) — Stage1 시점 sharing_rule_id NULL → ON CONFLICT 미발동
        preClear = true,
    )

    private val SHARING_RULE_TARGET = EntityMetadata(
        targetName = "SharingRuleTarget",
        sObjectName = null,
        tableName = "sharing_rule_target",
        csvFileName = "sharing-rule-target.csv",
        fields = listOf(
            FieldMapping("sObjectName", "sharing_rule_s_object_name", nullable = false),
            FieldMapping("parentDeveloperName", "sharing_rule_developer_name", nullable = false),
            FieldMapping("targetType", "target_type", nullable = false),
            FieldMapping("targetDeveloperName", "target_developer_name"),
        ),
        // partial UNIQUE (sharing_rule_id, target_type, target_sfid) WHERE target_sfid IS NOT NULL —
        // Stage1 시점 sharing_rule_id NULL → 매칭 안 됨
        preClear = true,
    )

    private val USER_ROLE_HIERARCHY_SNAPSHOT = EntityMetadata(
        targetName = "UserRoleHierarchySnapshot",
        sObjectName = null,
        tableName = "user_role_hierarchy_snapshot",
        csvFileName = "user-role-hierarchy.csv",
        fields = listOf(
            FieldMapping("developerName", "developer_name", nullable = false),
            FieldMapping("name", "name", nullable = false),
            FieldMapping("parentDeveloperName", "parent_developer_name"),
            FieldMapping("description", "description"),
            FieldMapping("mayForecastManagerShare", "may_forecast_manager_share"),
        ),
    )

    private val PROFILE_FLAGS = EntityMetadata(
        targetName = "ProfileFlags",
        sObjectName = null,
        tableName = "profile_flags",
        csvFileName = "profile-flags.csv",
        fields = listOf(
            FieldMapping("profileName", "profile_name", nullable = false),
            FieldMapping("permissionsViewAllData", "permissions_view_all_data", nullable = false, nullPlaceholder = "false"),
            FieldMapping("permissionsModifyAllData", "permissions_modify_all_data", nullable = false, nullPlaceholder = "false"),
            FieldMapping("permissionsViewAllUsers", "permissions_view_all_users", nullable = false, nullPlaceholder = "false"),
            FieldMapping("permissionsManageUsers", "permissions_manage_users", nullable = false, nullPlaceholder = "false"),
            FieldMapping("permissionsApiEnabled", "permissions_api_enabled", nullable = false, nullPlaceholder = "false"),
            // Profile 객체권한 JSON — ObjectPermissions SOQL (Parent.IsOwnedByProfile=TRUE) 출처.
            // extract-sharing-meta.main.kts 가 Profile 별로 묶어 채움. PermissionSetFlags 와 동일 구조.
            FieldMapping("objectPermissionsJson", "object_permissions"),
        ),
    )

    private val PERMISSION_SET_FLAGS = EntityMetadata(
        targetName = "PermissionSetFlags",
        sObjectName = null,
        tableName = "permission_set_flags",
        csvFileName = "permission-set-flags.csv",
        fields = listOf(
            FieldMapping("permissionSetName", "permission_set_name", nullable = false),
            FieldMapping("permissionsViewAllData", "permissions_view_all_data", nullable = false, nullPlaceholder = "false"),
            FieldMapping("permissionsModifyAllData", "permissions_modify_all_data", nullable = false, nullPlaceholder = "false"),
            FieldMapping("objectPermissionsJson", "object_permissions"),
        ),
        // partial UNIQUE on sfid WHERE sfid IS NOT NULL — Stage1 시점 sfid NULL (Stage2 fk substep 후 채움).
        // permission_set_name 자체에 UNIQUE 없음 → preClear 필요.
        preClear = true,
    )

    // ─────────────────────────────────────────────────────────
    // spec #796 — PermissionSet 정규 entity (SOQL 출처)
    // SOQL: SELECT Id, Name, Label FROM PermissionSet WHERE IsCustom = TRUE
    // permission_set_flags / permission_set_record_type / permission_set_field_permission 의 자연 키 lookup ref.
    // ─────────────────────────────────────────────────────────

    private val PERMISSION_SET = EntityMetadata(
        targetName = "PermissionSet",
        sObjectName = "PermissionSet", // SOQL 출처 — extract-csv.sh PS_SOQL
        tableName = "permission_set",
        csvFileName = "permission_sets.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name", nullable = false),
            FieldMapping("Label", "label"),
        ),
    )

    private val GROUP_MEMBER = EntityMetadata(
        targetName = "GroupMember",
        sObjectName = "GroupMember", // SOQL 출처 — extract-csv.sh
        tableName = "group_member",
        csvFileName = "group_members.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("GroupId", "group_sfid", nullable = false),
            FieldMapping("UserOrGroupId", "user_or_group_sfid", nullable = false),
        ),
    )

    // ─────────────────────────────────────────────────────────
    // spec #798 — PermissionSetAssignment 정규 적재 (SOQL 출처)
    // SOQL: SELECT Id, AssigneeId, PermissionSetId, IsActive, CreatedDate FROM PermissionSetAssignment WHERE IsActive = TRUE
    // ─────────────────────────────────────────────────────────

    // PSA 는 SF 표준 필드만 8개 — CreatedDate / LastModifiedDate / *ById 모두 미보유 (prod describe
    // 2026-05-24 확인). assigned_at 은 SystemModstamp ("최종 시스템 modify" 시각) 로 근사 — PSA 는
    // 신규 row 가 일반적으로 update 거의 없어 사실상 created_at 과 동등.
    private val PERMISSION_SET_ASSIGNMENT = EntityMetadata(
        targetName = "PermissionSetAssignment",
        sObjectName = "PermissionSetAssignment", // SOQL 출처 — extract-csv.sh
        tableName = "permission_set_assignment",
        csvFileName = "permission_set_assignments.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("AssigneeId", "assignee_user_sfid", nullable = false),
            FieldMapping("PermissionSetId", "permission_set_sfid", nullable = false),
            FieldMapping("IsActive", "is_active", nullable = false, nullPlaceholder = "true"),
            FieldMapping("SystemModstamp", "assigned_at"),
        ),
    )

    // ─────────────────────────────────────────────────────────
    // spec #791 — SF OWD + master-detail relationship 메타
    // (XML 메타 출처 — Custom SObject <sharingModel> / Standard Sharing.settings <sharingSettings>+<sharingHierarchy>)
    // ─────────────────────────────────────────────────────────

    private val SOBJECT_SETTING = EntityMetadata(
        targetName = "SObjectSetting",
        sObjectName = null, // XML 메타 출처 — #790 Q1 옵션 1 정합
        tableName = "sobject_setting",
        csvFileName = "sobject-setting.csv",
        fields = listOf(
            FieldMapping("sObjectName", "sobject_name", nullable = false),
            FieldMapping("orgWideDefault", "org_wide_default", nullable = false, nullPlaceholder = "Private"),
            FieldMapping("allowHierarchyGrant", "allow_hierarchy_grant", nullable = false, nullPlaceholder = "true"),
            FieldMapping("parentSObjectName", "parent_sobject_name"),
        ),
    )

    private val SOBJECT_RELATION = EntityMetadata(
        targetName = "SObjectRelation",
        sObjectName = null, // XML 메타 출처 — master-detail relationship 정규화 (#791 Q2 옵션 1)
        tableName = "sobject_relation",
        csvFileName = "sobject-relation.csv",
        fields = listOf(
            FieldMapping("childSObjectName", "child_sobject_name", nullable = false),
            FieldMapping("parentSObjectName", "parent_sobject_name", nullable = false),
            FieldMapping("relationFieldName", "relation_field_name", nullable = false),
            FieldMapping("isMasterDetail", "is_master_detail", nullable = false, nullPlaceholder = "true"),
        ),
    )

    // ─────────────────────────────────────────────────────────
    // spec #794 — SF Record Type 별 분기 권한 (XML 메타 3 출처 정규화)
    // ─────────────────────────────────────────────────────────

    private val RECORD_TYPE = EntityMetadata(
        // SOQL 출처 (extract-csv.sh RECORD_TYPE_SOQL) — `SELECT Id, SobjectType, DeveloperName,
        // Name, Description, IsActive FROM RecordType`. 기존 XML 출처는 18자리 RecordTypeId 가
        // 없어 record_type.sfid 가 NULL → SObject row 의 record_type_sfid → record_type_id FK
        // resolve(sfid 매칭)가 전량 미해소였다. Id 를 sfid 로 적재해 해소. CSV 헤더(SOQL 필드명)에
        // FieldMapping.sfFieldName 을 정합시킨다. Master RT 도 포함되나 record_type_id 매칭 전용이라 무해.
        targetName = "RecordType",
        sObjectName = "RecordType",
        tableName = "record_type",
        csvFileName = "record-type.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("SobjectType", "sobject_name", nullable = false),
            FieldMapping("DeveloperName", "developer_name", nullable = false),
            FieldMapping("Name", "label", nullable = false),
            FieldMapping("Description", "description"),
            FieldMapping("IsActive", "is_active", nullable = false, nullPlaceholder = "true"),
        ),
    )

    private val PROFILE_RECORD_TYPE = EntityMetadata(
        targetName = "ProfileRecordType",
        sObjectName = null, // XML 메타 출처 (Profile 운영 0건 — PermissionSet 위임)
        tableName = "profile_record_type",
        csvFileName = "profile-record-type.csv",
        fields = listOf(
            FieldMapping("profileName", "profile_name", nullable = false),
            FieldMapping("sObjectName", "sobject_name", nullable = false),
            FieldMapping("recordTypeDeveloperName", "record_type_developer_name", nullable = false),
            FieldMapping("visible", "visible", nullable = false, nullPlaceholder = "false"),
            FieldMapping("isDefault", "is_default", nullable = false, nullPlaceholder = "false"),
        ),
        // partial UNIQUE (profile_id, record_type_id) WHERE 둘 다 NOT NULL — Stage1 시점 둘 다 NULL
        preClear = true,
    )

    private val PERMISSION_SET_RECORD_TYPE = EntityMetadata(
        targetName = "PermissionSetRecordType",
        sObjectName = null, // XML 메타 출처 (운영 10건)
        tableName = "permission_set_record_type",
        csvFileName = "permission-set-record-type.csv",
        fields = listOf(
            FieldMapping("permissionSetName", "permission_set_name", nullable = false),
            FieldMapping("sObjectName", "sobject_name", nullable = false),
            FieldMapping("recordTypeDeveloperName", "record_type_developer_name", nullable = false),
            FieldMapping("visible", "visible", nullable = false, nullPlaceholder = "false"),
            FieldMapping("isDefault", "is_default", nullable = false, nullPlaceholder = "false"),
        ),
        // partial UNIQUE (permission_set_id, record_type_id) WHERE 둘 다 NOT NULL — Stage1 시점 둘 다 NULL
        preClear = true,
    )

    // ─────────────────────────────────────────────────────────
    // spec #795 — SF Field-Level Security (FLS, XML 메타 2 출처)
    // ─────────────────────────────────────────────────────────

    private val PROFILE_FIELD_PERMISSION = EntityMetadata(
        targetName = "ProfileFieldPermission",
        sObjectName = null, // XML 메타 출처 (Profile 운영 0건 — PermissionSet 위임)
        tableName = "profile_field_permission",
        csvFileName = "profile-field-permission.csv",
        fields = listOf(
            FieldMapping("profileName", "profile_name", nullable = false),
            FieldMapping("sObjectName", "sobject_name", nullable = false),
            FieldMapping("fieldName", "field_name", nullable = false),
            FieldMapping("readable", "readable", nullable = false, nullPlaceholder = "false"),
            FieldMapping("editable", "editable", nullable = false, nullPlaceholder = "false"),
        ),
        // partial UNIQUE (profile_id, sobject_name, field_name) WHERE profile_id IS NOT NULL — Stage1 시점 profile_id NULL
        preClear = true,
    )

    private val PERMISSION_SET_FIELD_PERMISSION = EntityMetadata(
        targetName = "PermissionSetFieldPermission",
        sObjectName = null, // XML 메타 출처 (운영 26 PermissionSet)
        tableName = "permission_set_field_permission",
        csvFileName = "permission-set-field-permission.csv",
        fields = listOf(
            FieldMapping("permissionSetName", "permission_set_name", nullable = false),
            FieldMapping("sObjectName", "sobject_name", nullable = false),
            FieldMapping("fieldName", "field_name", nullable = false),
            FieldMapping("readable", "readable", nullable = false, nullPlaceholder = "false"),
            FieldMapping("editable", "editable", nullable = false, nullPlaceholder = "false"),
        ),
        // partial UNIQUE (permission_set_id, sobject_name, field_name) WHERE permission_set_id IS NOT NULL — Stage1 시점 permission_set_id NULL
        preClear = true,
    )

    private val ALL: Map<String, EntityMetadata> = listOf(
        ORGANIZATION,
        EMPLOYEE,
        USER,
        ACCOUNT,
        PRODUCT,
        PROMOTION,
        NOTICE,
        GROUP,
        ACCOUNT_CATEGORY_MASTER,
        AGREEMENT_HISTORY,
        AGREEMENT_WORD,
        ALTERNATIVE_HOLIDAY,
        APPOINTMENT,
        ATTENDANCE_LOG,
        ATTEND_INFO,
        CLAIM,
        DISPLAY_WORK_SCHEDULE,
        EMPLOYEE_INPUT_CRITERIA_MASTER,
        ERP_ORDER,
        ERP_ORDER_PRODUCT,
        HOLIDAY_MASTER,
        INSPECTION_THEME,
        SITE_ACTIVITY,
        MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE,
        MONTHLY_SALES_HISTORY,
        DAILY_SALES_HISTORY,
        SALES_PROGRESS_RATE_MASTER,
        NEW_PRODUCT,
        ORDER_REQUEST,
        ORDER_REQUEST_PRODUCT,
        PRODUCT_BARCODE,
        PROFESSIONAL_PROMOTION_TEAM_HISTORY,
        PROFESSIONAL_PROMOTION_TEAM_MASTER,
        PROMOTION_EMPLOYEE,
        PROMOTION_PRODUCT,
        PUSH_MESSAGE,
        PUSH_MESSAGE_RECEIVER,
        SUGGESTION,
        TEAM_MEMBER_SCHEDULE,
        UPLOAD_FILE,
        USER_ROLE,
        PROFILE,
        // spec #790 — SF Sharing 메타
        SHARING_RULE,
        SHARING_RULE_CONDITION,
        SHARING_RULE_TARGET,
        USER_ROLE_HIERARCHY_SNAPSHOT,
        PROFILE_FLAGS,
        // spec #796 — PermissionSet 정규 entity (PermissionSetFlags / Record Type / FLS 자연 키 ref)
        PERMISSION_SET,
        PERMISSION_SET_FLAGS,
        GROUP_MEMBER,
        // spec #798 — PermissionSetAssignment SOQL 적재
        PERMISSION_SET_ASSIGNMENT,
        // spec #791 — SF OWD + master-detail relationship
        SOBJECT_SETTING,
        SOBJECT_RELATION,
        // spec #794 — SF Record Type 별 분기 권한
        RECORD_TYPE,
        PROFILE_RECORD_TYPE,
        PERMISSION_SET_RECORD_TYPE,
        // spec #795 — SF FLS (Field-Level Security)
        PROFILE_FIELD_PERMISSION,
        PERMISSION_SET_FIELD_PERMISSION,
    ).associateBy { it.targetName }

    /**
     * 일괄 실행 시 처리 순서 — scripts/common.kts 의 TARGET_DEPENDENCY_ORDER 1:1 이관.
     *
     * sfid 보존 적재라 실제 FK 의존성은 거의 없으나 운영상 사람-편의 순서.
     */
    val DEPENDENCY_ORDER: List<String> = listOf(
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
        "Claim",
        "DisplayWorkSchedule",
        "EmployeeInputCriteriaMaster",
        "ErpOrder",
        "ErpOrderProduct",
        "HolidayMaster",
        "InspectionTheme",
        "SiteActivity",
        "MonthlyFemaleEmployeeIntegrationSchedule",
        "MonthlySalesHistory",
        "DailySalesHistory",
        "SalesProgressRateMaster",
        "NewProduct",
        "OrderRequest",
        "OrderRequestProduct",
        "ProductBarcode",
        "ProfessionalPromotionTeamHistory",
        "ProfessionalPromotionTeamMaster",
        "PromotionEmployee",
        "PromotionProduct",
        "PushMessage",
        "PushMessageReceiver",
        "Suggestion",
        "TeamMemberSchedule",
        "UploadFile",
        "UserRole",
        "Profile",
        // spec #790 — SF Sharing 메타 (UserRole / Profile / Group 적재 후)
        "UserRoleHierarchySnapshot",
        "ProfileFlags",
        // spec #796 — PermissionSet 정규 entity (PermissionSetFlags / Record Type / FLS 자연 키 ref)
        "PermissionSet",
        "PermissionSetFlags",
        "SharingRule",
        "SharingRuleCondition",
        "SharingRuleTarget",
        "GroupMember",
        // spec #798 — PermissionSetAssignment (User + PermissionSetFlags 적재 후 sfid lookup)
        "PermissionSetAssignment",
        // spec #791 — SF OWD + master-detail relationship (의존 없음 — 독립 메타)
        "SObjectSetting",
        "SObjectRelation",
        // spec #794 — Record Type 권한 (RecordType 먼저, Profile/PermissionSet 의 RT visibility 는 자연 키로 join)
        "RecordType",
        "ProfileRecordType",
        "PermissionSetRecordType",
        // spec #795 — FLS (Field-Level Security)
        "ProfileFieldPermission",
        "PermissionSetFieldPermission",
    )

    fun get(targetName: String): EntityMetadata? = ALL[targetName]

    /**
     * 등록된 target 일람 (등록 순서). UI 의 dropdown 표시에 사용.
     */
    fun list(): List<String> = DEPENDENCY_ORDER

    /**
     * 등록된 target 의 (이름, csvFileName) 일람 (등록 순서). UI 가 prefix + csvFileName 으로
     * 최종 S3 key 를 미리보기 표시하기 위해 사용 (SINGLE 모드 파일명 자동조립).
     */
    fun listWithCsv(): List<TargetCsv> =
        DEPENDENCY_ORDER.mapNotNull { name -> ALL[name]?.let { TargetCsv(name, it.csvFileName) } }

    data class TargetCsv(val targetName: String, val csvFileName: String)

    /**
     * in-memory 권한 캐시 (AdminPermissionCache / AdminDataScopeCache) 의 입력 source 가 되는 target 집합.
     *
     * 이 중 하나라도 적재되면 캐시된 권한 set / DataScope 가 stale 이 되므로, 적재 완료 후
     * 캐시 무효화가 필요하다. SfPermissionResolver / AdminDataScopeService 가 조회하는 테이블 기준.
     */
    val PERMISSION_RELATED_TARGETS: Set<String> = setOf(
        "User",
        "UserRole",
        "UserRoleHierarchySnapshot",
        "Profile",
        "ProfileFlags",
        "PermissionSet",
        "PermissionSetFlags",
        "PermissionSetAssignment",
        "Group",
        "GroupMember",
        "SharingRule",
        "SharingRuleCondition",
        "SharingRuleTarget",
        "SObjectSetting",
        "SObjectRelation",
        "RecordType",
        "ProfileRecordType",
        "PermissionSetRecordType",
        "ProfileFieldPermission",
        "PermissionSetFieldPermission",
    )

    /** target 이 in-memory 권한 캐시에 영향을 주는지. */
    fun affectsPermissionCache(targetName: String): Boolean = targetName in PERMISSION_RELATED_TARGETS
}
