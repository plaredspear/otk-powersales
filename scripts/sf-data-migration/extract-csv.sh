#!/usr/bin/env bash
#
# SF 데이터 CSV 추출 스크립트 (Spec #764, 사용자 수동 실행)
#
# 사용법:
#   ./extract-csv.sh [--org <alias>] [--target=<list>] [--api-version <ver>] [--out-dir <dir>] [--max-query-limit <N>] [--skip-verify]
#                    [--bulk-threshold <N>] [--bulk] [--no-bulk]
#
# Bulk API 자동 분기:
#   각 entity 마다 SELECT COUNT(Id) 로 row 수를 확인하여, 임계값 이상이면 Bulk API 2.0
#   (sf data export bulk) 사용. 미만이면 일반 REST query API (sf data query) 사용.
#     --bulk-threshold N   (기본 50000)  자동 분기 임계값
#     --bulk               자동 분기 무시하고 모든 entity 에 Bulk API 강제 적용
#     --no-bulk            자동 분기 무시하고 모든 entity 에 REST 강제 적용
#   COUNT 쿼리 실패 (예: subquery / WHERE 절 파싱 실패) → REST 자동 fallback.
#   Bulk job 실패 → 즉시 abort (REST 재시도 안 함). 사용자에게 보고 후 별도 대응.
#
# 사전 검증:
#   추출 전에 자동으로 verify-metadata.main.kts 호출 — EntityMetadata 와 backend Entity 의
#   @SFField 정합 확인. 불일치 시 추출 중단 (--skip-verify 로 우회).
#
# 사전 준비:
#   1) sf CLI 설치: npm i -g @salesforce/cli
#   2) SF org 인증:
#        sf org login web --alias <alias>
#      또는 기존 인증 확인:
#        sf org list
#
# target 옵션:
#   --target=Organization,Account,Product,Promotion,Group,Employee,User,Notice,Permission   (기본: 모두)
#   --target=Organization                                                                   (선행 검증용)
#   --target=Account,Product                                                                (마스터만)
#
# 산출물 (기본 ./input/):
#   - organizations.csv                — Org__c
#   - accounts.csv                     — Account
#   - products.csv                     — DKRetail__Product__c
#   - promotions.csv                   — DKRetail__Promotion__c
#   - groups.csv                       — Group (Type IN Regular, Queue)
#   - employees.csv                    — DKRetail__Employee__c
#   - users.csv                        — User + Profile.Name relationship
#   - notices.csv                      — DKRetail__Notice__c
#   - permission_set_assignments.csv   — PermissionSetAssignment (활성 사용자 + 커스텀 PermSet)
#   - group_members.csv                — PublicGroup 멤버십 (선택, --skip-group-members 가능)
#
# 추출 결과는 migrate-stage1.main.kts 의 입력으로 그대로 사용 가능 (input dir 자동 연결).

set -euo pipefail

# -----------------------------------------------------------------------------
# 인자 파싱
# -----------------------------------------------------------------------------

SF_ORG=""
SF_API_VERSION="60.0"
OUT_DIR=""
TARGETS="Organization,Account,Product,Promotion,Group,Employee,User,Notice,AccountCategoryMaster,AgreementHistory,AgreementWord,AlternativeHoliday,Appointment,AttendanceLog,AttendInfo,Claim,DisplayWorkSchedule,EmployeeInputCriteriaMaster,ErpOrder,ErpOrderProduct,HolidayMaster,InspectionTheme,SiteActivity,MonthlyFemaleEmployeeIntegrationSchedule,MonthlySalesHistory,DailySalesHistory,SalesProgressRateMaster,NewProduct,OrderRequest,OrderRequestProduct,ProductBarcode,ProfessionalPromotionTeamHistory,ProfessionalPromotionTeamMaster,PromotionEmployee,PromotionProduct,PushMessage,PushMessageReceiver,Suggestion,TeamMemberSchedule,UploadFile,UserRole,Profile,Permission"
SKIP_GROUP_MEMBERS=0
SKIP_VERIFY=0
# spec #790 Q4 채택 — XML 메타 (extract-sharing-meta.sh) 자동 포함, --skip-sharing-meta 로 제외 가능
INCLUDE_SHARING_META=1
# SF CLI 기본 query 결과 limit 은 50,000 — Account 등 대용량 SObject 대응 위해 상향.
# 환경변수로 이미 설정되어 있으면 그 값을 존중.
MAX_QUERY_LIMIT="${SF_ORG_MAX_QUERY_LIMIT:-200000}"
# Bulk API 자동 분기 — threshold 이상은 Bulk, 미만은 REST.
BULK_THRESHOLD=50000
# BULK_MODE: auto | force | disabled
BULK_MODE="auto"
# Bulk API job 폴링 timeout (분 단위, sf data export bulk --wait 에 전달).
BULK_WAIT_MINUTES=120

while [[ $# -gt 0 ]]; do
    case "$1" in
        --org)
            SF_ORG="$2"
            shift 2
            ;;
        --target=*)
            TARGETS="${1#--target=}"
            shift
            ;;
        --api-version)
            SF_API_VERSION="$2"
            shift 2
            ;;
        --out-dir)
            OUT_DIR="$2"
            shift 2
            ;;
        --skip-group-members)
            SKIP_GROUP_MEMBERS=1
            shift
            ;;
        --include-sharing-meta)
            INCLUDE_SHARING_META=1
            shift
            ;;
        --skip-sharing-meta)
            INCLUDE_SHARING_META=0
            shift
            ;;
        --max-query-limit)
            MAX_QUERY_LIMIT="$2"
            shift 2
            ;;
        --skip-verify)
            SKIP_VERIFY=1
            shift
            ;;
        --bulk-threshold)
            BULK_THRESHOLD="$2"
            shift 2
            ;;
        --bulk)
            BULK_MODE="force"
            shift
            ;;
        --no-bulk)
            BULK_MODE="disabled"
            shift
            ;;
        --bulk-wait)
            BULK_WAIT_MINUTES="$2"
            shift 2
            ;;
        -h|--help)
            sed -n '2,30p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown arg: $1" >&2
            echo "Usage: $0 [--org <alias>] [--target=<list>] [--api-version <ver>] [--out-dir <dir>] [--skip-group-members] [--skip-sharing-meta] [--include-sharing-meta] [--max-query-limit <N>] [--skip-verify] [--bulk-threshold <N>] [--bulk] [--no-bulk] [--bulk-wait <min>]" >&2
            exit 1
            ;;
    esac
done

# SF CLI 의 query 결과 limit 상향 (env + sf config 양쪽 적용).
# sf 7.x 일부 버전에서 env 만으로는 인식 안 되는 케이스 보고됨 → config 직접 설정 병행.
export SF_ORG_MAX_QUERY_LIMIT="$MAX_QUERY_LIMIT"
sf config set org-max-query-limit="$MAX_QUERY_LIMIT" --global >/dev/null 2>&1 || true

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${OUT_DIR:-${SCRIPT_DIR}/input}"
mkdir -p "$OUT_DIR"

# -----------------------------------------------------------------------------
# 사전 검증
# -----------------------------------------------------------------------------

if ! command -v sf >/dev/null 2>&1; then
    echo "[ERROR] 'sf' CLI not found. Install: npm i -g @salesforce/cli" >&2
    exit 1
fi

# EntityMetadata ↔ backend @SFField 정합 검증 (--skip-verify 로 우회 가능)
if [[ "$SKIP_VERIFY" -eq 0 ]]; then
    if ! command -v kotlin >/dev/null 2>&1; then
        echo "[ERROR] 'kotlin' CLI not found — verify-metadata 실행 불가." >&2
        echo "        kotlin 설치 (brew install kotlin) 후 재시도하거나 --skip-verify 사용." >&2
        exit 1
    fi
    echo "[info] verify-metadata 실행 중 — 정합 검증 (skip: --skip-verify)"
    if ! kotlin "$SCRIPT_DIR/verify-metadata.main.kts"; then
        echo
        echo "[ERROR] EntityMetadata 정합 검증 실패 — 추출 중단." >&2
        echo "        common.kts 의 FieldMapping 을 보강하거나 --skip-verify 로 강제 진행." >&2
        exit 1
    fi
    echo
fi

echo "[info] sf CLI: $(sf --version | head -1)"

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
    echo "[info] target org: $SF_ORG"
else
    echo "[info] target org: (default — sf config set target-org)"
fi

echo "[info] api version: $SF_API_VERSION"
echo "[info] targets    : $TARGETS"
echo "[info] output dir : $OUT_DIR"
echo "[info] max query limit: $MAX_QUERY_LIMIT"
case "$BULK_MODE" in
    auto)     echo "[info] bulk policy : auto (threshold=${BULK_THRESHOLD}, wait=${BULK_WAIT_MINUTES}min)" ;;
    force)    echo "[info] bulk policy : FORCE — 모든 entity Bulk API 사용 (wait=${BULK_WAIT_MINUTES}min)" ;;
    disabled) echo "[info] bulk policy : DISABLED — 모든 entity REST API 사용" ;;
esac
echo

# -----------------------------------------------------------------------------
# SOQL 정의 (target 별)
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
EOF
)

# 전량 추출 정책 — 모든 SObject 에서 WHERE 필터(IsDeleted=FALSE 및 권한/그룹 필터)를 제거한다.
# 이유: soft-delete·퇴직·휴직 row 가 존재해야 Stage 2 FK resolve 가 부모를 찾고(employee_sfid 등),
# 통계 모집단에 포함된다. 퇴직/휴직/삭제 사원은 employee.status 로 구분되어 backend 조회에서
# status 필터로 걸러진다. 사번(DKRetail__EmpCode__c) blank 사원도 적재 (common.kts employee_code
# nullable + entity employeeCode String? 정합).
# 주석은 heredoc 밖에 둔다 — build_count_soql 가 개행을 공백 치환하므로 SOQL 내 `--` 주석은 COUNT 쿼리를 깬다.
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
EOF
)

# User 는 전량 추출 (필터 없음) — account.OwnerId / CreatedById / LastModifiedById 등은 퇴사·인사이동으로
# 비활성(IsActive=FALSE)된 User 를 대량 참조한다. 활성만 export 하면 Stage 2 FK resolve 에서 user lookup
# 미해소(부모 부재)가 다수 발생(특히 owner FK). 비활성 User 도 전량 적재하되 user.is_active=FALSE 로 들어가
# 로그인/목록 노출에서 걸러진다. Profile.Name NOT IN 시스템 프로필 제외도 전량 추출 정책에 따라 제거 —
# SF 시스템/통합 계정(Admin/Integration 등)도 audit FK 부모 확보를 위해 적재 (권한 모델 노출은 별도 게이팅).
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
EOF
)

PSA_SOQL=$(cat <<'EOF'
SELECT
    Id,
    AssigneeId,
    Assignee.DKRetail__EmployeeNumber__c,
    PermissionSetId,
    PermissionSet.Name,
    PermissionSet.Label,
    IsActive,
    SystemModstamp
FROM PermissionSetAssignment
EOF
)
# Note: PermissionSetAssignment 는 SF 표준 필드만 8개 (CreatedDate / LastModifiedDate 미보유 —
# 2026-05-24 prod describe 확인). SystemModstamp 가 단일 timestamp 로 grant/revoke 시점 근사.

GM_SOQL=$(cat <<'EOF'
SELECT
    Id, GroupId, Group.Name, UserOrGroupId
FROM GroupMember
EOF
)

# spec #790 Q3 채택 — PermissionSet 자체 SOQL (sfid 채움용)
PS_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Label
FROM PermissionSet
EOF
)

# Profile 객체권한 — ObjectPermissions 는 Profile 에 1:1 매핑된 hidden PermissionSet
# (Parent.IsOwnedByProfile = TRUE) 에 저장된다. Profile retrieve XML 에는 objectPermissions 블록이
# 비어 내려오므로 (동일 retrieve 호출에 SObject 가 없으면 SF 가 채우지 않음), 운영 실측은 본 SOQL 로 확보.
# extract-sharing-meta.main.kts 가 본 CSV 를 Profile 별로 그룹핑하여 profile-flags.csv 의
# objectPermissionsJson 컬럼을 채운다 (PermissionSet 은 XML 출처 유지 — 본 갭은 Profile 한정).
# 전량 추출 정책에 따라 Parent.IsOwnedByProfile=TRUE 필터는 제거됨 — Profile 소유 외 PermissionSet 의
# 객체권한도 함께 내려오나, extract-sharing-meta 가 Parent.Profile.Name 이 있는 row 만 Profile 별로
# 그룹핑하므로 (Profile 미소유 row 는 Parent.Profile.Name = NULL) profile-flags 산출에는 무해.
PROFILE_OBJECT_PERMISSIONS_SOQL=$(cat <<'EOF'
SELECT
    Parent.Profile.Name,
    SobjectType,
    PermissionsRead, PermissionsCreate, PermissionsEdit, PermissionsDelete,
    PermissionsViewAllRecords, PermissionsModifyAllRecords
FROM ObjectPermissions
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
EOF
)

PROMOTION_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__PromotionType__c, AccId__c, DKRetail__AccId__c,
    DKRetail__StartDate__c, DKRetail__EndDate__c,
    DKRetail__PrimaryProductId__c, DKRetail__OtherProduct__c,
    DKRetail__Message__c, DKRetail__StandLocation__c,
    CostCenterCode__c, DKRetail__Remark__c, DKRetail__ProductType__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    IsDeleted, DKRetail__ActualAmount__c, DKRetail__TargetAmount__c
FROM DKRetail__Promotion__c
EOF
)

NOTICE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Title__c, EmployeeId__c,
    DKRetail__Scope__c, DKRetail__Category__c, DKRetail__Contents__c,
    DKRetail__EduCategory__c,
    DKRetail__Jeejum__c, DKRetail__JeejumCode__c,
    IsDeleted, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById
FROM DKRetail__Notice__c
EOF
)
SUGGESTION_SOQL=$(cat <<'EOF'
SELECT
    Id, Name,
    DKRetail__Title__c, DKRetail__Description__c,
    Category__c, DKRetail__Category__c, Category1__c, Category2__c, Category3__c,
    AccountId__c, DKRetail__SAPAccountCode__c,
    DKRetail__EmployeeId__c,
    DKRetail__ProductId__c, ProductCode__c,
    OrgCostCenterCode__c, CostCenterCode__c,
    CarNumber__c, ClaimDate__c, ClaimType__c, ClaimTypeMeasures__c,
    LogisticsResponsibility__c, WERK1_TEXT2__c, WERK3_TEXT2__c,
    ActionStatus__c, DuplicateProposalNum__c,
    ActionContent__c, ActionManager__c, ActionNum__c,
    OwnerId, CreatedById, LastModifiedById,
    IsDeleted, CreatedDate, LastModifiedDate
FROM DKRetail__Proposal__c
EOF
)
ACCOUNT_CATEGORY_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, AccountCode__c, Name, useSearch__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM AccountCategoryMaster__c
EOF
)

AGREEMENT_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, EmployeeId__c, AgreementFlag__c, AgreementDate__c, AgreementWordId__c,
    IsDeleted, Name, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById
FROM AgreementHistory__c
EOF
)

AGREEMENT_WORD_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Contents__c, Active__c, ActiveDate__c, AfterActiveDate__c,
    IsDeleted, CreatedDate, LastModifiedDate, OwnerId, CreatedById,
    LastModifiedById
FROM AgreementWord__c
EOF
)

ALTERNATIVE_HOLIDAY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__ActualWorkDate__c,
    DKRetail__TargetAltHolidayDate__c, DKRetail__ConfirmAltHolidayDate__c,
    DKRetail__Status__c, DKRetail__ChangeReason__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__AlternativeHoliday__c
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
EOF
)

ATTENDANCE_LOG_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__CommuteDate__c,
    DKRetail__AccId__c, DKRetail__SecondWorkType__c, DKRetail__Reason__c,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    IsDeleted
FROM DKRetail__CommuteLog__c
EOF
)

ATTEND_INFO_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeCode__c, StartDate__c, EndDate__c, AttendType__c,
    Status__c, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM AttendInfo__c
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
EOF
)

DISPLAY_WORK_SCHEDULE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Account__c, FullName__c, StartDate__c, EndDate__c, Confirmed__c,
    TypeOfWork1__c, TypeOfWork3__c, TypeOfWork4__c, TypeOfWork5__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById,
    CostCenterCode__c, LastMonthRevenue__c, IsDeleted
FROM DisplayWorkScheduleMaster__c
EOF
)

EMPLOYEE_INPUT_CRITERIA_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, BifurcationHalfPersonStandard__c, Boundary__c, Category__c,
    Confirmed__c, StartDate__c, EndDate__c, Fixed1PersonStandardAmount__c,
    TypeOfWork1__c, OwnerId, CreatedDate, LastModifiedDate,
    CreatedById, LastModifiedById, IsDeleted
FROM EmployeeInputCriteriaMaster__c
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
EOF
)

HOLIDAY_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, HolidayDate__c, Name, Type__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM HolidayMaster__c
EOF
)

INSPECTION_THEME_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Title__c, StartDate__c, EndDate__c, Department__c, BranchCode__c,
    PublicFlag__c, IsDeleted, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Theme__c
EOF
)

# Formula 필드 (ThemeName__c / EmployeeOrgName__c / BranchName__c / OrgName__c) 는
# DB 컬럼 미적재 대상이라 SELECT 에서 제외.
SITE_ACTIVITY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__ActivityDate__c, DKRetail__Category__c,
    DKRetail__ProductType__c, DKRetail__Description__c, DKRetail__Title__c,
    DKRetail__SAPAccountCode__c, CostCenterCode__c,
    DKRetail__CompetitorName__c, DKRetail__CompetitorProductName__c,
    DKRetail__CompetitorActivityDescription__c, DKRetail__CompetitorProudctPrice__c,
    DKRetail__SampleTastFlag__c, DKRetail__SalesQuantity__c, IsDeleted,
    DKRetail__AccountId__c, DKRetail__EmployeeId__c, DKRetail__ProductId__c, ThemeId__c,
    OwnerId, CreatedById, CreatedDate, LastModifiedDate, LastModifiedById
FROM DKRetail__SiteAcitivity__c
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
EOF
)

DAILY_SALES_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, SAPAccountCode__c, SalesDate__c, Externalkey__c, AccountId__c,
    ERPSalesAmount1__c, ERPSalesAmount2__c, ERPSalesAmount3__c,
    ERPDistributionAmount1__c, ERPDistributionAmount2__c, ERPDistributionAmount3__c,
    ERPSalesAmount__c, ERPDistributionAmount__c, LedgerAmount__c,
    CreatedDate, LastModifiedDate
FROM DailySalesHistory__c
EOF
)

SALES_PROGRESS_RATE_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, AccountCDUpl__c, BusinessRate__c, CurrentMonthSalesAmount__c,
    ExternalKey__c, FOTartgetAmount__c, FRTargetAmount__c,
    PreviousMonthSalesAmount__c, RMTartgetAmount__c, RTTargetAmount__c,
    TargetMonth__c, TargetSumAmount__c, TargetYear__c, accountbranchView__c,
    AccountBranchCode__c, IsDeleted, Account__c, OwnerId, CreatedById,
    CreatedDate, LastModifiedDate, LastModifiedById
FROM SalesProgressRateMaster__c
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
EOF
)

ORDER_REQUEST_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__AccountId__c, OrderDate__c,
    DKRetail__OrderDate__c, DKRetail__RequestDate__c, TotalOrderAmount__c,
    DKRetail__RequestStatus__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM DKRetail__OrderRequest__c
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
EOF
)

PRODUCT_BARCODE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, ProductName__c, ProductBarcode__c, ProductUnit__c,
    ProductSequence__c, Product__c, ProductCode__c, CustomKey__c, IsDeleted,
    OwnerId, CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM ProductBarcode__c
EOF
)

PROFESSIONAL_PROMOTION_TEAM_HISTORY_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeId__c, oldValue__c, newValue__c, updateTime__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM ProfessionalPromotionTeamHistory__c
EOF
)

PROFESSIONAL_PROMOTION_TEAM_MASTER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Account__c, FullName__c, ProfessionalPromotionTeam__c,
    StartDate__c, EndDate__c, Confirmed__c, CostCenterCode__c, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById, IsDeleted
FROM ProfessionalPromotionTeamMaster__c
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
EOF
)

PROMOTION_PRODUCT_SOQL=$(cat <<'EOF'
SELECT
    Id, Name,
    DKRetail__PromotionId__c, DKRetail__ProductId__c,
    DKRetail__Price__c, PromotionIdExt__c,
    CreatedById, LastModifiedById,
    IsDeleted, CreatedDate, LastModifiedDate
FROM DKRetail__PromotionProduct__c
EOF
)

PUSH_MESSAGE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, Message__c, ScheduleDate__c, EmployeeId__c, Branch__c,
    BranchCode__c, SObjectRecordId__c, IsDeleted, OwnerId,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM PushMessage__c
EOF
)

PUSH_MESSAGE_RECEIVER_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, EmployeeId__c, MessageId__c, IsDeleted,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM PushMessageReceiver__c
EOF
)

TEAM_MEMBER_SCHEDULE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DKRetail__EmployeeId__c, DKRetail__WorkingDate__c,
    DKRetail__WorkingType__c, DKRetail__WorkingCategory1__c,
    DKRetail__WorkingCategory2__c, DKRetail__WorkingCategory3__c,
    WorkingCategory4__c, AccountId__c, DKRetail__AccountId__c, teamleadersfid__c,
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
EOF
)

UPLOAD_FILE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, UniqueKey__c, RecordId__c, Size__c, Object__c, Url__c,
    UploadKbn__c, FileId__c, Date__c, IsDeleted, CreatedDate, OwnerId,
    CreatedById, LastModifiedById
FROM UploadFile__c
EOF
)

# Spec #780 — SF UserRole / Profile entity 신규 시스템 편입.
# SF describe 실측 (UserRole 16→7 필드, Profile 573→8 필드). 둘 다 IsDeleted 필드 부재.

USER_ROLE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DeveloperName, RollupDescription,
    ParentRoleId,
    LastModifiedDate, LastModifiedById
FROM UserRole
EOF
)

PROFILE_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, UserType, Description,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Profile
EOF
)

GROUP_SOQL=$(cat <<'EOF'
SELECT
    Id, Name, DeveloperName, Type, RelatedId, OwnerId,
    Email, DoesSendEmailToMembers, DoesIncludeBosses,
    CreatedDate, LastModifiedDate, CreatedById, LastModifiedById
FROM Group
EOF
)

# RecordType — 운영 실측 Id(sfid) 출처. 기존엔 XML 메타(recordType-meta.xml)로 record-type.csv 를
# 만들었으나 XML 에는 18자리 RecordTypeId 가 없어 record_type.sfid 가 NULL → SObject row 의
# record_type_sfid → record_type_id FK resolve(sfid 매칭) 가 전량 미해소였다. SOQL 단독 출처로
# 전환해 Id 를 적재한다. (자연키 (SobjectType, DeveloperName) 는 XML 출처와 동일 — 기존 자연키 소비처
# profile_record_type / permission_set_record_type 정합 유지.) Master RT 도 포함되나 무해
# (record_type_id 매칭에만 쓰이고, Master 를 참조하는 row 가 없으면 미사용).
RECORD_TYPE_SOQL=$(cat <<'EOF'
SELECT
    Id, SobjectType, DeveloperName, Name, Description, IsActive
FROM RecordType
EOF
)

# -----------------------------------------------------------------------------
# 추출 함수
# -----------------------------------------------------------------------------

# 시간 측정용 — entity 별 elapsed (sec) 누적. 최종 요약에 사용.
declare -a TIMING_LABELS=()
declare -a TIMING_SECONDS=()
declare -a TIMING_ROWS=()
declare -a TIMING_API=()
SCRIPT_START_EPOCH="$(date +%s)"

# 초 → "Hh MMm SSs" / "MMm SSs" / "SSs" 포맷
format_duration() {
    local secs="$1"
    local h=$((secs / 3600))
    local m=$(((secs % 3600) / 60))
    local s=$((secs % 60))
    if [[ $h -gt 0 ]]; then
        printf "%dh %02dm %02ds" "$h" "$m" "$s"
    elif [[ $m -gt 0 ]]; then
        printf "%dm %02ds" "$m" "$s"
    else
        printf "%ds" "$s"
    fi
}

# SOQL 에서 root SObject 이름 추출.
# 다중 줄 SOQL / 첫 FROM 절 다음 토큰 / 서브쿼리(괄호 안 FROM) 제외.
# 실패 시 빈 문자열 반환.
extract_sobject_from_soql() {
    local soql="$1"
    # 모든 줄을 한 줄로 합치고 괄호 안 subquery 제거 (단순 휴리스틱).
    local flat
    flat="$(echo "$soql" | /usr/bin/tr '\n' ' ' | /usr/bin/sed -E 's/\([^()]*\)/ /g')"
    # 첫 FROM 다음 단어 추출 (대소문자 무시).
    local sobject
    sobject="$(echo "$flat" | /usr/bin/perl -ne 'if (/\bFROM\s+([A-Za-z0-9_]+)/i) { print "$1\n"; exit }')"
    echo "$sobject"
}

# SOQL 이 aggregate / GROUP BY 쿼리인지 감지 (COUNT 분기 불가).
is_aggregate_query() {
    local soql="$1"
    if echo "$soql" | /usr/bin/grep -iqE '\bGROUP[[:space:]]+BY\b|\bCOUNT\(|\bSUM\(|\bAVG\(|\bMIN\(|\bMAX\('; then
        return 0
    fi
    return 1
}

# SOQL 의 WHERE 절을 그대로 보존하면서 SELECT clause 만 COUNT(Id) 로 치환.
# 실패 시 빈 문자열 반환.
build_count_soql() {
    local soql="$1"
    local sobject="$2"
    # WHERE / ORDER BY / LIMIT 등 일부 절을 보존하기 위해 FROM 이후 절을 추출.
    # 첫 FROM 위치 검색 후 그 뒤를 그대로 사용.
    local from_onwards
    from_onwards="$(echo "$soql" | /usr/bin/tr '\n' ' ' | /usr/bin/perl -ne 'if (/(\bFROM\s+.+)/i) { print "$1"; }')"
    if [[ -z "$from_onwards" ]]; then
        echo ""
        return
    fi
    # ORDER BY / LIMIT / OFFSET 은 COUNT 결과에 영향 없으므로 제거 (속도 향상).
    local cleaned
    cleaned="$(echo "$from_onwards" | /usr/bin/perl -pe 's/\s+ORDER\s+BY\s+.+?(?=(\s+LIMIT|\s+OFFSET|$))//gi; s/\s+LIMIT\s+\d+//gi; s/\s+OFFSET\s+\d+//gi')"
    echo "SELECT COUNT(Id) total $cleaned"
}

# SF SObject 의 row count 조회. 실패 시 stderr 로 에러 + 빈 stdout.
fetch_sobject_count() {
    local count_soql="$1"
    local raw
    raw="$(sf data query \
        --query "$count_soql" \
        --result-format json \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}" 2>/dev/null || true)"
    if [[ -z "$raw" ]]; then
        echo ""
        return
    fi
    /usr/bin/python3 -c "import sys, json
try:
    d = json.loads(sys.stdin.read())
    r = d.get('result', {})
    recs = r.get('records', [])
    if recs:
        print(recs[0].get('total', recs[0].get('expr0', '')))
    else:
        print(r.get('totalSize', ''))
except Exception:
    pass" <<< "$raw" 2>/dev/null || echo ""
}

# REST query 실행 (소형 entity 용).
_run_query_rest() {
    local label="$1"
    local soql="$2"
    local out_file="$3"

    local start_epoch
    start_epoch="$(date +%s)"
    sf data query \
        --query "$soql" \
        --result-format csv \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}" \
        > "$out_file"
    local rc=$?
    local end_epoch
    end_epoch="$(date +%s)"
    local elapsed=$((end_epoch - start_epoch))
    if [[ $rc -ne 0 ]]; then
        echo "          [ERROR] REST query failed (exit=$rc) — $label" >&2
        TIMING_LABELS+=("$label")
        TIMING_SECONDS+=("$elapsed")
        TIMING_ROWS+=("0")
        TIMING_API+=("REST-FAIL")
        return $rc
    fi
    local row_count
    row_count=$(($(wc -l < "$out_file") - 1))
    local rate="-"
    if [[ $elapsed -gt 0 && $row_count -gt 0 ]]; then
        rate=$((row_count / elapsed))
    fi
    echo "          rows: $row_count  elapsed: $(format_duration "$elapsed")  rate: ${rate} rec/sec  [REST]"
    TIMING_LABELS+=("$label")
    TIMING_SECONDS+=("$elapsed")
    TIMING_ROWS+=("$row_count")
    TIMING_API+=("REST")
}

# Bulk API 2.0 export (대형 entity 용).
# 실패 시 abort — 사용자에게 보고 후 즉시 종료.
_run_query_bulk() {
    local label="$1"
    local soql="$2"
    local out_file="$3"

    echo "          → Bulk API 2.0 (sf data export bulk, wait=${BULK_WAIT_MINUTES}min)"
    local start_epoch
    start_epoch="$(date +%s)"
    # sf data export bulk: stdout 으로 result CSV 작성 (--output-file 옵션).
    sf data export bulk \
        --query "$soql" \
        --output-file "$out_file" \
        --result-format csv \
        --wait "$BULK_WAIT_MINUTES" \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}"
    local rc=$?
    local end_epoch
    end_epoch="$(date +%s)"
    local elapsed=$((end_epoch - start_epoch))
    if [[ $rc -ne 0 ]]; then
        echo
        echo "[FATAL] Bulk API export 실패 — $label (exit=$rc)" >&2
        echo "        정책: Bulk 실패 시 abort (REST 자동 fallback 안 함)." >&2
        echo "        대응: SF Bulk Data Load Jobs 콘솔에서 job 상태 확인 후 재시도하거나," >&2
        echo "              --no-bulk 로 REST 강제 실행 (단 대형 entity 는 사실상 불가)." >&2
        TIMING_LABELS+=("$label")
        TIMING_SECONDS+=("$elapsed")
        TIMING_ROWS+=("0")
        TIMING_API+=("BULK-FAIL")
        exit 1
    fi
    local row_count
    row_count=$(($(wc -l < "$out_file") - 1))
    local rate="-"
    if [[ $elapsed -gt 0 && $row_count -gt 0 ]]; then
        rate=$((row_count / elapsed))
    fi
    echo "          rows: $row_count  elapsed: $(format_duration "$elapsed")  rate: ${rate} rec/sec  [BULK]"
    TIMING_LABELS+=("$label")
    TIMING_SECONDS+=("$elapsed")
    TIMING_ROWS+=("$row_count")
    TIMING_API+=("BULK")
}

# 진입점 — count 기반 분기 + 정책 적용.
run_query() {
    local label="$1"
    local soql="$2"
    local out_file="$3"

    echo "[extract] $label → ${out_file##*/} ..."

    # 정책 결정: force / disabled 면 즉시 분기.
    case "$BULK_MODE" in
        force)
            _run_query_bulk "$label" "$soql" "$out_file"
            return
            ;;
        disabled)
            _run_query_rest "$label" "$soql" "$out_file"
            return
            ;;
    esac

    # auto 모드 — SObject 추출 + COUNT 시도.
    local sobject
    sobject="$(extract_sobject_from_soql "$soql")"
    if [[ -z "$sobject" ]]; then
        echo "          [auto] SObject 추출 실패 → REST 강제"
        _run_query_rest "$label" "$soql" "$out_file"
        return
    fi
    if is_aggregate_query "$soql"; then
        echo "          [auto] aggregate / GROUP BY 쿼리 ($sobject) → REST 강제"
        _run_query_rest "$label" "$soql" "$out_file"
        return
    fi
    local count_soql
    count_soql="$(build_count_soql "$soql" "$sobject")"
    if [[ -z "$count_soql" ]]; then
        echo "          [auto] COUNT SOQL 생성 실패 ($sobject) → REST 강제"
        _run_query_rest "$label" "$soql" "$out_file"
        return
    fi
    local row_count
    row_count="$(fetch_sobject_count "$count_soql")"
    if [[ -z "$row_count" || ! "$row_count" =~ ^[0-9]+$ ]]; then
        echo "          [auto] COUNT 쿼리 실패 ($sobject) → REST 강제"
        _run_query_rest "$label" "$soql" "$out_file"
        return
    fi

    if [[ $row_count -ge $BULK_THRESHOLD ]]; then
        echo "          [auto] count($sobject)=$row_count ≥ threshold $BULK_THRESHOLD → Bulk API"
        _run_query_bulk "$label" "$soql" "$out_file"
    else
        echo "          [auto] count($sobject)=$row_count < threshold $BULK_THRESHOLD → REST"
        _run_query_rest "$label" "$soql" "$out_file"
    fi
}

contains_target() {
    local needle="$1"
    [[ ",$TARGETS," == *",$needle,"* ]]
}

# -----------------------------------------------------------------------------
# 실행
# -----------------------------------------------------------------------------

echo "========================================================"
echo "SF 데이터 CSV 추출 (Spec #764)"
echo "========================================================"
echo

if contains_target "Organization"; then
    run_query "Organization (Org__c)" "$ORGANIZATION_SOQL" "$OUT_DIR/organizations.csv"
fi

if contains_target "Account"; then
    run_query "Account" "$ACCOUNT_SOQL" "$OUT_DIR/accounts.csv"
fi

if contains_target "Product"; then
    run_query "Product (DKRetail__Product__c)" "$PRODUCT_SOQL" "$OUT_DIR/products.csv"
fi

if contains_target "Promotion"; then
    run_query "Promotion (DKRetail__Promotion__c)" "$PROMOTION_SOQL" "$OUT_DIR/promotions.csv"
fi

if contains_target "Group"; then
    run_query "Group (Regular + Queue)" "$GROUP_SOQL" "$OUT_DIR/groups.csv"
fi

if contains_target "Employee"; then
    run_query "Employee (DKRetail__Employee__c)" "$EMPLOYEE_SOQL" "$OUT_DIR/employees.csv"
fi

if contains_target "User"; then
    run_query "User" "$USER_SOQL" "$OUT_DIR/users.csv"
fi

if contains_target "Notice"; then
    run_query "Notice (DKRetail__Notice__c)" "$NOTICE_SOQL" "$OUT_DIR/notices.csv"
fi

if contains_target "Suggestion"; then
    run_query "Suggestion (DKRetail__Proposal__c)" "$SUGGESTION_SOQL" "$OUT_DIR/suggestions.csv"
fi

if contains_target "AccountCategoryMaster"; then
    run_query "AccountCategoryMaster (AccountCategoryMaster__c)" "$ACCOUNT_CATEGORY_MASTER_SOQL" "$OUT_DIR/account_category_masters.csv"
fi

if contains_target "AgreementHistory"; then
    run_query "AgreementHistory (AgreementHistory__c)" "$AGREEMENT_HISTORY_SOQL" "$OUT_DIR/agreement_historys.csv"
fi

if contains_target "AgreementWord"; then
    run_query "AgreementWord (AgreementWord__c)" "$AGREEMENT_WORD_SOQL" "$OUT_DIR/agreement_words.csv"
fi

if contains_target "AlternativeHoliday"; then
    run_query "AlternativeHoliday (DKRetail__AlternativeHoliday__c)" "$ALTERNATIVE_HOLIDAY_SOQL" "$OUT_DIR/alternative_holidays.csv"
fi

if contains_target "Appointment"; then
    run_query "Appointment (Appointment__c)" "$APPOINTMENT_SOQL" "$OUT_DIR/appointments.csv"
fi

if contains_target "AttendanceLog"; then
    run_query "AttendanceLog (DKRetail__CommuteLog__c)" "$ATTENDANCE_LOG_SOQL" "$OUT_DIR/attendance_logs.csv"
fi

if contains_target "AttendInfo"; then
    run_query "AttendInfo (AttendInfo__c)" "$ATTEND_INFO_SOQL" "$OUT_DIR/attend_infos.csv"
fi

if contains_target "Claim"; then
    run_query "Claim (DKRetail__Claim__c)" "$CLAIM_SOQL" "$OUT_DIR/claims.csv"
fi

if contains_target "DisplayWorkSchedule"; then
    run_query "DisplayWorkSchedule (DisplayWorkScheduleMaster__c)" "$DISPLAY_WORK_SCHEDULE_SOQL" "$OUT_DIR/display_work_schedules.csv"
fi

if contains_target "EmployeeInputCriteriaMaster"; then
    run_query "EmployeeInputCriteriaMaster (EmployeeInputCriteriaMaster__c)" "$EMPLOYEE_INPUT_CRITERIA_MASTER_SOQL" "$OUT_DIR/employee_input_criteria_masters.csv"
fi

if contains_target "ErpOrder"; then
    run_query "ErpOrder (ERP_Order__c)" "$ERP_ORDER_SOQL" "$OUT_DIR/erp_orders.csv"
fi

if contains_target "ErpOrderProduct"; then
    run_query "ErpOrderProduct (ERP_OrderProduct__c)" "$ERP_ORDER_PRODUCT_SOQL" "$OUT_DIR/erp_order_products.csv"
fi

if contains_target "HolidayMaster"; then
    run_query "HolidayMaster (HolidayMaster__c)" "$HOLIDAY_MASTER_SOQL" "$OUT_DIR/holiday_masters.csv"
fi

if contains_target "InspectionTheme"; then
    run_query "InspectionTheme (Theme__c)" "$INSPECTION_THEME_SOQL" "$OUT_DIR/inspection_themes.csv"
fi

if contains_target "SiteActivity"; then
    run_query "SiteActivity (DKRetail__SiteAcitivity__c)" "$SITE_ACTIVITY_SOQL" "$OUT_DIR/site_activities.csv"
fi

if contains_target "MonthlyFemaleEmployeeIntegrationSchedule"; then
    run_query "MonthlyFemaleEmployeeIntegrationSchedule (MonthlyFemaleEmployeeIntegrationSchedule__c)" "$MONTHLY_FEMALE_EMPLOYEE_INTEGRATION_SCHEDULE_SOQL" "$OUT_DIR/monthly_female_employee_integration_schedules.csv"
fi

if contains_target "MonthlySalesHistory"; then
    run_query "MonthlySalesHistory (MonthlySalesHistory__c)" "$MONTHLY_SALES_HISTORY_SOQL" "$OUT_DIR/monthly_sales_historys.csv"
fi

if contains_target "DailySalesHistory"; then
    run_query "DailySalesHistory (DailySalesHistory__c)" "$DAILY_SALES_HISTORY_SOQL" "$OUT_DIR/daily_sales_historys.csv"
fi

if contains_target "SalesProgressRateMaster"; then
    run_query "SalesProgressRateMaster (SalesProgressRateMaster__c)" "$SALES_PROGRESS_RATE_MASTER_SOQL" "$OUT_DIR/sales_progress_rate_masters.csv"
fi

if contains_target "NewProduct"; then
    run_query "NewProduct (NewProduct__c)" "$NEW_PRODUCT_SOQL" "$OUT_DIR/new_products.csv"
fi

if contains_target "OrderRequest"; then
    run_query "OrderRequest (DKRetail__OrderRequest__c)" "$ORDER_REQUEST_SOQL" "$OUT_DIR/order_requests.csv"
fi

if contains_target "OrderRequestProduct"; then
    run_query "OrderRequestProduct (DKRetail__OrderRequestProduct__c)" "$ORDER_REQUEST_PRODUCT_SOQL" "$OUT_DIR/order_request_products.csv"
fi

if contains_target "ProductBarcode"; then
    run_query "ProductBarcode (ProductBarcode__c)" "$PRODUCT_BARCODE_SOQL" "$OUT_DIR/product_barcodes.csv"
fi

if contains_target "ProfessionalPromotionTeamHistory"; then
    run_query "ProfessionalPromotionTeamHistory (ProfessionalPromotionTeamHistory__c)" "$PROFESSIONAL_PROMOTION_TEAM_HISTORY_SOQL" "$OUT_DIR/professional_promotion_team_historys.csv"
fi

if contains_target "ProfessionalPromotionTeamMaster"; then
    run_query "ProfessionalPromotionTeamMaster (ProfessionalPromotionTeamMaster__c)" "$PROFESSIONAL_PROMOTION_TEAM_MASTER_SOQL" "$OUT_DIR/professional_promotion_team_masters.csv"
fi

if contains_target "PromotionEmployee"; then
    run_query "PromotionEmployee (DKRetail__PromotionEmployee__c)" "$PROMOTION_EMPLOYEE_SOQL" "$OUT_DIR/promotion_employees.csv"
fi

if contains_target "PromotionProduct"; then
    run_query "PromotionProduct (DKRetail__PromotionProduct__c)" "$PROMOTION_PRODUCT_SOQL" "$OUT_DIR/promotion_products.csv"
fi

if contains_target "PushMessage"; then
    run_query "PushMessage (PushMessage__c)" "$PUSH_MESSAGE_SOQL" "$OUT_DIR/push_messages.csv"
fi

if contains_target "PushMessageReceiver"; then
    run_query "PushMessageReceiver (PushMessageReceiver__c)" "$PUSH_MESSAGE_RECEIVER_SOQL" "$OUT_DIR/push_message_receivers.csv"
fi

if contains_target "TeamMemberSchedule"; then
    run_query "TeamMemberSchedule (DKRetail__TeamMemberSchedule__c)" "$TEAM_MEMBER_SCHEDULE_SOQL" "$OUT_DIR/team_member_schedules.csv"
fi

if contains_target "UploadFile"; then
    run_query "UploadFile (UploadFile__c)" "$UPLOAD_FILE_SOQL" "$OUT_DIR/upload_files.csv"
fi

if contains_target "UserRole"; then
    run_query "UserRole" "$USER_ROLE_SOQL" "$OUT_DIR/user_roles.csv"
fi

if contains_target "Profile"; then
    run_query "Profile" "$PROFILE_SOQL" "$OUT_DIR/profiles.csv"

    # Profile 객체권한 — extract-sharing-meta.main.kts 가 본 CSV 를 Profile 별로 묶어 profile-flags.csv 에 반영.
    run_query "ObjectPermissions (Profile-owned)" "$PROFILE_OBJECT_PERMISSIONS_SOQL" "$OUT_DIR/profile_object_permissions.csv"
fi

if contains_target "Permission"; then
    # spec #790 Q3 — PermissionSet 자체 (sfid 채움용)
    run_query "PermissionSet" "$PS_SOQL" "$OUT_DIR/permission_sets.csv"

    run_query "PermissionSetAssignment" "$PSA_SOQL" "$OUT_DIR/permission_set_assignments.csv"

    # RecordType — Id(sfid) 포함 SOQL 출처. record_type.sfid 를 채워 SObject row 의
    # record_type_sfid → record_type_id FK resolve 가 매칭되도록 한다 (XML 출처는 Id 부재).
    run_query "RecordType" "$RECORD_TYPE_SOQL" "$OUT_DIR/record-type.csv"

    if [[ "$SKIP_GROUP_MEMBERS" -eq 0 ]]; then
        run_query "GroupMember (PublicGroup)" "$GM_SOQL" "$OUT_DIR/group_members.csv"
    else
        echo "[info] Skipping GroupMember extraction (--skip-group-members)"
    fi
fi

# spec #790 Q4 — XML 메타 (sharing 정책) 자동 포함
if [[ "$INCLUDE_SHARING_META" -eq 1 ]]; then
    echo
    echo "[sharing-meta] extract-sharing-meta.sh 실행 — XML 메타 → 6 CSV"
    SHARING_META_DIR="${OUT_DIR}"
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    if [[ -x "$SCRIPT_DIR/extract-sharing-meta.sh" ]]; then
        "$SCRIPT_DIR/extract-sharing-meta.sh" --out-dir "$SHARING_META_DIR" || {
            echo "[sharing-meta] extract-sharing-meta.sh 실패 — 이후 단계 진행 중단" >&2
            exit 1
        }
    else
        echo "[sharing-meta] extract-sharing-meta.sh 미존재 또는 실행 권한 없음 — skip" >&2
    fi
else
    echo "[info] Skipping sharing meta extraction (--skip-sharing-meta)"
fi

SCRIPT_END_EPOCH="$(date +%s)"
TOTAL_ELAPSED=$((SCRIPT_END_EPOCH - SCRIPT_START_EPOCH))

echo
echo "========================================================"
echo "✅ 추출 완료"
echo "========================================================"
ls -lh "$OUT_DIR"/*.csv 2>/dev/null | awk '{print "  "$9"  ("$5")"}'

# Entity 별 소요 시간 요약 (느린 순 정렬)
if [[ ${#TIMING_LABELS[@]} -gt 0 ]]; then
    echo
    echo "------------------------------------------------------------"
    echo "엔티티별 소요 시간 (Download)"
    echo "------------------------------------------------------------"
    printf "%-44s  %10s  %10s  %10s  %-9s\n" "Entity" "Rows" "Elapsed" "rec/sec" "API"
    printf "%-44s  %10s  %10s  %10s  %-9s\n" "------" "----" "-------" "-------" "---"
    # idx 별 라인 만들고 elapsed 기준 desc 정렬
    sorted_lines=$(
        for i in "${!TIMING_LABELS[@]}"; do
            api="${TIMING_API[$i]:-?}"
            printf "%010d|%s|%s|%s|%s\n" "${TIMING_SECONDS[$i]}" "${TIMING_LABELS[$i]}" "${TIMING_ROWS[$i]}" "${TIMING_SECONDS[$i]}" "$api"
        done | /usr/bin/sort -t '|' -k1,1 -r
    )
    TOTAL_ROWS=0
    while IFS='|' read -r _ label rows secs api; do
        [[ -z "$label" ]] && continue
        rate="-"
        if [[ $secs -gt 0 && $rows -gt 0 ]]; then
            rate=$((rows / secs))
        fi
        printf "%-44s  %10s  %10s  %10s  %-9s\n" "${label:0:44}" "$rows" "$(format_duration "$secs")" "$rate" "$api"
        TOTAL_ROWS=$((TOTAL_ROWS + rows))
    done <<< "$sorted_lines"
    printf "%-44s  %10s  %10s  %10s  %-9s\n" "------" "----" "-------" "-------" "---"
    AVG_RATE="-"
    if [[ $TOTAL_ELAPSED -gt 0 && $TOTAL_ROWS -gt 0 ]]; then
        AVG_RATE=$((TOTAL_ROWS / TOTAL_ELAPSED))
    fi
    printf "%-44s  %10s  %10s  %10s  %-9s\n" "TOTAL" "$TOTAL_ROWS" "$(format_duration "$TOTAL_ELAPSED")" "$AVG_RATE" ""
fi

echo
echo "다음 단계 (사용자 직접 실행):"
echo "  kotlin $SCRIPT_DIR/migrate.main.kts --target=$TARGETS"
