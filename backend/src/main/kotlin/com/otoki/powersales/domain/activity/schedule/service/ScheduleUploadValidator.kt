package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.RowError
import com.otoki.powersales.domain.activity.schedule.dto.response.RowPreview
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.policy.ClosedAccountSalesExemption
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ScheduleUploadValidator {

    companion object {
        private val VALID_WORK_TYPE3 = setOf("고정", "격고", "순회")
        private val VALID_WORK_TYPE4 = setOf("상온", "냉동/냉장")
        private val VALID_WORK_TYPE5 = setOf("상시", "임시")
    }

    /**
     * V3a — 폐업 상태라서 등록을 막아야 하는 거래처인지.
     *
     * 면제 사유는 [ClosedAccountSalesExemption] 이 단일 출처이며 **거래처 lookup 게이팅과 동일 기준**이다
     * ([com.otoki.powersales.domain.foundation.account.repository.AccountRepositoryCustomImpl] 의
     * `lookupGating`). 기준이 갈라지면 "화면 검색으로는 나오는데 등록하면 반려" 또는 그 반대가 된다:
     * - `distribution` 비어 있지 않음 OR `abcTypeCode == 3062` — SF 원본 면제
     *   (`UplExcelBtnSchduleMasterController.cls:325-337` 정합). 종전 검증도 이 두 조건을 썼으나 조회 측이
     *   이를 반영하지 않아 **엑셀로는 등록되는데 화면에서는 찾을 수 없는** 불일치가 있었고, 조회 측을
     *   맞추는 것으로 해소했다.
     * - **당월·전월 마감실적 보유** ([salesExemptedAccountIds]) — 신규 추가 면제.
     *
     * SF 원본은 위 면제를 `accountGroup ∈ {1000,1010}` 안에서만 평가하지만, 계정그룹 조건은 V3a 이전에
     * 거래처 조회 단계에서 이미 걸러지므로 여기서 다시 보지 않는다.
     *
     * @param salesExemptedAccountIds 호출 측이
     *        [com.otoki.powersales.domain.foundation.account.service.ClosedAccountSalesExemptionResolver]
     *        로 미리 산출한 **매출 기준** 면제 대상 거래처 id 집합
     */
    private fun isBlockedClosedAccount(account: Account?, salesExemptedAccountIds: Set<Long>): Boolean {
        if (account == null) return false
        if (account.accountStatusName != ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED) return false
        if (ClosedAccountSalesExemption.isExemptByAccountAttributes(account)) return false
        return account.id !in salesExemptedAccountIds
    }

    data class ValidationResult(
        val errors: List<RowError>,
        val previews: List<RowPreview>,
        val validRows: List<ValidatedRow>
    )

    data class ValidatedRow(
        val userId: Long,
        val userEmployeeCode: String?,
        val accountId: Long,
        val typeOfWork3: String,
        val typeOfWork4: String,
        val typeOfWork5: String,
        val startDate: LocalDate,
        val endDate: LocalDate?,
        val costCenterCode: String? = null,
        val accountExternalKey: String? = null
    )

    /**
     * @param salesExemptedAccountIds 폐업이지만 당월·전월 매출 보유로 등록이 허용되는 거래처 id 집합
     *        ([isBlockedClosedAccount] 참조). 호출 측이 미리 산출해 주입한다. 기본값(빈 집합)은 **매출
     *        면제만 미적용**이라는 뜻으로, SF 원본 면제(distribution / ABC유형 3062)는 그대로 살아 있다 —
     *        주입을 빠뜨려도 SF 레거시 수준의 판정으로 떨어질 뿐 과대 차단되지 않는다.
     */
    fun validate(
        parsedRows: List<ScheduleExcelParser.ParsedRow>,
        usersByEmployeeCode: Map<String, Employee>,
        accountsByExternalKey: Map<String, Account>,
        existingSchedules: List<DisplayWorkSchedule>,
        salesExemptedAccountIds: Set<Long> = emptySet()
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
                rowErrors.add(RowError(row.rowNumber, "B", "사번", null, "행 ${row.rowNumber}: 사번은 필수입니다"))
            }
            if (row.accountCode.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "E", "거래처코드", null, "행 ${row.rowNumber}: 거래처코드는 필수입니다"))
            }
            if (row.typeOfWork3.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "G", "근무형태3", null, "행 ${row.rowNumber}: 근무형태3은 필수입니다"))
            }
            if (row.typeOfWork4.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "H", "근무형태4", null, "행 ${row.rowNumber}: 근무형태4는 필수 입력입니다"))
            }
            if (row.typeOfWork5.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "I", "근무형태5", null, "행 ${row.rowNumber}: 근무형태5는 필수입니다"))
            }
            if (row.startDateStr.isNullOrBlank()) {
                rowErrors.add(RowError(row.rowNumber, "J", "시작일", null, "행 ${row.rowNumber}: 시작일은 필수입니다"))
            } else if (row.startDate == null) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "J",
                        "시작일",
                        row.startDateStr,
                        "행 ${row.rowNumber}: 유효하지 않은 날짜 형식 (yyyy-MM-dd)"
                    )
                )
            }
            if (!row.endDateStr.isNullOrBlank() && row.endDate == null) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "K",
                        "종료일",
                        row.endDateStr,
                        "행 ${row.rowNumber}: 유효하지 않은 날짜 형식 (yyyy-MM-dd)"
                    )
                )
            }

            // 필수값 미달 시 다음 행으로
            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
                continue
            }

            val employeeCode = row.employeeCode!!
            val accountCode = row.accountCode!!
            val typeOfWork3 = row.typeOfWork3!!
            val typeOfWork4 = row.typeOfWork4!!
            val typeOfWork5 = row.typeOfWork5!!
            val startDate = row.startDate!!
            val endDate = row.endDate

            // V6b: 엑셀 대량 업로드에서는 '임시' 배치를 등록할 수 없다.
            // 임시(한시적) 배치는 반드시 화면에서 개별 등록해야 하므로, 업로드 파일에 '임시' 행이 있으면
            // 해당 행을 즉시 에러로 리포트하고 이후 검증(V7/V7a/C3 등 임시 세부 규칙)은 건너뛴다.
            // 단건 등록(validateSingle) 에는 적용하지 않는다 — '임시'는 화면 등록에서 계속 허용.
            if (typeOfWork5 == "임시") {
                errors.add(
                    RowError(
                        row.rowNumber,
                        "I",
                        "근무형태5",
                        typeOfWork5,
                        "행 ${row.rowNumber}: 엑셀 업로드로는 임시 배치를 등록할 수 없습니다"
                    )
                )
                continue
            }

            // V1: 사원번호 존재
            val employee = usersByEmployeeCode[employeeCode]
            if (employee == null) {
                rowErrors.add(RowError(row.rowNumber, "B", "사번", employeeCode, "사원번호 $employeeCode: 존재하지 않는 사원"))
            }

            // V2: 재직 상태 / 앱 로그인 활성화 — 종료일(endDate) 이 지난 경우만 차단 (유예 정책).
            // 레거시 SF DisplayWorkScheduleMasterTriggerHandler.retirementCheck() 와 동등:
            //   (Status != '재직' OR APPLoginActive != true) AND 오늘 > EndDate__c → 차단.
            // endDate 가 null 이거나 미래이면 퇴직 예정자 입력을 허용한다.
            if (employee != null) {
                val isInactive = employee.status != "재직" || employee.appLoginActive != true
                val endDate = employee.endDate
                if (isInactive && endDate != null && LocalDate.now().isAfter(endDate)) {
                    rowErrors.add(RowError(row.rowNumber, "B", "사번", employeeCode, "사원번호 $employeeCode: 퇴직한 사원"))
                }
            }

            // V3: 거래처코드 존재
            val account = accountsByExternalKey[accountCode]
            if (account == null) {
                rowErrors.add(RowError(row.rowNumber, "E", "거래처코드", accountCode, "거래처코드 $accountCode: 존재하지 않는 거래처"))
            }

            // V3a: 거래처 폐업 상태
            if (isBlockedClosedAccount(account, salesExemptedAccountIds)) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "E",
                        "거래처코드",
                        accountCode,
                        "거래처코드 $accountCode: 폐업 상태의 거래처입니다"
                    )
                )
            }

            // V5: 근무형태3 유효성
            if (typeOfWork3 !in VALID_WORK_TYPE3) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "G",
                        "근무형태3",
                        typeOfWork3,
                        "행 ${row.rowNumber}: 유효하지 않은 근무형태3 '$typeOfWork3'"
                    )
                )
            }

            // V5a: 근무형태4 유효성
            if (typeOfWork4 !in VALID_WORK_TYPE4) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "H",
                        "근무형태4",
                        typeOfWork4,
                        "행 ${row.rowNumber}: 유효하지 않은 근무형태4 '$typeOfWork4'"
                    )
                )
            }

            // V6: 근무형태5 유효성
            if (typeOfWork5 !in VALID_WORK_TYPE5) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "I",
                        "근무형태5",
                        typeOfWork5,
                        "행 ${row.rowNumber}: 유효하지 않은 근무형태5 '$typeOfWork5'"
                    )
                )
            }

            // V7: 임시 + 순회만 허용
            if (typeOfWork5 == "임시" && typeOfWork3 != "순회") {
                rowErrors.add(RowError(row.rowNumber, "G", "근무형태3", typeOfWork3, "행 ${row.rowNumber}: 임시 배치는 순회만 가능"))
            }

            // V7a: 임시 배치는 종료일 필수 (한시적 배치이므로 종료 시점이 반드시 지정되어야 함)
            if (typeOfWork5 == "임시" && endDate == null) {
                rowErrors.add(RowError(row.rowNumber, "K", "종료일", row.endDateStr, "행 ${row.rowNumber}: 임시 배치는 종료일이 필수입니다"))
            }

            // V4: 시작일 <= 종료일
            if (endDate != null && startDate.isAfter(endDate)) {
                rowErrors.add(
                    RowError(
                        row.rowNumber,
                        "J",
                        "시작일",
                        row.startDateStr,
                        "행 ${row.rowNumber}: 시작일이 종료일보다 이후"
                    )
                )
            }

            // 기본 검증 실패 시 V8, V9, C1~C3 건너뜀
            if (rowErrors.isNotEmpty()) {
                errors.addAll(rowErrors)
                continue
            }

            val userId = employee!!.id
            val userEmployeeCode = employee.employeeCode
            val accountIdVal = account!!.id

            // V8: DB 기존 레코드와 기간 중복 검사
            val overlappingDb = existingSchedules.filter { schedule ->
                schedule.employee?.id == userId &&
                    schedule.account?.id == accountIdVal &&
                    periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
            }
            if (overlappingDb.isNotEmpty()) {
                rowErrors.add(
                    RowError(
                        row.rowNumber, "J", "시작일", row.startDateStr,
                        "행 ${row.rowNumber}: 기존 스케줄과 기간 중복 (사원: $employeeCode, 거래처: $accountCode)"
                    )
                )
            }

            // V9: 파일 내 행 간 중복 검사
            val overlappingFile = validatedInFile.filter { prev ->
                prev.userId == userId &&
                    prev.accountId == accountIdVal &&
                    periodsOverlap(prev.startDate, prev.endDate, startDate, endDate)
            }
            if (overlappingFile.isNotEmpty()) {
                rowErrors.add(
                    RowError(
                        row.rowNumber, "J", "시작일", row.startDateStr,
                        "행 ${overlappingFile.first().rowNumber}과 행 ${row.rowNumber}: 파일 내 중복"
                    )
                )
            }

            // C1~C3: 근무유형 조합 규칙 (DB + 파일 내 선행 행)
            if (rowErrors.isEmpty()) {
                val sameEmployeeSamePeriod = existingSchedules.filter { schedule ->
                    schedule.employee?.id == userId &&
                        periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
                } + validatedInFile.filter { prev ->
                    prev.userId == userId &&
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
                    userId = userId,
                    userEmployeeCode = userEmployeeCode,
                    accountId = accountIdVal,
                    typeOfWork3 = typeOfWork3,
                    typeOfWork4 = typeOfWork4,
                    typeOfWork5 = typeOfWork5,
                    startDate = startDate,
                    endDate = endDate,
                    costCenterCode = employee.costCenterCode,
                    accountExternalKey = accountCode
                )
                validRows.add(validatedRow)
                validatedInFile.add(
                    FileRowData(
                        rowNumber = row.rowNumber,
                        userId = userId,
                        userEmployeeCode = userEmployeeCode,
                        accountId = accountIdVal,
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
                        employeeName = employee.name,
                        accountCode = accountCode,
                        accountName = account.name ?: "",
                        typeOfWork3 = typeOfWork3,
                        typeOfWork4 = typeOfWork4,
                        typeOfWork5 = typeOfWork5,
                        startDate = startDate.toString(),
                        endDate = endDate?.toString()
                    )
                )
            }
        }

        return ValidationResult(errors = errors, previews = previews, validRows = validRows)
    }

    /**
     * 단건 등록 검증.
     * 레거시 DisplayWorkScheduleMasterTriggerHandler before insert 의 검증 룰을 단건에 적용한다.
     * 위반 시 SingleValidationFailure(`messages` 목록) 반환. 정상이면 ValidatedRow 반환.
     */
    fun validateSingle(
        employeeCode: String,
        accountCode: String,
        typeOfWork3: String,
        typeOfWork4: String,
        typeOfWork5: String,
        startDate: LocalDate,
        endDate: LocalDate?,
        employee: Employee?,
        account: Account?,
        existingSchedules: List<DisplayWorkSchedule>,
        excludeScheduleId: Long? = null,
        maxAttendedWorkingDate: LocalDate? = null,
        salesExemptedAccountIds: Set<Long> = emptySet()
    ): SingleValidationResult {
        // 편집 시나리오에서는 자기 자신을 중복 검사 대상에서 제외 (UC-03 동일 레코드 update)
        val filteredExisting = if (excludeScheduleId != null) {
            existingSchedules.filter { it.id != excludeScheduleId }
        } else {
            existingSchedules
        }
        val messages = mutableListOf<String>()

        // V1: 사원번호 존재
        if (employee == null) {
            messages.add("사원번호 $employeeCode: 존재하지 않는 사원")
        }

        // V2: 재직 상태 / 앱 로그인 — 종료일(endDate) 이 지난 경우만 차단 (유예 정책).
        if (employee != null) {
            val isInactive = employee.status != "재직" || employee.appLoginActive != true
            val empEndDate = employee.endDate
            if (isInactive && empEndDate != null && LocalDate.now().isAfter(empEndDate)) {
                messages.add("사원번호 $employeeCode: 퇴직한 사원")
            }
        }

        // V3: 거래처코드 존재
        if (account == null) {
            messages.add("거래처코드 $accountCode: 존재하지 않는 거래처")
        }

        // V3a: 거래처 폐업 상태
        if (isBlockedClosedAccount(account, salesExemptedAccountIds)) {
            messages.add("거래처코드 $accountCode: 폐업 상태의 거래처입니다")
        }

        // V5: 근무형태3 유효성
        if (typeOfWork3 !in VALID_WORK_TYPE3) {
            messages.add("유효하지 않은 근무형태3 '$typeOfWork3'")
        }

        // V5a: 근무형태4 유효성
        if (typeOfWork4 !in VALID_WORK_TYPE4) {
            messages.add("유효하지 않은 근무형태4 '$typeOfWork4'")
        }

        // V6: 근무형태5 유효성
        if (typeOfWork5 !in VALID_WORK_TYPE5) {
            messages.add("유효하지 않은 근무형태5 '$typeOfWork5'")
        }

        // V7: 임시 + 순회만 허용
        if (typeOfWork5 == "임시" && typeOfWork3 != "순회") {
            messages.add("임시 배치는 순회만 가능합니다")
        }

        // V7a: 임시 배치는 종료일 필수 (한시적 배치이므로 종료 시점이 반드시 지정되어야 함)
        if (typeOfWork5 == "임시" && endDate == null) {
            messages.add("임시 배치는 종료일이 필수입니다")
        }

        // V4: 시작일 <= 종료일
        if (endDate != null && startDate.isAfter(endDate)) {
            messages.add("시작일이 종료일보다 이후입니다")
        }

        // V4a: 종료일 >= 이미 출근보고된 최종 근무일 (편집 시나리오 전용 — 신규 제약).
        // 종료일을 출근보고된 근무일보다 앞으로 당기면 해당 근무 행이 마스터 기간 밖으로 밀려나
        // 조회/배치 모수에서 사라지므로(고아 행) 차단한다. 미래로 연장하거나 종료일을 아예
        // 비우는 것(null = 무기한)은 근무 행을 배제하지 않으므로 허용한다.
        // 레거시 SF 는 `dateCheck()` 에 시작일<=종료일 규칙만 있어 이 제약이 없다 (의도적 신규 제약).
        if (endDate != null && maxAttendedWorkingDate != null && endDate.isBefore(maxAttendedWorkingDate)) {
            messages.add("종료일은 이미 근무등록된 최종 근무일($maxAttendedWorkingDate) 이후여야 합니다")
        }

        // 기본 검증 실패 시 V8 / C1~C3 skip
        if (messages.isNotEmpty() || employee == null || account == null) {
            return SingleValidationResult(messages = messages, validatedRow = null)
        }

        val userId = employee.id
        val accountIdVal = account.id

        // V8: DB 기존 레코드와 기간 중복 검사 (동일 사원 + 동일 거래처)
        val overlappingDb = filteredExisting.filter { schedule ->
            schedule.employee?.id == userId &&
                schedule.account?.id == accountIdVal &&
                periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
        }
        if (overlappingDb.isNotEmpty()) {
            messages.add("기간내에 동일한 거래처가 등록되어 있습니다")
        }

        // C1~C3: 동일 사원 + 동일 기간 근무유형 조합 규칙
        if (messages.isEmpty()) {
            val sameEmployeeSamePeriod = filteredExisting.filter { schedule ->
                schedule.employee?.id == userId &&
                    periodsOverlap(schedule.startDate, schedule.endDate, startDate, endDate)
            }

            // 레거시 트리거는 상시(고정/격고/순회) 카운팅에만 `ValidData__c == '유효'` 를 요구하고
            // (DisplayWorkScheduleMasterTriggerHandler.cls:152,155,158), 임시는 else 분기에서 필터
            // 없이 전건 카운트한다 (같은 파일 :161-163). 이 비대칭을 그대로 재현하기 위해 판정 집합을
            // 둘로 나눈다: 상시 카운트는 유효 집합, 임시 카운트는 필터 없는 원본 집합.
            val today = LocalDate.now()
            val existingTypes = sameEmployeeSamePeriod
                .filter { isValidData(it, today) }
                .map { Pair(it.typeOfWork3?.displayName, it.typeOfWork5?.displayName) }
            val existingTypesUnfiltered = sameEmployeeSamePeriod.map {
                Pair(it.typeOfWork3?.displayName, it.typeOfWork5?.displayName)
            }

            val counts = countTypes(regularSource = existingTypes, temporarySource = existingTypesUnfiltered)
            evaluateCombinationRules(typeOfWork3, typeOfWork5, counts)?.let { messages.add(it.message) }
        }

        if (messages.isNotEmpty()) {
            return SingleValidationResult(messages = messages, validatedRow = null)
        }

        val validatedRow = ValidatedRow(
            userId = userId,
            userEmployeeCode = employee.employeeCode,
            accountId = accountIdVal,
            typeOfWork3 = typeOfWork3,
            typeOfWork4 = typeOfWork4,
            typeOfWork5 = typeOfWork5,
            startDate = startDate,
            endDate = endDate,
            costCenterCode = employee.costCenterCode,
            accountExternalKey = accountCode
        )
        return SingleValidationResult(messages = emptyList(), validatedRow = validatedRow)
    }

    data class SingleValidationResult(
        val messages: List<String>,
        val validatedRow: ValidatedRow?
    )

    /**
     * SF formula `ValidData__c == '유효'` 판정.
     *
     * 원본 formula (DisplayWorkScheduleMaster__c/fields/ValidData__c.field-meta.xml) 의 '유효' 분기는
     * 아래 두 갈래의 OR 이며, 기간 조건은 양쪽 공통이다.
     *   ① 사원 재직 AND 기간이 오늘을 포함
     *   ② (사원 퇴직 OR appLoginActive=false) AND 기간이 오늘을 포함 AND 사원 종료일 >= 오늘
     *
     * 즉 스케줄이 오늘 시점에 진행 중이어야 하며, 미래 시작('예정')이나 이미 종료('종료')된 스케줄은
     * 충돌 후보에서 제외된다. 신규에는 ValidData 컬럼이 없으므로 동일 조건을 계산으로 재현한다.
     */
    private fun isValidData(schedule: DisplayWorkSchedule, today: LocalDate): Boolean {
        val start = schedule.startDate ?: return false
        // 기간이 오늘을 포함: startDate <= today AND (endDate IS NULL OR today <= endDate)
        if (start.isAfter(today)) return false
        val end = schedule.endDate
        if (end != null && end.isBefore(today)) return false

        val employee = schedule.employee ?: return false
        if (employee.status == "재직") return true
        // 퇴직 / 앱 비활성 사원은 사원 종료일이 아직 지나지 않은 경우에만 '유효'
        val isRetiredOrInactive = employee.status == "퇴직" || employee.appLoginActive == false
        if (!isRetiredOrInactive) return false
        val employeeEndDate = employee.endDate ?: return false
        return !employeeEndDate.isBefore(today)
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
        val userId: Long,
        val userEmployeeCode: String?,
        val accountId: Long,
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
                is DisplayWorkSchedule -> Pair(record.typeOfWork3?.displayName, record.typeOfWork5?.displayName)
                is ScheduleLike -> Pair(record.typeOfWork3, record.typeOfWork5)
                else -> Pair(null, null)
            }
        }

        // 레거시 엑셀 경로(UplExcelBtnSchduleMasterController.cls:456-468)는 트리거와 달리
        // ValidData 필터 없이 전건을 카운트한다. 이 비대칭은 레거시 자체의 것이므로 그대로 둔다.
        val counts = countTypes(regularSource = existingTypes, temporarySource = existingTypes)
        val violation = evaluateCombinationRules(newType3, newType5, counts) ?: return null
        val value = if (violation == CombinationViolation.TEMPORARY_EXISTS) newType5 else newType3

        return RowError(rowNumber, violation.column, violation.fieldName, value, "행 $rowNumber: ${violation.message}")
    }

    /**
     * 겹치는 기간의 기존 레코드를 레거시 카운터 4종으로 환산한다.
     * (DisplayWorkScheduleMasterTriggerHandler.cls:150-164)
     *
     * 상시(고정/격고/순회) 카운트와 임시 카운트의 모집단이 경로별로 다르므로 인자로 분리해 받는다.
     * - 단건 등록: 상시 = ValidData '유효' 집합, 임시 = 원본 집합 (레거시 트리거 :152,155,158 vs :161-163)
     * - 엑셀 업로드: 양쪽 모두 원본 집합 (레거시 엑셀 컨트롤러 :456-468)
     */
    private fun countTypes(
        regularSource: List<Pair<String?, String?>>,
        temporarySource: List<Pair<String?, String?>>
    ): TypeCounts {
        val regular = regularSource.filter { it.second == "상시" }
        return TypeCounts(
            fixed = regular.count { it.first == "고정" },
            alternate = regular.count { it.first == "격고" },
            patrol = regular.count { it.first == "순회" },
            // 레거시는 '상시'가 아닌 전건을 임시로 카운트한다 (:161-163 else 분기)
            temporary = temporarySource.count { it.second != "상시" }
        )
    }

    /**
     * 근무유형 조합 규칙 (C1~C3) — 레거시 트리거 :166-189 의 분기 구조를 그대로 재현한다.
     *
     * **바깥 분기는 신규 레코드의 근무형태5 이며 상시/임시가 배타적이다.** 신규가 '임시' 면
     * 고정/격고/순회 충돌 규칙(C1/C2/C2a)은 아예 평가되지 않고 '임시 1건' 규칙(C3)만 적용된다.
     * 즉 같은 기간에 고정 배치가 있어도 임시(순회) 배치는 등록할 수 있다.
     */
    private fun evaluateCombinationRules(
        newType3: String,
        newType5: String,
        counts: TypeCounts
    ): CombinationViolation? {
        // 레거시 :185-189 — 신규가 임시면 임시 1건 규칙만 본다
        if (newType5 != "상시") {
            return if (counts.temporary > 0) CombinationViolation.TEMPORARY_EXISTS else null
        }

        // 레거시 :167-171 — 신규가 상시+고정이면 상시 배치가 하나라도 있으면 차단
        if (newType3 == "고정") {
            val anyRegular = counts.fixed + counts.alternate + counts.patrol
            return if (anyRegular > 0) CombinationViolation.FIXED_WITH_OTHERS else null
        }

        // 레거시 :172-184 — 신규가 상시+격고/순회
        if (counts.fixed > 0) return CombinationViolation.FIXED_EXISTS
        if (counts.alternate >= 2) return CombinationViolation.ALTERNATE_LIMIT
        if (newType3 == "격고" && counts.patrol > 0 && counts.alternate >= 1) {
            return CombinationViolation.ALTERNATE_WITH_PATROL
        }

        return null
    }

    private data class TypeCounts(
        val fixed: Int,
        val alternate: Int,
        val patrol: Int,
        val temporary: Int
    )

    private enum class CombinationViolation(
        val message: String,
        val column: String,
        val fieldName: String
    ) {
        FIXED_EXISTS("해당 기간에 고정 배치가 이미 존재합니다", "G", "근무형태3"),
        FIXED_WITH_OTHERS("해당 기간에 다른 배치가 존재하여 고정을 추가할 수 없습니다", "G", "근무형태3"),
        ALTERNATE_LIMIT("격고 배치가 이미 2개 존재합니다", "G", "근무형태3"),
        ALTERNATE_WITH_PATROL("순회 레코드가 존재하므로 격고는 1건만 등록 가능합니다", "G", "근무형태3"),
        TEMPORARY_EXISTS("임시 배치가 이미 존재합니다", "I", "근무형태5")
    }
}
