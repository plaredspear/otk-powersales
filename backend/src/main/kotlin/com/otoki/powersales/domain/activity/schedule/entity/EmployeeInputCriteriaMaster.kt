package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.domain.activity.schedule.entity.converter.TypeOfWork1Converter
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 여사원 투입 기준 마스터 Entity (EmployeeInputCriteriaMaster__c).
 * MonthlyFemaleEmployeeIntegrationSchedule 의 인사 투입 기준 source.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("여사원 투입기준마스터")
@Entity
@Table(name = "employee_input_criteria_master")
@SFObject("EmployeeInputCriteriaMaster__c")
class EmployeeInputCriteriaMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("여사원투입기준마스터ID")
    @Column(name = "employee_input_criteria_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("BifurcationHalfPersonStandard__c")
    @FieldName("격고0.5명 기준금액")
    @Column(name = "bifurcation_half_person_standard", precision = 18, scale = 0)
    var bifurcationHalfPersonStandard: BigDecimal? = null,

    @SFField("Boundary__c")
    @FieldName("경계율")
    @Column(name = "boundary", precision = 18, scale = 0)
    var boundary: BigDecimal? = null,

    @SFField("Category__c")
    @Column(name = "category_sfid", length = 18)
    var categorySfid: String? = null,

    @SFField("Confirmed__c")
    @FieldName("확정")
    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false,

    @SFField("StartDate__c")
    @FieldName("시작일")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @FieldName("종료일")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Fixed1PersonStandardAmount__c")
    @FieldName("고정1명 기준금액")
    @Column(name = "fixed_1_person_standard_amount", precision = 18, scale = 0)
    var fixed1PersonStandardAmount: BigDecimal? = null,

    @SFField("TypeOfWork1__c")
    @Convert(converter = TypeOfWork1Converter::class)
    @FieldName("근무형태1")
    @Column(name = "type_of_work_1", length = 255)
    var typeOfWork1: TypeOfWork1? = null,

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
    var isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: AccountCategoryMaster? = null,

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
) : BaseEntity() {

    /**
     * SF Formula 재현 — `Category__r.AccountCode__c`.
     * (sf-meta-diff Q4 — §6.7 위반 컬럼 제거 후 application 재현)
     */
    val accountCategorizedCode: String?
        get() = category?.accountCode

    /**
     * SF Formula 재현 — `BifurcationHalfPersonStandard__c - (BifurcationHalfPersonStandard__c * Boundary__c)`.
     * (sf-meta-diff Q4)
     *
     * `Boundary__c` 는 SF Percent(백분율) 타입 — SF formula 엔진이 산술식에서 자동 /100 평가한다 (예: 저장값 20 → 0.20).
     * 신규 boundary 컬럼은 SF 값(정수 %)을 그대로 받으므로 비율 환산 시 /100 보정 필수.
     */
    val bifurcationHalfPersonMinAmountInRealmRange: BigDecimal?
        get() {
            val standard = bifurcationHalfPersonStandard ?: return null
            val rate = boundary?.divide(BigDecimal(100), 6, RoundingMode.HALF_UP) ?: return null
            return standard.subtract(standard.multiply(rate))
        }

    /**
     * SF Formula 재현 — `Fixed1PersonStandardAmount__c - (Fixed1PersonStandardAmount__c * Boundary__c)`.
     * (sf-meta-diff Q4)
     *
     * `Boundary__c` 는 SF Percent(백분율) 타입 — SF formula 엔진이 산술식에서 자동 /100 평가한다 (예: 저장값 20 → 0.20).
     * 신규 boundary 컬럼은 SF 값(정수 %)을 그대로 받으므로 비율 환산 시 /100 보정 필수.
     */
    val fixed1PersonMinAmountInRealmRange: BigDecimal?
        get() {
            val standard = fixed1PersonStandardAmount ?: return null
            val rate = boundary?.divide(BigDecimal(100), 6, RoundingMode.HALF_UP) ?: return null
            return standard.subtract(standard.multiply(rate))
        }

    /**
     * SF Formula 재현 — TODAY() 와 start/end 비교로 "유효" / "예정" / "종료" 산출.
     * SF 측이 사용자 세션의 TODAY() 기반이라 호출 시점 java.time.LocalDate.now() 사용.
     * (sf-meta-diff Q4)
     */
    val validData: String?
        get() {
            val start = startDate ?: return null
            val today = LocalDate.now()
            val endOpenOrInRange = (today.isEqual(start) || today.isAfter(start)) &&
                (endDate == null || today.isEqual(endDate) || today.isBefore(endDate))
            return when {
                endOpenOrInRange -> "유효"
                start.isAfter(today) -> "예정"
                else -> "종료"
            }
        }
}
