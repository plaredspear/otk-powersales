package com.otoki.powersales.schedule.dto.response

data class ScheduleUploadResultDto(
    val uploadId: String,
    val totalRows: Int,
    val successRows: Int,
    val errorRows: Int,
    val errors: List<RowError>,
    val previews: List<RowPreview>
)

data class RowError(
    val row: Int,
    val column: String,
    val field: String,
    val value: String?,
    val message: String
)

data class RowPreview(
    val row: Int,
    val employeeCode: String,
    val employeeName: String,
    val accountCode: String,
    val accountName: String,
    val typeOfWork3: String,
    val typeOfWork4: String,
    val typeOfWork5: String,
    val startDate: String,
    val endDate: String?
)

data class ScheduleConfirmResultDto(
    val insertedCount: Int
)

data class ScheduleListItemDto(
    val id: Long,
    val employeeId: Long?,
    val employeeCode: String,
    val employeeName: String,
    val branchName: String?,
    // 재직상태 (SF formula `ValidConditionData__c`) — 상세와 동일 계산값
    val employmentStatus: String?,
    val accountId: Int?,
    val accountCode: String?,
    val accountName: String?,
    // 거래처유형 (SF `Account.Type`)
    val accountType: String?,
    // 거래처상태 (SF `Account.AccountStatusName__c`)
    val accountStatus: String?,
    val typeOfWork3: String?,
    val typeOfWork4: String?,
    val typeOfWork5: String?,
    val startDate: java.time.LocalDate?,
    val endDate: java.time.LocalDate?,
    val confirmed: Boolean?,
    val costCenterCode: String?,
    val lastMonthRevenue: Long?
)

data class ScheduleBatchConfirmResultDto(
    val updatedCount: Int
)

/**
 * UC-03 단건 편집 모달 상세 — SF 「진열사원 스케줄 마스터」 레이아웃 정합.
 *
 * 편집 가능 필드 (좌측) + readonly 계산 정보 (우측, SF 「저장 시 이 필드가 계산됨」) 를 함께 반환한다.
 * readonly 정보 (사번/지점명/직위/재직상태/거래처코드/거래처상태/거래처유형/유효/유효데이터) 는
 * Employee/Account 조인 + SF formula 포팅으로 조회 시점에 계산 ([ScheduleDisplayStatusCalculator]).
 */
data class ScheduleDetailDto(
    // 식별 / 확정
    val id: Long,
    val name: String?,
    val confirmed: Boolean?,
    // 편집 가능 필드
    val employeeCode: String,
    val employeeName: String,
    val accountCode: String?,
    val accountName: String?,
    val typeOfWork1: String?,
    val typeOfWork3: String?,
    val typeOfWork4: String?,
    val typeOfWork5: String?,
    val startDate: java.time.LocalDate?,
    val endDate: java.time.LocalDate?,
    // readonly 계산 정보 (SF 「저장 시 이 필드가 계산됨」)
    val branchName: String?,
    val title: String?,
    val employmentStatus: String?,
    val accountStatus: String?,
    val accountType: String?,
    val valid: String?,
    val validData: String?,
    val costCenterCode: String?,
    val lastMonthRevenue: Long?,
)

/**
 * UC-07 일괄 삭제 결과 — partial success 지원.
 * 레거시 SF Mass Delete 의 row-level 결과 형식 동등 (성공 건 + 실패 건 사유 분리).
 */
data class ScheduleBatchDeleteResultDto(
    val deletedCount: Int,
    val failedCount: Int,
    val failures: List<ScheduleBatchDeleteFailure>
)

data class ScheduleBatchDeleteFailure(
    val id: Long,
    val errorCode: String,
    val message: String
)

data class ScheduleCreateResultDto(
    val id: Long,
    val employeeCode: String,
    val employeeName: String,
    val accountCode: String?,
    val accountName: String?,
    val typeOfWork3: String?,
    val typeOfWork4: String?,
    val typeOfWork5: String?,
    val startDate: java.time.LocalDate?,
    val endDate: java.time.LocalDate?,
    val costCenterCode: String?,
    val lastMonthRevenue: Long?
)
