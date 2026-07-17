package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * MFEIS(월별 여사원 통합일정) 외부 조회용 평면 projection.
 *
 * 필드 셋은 레거시 SF 여사원 통합일정 조회/엑셀 export
 * (ScheduleSearchByTeamMemberController / exportMonthlyFemaleEmployeeIntegrationSchedule.js) 가
 * 노출하던 컬럼 집합에 정합한다. 레거시는 이 SObject 에 외부 전송 API 가 없어 REST payload 선례는
 * 없으므로, "화면/엑셀에 실제 노출하던 필드" 를 외부 제공 필드의 기준으로 삼는다.
 *
 * MFEIS entity 전 컬럼 + account/employee fetch join(80여 컬럼) 대신, 필요한 관계 컬럼만
 * 명시적으로 projection select 하여 select 페이로드를 축소한다 (전량 스냅샷 배치 조회 부하 방지).
 *
 * [id] 는 keyset 커서(다음 페이지 조회 기준)로도 사용된다.
 */
data class MfeisScheduleRow(
    /** PK — keyset 커서 기준. */
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("sfid")
    val sfid: String?,

    @JsonProperty("externalKey")
    val externalKey: String?,

    @JsonProperty("year")
    val year: String?,

    @JsonProperty("month")
    val month: String?,

    @JsonProperty("costCenterCode")
    val costCenterCode: String?,

    // -- 사원(employee) --
    /** 소속 (레거시 export "소속" = FullName__r.OrgName__c). */
    @JsonProperty("orgName")
    val orgName: String?,

    /** 사번. */
    @JsonProperty("employeeCode")
    val employeeCode: String?,

    /** 이름. */
    @JsonProperty("employeeName")
    val employeeName: String?,

    /** 직위 (레거시 export "직위" = Title__c). */
    @JsonProperty("title")
    val title: String?,

    // -- 거래처(account) --
    /** 거래처코드 (레거시 export "거래처코드" = AccountCode__c → Account.ExternalKey). */
    @JsonProperty("accountCode")
    val accountCode: String?,

    /** 거래처명. */
    @JsonProperty("accountName")
    val accountName: String?,

    /** 거래처지점명. */
    @JsonProperty("accountBranchName")
    val accountBranchName: String?,

    /** 거래처 구분(Type). */
    @JsonProperty("accountType")
    val accountType: String?,

    /** ABC유형. */
    @JsonProperty("abcType")
    val abcType: String?,

    // -- 근무유형 --
    @JsonProperty("workingCategory1")
    val workingCategory1: String?,

    @JsonProperty("workingCategory3")
    val workingCategory3: String?,

    @JsonProperty("workingCategory4")
    val workingCategory4: String?,

    @JsonProperty("workingCategory5")
    val workingCategory5: String?,

    // -- 집계 수치 --
    /** 총 투입횟수. */
    @JsonProperty("numberOfInputs")
    val numberOfInputs: BigDecimal?,

    /** 총 환산근무일수. */
    @JsonProperty("equivalentNumberOfWorkingDays")
    val equivalentNumberOfWorkingDays: BigDecimal?,

    /** 총 환산인원. */
    @JsonProperty("convertedHeadcount")
    val convertedHeadcount: BigDecimal?
)
