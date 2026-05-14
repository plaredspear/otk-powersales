package com.otoki.powersales.promotion.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.promotion.dto.request.PPTMasterBulkItem
import com.otoki.powersales.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.promotion.dto.response.BulkValidationResultItem
import com.otoki.powersales.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.promotion.dto.response.PPTMasterHistoryResponse
import com.otoki.powersales.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.promotion.dto.response.PPTMasterResponse
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.exception.PPTMasterAccountNotFoundException
import com.otoki.powersales.promotion.exception.PPTMasterBulkValidationFailedException
import com.otoki.powersales.promotion.exception.PPTMasterDuplicateException
import com.otoki.powersales.promotion.exception.PPTMasterEmployeeNotFoundException
import com.otoki.powersales.promotion.exception.PPTMasterInvalidDateRangeException
import com.otoki.powersales.promotion.exception.PPTMasterNotFoundException
import com.otoki.powersales.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminPPTMasterService(
    private val pptMasterRepository: PPTMasterRepository,
    private val pptHistoryRepository: PPTHistoryRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    companion object {
        private const val BULK_MAX_SIZE = 450
    }

    fun getMasters(
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        validOnly: Boolean,
        pageable: Pageable
    ): PPTMasterListResponse {
        val teamTypeEnum = ProfessionalPromotionTeamType.Companion.fromDisplayNameOrNull(teamType)
        val page = pptMasterRepository.searchMasters(
            employeeName, employeeCode, teamTypeEnum, branchCode, validOnly, LocalDate.now(), pageable
        )
        return PPTMasterListResponse(
            content = page.content.map { PPTMasterResponse.Companion.from(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size
        )
    }

    fun getMaster(id: Long): PPTMasterResponse {
        val master = findMasterById(id)
        val employee = employeeRepository.findById(master.employeeId).orElse(null)
        val account = accountRepository.findById(master.accountId).orElse(null)
        return PPTMasterResponse.Companion.from(
            master, employee?.employeeCode, employee?.name,
            account?.externalKey, account?.name
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
            updateEmployeeTeam(employee, request.teamType)
        }

        return PPTMasterResponse.Companion.from(
            master, employee.employeeCode, employee.name,
            account.externalKey, account.name
        )
    }

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

        master.update(
            request.teamType,
             request.startDate,
             request.endDate,
             request.isConfirmed,
            request.accountId,
        )

        pptMasterRepository.save(master)

        // 즉시 반영
        if (request.startDate == LocalDate.now() && request.isConfirmed) {
            updateEmployeeTeam(employee, request.teamType)
        }

        return PPTMasterResponse.Companion.from(
            master, employee.employeeCode, employee.name,
            account.externalKey, account.name
        )
    }

    @Transactional
    fun deleteMaster(id: Long) {
        val master = findMasterById(id)
        val employeeId = master.employeeId

        pptMasterRepository.delete(master)

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
        val page = pptHistoryRepository.findByEmployeeIdOrderByChangedAtDesc(master.employeeId, pageable)
        val employee = employeeRepository.findById(master.employeeId).orElse(null)

        return PPTMasterHistoryListResponse(
            content = page.content.map { PPTMasterHistoryResponse.Companion.from(it, employee?.name) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            number = page.number,
            size = page.size
        )
    }

    fun generateExcelTemplate(): ByteArray {
        val employees = employeeRepository.findByRoleAndStatus(UserRole.WOMAN, "재직")

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
            row.createCell(3).setCellValue(emp.role?.toKorean() ?: "")
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
        val employeeMap = employeeRepository.findByEmployeeCodeIn(employeeCodes).associateBy { it.employeeCode }
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

    // --- Private helpers ---

    private fun findMasterById(id: Long): ProfessionalPromotionTeamMaster {
        return pptMasterRepository.findById(id).orElseThrow { PPTMasterNotFoundException() }
    }

    private fun findEmployeeById(employeeId: Long): Employee {
        return employeeRepository.findById(employeeId).orElseThrow { PPTMasterEmployeeNotFoundException() }
    }

    private fun findAccountById(accountId: Int): Account {
        return accountRepository.findById(accountId).orElseThrow { PPTMasterAccountNotFoundException() }
    }

    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate?) {
        if (endDate != null && startDate.isAfter(endDate)) throw PPTMasterInvalidDateRangeException()
    }

    private fun validateNoDuplicate(
        employeeId: Long, accountId: Int, teamType: ProfessionalPromotionTeamType, startDate: LocalDate, excludeId: Long? = null
    ) {
        val duplicates = pptMasterRepository.findValidMastersByEmployeeIdAndTeamType(
            employeeId, accountId, teamType, startDate, excludeId
        )
        if (duplicates.isNotEmpty()) throw PPTMasterDuplicateException()
    }

    private fun autoTerminateExistingMasters(employeeId: Long, newStartDate: LocalDate) {
        val existingValid = pptMasterRepository.findByEmployeeIdAndEndDateIsNull(employeeId)
        for (existing in existingValid) {
            existing.endDate = newStartDate.minusDays(1)
            pptMasterRepository.save(existing)
        }
    }

    internal fun updateEmployeeTeam(employee: Employee, newTeamType: ProfessionalPromotionTeamType?) {
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
