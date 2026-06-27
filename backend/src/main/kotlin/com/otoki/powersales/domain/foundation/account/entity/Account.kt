package com.otoki.powersales.domain.foundation.account.entity

import com.otoki.powersales.domain.foundation.account.entity.converter.AccountSourceConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.FreezerTypeConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.IndustryConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.OwnershipConverter
import com.otoki.powersales.domain.foundation.account.entity.converter.RatingConverter
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 거래처 마스터 Entity
 * Salesforce Account(거래처) 오브젝트 — SAP 거래처 마스터 동기화 대상 테이블.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("거래처")
@Entity
@Table(name = "account")
@SFObject("Account")
class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("거래처ID")
    @Column(name = "account_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 255)
    var name: String? = null,

    @SFField("Phone")
    @FieldName("전화번호(HP)")
    @Column(name = "phone", length = 40)
    var phone: String? = null,

    @SFField("MobilePhone__c")
    @FieldName("모바일번호")
    @Column(name = "mobile_phone", length = 40)
    var mobilePhone: String? = null,

    @SFField("Address1__c")
    @FieldName("주소")
    @Column(name = "address1", length = 120)
    var address1: String? = null,

    @SFField("Address2__c")
    @FieldName("상세주소")
    @Column(name = "address2", length = 120)
    var address2: String? = null,

    @SFField("Representative__c")
    @FieldName("대표자명")
    @Column(name = "representative", length = 100)
    var representative: String? = null,

    @SFField("ABCType__c")
    @FieldName("ABC유형")
    @Column(name = "abc_type", length = 20)
    var abcType: String? = null,

    @SFField("ABCTypeCode__c")
    @FieldName("ABC유형코드")
    @Column(name = "abc_type_code", length = 40)
    var abcTypeCode: String? = null,

    @SFField("ExternalKey__c")
    @FieldName("SAP거래처코드")
    @Column(name = "external_key", unique = true, length = 100)
    var externalKey: String? = null,

    @SFField("AccountGroup__c")
    @FieldName("계정그룹")
    @Column(name = "account_group", length = 10)
    var accountGroup: String? = null,

    @SFField("BranchCode__c")
    @FieldName("거래처지점코드")
    @Column(name = "branch_code", length = 100)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @FieldName("거래처지점명")
    @Column(name = "branch_name", length = 250)
    var branchName: String? = null,

    @SFField("Zipcode__c")
    @FieldName("우편번호")
    @Column(name = "zip_code", length = 100)
    var zipCode: String? = null,

    @SFField("Latitude__c")
    @FieldName("위도")
    @Column(name = "latitude", length = 100)
    var latitude: String? = null,

    @SFField("Longitude__c")
    @FieldName("경도")
    @Column(name = "longitude", length = 100)
    var longitude: String? = null,

    @SFField("ClosingTime1__c")
    @FieldName("주문마감시간_상온")
    @Column(name = "closing_time1", length = 50)
    var closingTime1: String? = null,

    @SFField("ClosingTime2__c")
    @FieldName("주문마감시간_냉장")
    @Column(name = "closing_time2", length = 50)
    var closingTime2: String? = null,

    @SFField("ClosingTime3__c")
    @FieldName("주문마감시간_냉동")
    @Column(name = "closing_time3", length = 50)
    var closingTime3: String? = null,

    @SFField("Industry")
    @Convert(converter = IndustryConverter::class)
    @FieldName("업종")
    @Column(name = "industry", length = 255)
    var industry: Industry? = null,

    @SFField("WERK1_TX__c")
    @FieldName("물류센터명_상온")
    @Column(name = "werk1_tx", length = 255)
    var werk1Tx: String? = null,

    @SFField("WERK2_TX__c")
    @FieldName("물류센터명_냉장")
    @Column(name = "werk2_tx", length = 255)
    var werk2Tx: String? = null,

    @SFField("WERK3_TX__c")
    @FieldName("물류센터명_냉동")
    @Column(name = "werk3_tx", length = 255)
    var werk3Tx: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // 거래처유형(=거래처유형마스터 Name, 운영 실제 raw 값). 운영 마스터(AccountCategoryMaster)에서
    // 유형이 추가/변경될 수 있어 enum 으로 고정하지 않고 String 으로 보관 — 마스터에 없는 새 값이 와도
    // DB read 시 null 로 소실되지 않는다(과거 AccountType enum + AccountTypeConverter 의 silent null 장애 제거).
    @SFField("Type")
    @FieldName("거래처타입")
    @Column(name = "account_type", length = 255)
    var accountType: String? = null,

    @SFField("AccountStatusName__c")
    @FieldName("거래처상태명")
    @Column(name = "account_status_name", length = 40)
    var accountStatusName: String? = null,

    @SFField("EmployeeCode__c")
    @FieldName("담당영업사원사번")
    @Column(name = "employee_code", length = 15)
    var employeeCode: String? = null,

    @SFField("Distribution__c")
    @FieldName("배부대상거래처")
    @Column(name = "distribution", length = 20)
    var distribution: String? = null,

    @SFField("AccountStatusCode__c")
    @FieldName("거래처상태코드")
    @Column(name = "account_status_code", length = 100)
    var accountStatusCode: String? = null,

    @SFField("BusinessType__c")
    @FieldName("사업유형")
    @Column(name = "business_type", length = 100)
    var businessType: String? = null,

    @SFField("BusinessCategory__c")
    @FieldName("업태")
    @Column(name = "business_category", length = 100)
    var businessCategory: String? = null,

    @SFField("Sic")
    @FieldName("사업자등록번호")
    @Column(name = "business_license_number", length = 20)
    var businessLicenseNumber: String? = null,

    @SFField("Email__c")
    @FieldName("이메일")
    @Column(name = "email", length = 241)
    var email: String? = null,

    @SFField("DivisionName__c")
    @FieldName("거래처사업부명")
    @Column(name = "division_name", length = 250)
    var divisionName: String? = null,

    @SFField("SalesDeptName__c")
    @FieldName("거래처영업부명")
    @Column(name = "sales_dept_name", length = 250)
    var salesDeptName: String? = null,

    @SFField("ConsignmentAcc__c")
    @FieldName("위탁거래처여부")
    @Column(name = "consignment_acc", length = 40)
    var consignmentAcc: String? = null,

    @SFField("WERK1__c")
    @FieldName("물류센터_상온")
    @Column(name = "werk1", length = 255)
    var werk1: String? = null,

    @SFField("WERK2__c")
    @FieldName("물류센터_냉장")
    @Column(name = "werk2", length = 255)
    var werk2: String? = null,

    @SFField("WERK3__c")
    @FieldName("물류센터_냉동")
    @Column(name = "werk3", length = 255)
    var werk3: String? = null,

    @SFField("SalesDeptCostCenter__c")
    @FieldName("거래처영업부 CC코드")
    @Column(name = "sales_dept_cost_center", length = 50)
    var salesDeptCostCenter: String? = null,

    @SFField("DivisionCostCenter__c")
    @FieldName("거래처사업부 CC코드")
    @Column(name = "division_cost_center", length = 50)
    var divisionCostCenter: String? = null,

    // -- Spec #602: SF 누락 컬럼 신규 도입 (Q1 옵션 1 + Q4 추가 → 총 24개) --

    @SFField("AccountNumber")
    @FieldName("거래처번호")
    @Column(name = "account_number", length = 40)
    var accountNumber: String? = null,

    @SFField("Site")
    @FieldName("사이트")
    @Column(name = "site", length = 80)
    var site: String? = null,

    @SFField("AccountSource")
    @Convert(converter = AccountSourceConverter::class)
    @FieldName("거래처소스")
    @Column(name = "account_source", length = 255)
    var accountSource: AccountSource? = null,

    @SFField("BranchCostCenter__c")
    @FieldName("거래처지점 CC코드")
    @Column(name = "branch_cost_center", length = 50)
    var branchCostCenter: String? = null,

    @SFField("DivisionCode__c")
    @FieldName("거래처사업부코드")
    @Column(name = "division_code", length = 100)
    var divisionCode: String? = null,

    @SFField("SalesDeptCode__c")
    @FieldName("거래처영업부코드")
    @Column(name = "sales_dept_code", length = 100)
    var salesDeptCode: String? = null,

    @SFField("LogisticsName__c")
    @FieldName("물류센터명")
    @Column(name = "logistics_name", length = 50)
    var logisticsName: String? = null,

    @SFField("LogisticsCode__c")
    @FieldName("물류센터코드")
    @Column(name = "logistics_code", length = 50)
    var logisticsCode: String? = null,

    @SFField("FreezerInstalled__c")
    @FieldName("냉장고설치여부")
    @Column(name = "freezer_installed")
    var freezerInstalled: Boolean? = null,

    @SFField("FreezerType__c")
    @Convert(converter = FreezerTypeConverter::class)
    @FieldName("냉장고종류")
    @Column(name = "freezer_type", length = 255)
    var freezerType: FreezerType? = null,

    @SFField("Field1__c")
    @FieldName("잔여여신")
    @Column(name = "remaining_credit", precision = 18, scale = 0)
    var remainingCredit: BigDecimal? = null,

    @SFField("TotalCredit__c")
    @FieldName("총여신")
    @Column(name = "total_credit", precision = 18, scale = 0)
    var totalCredit: BigDecimal? = null,

    @SFField("MapCoordinate__c")
    @FieldName("좌표(출퇴근)")
    @Column(name = "map_coordinate", length = 40)
    var mapCoordinate: String? = null,

    @SFField("OrderEndTime__c")
    @FieldName("주문마감시간")
    @Column(name = "order_end_time")
    var orderEndTime: LocalTime? = null,

    @SFField("FirstInstalled__c")
    @FieldName("최초설치일자")
    @Column(name = "first_installed")
    var firstInstalled: LocalDate? = null,

    @SFField("Description")
    @FieldName("행사대체제품")
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @SFField("Website")
    @FieldName("웹사이트")
    @Column(name = "website", length = 255)
    var website: String? = null,

    @SFField("Fax")
    @FieldName("팩스")
    @Column(name = "fax", length = 40)
    var fax: String? = null,

    @SFField("AnnualRevenue")
    @FieldName("연매출")
    @Column(name = "annual_revenue", precision = 18, scale = 0)
    var annualRevenue: BigDecimal? = null,

    @SFField("NumberOfEmployees")
    @FieldName("직원수")
    @Column(name = "number_of_employees")
    var numberOfEmployees: BigDecimal? = null,

    @SFField("ParentId")
    @Column(name = "parent_sfid", length = 18)
    var parentSfid: String? = null,

    @SFField("Rating")
    @Convert(converter = RatingConverter::class)
    @FieldName("등급")
    @Column(name = "rating", length = 255)
    var rating: Rating? = null,

    @SFField("Ownership")
    @Convert(converter = OwnershipConverter::class)
    @FieldName("소유형태")
    @Column(name = "ownership", length = 255)
    var ownership: Ownership? = null,

    @SFField("IsPriorityRecord")
    @FieldName("우선거래처여부")
    @Column(name = "is_priority_record")
    var isPriorityRecord: Boolean? = null,

    // -- Spec #758: Audit FK 타입 Employee → User 전환 (Account 단독 구현분) --
    // SF sobject 메타 정합 (referenceTo == User). application 코드는 FK 컬럼만 사용, sfid 직접 JOIN 금지.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    // V199 — SF Account.OwnerId.referenceTo = [User] 단일 (Group 미포함, Account 는 Standard SObject 의 enableQueues=false).
    // owner_id → owner_user_id rename. polymorphic owner XOR 패턴 불요.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Account? = null
) : BaseEntity() {

    /**
     * 유통형태 — 거래처상태코드(AccountStatusCode__c) + 거래처유형(Type) 을 공백으로 조합한 표시 문자열.
     *
     * 예: "02 슈퍼", "01 체인". 두 값이 모두 비어 있으면 null.
     * 표시용 폴백("-" 등)은 호출 측(web/엑셀)에서 처리한다.
     */
    fun distributionChannelLabel(): String? =
        distributionChannelLabel(accountStatusCode, accountType)

    /**
     * 거래처유형 — ABC유형코드(ABCTypeCode__c) + ABC유형(ABCType__c) 을 공백으로 조합한 표시 문자열.
     *
     * 예: "6111 이마트", "2001 슈퍼". 두 값이 모두 비어 있으면 null.
     * (거래처타입 enum 필드 [accountType] 과는 별개 — 화면 "거래처유형" 컬럼의 원천.)
     * 표시용 폴백("-" 등)은 호출 측(web/엑셀)에서 처리한다.
     */
    fun abcTypeLabel(): String? = abcTypeLabel(abcTypeCode, abcType)

    companion object {
        /**
         * 유통형태 라벨 조합 — 거래처상태코드 + 거래처유형명(displayName) 을 공백으로 조합.
         *
         * Account 엔티티를 hydrate 하지 않는 projection 조회(예: 월별 통합일정)에서도
         * 동일 조합 규칙을 재사용하도록 companion 으로 분리. 인스턴스 메서드
         * [distributionChannelLabel] 도 이 함수를 위임 호출한다.
         */
        fun distributionChannelLabel(accountStatusCode: String?, accountTypeName: String?): String? =
            listOfNotNull(accountStatusCode, accountTypeName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { null }

        /**
         * 거래처유형 라벨 조합 — ABC유형코드 + ABC유형 을 공백으로 조합.
         *
         * projection 조회에서 재사용하도록 companion 으로 분리. 인스턴스 메서드
         * [abcTypeLabel] 도 이 함수를 위임 호출한다.
         */
        fun abcTypeLabel(abcTypeCode: String?, abcType: String?): String? =
            listOfNotNull(abcTypeCode, abcType)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { null }
    }
}
