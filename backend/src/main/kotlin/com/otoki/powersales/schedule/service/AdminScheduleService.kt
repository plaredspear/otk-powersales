package com.otoki.powersales.schedule.service

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.SecondWorkType
import com.otoki.powersales.schedule.entity.TypeOfWork1
import com.otoki.powersales.schedule.entity.TypeOfWork3
import com.otoki.powersales.schedule.entity.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class AdminScheduleService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val templateGenerator: ScheduleTemplateGenerator,
    private val excelParser: ScheduleExcelParser,
    private val uploadValidator: ScheduleUploadValidator,
    private val scheduleRepository: DisplayWorkScheduleRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val REDIS_KEY_PREFIX = "schedule:upload:"
        private const val REDIS_TTL_MINUTES = 30L
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_ROWS = 500
    }

    fun generateTemplate(userId: Long): TemplateResult {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val costCenterCode = employee.costCenterCode
        if (costCenterCode.isNullOrBlank()) {
            throw MissingCostCenterException()
        }

        val org = organizationRepository.findFirstByCostCenterLevel5(costCenterCode)
            ?: organizationRepository.findFirstByCostCenterLevel4(costCenterCode)
            ?: throw OrganizationNotFoundException()

        val employees = if (employee.role == UserRole.SALES_SUPPORT) {
            val costCenterLevel3 = org.costCenterLevel3
                ?: throw OrganizationNotFoundException()
            val costCenterCodes = organizationRepository.findByCostCenterLevel3(costCenterLevel3)
                .mapNotNull { it.costCenterLevel5 }
                .distinct()
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                costCenterCodes, UserRole.WOMAN, "재직"
            )
        } else {
            employeeRepository.findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                costCenterCode, UserRole.WOMAN, "재직"
            )
        }.sortedWith(compareBy({ it.orgName }, { it.employeeCode }))

        val excelBytes = templateGenerator.generate(employees)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "진열마스터Template(신규작성용)_${timestamp}.xlsx"

        return TemplateResult(excelBytes, filename)
    }

    fun uploadAndValidate(file: MultipartFile): ScheduleUploadResultDto {
        // 파일 검증
        validateFile(file)

        // Excel 파싱
        val parseResult = excelParser.parse(file.inputStream)

        if (parseResult.totalRows == 0) {
            throw ScheduleEmptyFileException()
        }
        if (parseResult.totalRows > MAX_ROWS) {
            throw ScheduleRowLimitExceededException()
        }

        // 사원번호/거래처코드 일괄 조회
        val employeeCodes = parseResult.rows.mapNotNull { it.employeeCode }.distinct()
        val accountCodes = parseResult.rows.mapNotNull { it.accountCode }.distinct()

        val usersByEmployeeNumber = if (employeeCodes.isNotEmpty()) {
            employeeRepository.findByEmployeeCodeIn(employeeCodes).associateBy { it.employeeCode }
        } else {
            emptyMap()
        }

        val accountsByExternalKey = if (accountCodes.isNotEmpty()) {
            accountRepository.findByExternalKeyIn(accountCodes)
                .filter { it.externalKey != null }
                .associateBy { it.externalKey!! }
        } else {
            emptyMap()
        }

        // 기존 스케줄 조회 (중복 검증용)
        val userIds = usersByEmployeeNumber.values.map { it.id }
        val existingSchedules = if (userIds.isNotEmpty()) {
            scheduleRepository.findByEmployeeIdInAndNotDeleted(userIds)
        } else {
            emptyList()
        }

        // 검증
        val validationResult = uploadValidator.validate(
            parseResult.rows, usersByEmployeeNumber, accountsByExternalKey, existingSchedules
        )

        // UUID 생성 + Redis 저장
        val uploadId = UUID.randomUUID().toString()
        val cacheData = UploadCacheData(
            validRows = validationResult.validRows,
            errorCount = validationResult.errors.size
        )
        val json = objectMapper.writeValueAsString(cacheData)
        redisTemplate.opsForValue().set(
            "$REDIS_KEY_PREFIX$uploadId", json, REDIS_TTL_MINUTES, TimeUnit.MINUTES
        )

        return ScheduleUploadResultDto(
            uploadId = uploadId,
            totalRows = parseResult.totalRows,
            successRows = validationResult.validRows.size,
            errorRows = validationResult.errors.size,
            errors = validationResult.errors,
            previews = validationResult.previews
        )
    }

    @Transactional
    fun confirmUpload(uploadId: String): ScheduleConfirmResultDto {
        val redisKey = "$REDIS_KEY_PREFIX$uploadId"
        val json = redisTemplate.opsForValue().get(redisKey)
            ?: throw ScheduleUploadNotFoundException()

        val cacheData = objectMapper.readValue(json, UploadCacheData::class.java)

        if (cacheData.errorCount > 0) {
            throw ScheduleHasValidationErrorsException()
        }

        // Employee/Account 엔티티 일괄 조회
        val userIds = cacheData.validRows.map { it.userId }.distinct()
        val accountIds = cacheData.validRows.map { it.accountId }.distinct()
        val employeeMap = if (userIds.isNotEmpty()) {
            employeeRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }
        val accountMap = if (accountIds.isNotEmpty()) {
            accountRepository.findByIdIn(accountIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        // 조장 일괄 조회: costCenterCode 목록 → role=LEADER 사원
        val costCenterCodes = cacheData.validRows.mapNotNull { it.costCenterCode }.distinct()
        val managersByCostCenter = if (costCenterCodes.isNotEmpty()) {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(costCenterCodes, UserRole.LEADER)
                .groupBy { it.costCenterCode }
        } else {
            emptyMap()
        }

        // 전월 매출 일괄 조회
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)
        val salesYear = lastMonth.year.toString()
        val salesMonth = String.format("%02d", lastMonth.monthValue)
        val accountsForRevenue = accountMap.values.toList()
        val revenueByAccountId = if (accountsForRevenue.isNotEmpty()) {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                salesYear, salesMonth, accountsForRevenue
            ).filter { it.account != null }.associateBy { it.account!!.id }
        } else {
            emptyMap()
        }

        val entities = cacheData.validRows.map { row ->
            val costCenterCode = row.costCenterCode
            val owner = if (!costCenterCode.isNullOrBlank()) {
                managersByCostCenter[costCenterCode]?.firstOrNull()
            } else {
                null
            }
            val lastMonthRevenue = revenueByAccountId[row.accountId]?.lastMonthResults?.toLong()

            DisplayWorkSchedule(
                employee = employeeMap[row.userId],
                account = accountMap[row.accountId],
                typeOfWork1 = TypeOfWork1.DISPLAY,
                typeOfWork3 = TypeOfWork3.fromDisplayNameOrNull(row.typeOfWork3),
                typeOfWork4 = SecondWorkType.fromDisplayNameOrNull(row.typeOfWork4),
                typeOfWork5 = TypeOfWork5.fromDisplayNameOrNull(row.typeOfWork5),
                startDate = row.startDate,
                endDate = row.endDate,
                confirmed = false,
                costCenterCode = costCenterCode,
                lastMonthRevenue = lastMonthRevenue,
                owner = owner
            )
        }

        scheduleRepository.saveAll(entities)
        redisTemplate.delete(redisKey)

        return ScheduleConfirmResultDto(insertedCount = entities.size)
    }

    fun listSchedules(
        page: Int,
        size: Int,
        employeeCode: String?,
        accountName: String?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?
    ): Page<ScheduleListItemDto> {
        val pageSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(page, pageSize)

        val accountIds = if (!accountName.isNullOrBlank()) {
            accountRepository.findByNameContainingIgnoreCase(accountName).map { it.id }
        } else {
            null
        }

        val schedulePage = scheduleRepository.findScheduleList(
            employeeCode, accountIds, confirmed, typeOfWork3, startDateFrom, startDateTo, pageable
        )

        return schedulePage.map { schedule ->
            ScheduleListItemDto(
                id = schedule.id,
                employeeCode = schedule.employee?.employeeCode ?: "",
                employeeName = schedule.employee?.name ?: "",
                accountCode = schedule.account?.externalKey,
                accountName = schedule.account?.name,
                typeOfWork3 = schedule.typeOfWork3?.displayName,
                typeOfWork4 = schedule.typeOfWork4?.displayName,
                typeOfWork5 = schedule.typeOfWork5?.displayName,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                confirmed = schedule.confirmed,
                costCenterCode = schedule.costCenterCode,
                lastMonthRevenue = schedule.lastMonthRevenue
            )
        }
    }

    @Transactional
    fun batchConfirm(ids: List<Long>): ScheduleBatchConfirmResultDto {
        val schedules = scheduleRepository.findAllById(ids)
        validateScheduleIds(ids, schedules)

        var updatedCount = 0
        for (schedule in schedules) {
            if (schedule.confirmed != true) {
                schedule.confirmed = true
                updatedCount++
            }
        }

        return ScheduleBatchConfirmResultDto(updatedCount = updatedCount)
    }

    @Transactional
    fun batchUnconfirm(ids: List<Long>): ScheduleBatchConfirmResultDto {
        val schedules = scheduleRepository.findAllById(ids)
        validateScheduleIds(ids, schedules)

        var updatedCount = 0
        for (schedule in schedules) {
            if (schedule.confirmed == true) {
                schedule.confirmed = false
                updatedCount++
            }
        }

        return ScheduleBatchConfirmResultDto(updatedCount = updatedCount)
    }

    @Transactional
    fun deleteSchedule(userId: Long, scheduleId: Long) {
        val schedule = scheduleRepository.findById(scheduleId)
            .filter { it.isDeleted != true }
            .orElseThrow { ScheduleNotFoundException("존재하지 않거나 삭제된 스케줄입니다") }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val role = employee.role

        if (role in UserRole.ADMIN_GRADE) {
            schedule.isDeleted = true
            return
        }

        if (role == UserRole.BRANCH_MANAGER) {
            throw ScheduleDeleteForbiddenException()
        }

        if (schedule.confirmed == true) {
            val scheduleEmployee = schedule.employee
            val scheduleAccount = schedule.account
            val startDate = schedule.startDate
            val endDate = schedule.endDate

            if (scheduleEmployee != null && scheduleAccount != null && startDate != null && endDate != null) {
                val hasLinkedSchedule = teamMemberScheduleRepository
                    .existsByEmployeeAndAccountAndWorkingDateBetween(scheduleEmployee, scheduleAccount, startDate, endDate)
                if (hasLinkedSchedule) {
                    throw ScheduleDeleteConstraintException()
                }
            }
        }

        schedule.isDeleted = true
    }

    private fun validateScheduleIds(ids: List<Long>, schedules: List<DisplayWorkSchedule>) {
        if (schedules.size != ids.size) {
            throw ScheduleNotFoundException()
        }
        if (schedules.any { it.isDeleted == true }) {
            throw ScheduleNotFoundException()
        }
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw ScheduleFileRequiredException()
        }

        val originalFilename = file.originalFilename ?: ""
        if (!originalFilename.lowercase().endsWith(".xlsx")) {
            throw ScheduleInvalidFileTypeException()
        }

        if (file.size > MAX_FILE_SIZE) {
            throw ScheduleFileTooLargeException()
        }
    }

    data class TemplateResult(
        val bytes: ByteArray,
        val filename: String
    )

    data class UploadCacheData(
        val validRows: List<ScheduleUploadValidator.ValidatedRow>,
        val errorCount: Int
    )
}

class OrganizationNotFoundException : BusinessException(
    errorCode = "ORGANIZATION_NOT_FOUND",
    message = "존재하지 않는 지점 코드입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class MissingCostCenterException : BusinessException(
    errorCode = "MISSING_COST_CENTER",
    message = "소속 지점이 설정되지 않았습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
