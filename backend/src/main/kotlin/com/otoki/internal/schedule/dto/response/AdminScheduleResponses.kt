package com.otoki.internal.schedule.dto.response

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
