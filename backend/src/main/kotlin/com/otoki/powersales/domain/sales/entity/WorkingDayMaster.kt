package com.otoki.powersales.domain.sales.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

/**
 * 영업일관리마스터 Entity (SF `WorkingDayMaster__c`).
 *
 * 운영이 직접 관리하는 영업일 달력 — `workingDateCheck = 1` 인 날짜가 영업일이다 (주말/공휴일은 0).
 * 월매출 현황 "기준 진도율"(레거시 SF `calcBusinessRateOnlyThisMonth`) 의 영업일수 산출 source.
 *
 * 데이터 권위: SF (HC sync 대상 아님 — Heroku PG 미존재. SF → RDS 단방향 마이그레이션 Stage1).
 * Name 은 SF AutoNumber(`WM-{00000000}`) — 적재 정합 위해 보존하되 조회엔 미사용.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "working_day_master")
@SFObject("WorkingDayMaster__c")
class WorkingDayMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "working_day_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("WorkingDate__c")
    @Column(name = "working_date")
    var workingDate: LocalDate? = null,

    /** SF `WorkingDateCheck__c` (Number). 1 = 영업일. SF describe type = double. */
    @SFField("WorkingDateCheck__c")
    @Column(name = "working_date_check")
    var workingDateCheck: Double? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // SF OwnerId.referenceTo = [Group, User] polymorphic. owner_user_id (User FK) +
    // owner_group_id (Group FK) + XOR CHECK. Stage 2 fk substep 이 owner_sfid → FK 해소.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    // SF CreatedById/LastModifiedById.referenceTo = [User]. audit FK → User.
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,
) : BaseEntity() {

    /** `workingDateCheck == 1` 이면 영업일 (주말/공휴일은 0). SF `WorkingDateCheck__c` 판정 동등. */
    fun isWorkingDay(): Boolean = workingDateCheck == WORKING_DAY_CHECK_VALUE

    companion object {
        /** 영업일 판정 임계값 — `WorkingDateCheck__c = 1`. */
        const val WORKING_DAY_CHECK_VALUE: Double = 1.0
    }
}
