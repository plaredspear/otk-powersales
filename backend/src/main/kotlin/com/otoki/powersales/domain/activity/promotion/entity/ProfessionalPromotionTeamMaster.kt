package com.otoki.powersales.domain.activity.promotion.entity

import com.otoki.powersales.domain.activity.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("전문행사조 마스터")
@Entity
@Table(name = "professional_promotion_team_master")
@SFObject("ProfessionalPromotionTeamMaster__c")
class ProfessionalPromotionTeamMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("전문행사조마스터ID")
    @Column(name = "professional_promotion_team_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @Column(name = "full_name_sfid", length = 18)
    var fullNameSfid: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @FieldName("전문행사조")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "team_type", length = 255)
    var teamType: ProfessionalPromotionTeamType? = null,

    // team_type 컬럼의 raw 문자열 read-only 매핑 — 컨버터를 거치지 않은 원본 값.
    // enum 이 legacyAlias('카레행사조') 로 흡수하는 값까지 저장 문자열 그대로 필터링해야 할 때 쓴다.
    // (예: '카레세일조' 필터가 DB 의 '카레세일조'·'카레행사조' 두 문자열을 IN 으로 함께 조회). 쓰기 금지.
    @Column(name = "team_type", length = 255, insertable = false, updatable = false)
    val teamTypeRaw: String? = null,

    @SFField("StartDate__c")
    @FieldName("시작일")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @SFField("EndDate__c")
    @FieldName("종료일")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @FieldName("확정")
    @Column(name = "is_confirmed", nullable = false)
    var isConfirmed: Boolean = false,

    @SFField("CostCenterCode__c")
    @FieldName("조직유형")
    @Column(name = "branch_code", length = 255)
    var branchCode: String? = null,

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
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    val account: Account? = null,

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

    fun update(
        teamType: ProfessionalPromotionTeamType,
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
        accountId: Long? = null,
        employeeId: Long? = null,
        branchCode: String? = null,
    ) {
        this.teamType = teamType
        this.startDate = startDate
        this.endDate = endDate
        this.isConfirmed = isConfirmed
        if (accountId != null) this.accountId = accountId
        // 사원 변경 시 employee_id + branch_code(소속 지점) 동반 갱신 —
        // SF BranchName__c(FullName__r.OrgName) formula 자동 재계산 동등.
        if (employeeId != null) {
            this.employeeId = employeeId
            this.branchCode = branchCode
        }
    }
}
