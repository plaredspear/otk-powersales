package com.otoki.powersales.domain.org.employee.sfsync

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import java.time.LocalDate

/**
 * SF `StaffReview__c` 한 건의 fetch 결과 (사원평가 마스터 sync 입력).
 *
 * SF Apex/REST 응답을 역직렬화한 raw 표현. upsert 매칭 키는 SF 레코드 [sfid].
 * 사원(employee FK)은 [employeeSfid](= DKRetail_EmployeeId__c) 로 resolve 한다.
 *
 * audit FK([createdBySfid]/[lastModifiedBySfid])는 SF User Id buffer 로만 보존하며,
 * SF User → 신규 User 매핑(FK 연결)은 본 주기 sync 의 책임 밖이다 (SF→RDS 마이그레이션 Stage 권위).
 */
data class StaffReviewFetchDto(
    /** SF 18자리 레코드 Id. upsert 매칭 키. */
    val sfid: String?,
    /** SF Name (사원평가No). */
    val name: String?,
    /** SF DKRetail_EmployeeId__c (사원 SF Id). 신규 DB employee FK resolve 키. */
    val employeeSfid: String?,
    val employeeName: String?,
    val employeeCode: String?,
    val employeeType: String?,
    val entryDate: LocalDate?,
    /** SF BranchReviews__c (지점평가 SF Id) — buffer 보존. */
    val branchReviewSfid: String?,
    val employeeTotalScore: Double?,
    val jikwee: String?,
    val jobCode: String?,
    val firstDayOfMonth: LocalDate?,
    val branch: String?,
    val costCenterCode: String?,
    val workingCategory1: WorkingCategory1?,
    val workingCategory2: WorkingCategory2?,
    val workingCategory3: WorkingCategory3?,
    val displayEventGoalScore: Double?,
    val priorityItemEventScore: Double?,
    val productManageCallmentScore: Double?,
    val instructionDisobedienceScore: Double?,
    val accountPartnershipScore: Double?,
    val attendanceScore: Double?,
    val clothesHygieneScore: Double?,
    val educationEvaluationScore: Double?,
    /** SF CreatedById (SF User Id) — buffer 보존. */
    val createdBySfid: String?,
    /** SF LastModifiedById (SF User Id) — buffer 보존. */
    val lastModifiedBySfid: String?,
)
