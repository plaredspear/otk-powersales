package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkItem
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterConfirmByIdsRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResultItem
import com.otoki.powersales.domain.activity.promotion.dto.response.ConfirmByIdsResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterResponse
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterAccountNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterBulkValidationFailedException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterDuplicateException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterEmployeeNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterInvalidDateRangeException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterNotFoundException
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistorySearchResult
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminPPTMasterService(
    private val pptMasterRepository: PPTMasterRepository,
    private val pptHistoryRepository: PPTHistoryRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val pptHistoryExcelExporter: PPTHistoryExcelExporter
) {

    companion object {
        private const val BULK_MAX_SIZE = 450
        private const val EXPORT_MAX_ROWS = 50_000
    }

    fun getMasters(
        scope: DataScope,
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        validOnly: Boolean,
        pageable: Pageable
    ): PPTMasterListResponse {
        // 지점 스코프 — 여사원 현황/일정 화면과 동일하게 본인 소속 지점만 노출 (전사 권한은 전체).
        // 데이터의 branch_code 컬럼은 비어 있으므로 사원 소속 지점(costCenterCode) 기준으로 평가한다.
        val branchCodeFilter = when (val result = scope.effectiveBranchCodes(branchCode?.takeIf { it.isNotBlank() })) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyMasterList(pageable)
        }
        val teamTypeEnum = ProfessionalPromotionTeamType.fromDisplayNameOrNull(teamType)
        val page = pptMasterRepository.searchMasters(
            employeeName, employeeCode, teamTypeEnum, branchCodeFilter, validOnly, LocalDate.now(), pageable
        )
        return PPTMasterListResponse(
            content = page.content.map { PPTMasterResponse.from(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size
        )
    }

    private fun emptyMasterList(pageable: Pageable): PPTMasterListResponse =
        PPTMasterListResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = pageable.pageNumber,
            size = pageable.pageSize
        )

    private fun emptyHistoryList(pageable: Pageable): PPTMasterHistoryListResponse =
        PPTMasterHistoryListResponse(
            content = emptyList(),
            totalElements = 0,
            totalPages = 0,
            number = pageable.pageNumber,
            size = pageable.pageSize
        )

    fun getMaster(id: Long): PPTMasterResponse {
        val master = findMasterById(id)
        val employee = master.employeeId?.let { employeeRepository.findById(it).orElse(null) }
        val account = master.accountId?.let { accountRepository.findById(it).orElse(null) }
        return PPTMasterResponse.from(
            master, employee?.employeeCode, employee?.name,
            account?.externalKey, account?.name,
            branchName = employee?.orgName,
            employeeStatus = employee?.status,
            employeeAppLoginActive = employee?.appLoginActive,
            employeeEndDate = employee?.endDate,
            accountType = account?.accountType
        )
    }

    @Transactional
    fun createMaster(request: PPTMasterCreateRequest): PPTMasterResponse {
        val employee = findEmployeeById(request.employeeId)
        val account = findAccountById(request.accountId)

        validateDateRange(request.startDate, request.endDate)
        validateNoDuplicate(request.employeeId, request.accountId, request.teamType, request.startDate)

        // 동일 사원의 다른 teamType 유효 마스터 자동 종료
        autoTerminateExistingMasters(request.employeeId, request.startDate)

        val master = pptMasterRepository.save(
            ProfessionalPromotionTeamMaster(
                name = generateMasterName(),
                employeeId = request.employeeId,
                accountId = request.accountId,
                teamType = request.teamType,
                startDate = request.startDate,
                endDate = request.endDate,
                isConfirmed = request.isConfirmed,
                branchCode = employee.costCenterCode
            )
        )

        // 즉시 반영: startDate가 오늘이고 확정된 경우
        if (request.startDate == LocalDate.now() && request.isConfirmed) {
            updateEmployeeTeam(employee, request.teamType, masterId = master.id)
        }

        return PPTMasterResponse.from(
            master, employee.employeeCode, employee.name,
            account.externalKey, account.name,
            branchName = employee.orgName,
            employeeStatus = employee.status,
            employeeAppLoginActive = employee.appLoginActive,
            employeeEndDate = employee.endDate,
            accountType = account.accountType
        )
    }

    /**
     * 전문행사조 마스터 수정.
     *
     * 중복 검증 + 동일 사원의 다른 teamType 유효 마스터 자동 종료(신규 시작일 -1일) 후 마스터 update.
     * 확정=true + 시작일=오늘이면 직원의 전문행사조를 즉시 갱신하고 미래 일정 삭제 + 이력 insert.
     *
     * 레거시 매핑: PPTMasterTriggerHandler.ChangeToNormal (before update) + ChangeToNormalAfter (after update).
     * 레거시 동등성: before update 의 종료일 자동 set 룰을 신규에서 동등 적용 (자기 객체 자동 종료 — UC-05).
     */
    @Transactional
    fun updateMaster(id: Long, request: PPTMasterUpdateRequest): PPTMasterResponse {
        val master = findMasterById(id)
        val employee = findEmployeeById(request.employeeId)
        val account = findAccountById(request.accountId)

        validateDateRange(request.startDate, request.endDate)

        // teamType 변경 시 중복 검증
        if (master.teamType != request.teamType || master.accountId != request.accountId) {
            validateNoDuplicate(
                request.employeeId,
                request.accountId,
                request.teamType,
                request.startDate,
                id
            )
        }

        // 동일 사원의 다른 teamType 유효 마스터 자동 종료 (본 레코드 자신은 제외)
        autoTerminateExistingMasters(request.employeeId, request.startDate, excludeId = id)

        master.update(
            request.teamType,
            request.startDate,
            request.endDate,
            request.isConfirmed,
            request.accountId,
            // 사원 변경 시 branch_code 를 새 사원의 소속 지점으로 재계산 (SF BranchName__c formula 동등).
            employeeId = request.employeeId,
            branchCode = employee.costCenterCode,
        )

        pptMasterRepository.save(master)

        // 즉시 반영
        if (request.startDate == LocalDate.now() && request.isConfirmed) {
            updateEmployeeTeam(employee, request.teamType, masterId = master.id)
        }

        return PPTMasterResponse.from(
            master, employee.employeeCode, employee.name,
            account.externalKey, account.name,
            branchName = employee.orgName,
            employeeStatus = employee.status,
            employeeAppLoginActive = employee.appLoginActive,
            employeeEndDate = employee.endDate,
            accountType = account.accountType
        )
    }

    @Transactional
    fun deleteMaster(id: Long) {
        val master = findMasterById(id)
        val employeeId = master.employeeId

        pptMasterRepository.delete(master)

        // employee_id 미매핑 row 는 employee 후처리 스킵
        if (employeeId == null) return

        // 다른 유효 마스터가 없으면 미배정(null)으로 복귀
        val remainingValid = pptMasterRepository.findValidMastersByEmployeeId(employeeId, LocalDate.now())
        if (remainingValid.isEmpty()) {
            val employee = employeeRepository.findById(employeeId).orElse(null)
            if (employee != null) {
                updateEmployeeTeam(employee, null)
            }
        }
    }

    fun getHistory(masterId: Long, pageable: Pageable): PPTMasterHistoryListResponse {
        val master = findMasterById(masterId)
        val employeeId = master.employeeId
            ?: return PPTMasterHistoryListResponse(emptyList(), 0L, 0, pageable.pageNumber, pageable.pageSize)
        val page = pptHistoryRepository.findHistoriesByEmployeeId(employeeId, pageable)

        return PPTMasterHistoryListResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size
        )
    }

    /** [PPTHistorySearchResult] projection → 응답 DTO (사원 컨텍스트 + 원인 마스터 거래처 포함). */
    private fun PPTHistorySearchResult.toResponse(): PPTMasterHistoryResponse =
        PPTMasterHistoryResponse.from(
            history, employeeName, employeeCode, orgName, accountCode, accountName
        )

    fun getAllHistory(
        scope: DataScope,
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        changedAtFrom: LocalDate?,
        changedAtTo: LocalDate?,
        pageable: Pageable
    ): PPTMasterHistoryListResponse {
        // 지점 스코프 — 전문행사조 마스터 조회와 동일하게 본인 소속 지점만 노출 (전사 권한은 전체).
        // 데이터의 branch_code 컬럼은 비어 있으므로 사원 소속 지점(costCenterCode) 기준으로 평가한다.
        // branchCode 지정 시(다중지점 사용자가 지점 선택) 해당 지점만 — 권한 밖이면 NoAccess.
        val branchCodeFilter = when (val result = scope.effectiveBranchCodes(branchCode?.takeIf { it.isNotBlank() })) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> return emptyHistoryList(pageable)
        }
        // "일반" 은 enum 값이 아니라 미지정(해제) 상태 — new_value IS NULL 또는 raw '일반'
        // (SF 마이그레이션분 문자열) 필터로 해석 (repository 참조).
        val teamTypeGeneral = teamType == ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME
        val teamTypeEnum = ProfessionalPromotionTeamType.fromDisplayNameOrNull(teamType)
        val page = pptHistoryRepository.searchHistories(
            employeeName, employeeCode, teamTypeEnum, teamTypeGeneral,
            changedAtFrom, changedAtTo, branchCodeFilter, pageable
        )

        return PPTMasterHistoryListResponse(
            content = page.content.map { it.toResponse() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size
        )
    }

    /**
     * 현재 검색 조건 기준 전문행사조 이력 데이터를 xlsx 로 export — 목록 화면(`getAllHistory`)과 동일한 지점 가시 범위/필터.
     *
     * 페이징 없이 [EXPORT_MAX_ROWS] 단일 페이지로 조회 (초과분 잘라냄 — 마스터 export 정합).
     * NoAccess(권한 밖 지점)는 쿼리를 생략하고 헤더만 있는 빈 엑셀을 반환한다 (마스터 export 와 동일 분기).
     * 목록과 동일한 [PPTMasterHistoryResponse] 매핑 후 [PPTHistoryExcelExporter] 로 위임.
     */
    fun exportHistoryToExcel(
        scope: DataScope,
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        changedAtFrom: LocalDate?,
        changedAtTo: LocalDate?
    ): ExcelResult {
        val rows = when (val result = scope.effectiveBranchCodes(branchCode?.takeIf { it.isNotBlank() })) {
            is EffectiveBranchResult.NoAccess -> emptyList()
            else -> {
                val branchCodeFilter = when (result) {
                    is EffectiveBranchResult.All -> null
                    is EffectiveBranchResult.Filtered -> result.codes
                    is EffectiveBranchResult.NoAccess -> emptyList() // unreachable
                }
                // "일반" 은 enum 값이 아니라 미지정(해제) 상태 — 목록 화면과 동일하게
                // new_value IS NULL 또는 raw '일반'(SF 마이그레이션분) 필터로 해석.
                val teamTypeGeneral = teamType == ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME
                val teamTypeEnum = ProfessionalPromotionTeamType.fromDisplayNameOrNull(teamType)
                pptHistoryRepository.searchHistories(
                    employeeName, employeeCode, teamTypeEnum, teamTypeGeneral, changedAtFrom, changedAtTo,
                    branchCodeFilter, PageRequest.of(0, EXPORT_MAX_ROWS)
                ).content.map { it.toResponse() }
            }
        }

        val timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return pptHistoryExcelExporter.export(rows, "전문행사조이력_${timestamp}.xlsx")
    }

    /**
     * 현재 검색 조건 기준 전문행사조 마스터 데이터를 xlsx 로 export.
     *
     * 페이징 없이 매칭 레코드 전량을 단일 시트로 출력. 컬럼: 사번 / 사원명 / 거래처코드 / 거래처명 /
     * 전문행사조 / 시작일 / 종료일 / 확정 여부 / 지점코드. 응답은 byte array (Controller 에서 attachment 헤더 부여).
     *
     * 레거시 매핑: List View Button 「전문행사조마스터 다운로드」 (ExcelIO 관리형 패키지 호출) — UC-11.
     * 레거시 차이: ExcelIO 패키지 내부 로직 미공개로 컬럼 구성 / 정렬 / 헤더 스타일은 신규 자체 결정.
     */
    fun exportToExcel(
        scope: DataScope,
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        validOnly: Boolean
    ): ExcelResult {
        // 엑셀 다운로드도 목록 화면과 동일한 지점 가시 범위로 제한 (사원 소속 지점 기준).
        // NoAccess(권한 밖 지점 요청)는 쿼리를 생략하고 헤더만 있는 빈 엑셀을 반환한다 —
        // branchCodeFilter 를 빈 리스트로 두면 repository 가 필터를 생략해 전사가 노출되므로 분기로 차단.
        val scopeResult = scope.effectiveBranchCodes(branchCode?.takeIf { it.isNotBlank() })
        val rows = if (scopeResult is EffectiveBranchResult.NoAccess) {
            emptyList()
        } else {
            val branchCodeFilter = when (scopeResult) {
                is EffectiveBranchResult.All -> null
                is EffectiveBranchResult.Filtered -> scopeResult.codes
                is EffectiveBranchResult.NoAccess -> emptyList() // unreachable
            }
            val teamTypeEnum = ProfessionalPromotionTeamType.fromDisplayNameOrNull(teamType)
            pptMasterRepository.searchMasters(
                employeeName, employeeCode, teamTypeEnum, branchCodeFilter, validOnly,
                LocalDate.now(), PageRequest.of(0, EXPORT_MAX_ROWS)
            ).content
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("전문행사조마스터")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headerRow = sheet.createRow(0)
        val headers = listOf("사번", "사원명", "거래처코드", "거래처명", "전문행사조", "시작일", "종료일", "확정", "지점코드")
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        rows.forEachIndexed { index, result ->
            val m = result.master
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(result.employeeCode ?: "")
            row.createCell(1).setCellValue(result.employeeName ?: "")
            row.createCell(2).setCellValue(result.accountCode ?: "")
            row.createCell(3).setCellValue(result.accountName ?: "")
            row.createCell(4).setCellValue(m.teamType?.displayName ?: "")
            row.createCell(5).setCellValue(m.startDate.toString())
            row.createCell(6).setCellValue(m.endDate?.toString() ?: "")
            row.createCell(7).setCellValue(if (m.isConfirmed) "Y" else "N")
            row.createCell(8).setCellValue(m.branchCode ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val filename = "전문행사조마스터_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ExcelResult(bytes, filename)
    }

    fun generateExcelTemplate(): ByteArray {
        val employees = employeeRepository.findByRoleAndStatus(AppAuthority.WOMAN, "재직")

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("전문행사조마스터")

        // Header
        val headerRow = sheet.createRow(0)
        val headers = listOf("소속", "사번", "이름", "직위", "거래처코드", "거래처명", "전문행사조", "시작일", "종료일")
        headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

        // Data rows
        employees.forEachIndexed { index, emp ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(emp.orgName ?: "")
            row.createCell(1).setCellValue(emp.employeeCode)
            row.createCell(2).setCellValue(emp.name)
            row.createCell(3).setCellValue(emp.role ?: "")
        }

        val out = ByteArrayOutputStream()
        workbook.use { it.write(out) }
        return out.toByteArray()
    }

    fun validateBulk(request: PPTMasterBulkValidateRequest): BulkValidationResponse {
        val results = mutableListOf<BulkValidationResultItem>()

        // Pre-fetch employees and accounts for batch lookup
        val employeeCodes = request.items.map { it.employeeCode }.distinct()
        val accountCodes = request.items.map { it.accountCode }.distinct()
        val employeeMap = employeeRepository.findByEmployeeCodeIn(employeeCodes)
            .filter { it.employeeCode != null }.associateBy { it.employeeCode!! }
        val accountMap = accountRepository.findByExternalKeyIn(accountCodes).associateBy { it.externalKey }

        request.items.forEachIndexed { index, item ->
            val error = validateBulkItem(item, employeeMap, accountMap)
            results.add(BulkValidationResultItem(row = index + 1, valid = error == null, errorMessage = error))
        }

        val errorCount = results.count { !it.valid }
        return BulkValidationResponse(
            totalCount = results.size,
            successCount = results.size - errorCount,
            errorCount = errorCount,
            isAllValid = errorCount == 0,
            results = results
        )
    }

    @Transactional
    fun confirmBulk(request: PPTMasterBulkValidateRequest): BulkConfirmResponse {
        // Re-validate
        val validation = validateBulk(request)
        if (!validation.isAllValid) {
            throw PPTMasterBulkValidationFailedException()
        }

        val employeeCodes = request.items.map { it.employeeCode }.distinct()
        val accountCodes = request.items.map { it.accountCode }.distinct()
        val employeeMap = employeeRepository.findByEmployeeCodeIn(employeeCodes).associateBy { it.employeeCode }
        val accountMap = accountRepository.findByExternalKeyIn(accountCodes).associateBy { it.externalKey }

        var createdCount = 0
        for (item in request.items) {
            val employee = employeeMap[item.employeeCode]!!
            val account = accountMap[item.accountCode]!!

            // 중복 검증 (생성 규칙 5번)
            val duplicates = pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(
                employee.id, account.id, item.teamType, item.startDate
            )
            if (duplicates.isNotEmpty()) continue

            // 기존 유효 마스터 자동 종료 (생성 규칙 6번)
            autoTerminateExistingMasters(employee.id, item.startDate)

            pptMasterRepository.save(
                ProfessionalPromotionTeamMaster(
                    name = generateMasterName(),
                    employeeId = employee.id,
                    accountId = account.id,
                    teamType = item.teamType,
                    startDate = item.startDate,
                    endDate = item.endDate,
                    isConfirmed = false,
                    branchCode = employee.costCenterCode
                )
            )

            // 즉시 반영 (생성 규칙 8번 - bulk은 isConfirmed=false이므로 스킵)
            createdCount++
        }

        return BulkConfirmResponse(createdCount = createdCount)
    }

    /**
     * 선택 레코드 일괄 확정 (Mass Quick Action 대체).
     *
     * 입력된 ids 중 미확정(`isConfirmed = false`) 마스터만 `isConfirmed = true` 로 set 후 save.
     * 시작일=오늘 인 레코드는 추가로 `updateEmployeeTeam` 호출 (직원 전문행사조 즉시 갱신 + 이력 insert + 미래 일정 삭제).
     * 시작일이 미래인 레코드는 새벽 배치(UC-13)가 도래일에 동기화.
     *
     * 레거시 매핑: SF Mass Quick Action "Valid_Update" + PPTMasterTrigger after update 의 ChangeToNormalAfter — UC-12.
     */
    @Transactional
    fun confirmByIds(request: PPTMasterConfirmByIdsRequest): ConfirmByIdsResponse {
        val masters = pptMasterRepository.findAllById(request.ids)
        val today = LocalDate.now()

        var confirmedCount = 0
        var skippedCount = 0

        for (master in masters) {
            if (master.isConfirmed) {
                skippedCount++
                continue
            }
            master.isConfirmed = true
            pptMasterRepository.save(master)
            confirmedCount++

            // 시작일=오늘 인 레코드는 직원 행사조 즉시 갱신 (chain 1-hop)
            if (master.startDate == today) {
                val employee = employeeRepository.findById(master.employeeId!!).orElse(null)
                if (employee != null) {
                    updateEmployeeTeam(employee, master.teamType, masterId = master.id)
                }
            }
        }

        // 요청 ids 중 조회되지 않은 레코드도 skipped 로 집계
        skippedCount += request.ids.size - masters.size

        return ConfirmByIdsResponse(confirmedCount = confirmedCount, skippedCount = skippedCount)
    }

    // --- Private helpers ---

    /**
     * 전문행사조 마스터 번호(name) 채번 — SF AutoNumber(displayFormat PM{0000000}) 동등, PM + 7자리.
     * 시퀀스 채번값이 7자리를 초과하면 자릿수를 넘겨 그대로 표기(SF AutoNumber 동작과 동일).
     */
    private fun generateMasterName(): String {
        val seq = pptMasterRepository.getNextNameSeq()
        return "PM" + String.format("%07d", seq)
    }

    private fun findMasterById(id: Long): ProfessionalPromotionTeamMaster {
        return pptMasterRepository.findById(id).orElseThrow { PPTMasterNotFoundException() }
    }

    private fun findEmployeeById(employeeId: Long): Employee {
        return employeeRepository.findById(employeeId).orElseThrow { PPTMasterEmployeeNotFoundException() }
    }

    private fun findAccountById(accountId: Long): Account {
        return accountRepository.findById(accountId).orElseThrow { PPTMasterAccountNotFoundException() }
    }

    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate?) {
        if (endDate != null && startDate.isAfter(endDate)) throw PPTMasterInvalidDateRangeException()
    }

    private fun validateNoDuplicate(
        employeeId: Long, accountId: Long, teamType: ProfessionalPromotionTeamType, startDate: LocalDate, excludeId: Long? = null
    ) {
        val duplicates = pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(
            employeeId, accountId, teamType, startDate, excludeId
        )
        if (duplicates.isNotEmpty()) throw PPTMasterDuplicateException()
    }

    private fun autoTerminateExistingMasters(employeeId: Long, newStartDate: LocalDate, excludeId: Long? = null) {
        val existingValid = pptMasterRepository.findByEmployeeIdAndEndDateIsNull(employeeId)
        for (existing in existingValid) {
            if (excludeId != null && existing.id == excludeId) continue
            existing.endDate = newStartDate.minusDays(1)
            pptMasterRepository.save(existing)
        }
    }

    /**
     * @param masterId 변경을 유발한 전문행사조 마스터 id. 원인 마스터를 특정할 수 있는 경로
     *   (생성/수정/확정/sync/만료)는 마스터 id 를 넘기고, 삭제로 인한 해제 경로는 마스터가
     *   이미 제거되었으므로 `null` 을 넘긴다.
     */
    internal fun updateEmployeeTeam(
        employee: Employee,
        newTeamType: ProfessionalPromotionTeamType?,
        masterId: Long? = null,
    ) {
        val oldValue = employee.professionalPromotionTeam
        employee.professionalPromotionTeam = newTeamType
        employeeRepository.save(employee)

        if (oldValue != newTeamType) {
            teamMemberScheduleRepository.deleteFutureWorkSchedulesByEmployeeId(
                employee.id, LocalDate.now()
            )
        }

        if (newTeamType != null) {
            pptHistoryRepository.save(
                ProfessionalPromotionTeamHistory(
                    employeeId = employee.id,
                    masterId = masterId,
                    oldValue = oldValue,
                    newValue = newTeamType
                )
            )
        }
    }

    private fun validateBulkItem(
        item: PPTMasterBulkItem,
        employeeMap: Map<String, Employee>,
        accountMap: Map<String?, Account>
    ): String? {
        // 사번 검증
        val employee = employeeMap[item.employeeCode]
        if (employee == null) {
            return "사번 ${item.employeeCode}에 해당하는 사원이 존재하지 않습니다"
        }
        if (employee.status != "재직") {
            return "사번 ${item.employeeCode} 사원이 재직 중이 아닙니다"
        }

        // 거래처 검증
        if (accountMap[item.accountCode] == null) {
            return "거래처코드 ${item.accountCode}에 해당하는 거래처가 존재하지 않습니다"
        }

        // 날짜 검증
        if (item.endDate != null && item.startDate.isAfter(item.endDate)) {
            return "종료일이 시작일보다 이전입니다"
        }

        return null
    }
}
