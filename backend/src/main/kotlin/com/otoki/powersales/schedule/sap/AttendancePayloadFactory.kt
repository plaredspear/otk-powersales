package com.otoki.powersales.schedule.sap

import com.otoki.powersales.external.sap.SapConstants
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 일반 출근(REGULAR) SAP 페이로드 빌더 (Spec #588 P1-B §2.2).
 *
 * 레거시 `Batch_TeamMemberSchedule.cls:43-62` 의 페이로드 키 셋과 동일 규칙:
 * - `WorkDate` 형식 `yyyyMMdd`
 * - `WorkingCategory4` 는 어제 보정 row 만 채움 (today row 는 항상 null)
 * - wrapper `{ "request": [ ... ] }` 는 [AttendanceSapPayload] 생성 책임
 */
@Component
class AttendancePayloadFactory {

    fun build(rows: List<AttendanceSapPayloadRow>, today: LocalDate): AttendanceSapPayload {
        return AttendanceSapPayload(
            request = rows.map { toItem(it, today) }
        )
    }

    private fun toItem(row: AttendanceSapPayloadRow, today: LocalDate): AttendanceSapItem {
        val isYesterdayCorrection = row.workingDate.isBefore(today)
        return AttendanceSapItem(
            CompanyCode = SapConstants.OTOKI_COMPANY_CODE,
            EmployeeCode = row.employeeCode,
            SAPAccountCode = row.accountExternalKey,
            WorkDate = row.workingDate.format(DATE_FORMATTER),
            WorkingCategory1 = row.workingCategory1?.displayName,
            WorkingCategory2 = row.workingCategory2?.displayName,
            WorkingCategory3 = row.workingCategory3?.displayName,
            WorkingCategory4 = if (isYesterdayCorrection) row.secondWorkType?.displayName else null
        )
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

/** SAP 호출 wrapper. JSON: `{ "request": [...] }`. */
data class AttendanceSapPayload(
    val request: List<AttendanceSapItem>
)

/** SAP 단일 element. 키 명명은 레거시 SF 그대로 (PascalCase). */
@Suppress("ConstructorParameterNaming", "PropertyName")
data class AttendanceSapItem(
    val CompanyCode: String,
    val EmployeeCode: String,
    val SAPAccountCode: String?,
    val WorkDate: String,
    val WorkingCategory1: String?,
    val WorkingCategory2: String?,
    val WorkingCategory3: String?,
    val WorkingCategory4: String?
)
