package com.otoki.powersales.schedule.service

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleBatchDeleteFailure
import com.otoki.powersales.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.enums.SchedulePreset
import com.otoki.powersales.schedule.enums.SecondWorkType
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import com.otoki.powersales.schedule.service.internal.LastMonthRevenueLookup
import com.otoki.powersales.user.repository.UserRepository
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
    private val exportGenerator: ScheduleExportGenerator,
    private val excelParser: ScheduleExcelParser,
    private val uploadValidator: ScheduleUploadValidator,
    private val scheduleRepository: DisplayWorkScheduleRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val lastMonthRevenueLookup: LastMonthRevenueLookup,
    private val userRepository: UserRepository,
    private val profileRepository: com.otoki.powersales.auth.repository.ProfileRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val REDIS_KEY_PREFIX = "schedule:upload:"
        private const val REDIS_TTL_MINUTES = 30L
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_ROWS = 500

        /** SF 영업지원실 판별 기준 — Org.OrgCodeLevel3 (AppointmentTriggerHanlder.cls:328-331). */
        private const val SALES_SUPPORT_LEVEL3 = "3475"

        /** SF 시스템 관리자 Profile.Name (ProfileBootstrapRunner SoT). */
        private const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"
    }

    /**
     * SF ADMIN_GRADE 동등 — Profile.Name == "시스템 관리자" OR User.isSalesSupport.
     *
     * SF 원본 분기 (`AdminScheduleService` 의 ADMIN_GRADE = SYSTEM_ADMIN + SALES_SUPPORT) 정합.
     * Employee 인자만으로 판단 — 매칭 User 의 profileId + isSalesSupport 캐시 컬럼 조회.
     */
    private fun isAdminGrade(employeeCode: String?): Boolean {
        if (employeeCode == null) return false
        val user = userRepository.findByEmployeeCode(employeeCode) ?: return false
        if (user.isSalesSupport == true) return true
        val profile = user.profileId?.let { profileRepository.findById(it).orElse(null) }
        return profile?.name == SYSTEM_ADMIN_PROFILE_NAME
    }

    fun generateTemplate(userId: Long): TemplateResult {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val costCenterCode = employee.costCenterCode
        if (costCenterCode.isNullOrBlank()) {
            throw MissingCostCenterException()
        }

        // cost_center 컬럼 cascade (Level5 → Level4) — 정책은 OrganizationRepository 에 응집
        val org = organizationRepository.findFirstByCostCenterCascade(costCenterCode)
            ?: throw OrganizationNotFoundException()

        // SF 정합: 영업지원실 판별 = Org.OrgCodeLevel3 == "3475" (SF AppointmentTriggerHanlder.cls:328-331)
        val employees = if (org.orgCodeLevel3 == SALES_SUPPORT_LEVEL3) {
            val costCenterLevel3 = org.costCenterLevel3
                ?: throw OrganizationNotFoundException()
            val costCenterCodes = organizationRepository.findByCostCenterLevel3(costCenterLevel3)
                .mapNotNull { it.costCenterLevel5 }
                .distinct()
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                costCenterCodes, AppAuthority.WOMAN, "재직"
            )
        } else {
            employeeRepository.findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                costCenterCode, AppAuthority.WOMAN, "재직"
            )
        }.sortedWith(compareBy({ it.orgName }, { it.employeeCode }))

        val excelBytes = templateGenerator.generate(employees)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "진열마스터Template(신규작성용)_${timestamp}.xlsx"

        return TemplateResult(excelBytes, filename)
    }

    /**
     * UC-08 진열마스터 Excel 다운로드 (선택 레코드).
     * 레거시 SF `ExcelIO_ExportDisplayWorkScheduleMaster2.page` (ecio 패키지) 동등 — 선택된 레코드만 export.
     * 입력 순서 보존하여 출력 (사용자가 선택한 순서대로 행 정렬).
     */
    fun exportSchedules(scope: DataScope, ids: List<Long>): TemplateResult {
        // UC-12 사업소 가시 범위 — scope 밖 ID 는 조용히 제외 (레거시 SF Sharing Rule row-level filter 동등)
        val schedules = scheduleRepository.findAllById(ids)
            .filter { it.isDeleted != true }
            .filter { scope.validateAccess(it.costCenterCode) }
            .associateBy { it.id }
        val ordered = ids.mapNotNull { schedules[it] }

        val bytes = exportGenerator.generate(ordered)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "진열스케줄_${timestamp}.xlsx"

        return TemplateResult(bytes, filename)
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

        val usersByEmployeeCode = if (employeeCodes.isNotEmpty()) {
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
        val userIds = usersByEmployeeCode.values.map { it.id }
        val existingSchedules = if (userIds.isNotEmpty()) {
            scheduleRepository.findByEmployeeIdInAndNotDeleted(userIds)
        } else {
            emptyList()
        }

        // 검증
        val validationResult = uploadValidator.validate(
            parseResult.rows, usersByEmployeeCode, accountsByExternalKey, existingSchedules
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

        // 조장 일괄 조회: costCenterCode 목록 → role=LEADER 사원 → User 매핑.
        // 레거시 SF DisplayWorkScheduleMasterTriggerHandler.setOwner() 와 동등한 chain:
        // 여사원(FullName__c) → CostCenterCode → 조장 Employee → Employee.employeeCode == User.employeeCode → User.
        val costCenterCodes = cacheData.validRows.mapNotNull { it.costCenterCode }.distinct()
        val leadersByCostCenter = if (costCenterCodes.isNotEmpty()) {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(costCenterCodes, AppAuthority.LEADER)
                .groupBy { it.costCenterCode }
        } else {
            emptyMap()
        }
        val leaderEmployeeCodes = leadersByCostCenter.values
            .flatten()
            .mapNotNull { it.employeeCode }
            .distinct()
        val userByEmployeeCode = if (leaderEmployeeCodes.isNotEmpty()) {
            userRepository.findByEmployeeCodeIn(leaderEmployeeCodes).associateBy { it.employeeCode }
        } else {
            emptyMap()
        }

        // 전월 매출 일괄 조회
        val revenueByAccountId = lastMonthRevenueLookup.forAccounts(accountMap.values.toList())

        val entities = cacheData.validRows.map { row ->
            val costCenterCode = row.costCenterCode
            val ownerUser = if (!costCenterCode.isNullOrBlank()) {
                leadersByCostCenter[costCenterCode]
                    ?.firstOrNull()
                    ?.employeeCode
                    ?.let { userByEmployeeCode[it] }
            } else {
                null
            }
            val lastMonthRevenue = revenueByAccountId[row.accountId]

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
                ownerUser = ownerUser
            )
        }

        scheduleRepository.saveAll(entities)
        redisTemplate.delete(redisKey)

        return ScheduleConfirmResultDto(insertedCount = entities.size)
    }

    fun listSchedules(
        scope: DataScope,
        page: Int,
        size: Int,
        employeeCode: String?,
        accountName: String?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        preset: SchedulePreset?,
        sort: Sort,
    ): Page<ScheduleListItemDto> {
        val pageSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(page, pageSize, sort)

        val accountIds = if (!accountName.isNullOrBlank()) {
            accountRepository.findByNameContainingIgnoreCase(accountName).map { it.id }
        } else {
            null
        }

        // UC-12 사업소 가시 범위 필터 — LEADER/BRANCH_MANAGER 는 본인 담당 사업소만, ADMIN_GRADE 는 무제한(null)
        val costCenterCodes = scopeBranchCodesOrNull(scope)

        val schedulePage = scheduleRepository.findScheduleList(
            employeeCode, accountIds, confirmed, typeOfWork3, startDateFrom, startDateTo, preset, costCenterCodes, pageable
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
                lastMonthRevenue = schedule.lastMonthRevenue?.toLong()
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

    /**
     * 단건 신규 등록.
     * 레거시 SF DisplayWorkScheduleMasterTriggerHandler before insert 의 검증 + before insert 의 자동채움
     * (전월 매출액 / costCenterCode / ownerUser) 을 단건 시나리오로 적용한다.
     */
    @Transactional
    fun createSchedule(scope: DataScope, userId: Long, request: AdminScheduleCreateRequest): ScheduleCreateResultDto {
        val employee = employeeRepository.findByEmployeeCode(request.employeeCode).orElse(null)
        val account = accountRepository.findByExternalKey(request.accountCode)

        // UC-12 등록할 사원의 costCenterCode 가 사용자 scope 내인지 검증
        if (employee != null) {
            requireCostCenterCodeScope(scope, employee.costCenterCode)
        }

        // 동일 사원 기존 스케줄 조회 (V8 + C1~C3 검증용). 사원이 존재할 때만 조회.
        val existingSchedules = if (employee != null) {
            scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(employee.id))
        } else {
            emptyList()
        }

        val result = uploadValidator.validateSingle(
            employeeCode = request.employeeCode,
            accountCode = request.accountCode,
            typeOfWork3 = request.typeOfWork3,
            typeOfWork4 = request.typeOfWork4,
            typeOfWork5 = request.typeOfWork5,
            startDate = request.startDate,
            endDate = request.endDate,
            employee = employee,
            account = account,
            existingSchedules = existingSchedules
        )

        if (result.validatedRow == null) {
            throw ScheduleValidationException(result.messages.joinToString("; "))
        }

        val validatedRow = result.validatedRow

        // 자동채움 1: 전월 매출액 (월별 매출 이력에서 lastMonth 의 lastMonthResults 조회)
        val lastMonthRevenue = lastMonthRevenueLookup.forAccount(account)

        // 자동채움 2: 소속 조장 사용자 (조장 Employee.employeeCode → User)
        val costCenterCode = validatedRow.costCenterCode
        val ownerUser = if (!costCenterCode.isNullOrBlank()) {
            val leaders = employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(
                listOf(costCenterCode), AppAuthority.LEADER
            )
            leaders.firstOrNull()
                ?.employeeCode
                ?.let { code -> userRepository.findByEmployeeCodeIn(listOf(code)).firstOrNull() }
        } else {
            null
        }

        val entity = DisplayWorkSchedule(
            employee = employee,
            account = account,
            typeOfWork1 = TypeOfWork1.DISPLAY,
            typeOfWork3 = TypeOfWork3.fromDisplayNameOrNull(validatedRow.typeOfWork3),
            typeOfWork4 = SecondWorkType.fromDisplayNameOrNull(validatedRow.typeOfWork4),
            typeOfWork5 = TypeOfWork5.fromDisplayNameOrNull(validatedRow.typeOfWork5),
            startDate = validatedRow.startDate,
            endDate = validatedRow.endDate,
            confirmed = false,
            costCenterCode = costCenterCode,
            lastMonthRevenue = lastMonthRevenue,
            ownerUser = ownerUser
        )

        val saved = scheduleRepository.save(entity)

        return ScheduleCreateResultDto(
            id = saved.id,
            employeeCode = employee?.employeeCode ?: "",
            employeeName = employee?.name ?: "",
            accountCode = account?.externalKey,
            accountName = account?.name,
            typeOfWork3 = saved.typeOfWork3?.displayName,
            typeOfWork4 = saved.typeOfWork4?.displayName,
            typeOfWork5 = saved.typeOfWork5?.displayName,
            startDate = saved.startDate,
            endDate = saved.endDate,
            costCenterCode = saved.costCenterCode,
            lastMonthRevenue = saved.lastMonthRevenue?.toLong()
        )
    }

    /**
     * UC-03 단건 편집.
     * 레거시 SF Validation Rule `EditDisableForDisplayMaster` (UC-05) 동등:
     *   confirmed=true 이고 사용자가 ADMIN_GRADE(SYSTEM_ADMIN/SALES_SUPPORT) 가 아닐 때
     *   거래처·근무형태1·근무형태3·근무형태5·사원·시작일·확정 변경 시 차단.
     * 통과 시 validateSingle 재실행 (자기 자신 row 는 V8/C1~C3 중복 검사에서 제외) + 자동채움 재계산.
     */
    @Transactional
    fun updateSchedule(
        scope: DataScope,
        userId: Long,
        scheduleId: Long,
        request: AdminScheduleUpdateRequest
    ): ScheduleCreateResultDto {
        val schedule = scheduleRepository.findById(scheduleId)
            .filter { it.isDeleted != true }
            .orElseThrow { ScheduleNotFoundException("존재하지 않거나 삭제된 스케줄입니다") }

        val user = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // UC-12 기존 schedule 의 costCenterCode 가 사용자 scope 내인지 먼저 검증.
        requireScheduleScope(scope, schedule)

        // UC-05 차단 룰: 확정 후 ADMIN_GRADE 외 사용자가 종료일 외 필드 변경 시도하면 차단.
        if (schedule.confirmed == true && !isAdminGrade(user.employeeCode)) {
            val originalEmployeeCode = schedule.employee?.employeeCode
            val originalAccountCode = schedule.account?.externalKey
            val originalType3 = schedule.typeOfWork3?.displayName
            val originalType4 = schedule.typeOfWork4?.displayName
            val originalType5 = schedule.typeOfWork5?.displayName
            val originalStart = schedule.startDate

            val blockedFieldChanged =
                originalEmployeeCode != request.employeeCode ||
                    originalAccountCode != request.accountCode ||
                    originalType3 != request.typeOfWork3 ||
                    originalType4 != request.typeOfWork4 ||
                    originalType5 != request.typeOfWork5 ||
                    originalStart != request.startDate
            if (blockedFieldChanged) {
                throw ScheduleEditBlockedAfterConfirmException()
            }
        }

        val employee = employeeRepository.findByEmployeeCode(request.employeeCode).orElse(null)
        val account = accountRepository.findByExternalKey(request.accountCode)

        // UC-12 변경 후 사원의 costCenterCode 도 사용자 scope 내인지 검증 (사원 변경 시 다른 사업소로 이전 차단)
        if (employee != null) {
            requireCostCenterCodeScope(scope, employee.costCenterCode)
        }

        val existingSchedules = if (employee != null) {
            scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(employee.id))
        } else {
            emptyList()
        }

        val result = uploadValidator.validateSingle(
            employeeCode = request.employeeCode,
            accountCode = request.accountCode,
            typeOfWork3 = request.typeOfWork3,
            typeOfWork4 = request.typeOfWork4,
            typeOfWork5 = request.typeOfWork5,
            startDate = request.startDate,
            endDate = request.endDate,
            employee = employee,
            account = account,
            existingSchedules = existingSchedules,
            excludeScheduleId = scheduleId
        )

        if (result.validatedRow == null) {
            throw ScheduleValidationException(result.messages.joinToString("; "))
        }

        val validatedRow = result.validatedRow

        // 자동채움 재실행 (거래처·사원이 바뀌었을 수 있으므로 항상 재계산)
        val lastMonthRevenue = lastMonthRevenueLookup.forAccount(account)

        val costCenterCode = validatedRow.costCenterCode
        val ownerUser = if (!costCenterCode.isNullOrBlank()) {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(
                listOf(costCenterCode), AppAuthority.LEADER
            ).firstOrNull()
                ?.employeeCode
                ?.let { code -> userRepository.findByEmployeeCodeIn(listOf(code)).firstOrNull() }
        } else {
            null
        }

        schedule.employee = employee
        schedule.account = account
        schedule.typeOfWork3 = TypeOfWork3.fromDisplayNameOrNull(validatedRow.typeOfWork3)
        schedule.typeOfWork4 = SecondWorkType.fromDisplayNameOrNull(validatedRow.typeOfWork4)
        schedule.typeOfWork5 = TypeOfWork5.fromDisplayNameOrNull(validatedRow.typeOfWork5)
        schedule.startDate = validatedRow.startDate
        schedule.endDate = validatedRow.endDate
        schedule.costCenterCode = costCenterCode
        schedule.lastMonthRevenue = lastMonthRevenue
        schedule.ownerUser = ownerUser

        return ScheduleCreateResultDto(
            id = schedule.id,
            employeeCode = employee?.employeeCode ?: "",
            employeeName = employee?.name ?: "",
            accountCode = account?.externalKey,
            accountName = account?.name,
            typeOfWork3 = schedule.typeOfWork3?.displayName,
            typeOfWork4 = schedule.typeOfWork4?.displayName,
            typeOfWork5 = schedule.typeOfWork5?.displayName,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            costCenterCode = schedule.costCenterCode,
            lastMonthRevenue = schedule.lastMonthRevenue?.toLong()
        )
    }

    /**
     * UC-07 일괄 삭제 — partial success.
     * 레거시 SF Mass Delete 와 동등 — 각 레코드에 UC-06 차단 룰 적용 후
     * 차단된 건만 실패로 기록하고 나머지는 삭제 진행.
     * BRANCH_MANAGER 는 전체 거부 (단건 deleteSchedule 정책과 동등).
     */
    @Transactional
    fun batchDelete(scope: DataScope, userId: Long, ids: List<Long>): ScheduleBatchDeleteResultDto {
        val user = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        if (user.role == AppAuthority.BRANCH_MANAGER) {
            throw ScheduleDeleteForbiddenException()
        }

        val isAdmin = isAdminGrade(user.employeeCode)
        val schedules = scheduleRepository.findAllById(ids).associateBy { it.id }

        var deletedCount = 0
        val failures = mutableListOf<ScheduleBatchDeleteFailure>()

        for (id in ids) {
            val schedule = schedules[id]
            if (schedule == null || schedule.isDeleted == true) {
                failures.add(
                    ScheduleBatchDeleteFailure(
                        id = id,
                        errorCode = "SCHEDULE_NOT_FOUND",
                        message = "존재하지 않거나 삭제된 스케줄입니다"
                    )
                )
                continue
            }

            // UC-12 사업소 가시 범위 검증 — 본인 담당 사업소 외 레코드는 partial fail 로 기록
            if (!scope.validateAccess(schedule.costCenterCode)) {
                failures.add(
                    ScheduleBatchDeleteFailure(
                        id = id,
                        errorCode = "SCHEDULE_FORBIDDEN",
                        message = "본인 담당 사업소 외 레코드는 접근할 수 없습니다"
                    )
                )
                continue
            }

            // UC-06 차단 룰: ADMIN_GRADE 외 사용자 + 확정 + FK 매칭 여사원일정 존재 시 차단
            if (!isAdmin && schedule.confirmed == true) {
                val hasLinkedSchedule = teamMemberScheduleRepository.existsByDisplayWorkSchedule(schedule)
                if (hasLinkedSchedule) {
                    failures.add(
                        ScheduleBatchDeleteFailure(
                            id = id,
                            errorCode = "SCHEDULE_DELETE_CONSTRAINT",
                            message = "확정된 스케줄에 연결된 여사원 일정이 존재하여 삭제할 수 없습니다"
                        )
                    )
                    continue
                }
            }

            schedule.isDeleted = true
            deletedCount++
        }

        return ScheduleBatchDeleteResultDto(
            deletedCount = deletedCount,
            failedCount = failures.size,
            failures = failures
        )
    }

    @Transactional
    fun deleteSchedule(scope: DataScope, userId: Long, scheduleId: Long) {
        val schedule = scheduleRepository.findById(scheduleId)
            .filter { it.isDeleted != true }
            .orElseThrow { ScheduleNotFoundException("존재하지 않거나 삭제된 스케줄입니다") }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // UC-12 사업소 가시 범위 검증 — 본인 담당 사업소 외 레코드 차단 (ADMIN_GRADE 는 무제한)
        requireScheduleScope(scope, schedule)

        val role = employee.role

        if (isAdminGrade(employee.employeeCode)) {
            schedule.isDeleted = true
            return
        }

        if (role == AppAuthority.BRANCH_MANAGER) {
            throw ScheduleDeleteForbiddenException()
        }

        // UC-06 차단 룰: 확정 + ADMIN_GRADE 외 사용자 + FK 로 본 진열마스터를 가리키는 여사원일정 존재 시 차단.
        // 레거시 SF `DisplayWorkScheduleMasterTriggerHandler.deleteCheck` 의 lookup FK 매칭
        // (WHERE DisplayWorkScheduleMaster__c IN :masterIds) 와 동등.
        if (schedule.confirmed == true) {
            val hasLinkedSchedule = teamMemberScheduleRepository.existsByDisplayWorkSchedule(schedule)
            if (hasLinkedSchedule) {
                throw ScheduleDeleteConstraintException()
            }
        }

        schedule.isDeleted = true
    }

    /**
     * UC-12 helper — 사용자 scope 의 사업소 코드 목록을 반환.
     * ADMIN_GRADE / SYSTEM_ADMIN 은 무제한 → null 반환 (repository 측 무필터).
     * LEADER / BRANCH_MANAGER 는 본인 담당 사업소 코드 목록.
     */
    private fun scopeBranchCodesOrNull(scope: DataScope): List<String>? {
        return if (scope.isAllBranches) null else scope.branchCodes
    }

    /**
     * UC-12 helper — 스케줄 entity 의 costCenterCode 가 사용자 scope 내인지 검증.
     * 위반 시 ScheduleForbiddenException.
     */
    private fun requireScheduleScope(scope: DataScope, schedule: DisplayWorkSchedule) {
        if (!scope.validateAccess(schedule.costCenterCode)) {
            throw ScheduleForbiddenException()
        }
    }

    /**
     * UC-12 helper — 등록·편집 시 「대상 사원의 costCenterCode」 가 사용자 scope 내인지 검증.
     */
    private fun requireCostCenterCodeScope(scope: DataScope, costCenterCode: String?) {
        if (!scope.validateAccess(costCenterCode)) {
            throw ScheduleForbiddenException()
        }
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
