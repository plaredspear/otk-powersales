package com.otoki.internal.admin.dto.response

data class BranchDto(
    val costCenterCode: String,
    val branchName: String
)

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
