package com.otoki.powersales.sales.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 영업일관리마스터 Entity (SF `WorkingDayMaster__c`).
 *
 * 운영이 직접 관리하는 영업일 달력 — `workingDateCheck = 1` 인 날짜가 영업일이다 (주말/공휴일은 0).
 * 월매출 현황 "기준 진도율"(레거시 SF `calcBusinessRateOnlyThisMonth`) 의 영업일수 산출 source.
 *
 * 데이터 권위: SF (HC sync 대상 아님 — Heroku PG 미존재. SF → RDS 단방향 마이그레이션 Stage1).
 * Name 은 SF AutoNumber(`WM-{00000000}`) — 적재 정합 위해 보존하되 조회엔 미사용.
 */
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

    /** SF `WorkingDateCheck__c` (Number, scale 0). 1 = 영업일. */
    @SFField("WorkingDateCheck__c")
    @Column(name = "working_date_check")
    var workingDateCheck: Int? = null,

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
) : BaseEntity()
