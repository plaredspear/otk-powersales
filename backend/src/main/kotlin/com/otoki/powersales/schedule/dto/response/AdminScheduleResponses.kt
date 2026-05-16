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
    val employeeCode: String,
    val employeeName: String,
    val accountCode: String?,
    val accountName: String?,
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
