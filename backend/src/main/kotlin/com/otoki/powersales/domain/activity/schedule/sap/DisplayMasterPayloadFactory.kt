package com.otoki.powersales.domain.activity.schedule.sap

import com.otoki.powersales.external.sap.SapConstants
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 진열 마스터(DISPLAY) SAP 페이로드 빌더 (Spec #588 P2-B §2.2).
 *
 * 레거시 `Batch_TeamMemberMasterSchedule.cls:42-66` 페이로드 키 셋과 동일:
 * `CompanyCode`, `EmployeeCode`, `SAPAccountCode`, `WorkDate`,
 * `WorkingCategory1`, `WorkingCategory3`, `WorkingCategory5`.
 *
 * P1 과 키 셋이 다름 — `WorkingCategory2`/`WorkingCategory4` 없고 `WorkingCategory5` 있음.
 * `WorkDate` 는 배치 실행 시점의 today.
 */
@Component
class DisplayMasterPayloadFactory {

    fun build(rows: List<DisplayMasterSapPayloadRow>, today: LocalDate): DisplayMasterSapPayload {
        val workDate = today.format(DATE_FORMATTER)
        return DisplayMasterSapPayload(
            request = rows.map { toItem(it, workDate) }
        )
    }

    private fun toItem(row: DisplayMasterSapPayloadRow, workDate: String): DisplayMasterSapItem {
        return DisplayMasterSapItem(
            CompanyCode = SapConstants.OTOKI_COMPANY_CODE,
            EmployeeCode = row.employeeCode,
            SAPAccountCode = row.accountExternalKey,
            WorkDate = workDate,
            WorkingCategory1 = row.typeOfWork1,
            WorkingCategory3 = row.typeOfWork3,
            WorkingCategory5 = row.typeOfWork5
        )
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

/** SAP 호출 wrapper. JSON: `{ "request": [...] }`. */
data class DisplayMasterSapPayload(
    val request: List<DisplayMasterSapItem>
)

/** SAP 단일 element. 키 명명은 레거시 SF 그대로 (PascalCase). */
@Suppress("ConstructorParameterNaming", "PropertyName")
data class DisplayMasterSapItem(
    val CompanyCode: String,
    val EmployeeCode: String?,
    val SAPAccountCode: String?,
    val WorkDate: String,
    val WorkingCategory1: String?,
    val WorkingCategory3: String?,
    val WorkingCategory5: String?
)
