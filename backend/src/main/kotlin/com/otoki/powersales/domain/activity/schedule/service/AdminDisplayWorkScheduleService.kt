package com.otoki.powersales.domain.activity.schedule.service

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteFailure
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchUnconfirmFailure
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchUnconfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleDetailDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleDeleteConstraintException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleDeleteForbiddenException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleEditBlockedAfterAttendanceException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleEditBlockedAfterConfirmException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleEmptyFileException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleFileRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleFileTooLargeException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleForbiddenException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleHasValidationErrorsException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleInvalidFileTypeException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleRowLimitExceededException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleUploadNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleValidationException
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.internal.LastMonthRevenueLookup
import com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleDisplayStatusCalculator
import com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleValidLight
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.domain.activity.schedule.entity.QDisplayWorkSchedule.Companion.displayWorkSchedule as qDisplayWorkSchedule
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
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
class AdminDisplayWorkScheduleService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val templateGenerator: ScheduleTemplateGenerator,
    private val exportGenerator: ScheduleExportGenerator,
    private val listExcelExporter: ScheduleListExcelExporter,
    private val excelParser: ScheduleExcelParser,
    private val uploadValidator: ScheduleUploadValidator,
    private val scheduleRepository: DisplayWorkScheduleRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val lastMonthRevenueLookup: LastMonthRevenueLookup,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val branchCodeExpander: BranchCodeExpander,
    private val policyEvaluator: SharingRulePolicyEvaluator,
    private val displayStatusCalculator: ScheduleDisplayStatusCalculator,
) {

    companion object {
        private const val REDIS_KEY_PREFIX = "schedule:upload:"
        private const val REDIS_TTL_MINUTES = 30L
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_ROWS = 500

        /** 검색결과 전체 엑셀 export 최대 건수 (초과분 잘라냄 — promotion export 정합). */
        private const val EXPORT_MAX_ROWS = 50_000

        /** SF 시스템 관리자 Profile.Name ([SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME] 와 동일 값). */
        private const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"
    }

    /**
     * SF ADMIN_GRADE 동등 — Profile.Name == "시스템 관리자" OR User.isSalesSupport.
     *
     * SF 원본 분기 (`AdminDisplayWorkScheduleService` 의 ADMIN_GRADE = SYSTEM_ADMIN + SALES_SUPPORT) 정합.
     * Employee 인자만으로 판단 — 매칭 User 의 profileId + isSalesSupport 캐시 컬럼 조회.
     */
    private fun isAdminGrade(employeeCode: String?): Boolean {
        if (employeeCode == null) return false
        val user = userRepository.findByEmployeeCode(employeeCode) ?: return false
        if (user.isSalesSupport == true) return true
        val profile = user.profileId?.let { profileRepository.findById(it).orElse(null) }
        return profile?.name == SYSTEM_ADMIN_PROFILE_NAME
    }

    /**
     * UC 진열마스터 "양식(신규작성용)" Excel 다운로드 — 현재 사용자가 관할하는 지점들의 재직 여사원 명단이
     * 미리 채워진 빈 양식을 생성한다.
     *
     * ## 레거시 매핑
     * SF `DisplayWorkScheduleMasterTriggerHandler.getEmpList()` → `CurrentUserBranchNameList.getOrgList()/getBranchNames()`.
     *
     * ## 레거시 동작 요약
     * - 입력: 인증 주체 `Employee.costCenterCode` (대행 시 대행 대상) + `DataScope`.
     * - 영업지원실 판별: `scope.isAllBranches` (= `User.isSalesSupport` OR `ALL_BRANCHES_PROFILES`,
     *   레거시 `isSalesSupportOffice` 의 `UserRole.Name LIKE '%영업지원%' OR == '영업본부'` 동등). 과거의
     *   `org_cd3 == "3475"` 단독 휴리스틱(Profile 산출용 상수의 전용)을 폐기하고 인증 전반과 동일한
     *   DataScope 경로로 판별 기준을 일원화.
     * - 범위 산출: [OrganizationRepository.findTeamScheduleBranches] 가 레거시 `getOrgList()` 의
     *   isAll==true (Retail/제1사업부/영업지원1·2팀 leaf 합집합) / isAll==false (본인 hrCode 가
     *   OrgCode Level5/4/3/2 매칭되는 leaf + 사업부 게이트) 분기를 그대로 구현. 레거시에는 "전사 무필터"
     *   경로가 없어 시스템 관리자도 isAllBranches 분기(4개 조직 한정)로 수렴 — 레거시 정합.
     * - 사원 조회 키(`Employee.costCenterCode`)는 OrgCode 차원이므로 `BranchResponse.branchCode` 를 그대로
     *   `IN` 절에 사용 (BranchMapping 확장은 거래처/일정 단계용이라 사원 명단에는 미적용 — 레거시 getEmpList 가
     *   keySet 직접 사용하는 것과 정합).
     * - org 매칭 0건은 fatal 이 아니다: 레거시 `getOrgList()` 가 빈 결과를 흘려 여사원 0명(빈 양식)이 된다.
     *
     * ## 신규 차이
     * SF UserRole Object 미보존 → `findTeamScheduleBranches` 가 `Organization` OrgName/OrgCode 매칭으로
     * 동등 산출. costCenterCode null/blank 는 명시적 [MissingCostCenterException] (레거시는 단건 SOQL no-row
     * QueryException 으로 표출되던 경계).
     */
    fun generateTemplate(scope: DataScope, userId: Long): TemplateResult {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val costCenterCode = employee.costCenterCode
        if (costCenterCode.isNullOrBlank()) {
            throw MissingCostCenterException()
        }

        val branchCodes = organizationRepository
            .findTeamScheduleBranches(costCenterCode, scope.isAllBranches)
            .map { it.branchCode }
            .distinct()

        // 레거시 getEmpList: WHERE CostCenterCode__c IN: keySet ... 키셋이 비면 여사원 0명 (빈 양식).
        val employees = if (branchCodes.isEmpty()) {
            emptyList()
        } else {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                branchCodes, AppAuthority.WOMAN, "재직"
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
        // SF 가시 범위 — scope 밖 ID 는 조용히 제외 (목록과 동일한 evaluator Predicate, SF Sharing Rule row-level filter 동등)
        val policyPredicate = schedulePolicyPredicate(scope)
        val schedules = scheduleRepository.findAllById(ids)
            .filter { it.isDeleted != true }
            .filter { scheduleRepository.existsVisibleById(it.id, policyPredicate) }
            .associateBy { it.id }
        val ordered = ids.mapNotNull { schedules[it] }

        val bytes = exportGenerator.generate(ordered)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "진열스케줄_${timestamp}.xlsx"

        return TemplateResult(bytes, filename)
    }

    fun uploadAndValidate(scope: DataScope, file: MultipartFile): ScheduleUploadResultDto {
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

        // SF UplExcelBtnSchduleMasterController 정합 — 조장 지점 (BranchMapping 이력 합집합) 필터
        val expandedBranchCodes = expandUserBranchCodes(scope)

        // 사원번호/거래처코드 일괄 조회
        val employeeCodes = parseResult.rows.mapNotNull { it.employeeCode }.distinct()
        val accountCodes = parseResult.rows.mapNotNull { it.accountCode }.distinct()

        // SF checkResult L181 정합 — CostCenterCode IN :newOrgValues AND EmpCode IN :empCodes
        val usersByEmployeeCode = if (employeeCodes.isNotEmpty() && expandedBranchCodes.isNotEmpty()) {
            employeeRepository.findByCostCenterCodeInAndEmployeeCodeIn(expandedBranchCodes, employeeCodes)
                .filter { it.employeeCode != null }
                .associateBy { it.employeeCode!! }
        } else {
            emptyMap()
        }

        // SF checkResult L174 정합 — BranchCode IN :newOrgValues AND ExternalKey IN :accCodes
        val accountsByExternalKey = if (accountCodes.isNotEmpty() && expandedBranchCodes.isNotEmpty()) {
            accountRepository.findByBranchCodeInAndExternalKeyIn(expandedBranchCodes, accountCodes)
                .filter { it.externalKey != null }
                .associateBy { it.externalKey!! }
        } else {
            emptyMap()
        }

        // SF checkResult L205 정합 — CostCenterCode IN :newOrgValues AND EmpNumber IN :empCodes AND 기간 겹침
        val existingSchedules = if (employeeCodes.isNotEmpty() && expandedBranchCodes.isNotEmpty()) {
            val (earliestStart, latestEnd) = computeExcelDateRange(parseResult.rows)
            scheduleRepository.findByCostCenterCodeInAndEmployeeCodeInOverlappingPeriod(
                expandedBranchCodes, employeeCodes, earliestStart, latestEnd
            )
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
        accountType: String?,
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
            // 거래처명 input 에 거래처코드(externalKey)를 입력해도 조회되도록 OR 매칭
            accountRepository
                .findByNameContainingIgnoreCaseOrExternalKeyContainingIgnoreCase(accountName, accountName)
                .map { it.id }
        } else {
            null
        }

        // SF DisplayWorkScheduleMaster__c OWD=Private — owner / role hierarchy / sharing rule
        // (CostCenterCode 코드쌍 + CreatedById) / legacy branch OR 합성 가시 범위. 단일 코드 필터(방식 B) 대체.
        val policyPredicate = schedulePolicyPredicate(scope)

        val schedulePage = scheduleRepository.findScheduleList(
            employeeCode, accountIds, accountType, confirmed, typeOfWork3, startDateFrom, startDateTo, preset, policyPredicate, pageable
        )

        // 페이지 단위 출근등록 수 집계 (N+1 회피 — id IN + GROUP BY 1쿼리)
        val attendanceCountById = buildAttendanceCountMap(schedulePage.content.map { it.id })

        return schedulePage.map { toListItemDto(it, attendanceCountById) }
    }

    /**
     * UC-08 진열스케줄마스터 "검색결과 다운로드" — 목록(`listSchedules`)과 동일한 가시 범위/필터로 전량 추출.
     *
     * 페이징 없이 [EXPORT_MAX_ROWS] 단일 페이지로 조회 (초과분 잘라냄 — promotion export 정합).
     * 목록과 동일한 [ScheduleListItemDto] 매핑([toListItemDto]) 후 [ScheduleListExcelExporter] 로 위임.
     * 선택 레코드 export([exportSchedules]) 와 달리 ids 가 아닌 검색 조건 기반.
     */
    fun exportAllSchedules(
        scope: DataScope,
        employeeCode: String?,
        accountName: String?,
        accountType: String?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        preset: SchedulePreset?,
        sort: Sort,
    ): ExcelResult {
        val accountIds = if (!accountName.isNullOrBlank()) {
            // 거래처명 input 에 거래처코드(externalKey)를 입력해도 조회되도록 OR 매칭
            accountRepository
                .findByNameContainingIgnoreCaseOrExternalKeyContainingIgnoreCase(accountName, accountName)
                .map { it.id }
        } else {
            null
        }

        val policyPredicate = schedulePolicyPredicate(scope)
        val pageable = PageRequest.of(0, EXPORT_MAX_ROWS, sort)

        val schedulePage = scheduleRepository.findScheduleList(
            employeeCode, accountIds, accountType, confirmed, typeOfWork3, startDateFrom, startDateTo, preset, policyPredicate, pageable
        )

        val items = schedulePage.content.map { toListItemDto(it) }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return listExcelExporter.export(items, "진열스케줄_${timestamp}.xlsx")
    }

    /**
     * [com.otoki.powersales.domain.activity.schedule.repository.ScheduleListRow] projection → [ScheduleListItemDto] 매핑.
     * 목록 조회와 검색결과 엑셀 export 가 동일 매핑(재직상태 계산 포함)을 공유한다.
     */
    private fun toListItemDto(
        row: com.otoki.powersales.domain.activity.schedule.repository.ScheduleListRow,
        attendanceCountById: Map<Long, Long> = emptyMap(),
    ): ScheduleListItemDto {
        // 유효데이터 (`ValidData__c`) + 유효 신호등 (`Valid__c`) — 상세와 동일 계산식, projection raw 필드 사용
        val validData = if (row.employeeId != null) {
            displayStatusCalculator.validData(
                row.employeeStatus, row.employeeAppLoginActive, row.employeeEndDate,
                row.startDate, row.endDate
            )
        } else {
            null
        }
        return ScheduleListItemDto(
            id = row.id,
            employeeId = row.employeeId,
            employeeCode = row.employeeCode ?: "",
            employeeName = row.employeeName ?: "",
            branchName = row.branchName,
            employmentStatus = if (row.employeeId != null) {
                displayStatusCalculator.employmentStatus(
                    row.employeeStatus, row.employeeAppLoginActive, row.employeeEndDate
                )
            } else {
                null
            },
            validData = validData,
            valid = displayStatusCalculator.validLight(validData)?.name,
            accountId = row.accountId,
            accountCode = row.accountCode,
            accountName = row.accountName,
            accountType = row.accountType,
            accountStatus = row.accountStatusName,
            typeOfWork3 = row.typeOfWork3?.displayName,
            typeOfWork4 = row.typeOfWork4?.displayName,
            typeOfWork5 = row.typeOfWork5?.displayName,
            startDate = row.startDate,
            endDate = row.endDate,
            confirmed = row.confirmed,
            costCenterCode = row.costCenterCode,
            lastMonthRevenue = row.lastMonthRevenue?.toLong(),
            attendanceCount = attendanceCountById[row.id] ?: 0
        )
    }

    /**
     * 진열마스터 id 목록의 출근등록 수 집계 Map (페이지 단위 1쿼리, N+1 회피 — QueryDSL).
     * 출근 0건 마스터는 Map 에 없으므로 호출처에서 `?: 0` 으로 기본 처리.
     */
    private fun buildAttendanceCountMap(scheduleIds: List<Long>): Map<Long, Long> {
        if (scheduleIds.isEmpty()) return emptyMap()
        return teamMemberScheduleRepository.countAttendedByDisplayWorkScheduleIds(scheduleIds)
    }

    /**
     * UC-03 단건 편집 모달 상세 조회 — SF 「진열사원 스케줄 마스터」 레이아웃 정합.
     * 편집 필드 + readonly 계산 정보 (재직상태/유효데이터/유효 formula 포팅) 를 함께 반환한다.
     */
    fun getScheduleDetail(scope: DataScope, scheduleId: Long): ScheduleDetailDto {
        val schedule = scheduleRepository.findById(scheduleId)
            .filter { it.isDeleted != true }
            .orElseThrow { ScheduleNotFoundException("존재하지 않거나 삭제된 스케줄입니다") }

        // SF OWD=Private 가시 범위 검증 (목록과 동일 evaluator Predicate)
        requireScheduleScope(scope, schedule)

        val employee = schedule.employee
        val account = schedule.account

        val employmentStatus = displayStatusCalculator.employmentStatus(employee)
        val validData = displayStatusCalculator.validData(employee, schedule.startDate, schedule.endDate)
        val valid = displayStatusCalculator.validLight(validData)?.let { light ->
            when (light) {
                ScheduleValidLight.GREEN -> "유효"
                ScheduleValidLight.YELLOW -> "예정"
                ScheduleValidLight.RED -> "종료"
            }
        }

        return ScheduleDetailDto(
            id = schedule.id,
            name = schedule.name,
            confirmed = schedule.confirmed,
            employeeCode = employee?.employeeCode ?: "",
            employeeName = employee?.name ?: "",
            accountCode = account?.externalKey,
            accountName = account?.name,
            typeOfWork1 = schedule.typeOfWork1?.displayName,
            typeOfWork3 = schedule.typeOfWork3?.displayName,
            typeOfWork4 = schedule.typeOfWork4?.displayName,
            typeOfWork5 = schedule.typeOfWork5?.displayName,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            branchName = employee?.orgName,
            title = employee?.jikwee,
            employmentStatus = employmentStatus,
            accountStatus = account?.accountStatusName,
            accountType = account?.accountType,
            valid = valid,
            validData = validData,
            costCenterCode = schedule.costCenterCode,
            lastMonthRevenue = schedule.lastMonthRevenue?.toLong(),
        )
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

    /**
     * 확정 해제 — partial success. SF 레거시 정합 가드 적용:
     *  - 관리자 등급 게이트: SF Validation Rule `EditDisableForDisplayMaster` (ISCHANGED(Confirmed__c) 차단,
     *    영업지원실/시스템관리자 예외) 정합 — ADMIN_GRADE 외 사용자는 확정 해제 차단.
     *  - 사업소 가시 범위: SF OWD Private + CostCenterCode Sharing 정합 — 본인 담당 사업소 외 레코드 차단.
     *  - 출근 안전망 (SF 보다 엄격): 출근 등록(connected 여사원일정 commuteReportDatetime 채워짐)된 건은
     *    관리자라도 확정 해제 차단 — 해제 후 "미확정인데 출근 데이터 존재" 모순 방지.
     * 차단된 건은 failures 로 기록하고 나머지는 해제 진행.
     */
    @Transactional
    fun batchUnconfirm(scope: DataScope, userId: Long, ids: List<Long>): ScheduleBatchUnconfirmResultDto {
        val user = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val isAdmin = isAdminGrade(user.employeeCode)

        val schedules = scheduleRepository.findAllById(ids).associateBy { it.id }
        // SF 가시 범위 — 목록과 동일한 evaluator Predicate (루프 밖 1회 산출)
        val policyPredicate = schedulePolicyPredicate(scope)

        var updatedCount = 0
        val failures = mutableListOf<ScheduleBatchUnconfirmFailure>()

        for (id in ids) {
            val schedule = schedules[id]
            if (schedule == null || schedule.isDeleted == true) {
                failures.add(
                    ScheduleBatchUnconfirmFailure(id, "SCHEDULE_NOT_FOUND", "존재하지 않거나 삭제된 스케줄입니다")
                )
                continue
            }

            // 사업소 가시 범위 검증 — 가시 범위 외 레코드는 partial fail
            if (!scheduleRepository.existsVisibleById(schedule.id, policyPredicate)) {
                failures.add(
                    ScheduleBatchUnconfirmFailure(id, "SCHEDULE_FORBIDDEN", "본인 담당 사업소 외 레코드는 접근할 수 없습니다")
                )
                continue
            }

            // 관리자 등급 게이트 — SF Validation Rule 정합 (영업지원실/시스템관리자만 확정 해제 가능)
            if (!isAdmin) {
                failures.add(
                    ScheduleBatchUnconfirmFailure(
                        id, "SCHEDULE_UNCONFIRM_FORBIDDEN", "확정 해제 권한이 없습니다. 시스템 관리자에게 문의하십시오"
                    )
                )
                continue
            }

            // 출근 안전망 — 출근 등록된 건은 확정 해제 차단 (모순 방지)
            if (teamMemberScheduleRepository.existsByDisplayWorkScheduleAndCommuteReportDatetimeIsNotNull(schedule)) {
                failures.add(
                    ScheduleBatchUnconfirmFailure(
                        id, "SCHEDULE_UNCONFIRM_ATTENDANCE", "출근 등록된 스케줄은 확정 해제할 수 없습니다"
                    )
                )
                continue
            }

            if (schedule.confirmed == true) {
                schedule.confirmed = false
                updatedCount++
            }
        }

        return ScheduleBatchUnconfirmResultDto(
            updatedCount = updatedCount,
            failedCount = failures.size,
            failures = failures
        )
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
            typeOfWork1 = TypeOfWork1.fromDisplayNameOrNull(request.typeOfWork1) ?: TypeOfWork1.DISPLAY,
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

        // 수정 차단 룰 (ADMIN_GRADE 외 사용자 + 종료일 외 키 필드 변경 시):
        //  - UC-05: 확정(Confirmed) 된 스케줄
        //  - 출근 차단: 연결 여사원일정 중 실제 출근(commuteReportDatetime 채워짐)한 건이 있는 스케줄
        // 둘 중 하나라도 해당되면 종료일을 제외한 키 필드 변경을 차단한다.
        if (!isAdminGrade(user.employeeCode)) {
            val attendanceReported =
                teamMemberScheduleRepository.existsByDisplayWorkScheduleAndCommuteReportDatetimeIsNotNull(schedule)

            if (schedule.confirmed == true || attendanceReported) {
                val originalEmployeeCode = schedule.employee?.employeeCode
                val originalAccountCode = schedule.account?.externalKey
                val originalType1 = schedule.typeOfWork1?.displayName
                val originalType3 = schedule.typeOfWork3?.displayName
                val originalType4 = schedule.typeOfWork4?.displayName
                val originalType5 = schedule.typeOfWork5?.displayName
                val originalStart = schedule.startDate

                val blockedFieldChanged =
                    originalEmployeeCode != request.employeeCode ||
                        originalAccountCode != request.accountCode ||
                        originalType1 != request.typeOfWork1 ||
                        originalType3 != request.typeOfWork3 ||
                        originalType4 != request.typeOfWork4 ||
                        originalType5 != request.typeOfWork5 ||
                        originalStart != request.startDate
                if (blockedFieldChanged) {
                    // 확정 사유 우선, 확정 아닌데 출근만으로 막힌 경우 출근 메시지로 안내
                    if (schedule.confirmed == true) {
                        throw ScheduleEditBlockedAfterConfirmException()
                    } else {
                        throw ScheduleEditBlockedAfterAttendanceException()
                    }
                }
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
        schedule.typeOfWork1 = TypeOfWork1.fromDisplayNameOrNull(request.typeOfWork1) ?: TypeOfWork1.DISPLAY
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
        // SF 가시 범위 — 목록과 동일한 evaluator Predicate (루프 밖 1회 산출)
        val policyPredicate = schedulePolicyPredicate(scope)

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

            // SF 가시 범위 검증 — 가시 범위 외 레코드는 partial fail 로 기록
            if (!scheduleRepository.existsVisibleById(schedule.id, policyPredicate)) {
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
     * SF `DisplayWorkScheduleMaster__c` 가시 범위 Predicate 산출 ([SharingRulePolicyEvaluator]).
     *
     * OWD Private → owner / role hierarchy / sharing rule(CostCenterCode 코드쌍 + CreatedById) /
     * legacy branch OR 합성. 목록(listSchedules)과 단건 가시성 검증이 동일 기준 사용.
     */
    private fun schedulePolicyPredicate(scope: DataScope) = policyEvaluator.buildPredicate(
        scope = scope,
        sObjectName = "DisplayWorkScheduleMaster__c",
        entityPath = qDisplayWorkSchedule,
    )

    /**
     * SF 가시 범위 검증 — 스케줄 단건이 가시 범위 안인지 (목록과 동일한 evaluator Predicate).
     * 위반 시 ScheduleForbiddenException. SF OWD=Private + owner/hierarchy/sharingRule 동등.
     */
    private fun requireScheduleScope(scope: DataScope, schedule: DisplayWorkSchedule) {
        if (!scheduleRepository.existsVisibleById(schedule.id, schedulePolicyPredicate(scope))) {
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

    /**
     * SF `UplExcelBtnSchduleMasterController` 의
     * `CurrentUserBranchNameList.getBranchNames().keySet() → getIncludedBranchCode2(...)` 정합.
     *
     * `scope.isAllBranches` (영업지원실/시스템 관리자) 인 경우 SF `CurrentUserBranchNameList.getOrgList()` L35 분기와 동등 — 전사 leaf branch_codes 합집합.
     * 그 외에는 scope.branchCodes 를 base 로 사용.
     * 모든 케이스에서 BranchMapping 이력 합집합 (`BranchCodeExpander.expand`) 적용.
     */
    private fun expandUserBranchCodes(scope: DataScope): List<String> {
        val baseBranchCodes: Collection<String> = if (scope.isAllBranches) {
            organizationRepository.findAllLeafBranchCodes()
        } else {
            scope.branchCodes
        }
        if (baseBranchCodes.isEmpty()) return emptyList()
        return branchCodeExpander.expand(baseBranchCodes).toList()
    }

    /**
     * SF `UplExcelBtnSchduleMasterController.checkResult` L124-165 정합 — 엑셀 행들의 가장 이른 시작일 / 가장 늦은 종료일 산출.
     * 종료일 미지정이면 today + 5년 (SF L73-75 fallback 정합).
     */
    private fun computeExcelDateRange(rows: List<ScheduleExcelParser.ParsedRow>): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        var earliestStart = today.plusYears(100)
        var latestEnd = today.minusYears(100)
        val latestEndSentinel = latestEnd
        for (row in rows) {
            row.startDate?.let { if (it.isBefore(earliestStart)) earliestStart = it }
            row.endDate?.let { if (it.isAfter(latestEnd)) latestEnd = it }
        }
        if (latestEnd == latestEndSentinel) {
            latestEnd = today.plusYears(5)
        }
        return earliestStart to latestEnd
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

class MissingCostCenterException : BusinessException(
    errorCode = "MISSING_COST_CENTER",
    message = "소속 지점이 설정되지 않았습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
