package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountSnapshotRow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 거래처(Account) 외부 조회용 평면 row — **entity 전 컬럼 노출**.
 *
 * MFEIS([MfeisScheduleRow]) 는 레거시 SF 화면/엑셀 노출 컬럼에 정합하는 축소 필드 셋이지만, 본 DTO 는
 * "해당 엔티티의 필드를 모두 조회" 요구에 따라 Account entity 의 매핑 컬럼 전량을 노출한다.
 * (SF 레거시에는 거래처를 외부로 인출하는 REST/아웃바운드 선례가 없어 정합 기준으로 삼을 필드 셋이 없다 —
 *  레거시 목록 컨트롤러는 `Id, Name, ExternalKey__c, BranchName__c, BranchCode__c, DivisionName__c,
 *  SalesDeptName__c` 7개만 노출했으나 외부 제공 목적에는 부족하다.)
 *
 * 관계(ownerUser / createdBy / lastModifiedBy / parent)는 **객체를 펼치지 않고** FK id 와 함께 entity 가
 * 이미 보유한 `*_sfid` 컬럼으로만 노출한다. LAZY 관계를 직렬화하면 row 마다 추가 쿼리(N+1)가 발생하고,
 * `User` 를 통째로 내보내면 조회 목적과 무관한 계정 정보까지 노출되기 때문이다.
 * FK id 는 entity 의 관계 필드가 아니라 [AccountSnapshotRow] 가 쿼리에서 함께 가져온 값을 쓴다 —
 * 관계 필드에 의존하지 않아야 대량 조회에서 추가 쿼리 여지가 원천적으로 없다.
 *
 * [id] 는 keyset 커서(다음 페이지 조회 기준)로도 사용된다.
 */
data class AccountRow(
    /** PK — keyset 커서 기준. */
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("sfid")
    val sfid: String?,

    @JsonProperty("name")
    val name: String?,

    @JsonProperty("phone")
    val phone: String?,

    @JsonProperty("mobilePhone")
    val mobilePhone: String?,

    @JsonProperty("address1")
    val address1: String?,

    @JsonProperty("address2")
    val address2: String?,

    @JsonProperty("representative")
    val representative: String?,

    @JsonProperty("abcType")
    val abcType: String?,

    @JsonProperty("abcTypeCode")
    val abcTypeCode: String?,

    /** SAP 거래처코드. */
    @JsonProperty("externalKey")
    val externalKey: String?,

    @JsonProperty("accountGroup")
    val accountGroup: String?,

    @JsonProperty("branchCode")
    val branchCode: String?,

    @JsonProperty("branchName")
    val branchName: String?,

    @JsonProperty("zipCode")
    val zipCode: String?,

    @JsonProperty("latitude")
    val latitude: String?,

    @JsonProperty("longitude")
    val longitude: String?,

    @JsonProperty("geocodeUnresolved")
    val geocodeUnresolved: Boolean?,

    @JsonProperty("closingTime1")
    val closingTime1: String?,

    @JsonProperty("closingTime2")
    val closingTime2: String?,

    @JsonProperty("closingTime3")
    val closingTime3: String?,

    /** 업종 — DB 저장값과 동일한 SF 원본 옵션값(displayName). */
    @JsonProperty("industry")
    val industry: String?,

    @JsonProperty("werk1Tx")
    val werk1Tx: String?,

    @JsonProperty("werk2Tx")
    val werk2Tx: String?,

    @JsonProperty("werk3Tx")
    val werk3Tx: String?,

    @JsonProperty("isDeleted")
    val isDeleted: Boolean?,

    @JsonProperty("accountType")
    val accountType: String?,

    @JsonProperty("accountStatusName")
    val accountStatusName: String?,

    /** 담당 영업사원 사번. */
    @JsonProperty("employeeCode")
    val employeeCode: String?,

    @JsonProperty("distribution")
    val distribution: String?,

    @JsonProperty("accountStatusCode")
    val accountStatusCode: String?,

    @JsonProperty("businessType")
    val businessType: String?,

    @JsonProperty("businessCategory")
    val businessCategory: String?,

    @JsonProperty("businessLicenseNumber")
    val businessLicenseNumber: String?,

    @JsonProperty("email")
    val email: String?,

    @JsonProperty("divisionName")
    val divisionName: String?,

    @JsonProperty("salesDeptName")
    val salesDeptName: String?,

    @JsonProperty("consignmentAcc")
    val consignmentAcc: String?,

    @JsonProperty("werk1")
    val werk1: String?,

    @JsonProperty("werk2")
    val werk2: String?,

    @JsonProperty("werk3")
    val werk3: String?,

    @JsonProperty("salesDeptCostCenter")
    val salesDeptCostCenter: String?,

    @JsonProperty("divisionCostCenter")
    val divisionCostCenter: String?,

    @JsonProperty("accountNumber")
    val accountNumber: String?,

    @JsonProperty("site")
    val site: String?,

    /** 거래처소스 — DB 저장값과 동일한 SF 원본 옵션값(displayName). */
    @JsonProperty("accountSource")
    val accountSource: String?,

    @JsonProperty("branchCostCenter")
    val branchCostCenter: String?,

    @JsonProperty("divisionCode")
    val divisionCode: String?,

    @JsonProperty("salesDeptCode")
    val salesDeptCode: String?,

    @JsonProperty("logisticsName")
    val logisticsName: String?,

    @JsonProperty("logisticsCode")
    val logisticsCode: String?,

    @JsonProperty("freezerInstalled")
    val freezerInstalled: Boolean?,

    /** 냉장고종류 — DB 저장값과 동일한 SF 원본 옵션값(displayName). */
    @JsonProperty("freezerType")
    val freezerType: String?,

    @JsonProperty("remainingCredit")
    val remainingCredit: BigDecimal?,

    @JsonProperty("totalCredit")
    val totalCredit: BigDecimal?,

    @JsonProperty("mapCoordinate")
    val mapCoordinate: String?,

    @JsonProperty("orderEndTime")
    val orderEndTime: LocalTime?,

    @JsonProperty("firstInstalled")
    val firstInstalled: LocalDate?,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("website")
    val website: String?,

    @JsonProperty("fax")
    val fax: String?,

    @JsonProperty("annualRevenue")
    val annualRevenue: BigDecimal?,

    @JsonProperty("numberOfEmployees")
    val numberOfEmployees: BigDecimal?,

    /** 등급 — DB 저장값과 동일한 SF 원본 옵션값(displayName). */
    @JsonProperty("rating")
    val rating: String?,

    /** 소유형태 — DB 저장값과 동일한 SF 원본 옵션값(displayName). */
    @JsonProperty("ownership")
    val ownership: String?,

    @JsonProperty("isPriorityRecord")
    val isPriorityRecord: Boolean?,

    // -- 관계 FK (객체 미전개 — sfid + FK id 만) --

    @JsonProperty("parentSfid")
    val parentSfid: String?,

    @JsonProperty("parentId")
    val parentId: Long?,

    @JsonProperty("ownerSfid")
    val ownerSfid: String?,

    @JsonProperty("ownerUserId")
    val ownerUserId: Long?,

    @JsonProperty("createdBySfid")
    val createdBySfid: String?,

    @JsonProperty("createdById")
    val createdById: Long?,

    @JsonProperty("lastModifiedBySfid")
    val lastModifiedBySfid: String?,

    @JsonProperty("lastModifiedById")
    val lastModifiedById: Long?,

    // -- audit (BaseEntity) --

    @JsonProperty("createdAt")
    val createdAt: LocalDateTime?,

    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * 스냅샷 row(entity + 관계 FK) → 외부 노출 row 변환.
         *
         * entity 의 관계 필드는 **읽지 않는다** — FK 는 [snapshot] 이 쿼리에서 이미 가져왔다.
         */
        fun from(snapshot: AccountSnapshotRow): AccountRow = with(snapshot.account) { AccountRow(
            id = id,
            sfid = sfid,
            name = name,
            phone = phone,
            mobilePhone = mobilePhone,
            address1 = address1,
            address2 = address2,
            representative = representative,
            abcType = abcType,
            abcTypeCode = abcTypeCode,
            externalKey = externalKey,
            accountGroup = accountGroup,
            branchCode = branchCode,
            branchName = branchName,
            zipCode = zipCode,
            latitude = latitude,
            longitude = longitude,
            geocodeUnresolved = geocodeUnresolved,
            closingTime1 = closingTime1,
            closingTime2 = closingTime2,
            closingTime3 = closingTime3,
            industry = industry?.displayName,
            werk1Tx = werk1Tx,
            werk2Tx = werk2Tx,
            werk3Tx = werk3Tx,
            isDeleted = isDeleted,
            accountType = accountType,
            accountStatusName = accountStatusName,
            employeeCode = employeeCode,
            distribution = distribution,
            accountStatusCode = accountStatusCode,
            businessType = businessType,
            businessCategory = businessCategory,
            businessLicenseNumber = businessLicenseNumber,
            email = email,
            divisionName = divisionName,
            salesDeptName = salesDeptName,
            consignmentAcc = consignmentAcc,
            werk1 = werk1,
            werk2 = werk2,
            werk3 = werk3,
            salesDeptCostCenter = salesDeptCostCenter,
            divisionCostCenter = divisionCostCenter,
            accountNumber = accountNumber,
            site = site,
            accountSource = accountSource?.displayName,
            branchCostCenter = branchCostCenter,
            divisionCode = divisionCode,
            salesDeptCode = salesDeptCode,
            logisticsName = logisticsName,
            logisticsCode = logisticsCode,
            freezerInstalled = freezerInstalled,
            freezerType = freezerType?.displayName,
            remainingCredit = remainingCredit,
            totalCredit = totalCredit,
            mapCoordinate = mapCoordinate,
            orderEndTime = orderEndTime,
            firstInstalled = firstInstalled,
            description = description,
            website = website,
            fax = fax,
            annualRevenue = annualRevenue,
            numberOfEmployees = numberOfEmployees,
            rating = rating?.displayName,
            ownership = ownership?.displayName,
            isPriorityRecord = isPriorityRecord,
            parentSfid = parentSfid,
            parentId = snapshot.parentId,
            ownerSfid = ownerSfid,
            ownerUserId = snapshot.ownerUserId,
            createdBySfid = createdBySfid,
            createdById = snapshot.createdById,
            lastModifiedBySfid = lastModifiedBySfid,
            lastModifiedById = snapshot.lastModifiedById,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ) }
    }
}
