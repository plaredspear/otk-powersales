package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.RowError
import com.otoki.internal.admin.dto.response.RowPreview
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.User
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ScheduleUploadValidator {

    companion object {
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private val VALID_WORK_TYPE5 = setOf("상시", "임시")
    }

    data class ValidationResult(
        val errors: List<RowError>,
        val previews: List<RowPreview>,
        val validRows: List<ValidatedRow>
    )

    data class ValidatedRow(
        val userSfid: String,
        val accountSfid: String,
        val typeOfWork3: String,
        val typeOfWork5: String,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    fun validate(
        parsedRows: List<ScheduleExcelParser.ParsedRow>,
        usersByEmployeeId: Map<String, User>,
        accountsByExternalKey: Map<String, Account>,
        existingSchedules: List<DisplayWorkSchedule>
    ): ValidationResult {
        val errors = mutableListOf<RowError>()
        val previews = mutableListOf<RowPreview>()
        val validRows = mutableListOf<ValidatedRow>()
        // 파일 내 행들의 유효한 데이터 (V9, C1~C3 검증용)
        val validatedInFile = mutableListOf<FileRowData>()

        for (row in parsedRows) {
            val rowErrors = mutableListOf<RowError>()

            // 필수값 검증
            if (row.employeeCode.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "A", "사원번호", null, "행 ${row.rowNumber}: 사원번호는 필수입니다"))
            }
            if (row.accountCode.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "C", "거래처코드", null, "행 ${row.rowNumber}: 거래처코드는 필수입니다"))
            }
            if (row.typeOfWork3.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "E", "근무유형3", null, "행 ${row.rowNumber}: 근무유형3은 필수입니다"))
            }
            if (row.typeOfWork5.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "F", "근무유형5", null, "행 ${row.rowNumber}: 근무유형5는 필수입니다"))
            }
            if (row.startDateStr.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "G", "시작일", null, "행 ${row.rowNumber}: 시작일은 필수입니다"))
            } else if (row.startDate == null) {
                rowErrors.add(RowError(row.rowNumber, "G", "시작일", row.startDateStr, "행 ${row.rowNumber}: 유효하지 않은 날짜 형식 (yyyy-MM-dd)"))
            }
            if (!row.endDateStr.isNullOrBlank() && row.endDate == null) {
                rowErrors.add(RowError(row.rowNumber, "H", "종료일", row.endDateStr, "행 ${row.rowNumber}: 유효하지 않은 날짜 형식 (yyyy-MM-dd)"))
            }

            // 필수값 미달 시 다음 행으로
            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
                continue
            }

            val employeeCode = row.employeeCode!!
            val accountCode = row.accountCode!!
            val typeOfWork3 = row.typeOfWork3!!
            val typeOfWork5 = row.typeOfWork5!!
            val startDate = row.startDate!!
            val endDate = row.endDate

            // V1: 사원번호 존재
            val user = usersByEmployeeId[employeeCode]
            if (user == null) {
                rowErrors.add(RowError(row.rowNumber, "A", "사원번호", employeeCode, "사원번호 $employeeCode: 존재하지 않는 사원"))
            }

            // V2: 재직 상태
            if (user != null && user.status != "재직") {
                rowErrors.add(RowError(row.rowNumber, "A", "사원번호", employeeCode, "사원번호 $employeeCode: 퇴직한 사원"))
            }

            // V3: 거래처코드 존재
            val account = accountsByExternalKey[accountCode]
            if (account == null) {
                rowErrors.add(RowError(row.rowNumber, "C", "거래처코드", accountCode, "거래처코드 $accountCode: 존재하지 않는 거래처"))
            }

            // V5: 근무유형3 유효성
            if (typeOfWork3 !in VALID_WORK_TYPE3) {
                rowErrors.add(RowError(row.rowNumber, "E", "근무유형3", typeOfWork3, "행 ${row.rowNumber}: 유효하지 않은 근무유형3 '$typeOfWork3'"))
            }

            // V6: 근무유형5 유효성
            if (typeOfWork5 !in VALID_WORK_TYPE5) {
                rowErrors.add(RowError(row.rowNumber, "F", "근무유형5", typeOfWork5, "행 ${row.rowNumber}: 유효하지 않은 근무유형5 '$typeOfWork5'"))
            }

            // V7: 임시 + 순회만 허용
            if (typeOfWork5 == "임시" && typeOfWork3 != "순회") {
                rowErrors.add(RowError(row.rowNumber, "E", "근무유형3", typeOfWork3, "행 ${row.rowNumber}: 임시 배치는 순회만 가능"))
            }

            // V4: 시작일 <= 종료일
            if (endDate != null && startDate.isAfter(endDate)) {
                rowErrors.add(RowError(row.rowNumber, "G", "시작일", row.startDateStr, "행 ${row.rowNumber}: 시작일이 종료일보다 이후"))
            }

            // 기본 검증 실패 시 V8, V9, C1~C3 건너뜀
            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
                continue
            }

            val userSfid = user!!.sfid!!
            val accountSfid = account!!.sfid!!

            // V8: DB 기존 레코드와 기간 중복 검사
            val overlappingDb = existingSchedules.filter { schedule ->
                schedule.fullName == userSfid &&
                    schedule.account == accountSfid &&
                    periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
            }
            if (overlappingDb.isNotEmpty()) {
                rowErrors.add(
                    RowError(
                        row.rowNumber, "G", "시작일", row.startDateStr,
                        "행 ${row.rowNumber}: 기존 스케줄과 기간 중복 (사원: $employeeCode, 거래처: $accountCode)"
                    )
                )
            }

            // V9: 파일 내 행 간 중복 검사
            val overlappingFile = validatedInFile.filter { prev ->
                prev.userSfid == userSfid &&
                    prev.accountSfid == accountSfid &&
                    periodsOverlap(prev.startDate, prev.endDate, startDate, endDate)
            }
            if (overlappingFile.isNotEmpty()) {
                rowErrors.add(
                    RowError(
                        row.rowNumber, "G", "시작일", row.startDateStr,
                        "행 ${overlappingFile.first().rowNumber}과 행 ${row.rowNumber}: 파일 내 중복"
                    )
                )
            }

            // C1~C3: 근무유형 조합 규칙 (DB + 파일 내 선행 행)
            if (rowErrors.isEmpty()) {
                val sameEmployeeSamePeriod = existingSchedules.filter { schedule ->
                    schedule.fullName == userSfid &&
                        periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
                } + validatedInFile.filter { prev ->
                    prev.userSfid == userSfid &&
                        periodsOverlap(prev.startDate, prev.endDate, startDate, endDate)
                }.map { toScheduleLike(it) }

                val combinationError = checkCombinationRules(row.rowNumber, typeOfWork3, typeOfWork5, sameEmployeeSamePeriod)
                if (combinationError != null) {
                    rowErrors.add(combinationError)
                }
            }

            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
            } else {
                val validatedRow = ValidatedRow(
                    userSfid = userSfid,
                    accountSfid = accountSfid,
                    typeOfWork3 = typeOfWork3,
                    typeOfWork5 = typeOfWork5,
                    startDate = startDate,
                    endDate = endDate
                )
                validRows.add(validatedRow)
                validatedInFile.add(
                    FileRowData(
                        rowNumber = row.rowNumber,
                        userSfid = userSfid,
                        accountSfid = accountSfid,
                        typeOfWork3 = typeOfWork3,
                        typeOfWork5 = typeOfWork5,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
                previews.add(
                    RowPreview(
                        row = row.rowNumber,
                        employeeCode = employeeCode,
                        employeeName = user.name,
                        accountCode = accountCode,
                        accountName = account.name ?: "",
                        typeOfWork3 = typeOfWork3,
                        typeOfWork5 = typeOfWork5,
                        startDate = startDate.toString(),
                        endDate = endDate?.toString()
                    )
                )
            }
        }

        return ValidationResult(errors = errors, previews = previews, validRows = validRows)
    }

    private fun periodsOverlap(
        start1: LocalDate?,
        end1: LocalDate?,
        start2: LocalDate,
        end2: LocalDate?
    ): Boolean {
        if (start1 == null) return false
        // period1: [start1, end1 or ∞), period2: [start2, end2 or ∞)
        // Overlap: start1 <= (end2 or ∞) AND (end1 or ∞) >= start2
        val end1Effective = end1 ?: LocalDate.MAX
        val end2Effective = end2 ?: LocalDate.MAX
        return !start1.isAfter(end2Effective) && !end1Effective.isBefore(start2)
    }

    private data class FileRowData(
        val rowNumber: Int,
        val userSfid: String,
        val accountSfid: String,
        val typeOfWork3: String,
        val typeOfWork5: String,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    private data class ScheduleLike(
        val typeOfWork3: String?,
        val typeOfWork5: String?
    )

    private fun toScheduleLike(data: FileRowData) = ScheduleLike(data.typeOfWork3, data.typeOfWork5)

    private fun checkCombinationRules(
        rowNumber: Int,
        newType3: String,
        newType5: String,
        existingInPeriod: List<Any>
    ): RowError? {
        // Extract typeOfWork3 and typeOfWork5 from existing records
        val existingTypes = existingInPeriod.map { record ->
            when (record) {
                is DisplayWorkSchedule -> Pair(record.typeOfWork3, record.typeOfWork5)
                is ScheduleLike -> Pair(record.typeOfWork3, record.typeOfWork5)
                else -> Pair(null, null)
            }
        }

        // C1: 고정이 존재하면 다른 유형 추가 불가
        val hasFixed = existingTypes.any { it.first == "고정" }
        if (hasFixed) {
            return RowError(rowNumber, "E", "근무유형3", newType3, "행 $rowNumber: 해당 기간에 고정 배치가 이미 존재")
        }
        if (newType3 == "고정" && existingTypes.isNotEmpty()) {
            return RowError(rowNumber, "E", "근무유형3", newType3, "행 $rowNumber: 해당 기간에 고정 배치가 이미 존재")
        }

        // C2: 격고 최대 2개
        if (newType3 == "격고") {
            val existingAlternateCount = existingTypes.count { it.first == "격고" }
            if (existingAlternateCount >= 2) {
                return RowError(rowNumber, "E", "근무유형3", newType3, "행 $rowNumber: 격고 배치가 이미 2개 존재")
            }
        }

        // C3: 임시 최대 1개
        if (newType5 == "임시") {
            val existingTempCount = existingTypes.count { it.second == "임시" }
            if (existingTempCount >= 1) {
                return RowError(rowNumber, "F", "근무유형5", newType5, "행 $rowNumber: 임시 배치가 이미 존재")
            }
        }

        return null
    }
}
