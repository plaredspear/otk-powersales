package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 월별 여사원 통합일정 Entity
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("월별 여사원 통합일정")
@Entity
@Table(name = "monthly_female_employee_integration_schedule")
@SFObject("MonthlyFemaleEmployeeIntegrationSchedule__c")
class MonthlyFemaleEmployeeIntegrationSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("월별여사원통합일정ID")
    @Column(name = "monthly_female_employee_integration_schedule_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("ExternalKey__c")
    @FieldName("ExternalKey")
    @Column(name = "external_key", length = 255, unique = true)
    val externalKey: String? = null,

    @SFField("Year__c")
    @FieldName("연도")
    @Column(name = "year", length = 255)
    val year: String? = null,

    @SFField("Month__c")
    @FieldName("월")
    @Column(name = "month", length = 255)
    val month: String? = null,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("CostCenterCode__c")
    @FieldName("조직유형")
    @Column(name = "cost_center_code", length = 40)
    val costCenterCode: String? = null,

    @SFField("WorkingCategory1__c")
    @FieldName("근무유형1")
    @Column(name = "working_category1", length = 255)
    val workingCategory1: String? = null,

    @SFField("WorkingCategory3__c")
    @FieldName("근무유형3")
    @Column(name = "working_category3", length = 255)
    val workingCategory3: String? = null,

    @SFField("WorkingCategory4__c")
    @FieldName("근무유형4")
    @Column(name = "working_category4", length = 255)
    val workingCategory4: String? = null,

    @SFField("WorkingCategory5__c")
    @FieldName("근무유형5")
    @Column(name = "working_category5", length = 255)
    val workingCategory5: String? = null,

    @SFField("EmpBranchName__c")
    @FieldName("사원지점명")
    @Column(name = "emp_branch_name", length = 255)
    var empBranchName: String? = null,

    // 전문행사조 — TMS 의 stamp 값(String, 미배정은 '일반')을 무변환 저장한다. Employee/MFEIS 를
    // enum(ProfessionalPromotionTeamType) 으로 강타입화하면 converter 가 '일반' 을 5개 정식 조가 아니라며
    // null 로 떨어뜨려, 재집계 row 만 null 이 되고 SF 마이그레이션 row('일반' 문자열)와 값이 어긋난다.
    // 레거시 SF 는 3계층(Employee/TMS/MFEIS) 모두 Text(255) 로 '일반' 을 그대로 흘려보내므로,
    // MFEIS 컬럼은 String 원본을 유지해 마이그레이션 ↔ 재집계 저장값을 일치시킨다.
    // (이 컬럼은 필터/집계/응답/SF전송 어디에서도 read 되지 않아 String 화의 하위 영향이 없다.)
    @SFField("ProfessionalPromotionTeam__c")
    @FieldName("전문행사조")
    @Column(name = "professional_promotion_team", length = 255)
    val professionalPromotionTeam: String? = null,

    @SFField("WorkingDaysMonth__c")
    @FieldName("당월 근무일수")
    @Column(name = "working_days_month", precision = 30, scale = 18)
    var workingDaysMonth: BigDecimal? = null,

    @SFField("NumberOfInputs__c")
    @FieldName("총 투입횟수")
    @Column(name = "number_of_inputs")
    var numberOfInputs: BigDecimal? = null,

    @SFField("EquivalentNumberOfWorkingDays__c")
    @FieldName("총 환산근무일수")
    @Column(name = "equivalent_number_of_working_days", precision = 30, scale = 18)
    var equivalentNumberOfWorkingDays: BigDecimal? = null,

    @SFField("ConvertedHeadcount__c")
    @FieldName("총 환산인원")
    @Column(name = "converted_headcount", precision = 30, scale = 18)
    var convertedHeadcount: BigDecimal? = null,

    @SFField("EDI_POS__c")
    @FieldName("EDI/POS")
    @Column(name = "edi_pos")
    val ediPos: BigDecimal? = null,

    @SFField("ThisMonthAmount__c")
    @FieldName("【검증용】6개월평균매출")
    @Column(name = "this_month_amount")
    var thisMonthAmount: BigDecimal? = null,

    @SFField("AccountConvertedHeadcount__c")
    @FieldName("【검증용】총 진열환산인원(거래처별)")
    @Column(name = "account_converted_headcount", precision = 30, scale = 18)
    var accountConvertedHeadcount: BigDecimal? = null,

    @SFField("EmployeeInputCriteriaMaster__c")
    @Column(name = "employee_input_criteria_master_sfid", length = 18)
    var employeeInputCriteriaMasterSfid: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account? = null,

    // OwnerId polymorphic [Group, User] — owner_sfid sync buffer + (owner_user XOR owner_group) FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_input_criteria_master_id")
    var employeeInputCriteriaMaster: EmployeeInputCriteriaMaster? = null,

    ) : BaseEntity()
