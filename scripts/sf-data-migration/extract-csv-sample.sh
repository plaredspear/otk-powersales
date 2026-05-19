#!/usr/bin/env bash
#
# 샘플 추출 — 각 entity 의 최근 N row 만 CSV 로 export (REST API only).
#
# 사용:
#   ./extract-csv-sample.sh [--limit N] [--target=A,B,C] [--org <alias>] [--out-dir <dir>] [--api-version <ver>]
#
# 옵션:
#   --limit N        각 entity 추출 최대 row 수 (기본 100)
#   --target=...     특정 entity 만 추출 (기본 전체 36 + GroupMember + Permission)
#   --org <alias>    SF org alias
#   --out-dir <dir>  출력 디렉토리 (기본 ./input-sample)
#   --api-version    SF API 버전 (기본 60.0)
#
# 동작:
#   - SF REST query 만 사용 (Bulk API 미사용)
#   - 각 SOQL 에 `ORDER BY CreatedDate DESC LIMIT N` (GroupMember 는 Id DESC) 자동 append
#   - 실제 input/ 과 분리된 input-sample/ 에 출력 (사고 방지)
#
# 예시:
#   ./extract-csv-sample.sh                          # 전체 entity 최근 100 row
#   ./extract-csv-sample.sh --limit 1000             # 전체 entity 최근 1000 row
#   ./extract-csv-sample.sh --target=Account --limit 500
#
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# -----------------------------------------------------------------------------
# 옵션 파싱
# -----------------------------------------------------------------------------
LIMIT=100
SF_ORG=""
SF_API_VERSION="60.0"
OUT_DIR=""
TARGETS="Organization,Account,Product,Promotion,Group,Employee,User,Notice,AccountCategoryMaster,AgreementHistory,AgreementWord,AlternativeHoliday,Appointment,AttendanceLog,AttendInfo,Claim,DisplayWorkSchedule,EmployeeInputCriteriaMaster,ErpOrder,ErpOrderProduct,HolidayMaster,InspectionTheme,MonthlyFemaleEmployeeIntegrationSchedule,MonthlySalesHistory,NewProduct,OrderRequest,OrderRequestProduct,ProductBarcode,ProfessionalPromotionTeamHistory,ProfessionalPromotionTeamMaster,PromotionEmployee,PushMessage,PushMessageReceiver,TeamMemberSchedule,UploadFile,Permission,GroupMember"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --limit) LIMIT="$2"; shift 2 ;;
        --limit=*) LIMIT="${1#--limit=}"; shift ;;
        --org) SF_ORG="$2"; shift 2 ;;
        --target=*) TARGETS="${1#--target=}"; shift ;;
        --api-version) SF_API_VERSION="$2"; shift 2 ;;
        --out-dir) OUT_DIR="$2"; shift 2 ;;
        --help|-h) sed -n '2,24p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) echo "[error] 알 수 없는 옵션: $1" >&2; exit 1 ;;
    esac
done

if ! [[ "$LIMIT" =~ ^[0-9]+$ ]] || [[ "$LIMIT" -lt 1 ]]; then
    echo "[error] --limit 은 양의 정수여야 합니다: $LIMIT" >&2
    exit 1
fi

if [[ -z "$OUT_DIR" ]]; then
    OUT_DIR="$SCRIPT_DIR/input-sample"
fi
mkdir -p "$OUT_DIR"

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

echo "============================================================"
echo "SF 샘플 추출 — 각 entity 최근 $LIMIT row (REST API)"
echo "============================================================"
echo "정렬: CreatedDate DESC (GroupMember 는 Id DESC)"
echo "출력: $OUT_DIR"
echo "API : v$SF_API_VERSION"
echo

# -----------------------------------------------------------------------------
# SOQL 정의 (extract-csv.sh 와 동기 유지 — Spec #764)
# -----------------------------------------------------------------------------
ORGANIZATION_SOQL=$(cat <<'EOF'
SELECT
    Id, Name,
    CostCenterLevel2__c, OrgCodeLevel2__c, OrgNameLevel2__c,
    CostCenterLevel3__c, OrgCodeLevel3__c, OrgNameLevel3__c,
    CostCenterLevel4__c, OrgCodeLevel4__c, OrgNameLevel4__c,
    CostCenterLevel5__c, OrgCodeLevel5__c, OrgNameLevel5__c,
    ExternalKey__c, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM Org__c
WHERE IsDeleted = FALSE
EOF
)

EMPLOYEE_SOQL=$(cat <<'EOF'
SELECT
    Id,
    DKRetail__EmpCode__c, Name,
    DKRetail__Birthdate__c, DKRetail__Status__c, DKRetail__APPLoginActive__c,
    DKRetail__AppAuthority__c, DKRetail__OrgName__c, CostCenterCode__c,
    DKRetail__WorkPhone__c, Phone__c, DKRetail__HomePhone__c,
    DKRetail__WorkEmail__c, DKRetail__Email__c, DKRetail__Sex__c,
    DKRetail__StartDate__c, DKRetail__EndDate__c, AgreementFlag__c,
    IsDeleted,
    ProfessionalPromotionTeam__c,
    DKRetail__Jikchak__c, DKRetail__Jikwee__c, DKRetail__Jikgub__c,
    DKRetail__WorkType__c, DKRetail__JobCode__c, DKRetail__WorkArea__c,
    DKRetail__Jikjong__c, DKRetail__AppointmentDate__c, OrdDetailNode__c,
    DKRetail__CRM_WorkStartDate__c, DKRetail__CostCenterCode__c,
    DKRetail__LocationCode__c, DKRetail__TotalAnnualLeave__c,
    DKRetail__UsedAnnualLeave__c, DKRetail__ManagerId__c,
    PostponedAppointment__c, LockingFlag__c, OfficePhone__c,
    DKRetail__CRM_WorkType__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM DKRetail__Employee__c
WHERE IsDeleted = FALSE
  AND DKRetail__Status__c = '재직'
EOF
)

USER_SOQL=$(cat <<'EOF'
SELECT
    Id, Username, Email, IsActive,
    DKRetail__EmployeeNumber__c,
    Name, LastName, FirstName, Alias,
    Title, Department, Division,
    MobilePhone, Phone, HR_Code__c, Branch__c,
    LastLoginDate, ManagerId,
    ProfileId, UserRoleId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    Profile.Name
FROM User
WHERE IsActive = TRUE
  AND Profile.Name NOT IN (
    'Admin', 'Standard', 'Read Only',
    'Analytics Cloud Integration User', 'Analytics Cloud Security User',
    'Anypoint Integration', 'Chatter External User', 'Chatter Free User',
    'Chatter Moderator User', 'ContractManager',
    'CPQ Integration User', 'Einstein Agent User',
    'External Apps Login User', 'External Einstein Agent User',
    'Guest License User', 'Identity User',
    'Minimum Access - API Only Integrations', 'Minimum Access - Salesforce',
    'Sales Insights Integration User',
    'Salesforce API Only System Integrations', 'SalesforceIQ Integration User',
    'SolutionManager', 'StandardAul', 'Test Standard Platform User'
  )
EOF
)

PSA_SOQL=$(cat <<'EOF'
SELECT
    AssigneeId,
    Assignee.DKRetail__EmployeeNumber__c,
    PermissionSetId,
    PermissionSet.Name,
    PermissionSet.Label
FROM PermissionSetAssignment
WHERE Assignee.IsActive = TRUE
  AND PermissionSet.IsCustom = TRUE
EOF
)

GM_SOQL=$(cat <<'EOF'
SELECT
    GroupId, Group.Name, UserOrGroupId
FROM GroupMember
WHERE Group.Type = 'Regular'
EOF
)

ACCOUNT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Phone, MobilePhone__c,
    Address1__c, Address2__c, Representative__c,
    ABCType__c, ABCTypeCode__c, ExternalKey__c,
    AccountGroup__c, BranchCode__c, BranchName__c,
    Zipcode__c, Latitude__c, Longitude__c,
    ClosingTime1__c, ClosingTime2__c, ClosingTime3__c,
    Industry, WERK1_TX__c, WERK2_TX__c, WERK3_TX__c,
    IsDeleted, Type, AccountStatusName__c,
    EmployeeCode__c, Distribution__c, AccountStatusCode__c,
    BusinessType__c, BusinessCategory__c, Sic, Email__c,
    DivisionName__c, SalesDeptName__c, ConsignmentAcc__c,
    WERK1__c, WERK2__c, WERK3__c,
    SalesDeptCostCenter__c, DivisionCostCenter__c,
    AccountNumber, Site, AccountSource,
    BranchCostCenter__c, DivisionCode__c, SalesDeptCode__c,
    LogisticsName__c, LogisticsCode__c,
    FreezerInstalled__c, FreezerType__c,
    Field1__c, TotalCredit__c, MapCoordinate__c,
    OrderEndTime__c, FirstInstalled__c,
    Description, Website, Fax,
    AnnualRevenue, NumberOfEmployees,
    ParentId, Rating, Ownership,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Account
WHERE IsDeleted = FALSE
EOF
)

PRODUCT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__ProductCode__c,
    DKRetail__ProductType__c, DKRetail__ProductStatus__c,
    DKRetail__StoreCondition__c, DKRetail__ShelfLife__c, DKRetail__ShelfLifeUnit__c,
    DKRetail__Category1__c, DKRetail__Category2__c, DKRetail__Category3__c,
    DKRetail__CategoryCode1__c, DKRetail__CategoryCode2__c, DKRetail__CategoryCode3__c,
    DKRetail__Unit__c, DKRetail__OrderingUnit__c, DKRetail__ConversionQuantity__c,
    DKRetail__BoxReceivingQuantity__c, DKRetail__StandardUnitPrice__c, SuperTax__c,
    DKRetail__LaunchDate__c, DKRetail__LogisticsBarCode__c,
    TasteGift__c, ProductFeatures__c, SellingPoint__c, Purpose__c,
    TargetAccountType__c, Allergen__c, CrossContamination__c,
    ImgRefPath__c, ImgRefPath_front__c, ImgRefPath_back__c, ImgRefPathTXT__c,
    IsDeleted, Pallet__c, DKRetail__Barcode__c,
    manufacture__c, manufacture_detail__c,
    Claim_Management__c, New_Product__c, StoreCondition__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM DKRetail__Product__c
WHERE IsDeleted = FALSE
EOF
)

PROMOTION_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__PromotionType__c, AccId__c,
    DKRetail__StartDate__c, DKRetail__EndDate__c,
    DKRetail__PrimaryProductId__c, DKRetail__OtherProduct__c,
    DKRetail__Message__c, DKRetail__StandLocation__c,
    CostCenterCode__c, DKRetail__Remark__c, DKRetail__ProductType__c,
    Category1__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    IsDeleted, DKRetail__ActualAmount__c, DKRetail__TargetAmount__c
FROM DKRetail__Promotion__c
WHERE IsDeleted = FALSE
EOF
)

NOTICE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Title__c, EmployeeId__c,
    DKRetail__Scope__c, DKRetail__Category__c, DKRetail__Contents__c,
    DKRetail__Jeejum__c, DKRetail__JeejumCode__c,
    IsDeleted, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById
FROM DKRetail__Notice__c
WHERE IsDeleted = FALSE
EOF
)
ACCOUNT_CATEGORY_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, AccountCode__c, Name, useSearch__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM AccountCategoryMaster__c
WHERE IsDeleted = FALSE
EOF
)

AGREEMENT_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, EmployeeId__c, AgreementFlag__c, AgreementDate__c, AgreementWordId__c,
    IsDeleted, Name, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById
FROM AgreementHistory__c
WHERE IsDeleted = FALSE
EOF
)

AGREEMENT_WORD_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Contents__c, Active__c, ActiveDate__c, AfterActiveDate__c,
    IsDeleted, CreatedDate, LastModifiedDate, OwnerId, CreatedById,
    LastModifiedById
FROM AgreementWord__c
WHERE IsDeleted = FALSE
EOF
)

ALTERNATIVE_HOLIDAY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__ActualWorkDate__c,
    DKRetail__TargetAltHolidayDate__c, DKRetail__ConfirmAltHolidayDate__c,
    DKRetail__Status__c, DKRetail__ChangeReason__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__AlternativeHoliday__c
WHERE IsDeleted = FALSE
EOF
)

APPOINTMENT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeCode__c, isEmpCodeExist__c, OrgCode__c, OrgName__c,
    Jikchak__c, Jikwee__c, Jikgub__c, WorkType__c, ManageType__c, JobCode__c,
    WorkArea__c, Jikjong__c, AppointmentDate__c, JobName__c, OrdDetailCode__c,
    OrdDetailNode__c, IsDeleted, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, OwnerId
FROM Appointment__c
WHERE IsDeleted = FALSE
EOF
)

ATTENDANCE_LOG_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__CommuteDate__c,
    DKRetail__AccId__c, DKRetail__SecondWorkType__c, DKRetail__Reason__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    IsDeleted
FROM DKRetail__CommuteLog__c
WHERE IsDeleted = FALSE
EOF
)

ATTEND_INFO_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeCode__c, StartDate__c, EndDate__c, AttendType__c,
    Status__c, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM AttendInfo__c
WHERE IsDeleted = FALSE
EOF
)

CLAIM_SOQL=$(cat <<'EOF'
SELECT
    Id, DKRetail__EmployeeId__c, DKRetail__AccountId__c,
    DKRetail__ClaimDate__c, DKRetail__ClaimType1__c, DKRetail__ClaimType2__c,
    DKRetail__Description__c, DKRetail__Quantity__c, DKRetail__Amount__c,
    DKRetail__PurchaseMethod__c, DKRetail__RequestType__c, DKRetail__Status__c,
    Name, DKRetail__counselNumber__c, DKRetail__ActionCode__c,
    DKRetail__ActionStatus__c, ActContent__c, DKRetail__ReasonType__c,
    DKRetail__CosmosKey__c, DKRetail__ProductId__c, ReturnOrderNumber__c,
    DKRetail__ReturnOrderNumber__c, DKRetail__ExpirationDate__c,
    DKRetail__InterfaceDate__c, DKRetail__ManufacturingDate__c,
    DKRetail__InitialClaim__c, DKRetail__LogisticsCenter__c, ClaimSequence__c,
    DKRetail__DetailSNSName__c, CostCenterCode__c, division__c,
    DKRetail__Channel__c, DKRetail__SampleCollectionFlag__c, ImageCount__c,
    DKRetail__ActionDate__c, IsDeleted, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM DKRetail__Claim__c
WHERE IsDeleted = FALSE
EOF
)

DISPLAY_WORK_SCHEDULE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Account__c, FullName__c, StartDate__c, EndDate__c, Confirmed__c,
    TypeOfWork1__c, TypeOfWork3__c, TypeOfWork4__c, TypeOfWork5__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    CostCenterCode__c, LastMonthRevenue__c, IsDeleted
FROM DisplayWorkScheduleMaster__c
WHERE IsDeleted = FALSE
EOF
)

EMPLOYEE_INPUT_CRITERIA_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, BifurcationHalfPersonStandard__c, Boundary__c, Category__c,
    Confirmed__c, StartDate__c, EndDate__c, Fixed1PersonStandardAmount__c,
    TypeOfWork1__c, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM EmployeeInputCriteriaMaster__c
WHERE IsDeleted = FALSE
EOF
)

ERP_ORDER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, SAPAccountCode__c, SAPAccountName__c, DeliveryRequestDate__c,
    OrderDate__c, EmployeeCode__c, EmployeeName__c, TotalOrderAmount__c,
    OrderChannel__c, OrderChannel_NM__c, OrderType__c, OrderType_NM__c,
    AccountId__c, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    IsDeleted
FROM ERP_Order__c
WHERE IsDeleted = FALSE
EOF
)

ERP_ORDER_PRODUCT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, ERPOrderId__c, SAPOrderNumber__c, LineNumber__c, ExternalKey__c,
    ProductCode__c, ProductName__c, OrderQuantity__c, Unit__c,
    ConfirmQuantity_Box__c, ConfirmQuantity__c, Confirm_Unit__c,
    DefaultReason__c, LineItemStatus__c, OrderStatus__c, ShippingDriverName__c,
    ShippingVehicle__c, ShippingDriverPhone__c, ShippingScheduleTime__c,
    ShippingCompleteTime__c, ShippingQuantity_Box__c, ShippingQuantity__c,
    OrderSalesLineAmount__c, ShippingAmount__c, Plant__c, Plant_NM__c,
    ReleaseQuantity__c, ReleaseAmount__c, BoxQuantity__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM ERP_OrderProduct__c
WHERE IsDeleted = FALSE
EOF
)

HOLIDAY_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, HolidayDate__c, Name, Type__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM HolidayMaster__c
WHERE IsDeleted = FALSE
EOF
)

INSPECTION_THEME_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Title__c, StartDate__c, EndDate__c, Department__c, BranchCode__c,
    PublicFlag__c, IsDeleted, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Theme__c
WHERE IsDeleted = FALSE
EOF
)

MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, ExternalKey__c, Year__c, Month__c, Account__c, FullName__c,
    CostCenterCode__c, WorkingCategory1__c, WorkingCategory3__c,
    WorkingCategory4__c, WorkingCategory5__c, EmpBranchName__c,
    ProfessionalPromotionTeam__c, WorkingDaysMonth__c, NumberOfInputs__c,
    EquivalentNumberOfWorkingDays__c, ConvertedHeadcount__c, EDI_POS__c,
    ThisMonthAmount__c, AccountConvertedHeadcount__c,
    EmployeeInputCriteriaMaster__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM MonthlyFemaleEmployeeIntegrationSchedule__c
WHERE IsDeleted = FALSE
EOF
)

MONTHLY_SALES_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, SalesYear__c, SalesMonth__c, LastMonthResults__c,
    ShipClosingAmount__c, ABCClosingAmount1__c, ABCClosingAmount2__c,
    ABCClosingAmount3__c, AmbientPurpose__c, FridgePurpose__c, IsDeleted,
    Externalkey__c, TotalLedgerAmount__c, AccountId__c, SAPAccountCode__c,
    SalesDate__c, LastMonthlySalesHistory__c, Confirm__c,
    Remark__c, ShipClosingAmountNH__c, ShipClosingAmount1__c,
    ShipClosingAmount2__c, ShipClosingAmount3__c, ShipClosingAmount4__c,
    ShipClosingSumAmount__c, ABCClosingAmount4__c, ABCClosingSumAmount__c,
    LastMonthTargetByHand__c, ThisMonthTarget__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM MonthlySalesHistory__c
WHERE IsDeleted = FALSE
EOF
)

NEW_PRODUCT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Customer_Survey__c, Initiator__c, ManagementType__c,
    Marketability_Review_Report__c, Product_Code__c, Product_Name__c,
    Product_code1__c, Purpose__c, Release_Review_Report__c, Release__c,
    Status__c, firstpropose__c, friday_taste__c, Upload_Description__c,
    MarketingTeam__c, IsDeleted, RecordTypeId, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM NewProduct__c
WHERE IsDeleted = FALSE
EOF
)

ORDER_REQUEST_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__AccountId__c, OrderDate__c,
    DKRetail__OrderDate__c, DKRetail__RequestDate__c, TotalOrderAmount__c,
    DKRetail__RequestStatus__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__OrderRequest__c
WHERE IsDeleted = FALSE
EOF
)

ORDER_REQUEST_PRODUCT_SOQL=$(cat <<'EOF'
SELECT
    Id, DKRetail__RequestNumber__c, Name, LineNumber__c,
    DKRetail__LineNumber__c, ProductCode__c, TotalQuantity_Box__c,
    TotalQuantity_Each__c, DKRetail__OrderingUnit__c, DKRetail__TotalAmount__c,
    DKRetail__LineChangeType__c, Status__c, DKRetail__ProductId__c,
    DKRetail__Box__c, DKRetail__Piece__c, DKRetail__BoxQuantity__c,
    DKRetail__TotalCount__c, TotalCount__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__OrderRequestProduct__c
WHERE IsDeleted = FALSE
EOF
)

PRODUCT_BARCODE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, ProductName__c, ProductBarcode__c, ProductUnit__c,
    ProductSequence__c, Product__c, ProductCode__c, CustomKey__c, IsDeleted,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM ProductBarcode__c
WHERE IsDeleted = FALSE
EOF
)

PROFESSIONAL_PROMOTION_TEAM_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeId__c, oldValue__c, newValue__c, updateTime__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM ProfessionalPromotionTeamHistory__c
WHERE IsDeleted = FALSE
EOF
)

PROFESSIONAL_PROMOTION_TEAM_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Account__c, FullName__c, ProfessionalPromotionTeam__c,
    StartDate__c, EndDate__c, Confirmed__c, CostCenterCode__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM ProfessionalPromotionTeamMaster__c
WHERE IsDeleted = FALSE
EOF
)

PROMOTION_EMPLOYEE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__PromotionId__c, DKRetail__EmployeeId__c,
    DKRetail__ScheduleDate__c, DKRetail__WorkStatus__c, DKRetail__WorkType1__c,
    DKRetail__WorkType3__c, DKRetail__ScheduleId__c, PromoCloseByTm__c,
    DKRetail__BasePrice__c, DKRetail__DailyTargetCount__c,
    PrimaryProductAmount__c, DKRetail__PrimarySalesQuantity__c,
    DKRetail__PrimarySalesPrice__c, DKRetail__OtherSalesAmount__c,
    DKRetail__OtherSalesQuantity__c, S3ImageUniqueKey__c, Description__c,
    DKRetail__WorkType2__c, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__PromotionEmployee__c
WHERE IsDeleted = FALSE
EOF
)

PUSH_MESSAGE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Message__c, ScheduleDate__c, EmployeeId__c, Branch__c,
    BranchCode__c, SObjectRecordId__c, IsDeleted, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM PushMessage__c
WHERE IsDeleted = FALSE
EOF
)

PUSH_MESSAGE_RECEIVER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeId__c, MessageId__c, IsDeleted,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM PushMessageReceiver__c
WHERE IsDeleted = FALSE
EOF
)

TEAM_MEMBER_SCHEDULE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__WorkingDate__c,
    DKRetail__WorkingType__c, DKRetail__WorkingCategory1__c,
    DKRetail__WorkingCategory2__c, DKRetail__WorkingCategory3__c,
    WorkingCategory4__c, AccountId__c, teamleadersfid__c,
    DKRetail__AltHolidayId__c, DKRetail__CommuteLogId__c,
    DKRetail__PromotionEmpId__c, DisplayWorkScheduleMaster__c,
    CommuteReportDateTime__c, ID__c, TraversalFlag__c, Equipment1__c,
    Equipment2__c, Equipment3__c, Equipment4__c, Equipment5__c, Equipment6__c,
    Equipment7__c, Equipment8__c, Equipment9__c, Equipment10__c, Yes_ChkCnt__c,
    No_ChkCnt__c, precaution_chk__c, precaution__c, StartTime__c,
    CompleteTime__c, IsDeleted, HRCode__c, DKRetail__PromotionEmpIdExt__c,
    SecondWorkType__c, WorkingCategory5__c, ref_accountName__c,
    MonthlyFemaleEmployeeIntegrationSchedule__c, ProfessionalPromotionTeam__c,
    CostCenterCode__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM DKRetail__TeamMemberSchedule__c
WHERE IsDeleted = FALSE
EOF
)

UPLOAD_FILE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, UniqueKey__c, RecordId__c, Size__c, Object__c, Url__c,
    UploadKbn__c, FileId__c, Date__c, IsDeleted, CreatedDate, OwnerId,
    CreatedById, LastModifiedById
FROM UploadFile__c
WHERE IsDeleted = FALSE
EOF
)



GROUP_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DeveloperName, Type, RelatedId, OwnerId,
    Email, DoesSendEmailToMembers, DoesIncludeBosses,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Group
WHERE Type IN ('Regular', 'Queue')
EOF
)

# -----------------------------------------------------------------------------
# 쿼리 실행 — SOQL 에 ORDER BY ... DESC LIMIT N append 후 sf data query
# -----------------------------------------------------------------------------
apply_sample_limit() {
    local soql="$1"
    local order_by="CreatedDate DESC"
    # CreatedDate 가 노출되지 않는 시스템 SObject 는 Id DESC fallback.
    if echo "$soql" | grep -qE "FROM\s+(GroupMember|PermissionSetAssignment)\b"; then
        order_by="Id DESC"
    fi
    echo "$soql ORDER BY $order_by LIMIT $LIMIT"
}

run_query() {
    local label="$1"
    local soql="$2"
    local out_file="$3"

    echo "[extract] $label → ${out_file##*/} ..."
    local final_soql
    final_soql="$(apply_sample_limit "$soql")"

    local start_epoch end_epoch elapsed
    start_epoch="$(date +%s)"
    if sf data query \
        --query "$final_soql" \
        --result-format csv \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}" \
        > "$out_file"; then
        end_epoch="$(date +%s)"
        elapsed=$((end_epoch - start_epoch))
        local row_count
        row_count=$(($(wc -l < "$out_file") - 1))
        echo "          rows: $row_count  elapsed: ${elapsed}s"
    else
        echo "          [ERROR] REST query 실패 — $label" >&2
        return 1
    fi
}

# -----------------------------------------------------------------------------
# Target dispatch
# -----------------------------------------------------------------------------
IFS=',' read -ra TARGET_ARR <<< "$TARGETS"
for target in "${TARGET_ARR[@]}"; do
    case "$target" in
        Organization) run_query "Organization (Org__c)" "$ORGANIZATION_SOQL" "$OUT_DIR/organizations.csv" ;;
        Account) run_query "Account" "$ACCOUNT_SOQL" "$OUT_DIR/accounts.csv" ;;
        Product) run_query "Product (DKRetail__Product__c)" "$PRODUCT_SOQL" "$OUT_DIR/products.csv" ;;
        Promotion) run_query "Promotion (DKRetail__Promotion__c)" "$PROMOTION_SOQL" "$OUT_DIR/promotions.csv" ;;
        Group) run_query "Group (Regular + Queue)" "$GROUP_SOQL" "$OUT_DIR/groups.csv" ;;
        Employee) run_query "Employee (DKRetail__Employee__c)" "$EMPLOYEE_SOQL" "$OUT_DIR/employees.csv" ;;
        User) run_query "User" "$USER_SOQL" "$OUT_DIR/users.csv" ;;
        Notice) run_query "Notice (DKRetail__Notice__c)" "$NOTICE_SOQL" "$OUT_DIR/notices.csv" ;;
        AccountCategoryMaster) run_query "AccountCategoryMaster" "$ACCOUNT_CATEGORY_MASTER_SOQL" "$OUT_DIR/account_category_masters.csv" ;;
        AgreementHistory) run_query "AgreementHistory" "$AGREEMENT_HISTORY_SOQL" "$OUT_DIR/agreement_historys.csv" ;;
        AgreementWord) run_query "AgreementWord" "$AGREEMENT_WORD_SOQL" "$OUT_DIR/agreement_words.csv" ;;
        AlternativeHoliday) run_query "AlternativeHoliday" "$ALTERNATIVE_HOLIDAY_SOQL" "$OUT_DIR/alternative_holidays.csv" ;;
        Appointment) run_query "Appointment" "$APPOINTMENT_SOQL" "$OUT_DIR/appointments.csv" ;;
        AttendanceLog) run_query "AttendanceLog" "$ATTENDANCE_LOG_SOQL" "$OUT_DIR/attendance_logs.csv" ;;
        AttendInfo) run_query "AttendInfo" "$ATTEND_INFO_SOQL" "$OUT_DIR/attend_infos.csv" ;;
        Claim) run_query "Claim" "$CLAIM_SOQL" "$OUT_DIR/claims.csv" ;;
        DisplayWorkSchedule) run_query "DisplayWorkSchedule" "$DISPLAY_WORK_SCHEDULE_SOQL" "$OUT_DIR/display_work_schedules.csv" ;;
        EmployeeInputCriteriaMaster) run_query "EmployeeInputCriteriaMaster" "$EMPLOYEE_INPUT_CRITERIA_MASTER_SOQL" "$OUT_DIR/employee_input_criteria_masters.csv" ;;
        ErpOrder) run_query "ErpOrder" "$ERP_ORDER_SOQL" "$OUT_DIR/erp_orders.csv" ;;
        ErpOrderProduct) run_query "ErpOrderProduct" "$ERP_ORDER_PRODUCT_SOQL" "$OUT_DIR/erp_order_products.csv" ;;
        HolidayMaster) run_query "HolidayMaster" "$HOLIDAY_MASTER_SOQL" "$OUT_DIR/holiday_masters.csv" ;;
        InspectionTheme) run_query "InspectionTheme" "$INSPECTION_THEME_SOQL" "$OUT_DIR/inspection_themes.csv" ;;
        MonthlyFemaleEmployeeIntegrationSchedule) run_query "MonthlyFemaleEmployeeIntegrationSchedule" "$MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE_SOQL" "$OUT_DIR/monthly_female_employee_integration_schedules.csv" ;;
        MonthlySalesHistory) run_query "MonthlySalesHistory" "$MONTHLY_SALES_HISTORY_SOQL" "$OUT_DIR/monthly_sales_historys.csv" ;;
        NewProduct) run_query "NewProduct" "$NEW_PRODUCT_SOQL" "$OUT_DIR/new_products.csv" ;;
        OrderRequest) run_query "OrderRequest" "$ORDER_REQUEST_SOQL" "$OUT_DIR/order_requests.csv" ;;
        OrderRequestProduct) run_query "OrderRequestProduct" "$ORDER_REQUEST_PRODUCT_SOQL" "$OUT_DIR/order_request_products.csv" ;;
        ProductBarcode) run_query "ProductBarcode" "$PRODUCT_BARCODE_SOQL" "$OUT_DIR/product_barcodes.csv" ;;
        ProfessionalPromotionTeamHistory) run_query "ProfessionalPromotionTeamHistory" "$PROFESSIONAL_PROMOTION_TEAM_HISTORY_SOQL" "$OUT_DIR/professional_promotion_team_historys.csv" ;;
        ProfessionalPromotionTeamMaster) run_query "ProfessionalPromotionTeamMaster" "$PROFESSIONAL_PROMOTION_TEAM_MASTER_SOQL" "$OUT_DIR/professional_promotion_team_masters.csv" ;;
        PromotionEmployee) run_query "PromotionEmployee" "$PROMOTION_EMPLOYEE_SOQL" "$OUT_DIR/promotion_employees.csv" ;;
        PushMessage) run_query "PushMessage" "$PUSH_MESSAGE_SOQL" "$OUT_DIR/push_messages.csv" ;;
        PushMessageReceiver) run_query "PushMessageReceiver" "$PUSH_MESSAGE_RECEIVER_SOQL" "$OUT_DIR/push_message_receivers.csv" ;;
        TeamMemberSchedule) run_query "TeamMemberSchedule" "$TEAM_MEMBER_SCHEDULE_SOQL" "$OUT_DIR/team_member_schedules.csv" ;;
        UploadFile) run_query "UploadFile" "$UPLOAD_FILE_SOQL" "$OUT_DIR/upload_files.csv" ;;
        Permission) run_query "PermissionSetAssignment" "$PSA_SOQL" "$OUT_DIR/permission_set_assignments.csv" ;;
        GroupMember) run_query "GroupMember" "$GM_SOQL" "$OUT_DIR/group_members.csv" ;;
        *) echo "[warn] 알 수 없는 target: $target — skip" ;;
    esac
done

echo
echo "============================================================"
echo "샘플 추출 완료 — $OUT_DIR"
echo "============================================================"
