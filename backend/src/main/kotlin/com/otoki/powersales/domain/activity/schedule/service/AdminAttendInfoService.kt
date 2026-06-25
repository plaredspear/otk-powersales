package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.inbound.service.AttendInfoToScheduleConverter
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoSearchRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoDeleteResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendInfoListItemResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberDto
import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.AttendType
import com.otoki.powersales.domain.activity.schedule.exception.AttendInfoNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoDateException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoDateRangeException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoStatusException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoTypeException
import com.otoki.powersales.domain.activity.schedule.repository.AttendInfoRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 근태정보 admin endpoint 서비스. (Spec #772)
 *
 * SAP 인바운드 측 `SapAttendInfoService` 와 분리. admin 보정 입력 / 수정 / 삭제 경로 전용.
 * UC-05/UC-06 의 레거시 한계 (after insert Trigger 만 발화) 보정 — 수정·삭제 시 명시적 cascade.
 */
@Service
@Transactional(readOnly = true)
class AdminAttendInfoService(
    private val attendInfoRepository: AttendInfoRepository,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val attendInfoToScheduleConverter: AttendInfoToScheduleConverter,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val branchCodeExpander: BranchCodeExpander,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 근무기간 조회 화면 "지점 선택" 드롭다운 옵션 — 권한별 조회 허용 지점.
     *
     * 여사원 일정관리와 동일한 권한 스코프([WomenScheduleBranchResolver]). 단일지점 사용자는 1건만
     * 반환되어 web 에서 고정 표시, 다중/전사 권한자는 선택 드롭다운으로 노출된다.
     */
    fun getBranches(principal: WebUserPrincipal): List<BranchResponse> =
        womenScheduleBranchResolver.resolveBranches(principal)

    /**
     * 근무기간 조회 화면 좌측 여사원 선택 목록.
     *
     * - `branchCode == null`: 본인 지점 스코프([WomenMemberScopeResolver]) — 기존 동작.
     * - `branchCode != null`: 다중/전사 권한자가 선택한 지점. [WomenScheduleBranchResolver] 화이트리스트로
     *   검증하여 권한 밖 지점 조회(IDOR)를 차단하고, 통과 시 해당 지점 cost center(매핑 확장)로 조회.
     *
     * 과거 근무내역 조회 화면이므로 **퇴사/휴직 등 비활성 여사원도 포함**한다
     * (findWomenByCostCenterCodes — appLoginActive 조건 제외). DTO 의 status 로 재직상태 구분/필터.
     */
    fun getMembers(principal: WebUserPrincipal, branchCode: String? = null): List<TeamMemberDto> {
        val targetCostCenterCodes: List<String> = if (branchCode.isNullOrBlank()) {
            WomenMemberScopeResolver.resolveCostCenterCodes(principal.employeeCode, principal.costCenterCode)
        } else {
            // 권한 밖 지점 조회 차단 — 화이트리스트 미포함이면 빈 목록.
            if (!womenScheduleBranchResolver.isBranchAllowed(principal, branchCode)) return emptyList()
            branchCodeExpander.expand(setOf(branchCode)).toList()
        }
        if (targetCostCenterCodes.isEmpty()) return emptyList()

        return employeeRepository.findWomenByCostCenterCodes(targetCostCenterCodes)
            .map { TeamMemberDto.from(it) }
    }

    fun search(filter: AdminAttendInfoSearchRequest, pageable: Pageable): Page<AdminAttendInfoListItemResponse> {
        val page = attendInfoRepository.searchByFilter(filter, pageable)
        val employeeCodes = page.content.mapNotNull { it.employeeCode }.distinct()
        val employeeMap = employeeRepository.findByEmployeeCodeIn(employeeCodes).associateBy { it.employeeCode }
        return page.map { AdminAttendInfoListItemResponse.from(it, employeeMap[it.employeeCode]) }
    }

    fun get(id: Long): AdminAttendInfoDetailResponse {
        val entity = attendInfoRepository.findById(id).orElseThrow { AttendInfoNotFoundException() }
        val employee = entity.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        val linked = findLinkedSchedules(entity)
        return AdminAttendInfoDetailResponse.from(entity, employee, linked)
    }

    @Transactional
    fun create(request: AdminAttendInfoCreateRequest): AdminAttendInfoDetailResponse {
        validateAttendType(request.attendType)
        val normalizedStatus = normalizeStatus(request.status)
        validateDateFormat(request.startDate, request.endDate)
        validateDateRange(request.startDate, request.endDate)

        val employee = employeeRepository.findByEmployeeCode(request.employeeCode)
            .orElseThrow { EmployeeNotFoundException(0L) }

        val entity = AttendInfo(
            employeeCode = request.employeeCode,
            attendType = request.attendType,
            startDate = request.startDate,
            endDate = request.endDate,
            status = normalizedStatus,
        )
        val saved = attendInfoRepository.save(entity)
        log.info(
            "admin attend_info create id={} employee_code={} reason={}",
            saved.id, saved.employeeCode, request.reason
        )

        val summary = attendInfoToScheduleConverter.convert(listOf(saved))
        log.info("admin attend_info create cascade {}", summary.toReason())

        val linked = findLinkedSchedules(saved)
        return AdminAttendInfoDetailResponse.from(saved, employee, linked, summary)
    }

    @Transactional
    fun update(id: Long, request: AdminAttendInfoUpdateRequest): AdminAttendInfoDetailResponse {
        val entity = attendInfoRepository.findById(id).orElseThrow { AttendInfoNotFoundException() }

        // snapshot 보존 (cascade DELETE 용)
        val beforeAttendType = entity.attendType
        val beforeStartDate = entity.startDate
        val beforeEndDate = entity.endDate
        val beforeStatus = entity.status

        request.attendType?.let {
            validateAttendType(it)
            entity.attendType = it
        }
        request.startDate?.let {
            validateDateFormat(it, request.endDate ?: entity.endDate)
            entity.startDate = it
        }
        request.endDate?.let {
            entity.endDate = it
        }
        request.status?.let {
            entity.status = normalizeStatus(it)
        }
        validateDateRange(entity.startDate, entity.endDate)
        log.info(
            "admin attend_info update id={} employee_code={} reason={}",
            entity.id, entity.employeeCode, request.reason
        )

        // Step 1 (DELETE): 변경 전 snapshot 으로 기존 연차 일정 삭제 (해당 사원·기간·연차 유형 row 전량)
        val deletedCount = cascadeDeleteSchedules(
            employeeCode = entity.employeeCode,
            startDateText = beforeStartDate,
            endDateText = beforeEndDate,
            attendType = beforeAttendType,
        )

        // Step 2 (INSERT): 변경 후 entity 로 Converter 재호출 (status='N' 분기 진입 시에만 INSERT)
        val summary = attendInfoToScheduleConverter.convert(listOf(entity))
        val totalSummary = summary.copy(deletedScheduleCount = summary.deletedScheduleCount + deletedCount)
        log.info("admin attend_info update cascade {}", totalSummary.toReason())

        val employee = entity.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        val linked = findLinkedSchedules(entity)
        return AdminAttendInfoDetailResponse.from(entity, employee, linked, totalSummary)
    }

    @Transactional
    fun delete(id: Long): AdminAttendInfoDeleteResponse {
        val entity = attendInfoRepository.findById(id).orElseThrow { AttendInfoNotFoundException() }
        log.info("admin attend_info delete id={} employee_code={}", entity.id, entity.employeeCode)

        val deletedCount = cascadeDeleteSchedules(
            employeeCode = entity.employeeCode,
            startDateText = entity.startDate,
            endDateText = entity.endDate,
            attendType = entity.attendType,
        )

        attendInfoRepository.delete(entity)
        return AdminAttendInfoDeleteResponse(deletedScheduleCount = deletedCount)
    }

    /**
     * 연결 연차 일정 미리보기 조회. attend_type 이 연차류일 때만 의미 있음.
     */
    private fun findLinkedSchedules(entity: AttendInfo): List<TeamMemberSchedule> {
        val attendType = entity.attendType ?: return emptyList()
        if (attendType !in AttendType.ANNUAL_LEAVE_CODES) return emptyList()
        val empCode = entity.employeeCode ?: return emptyList()
        val employee = employeeRepository.findByEmployeeCode(empCode).orElse(null) ?: return emptyList()
        val start = parseDate(entity.startDate) ?: return emptyList()
        val end = parseDate(entity.endDate) ?: start
        return teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
            employee, start, end, WorkingType.ANNUAL_LEAVE
        )
    }

    /**
     * UC-05/UC-06 cascade DELETE — 해당 사원·기간·연차 유형의 team_member_schedule row 전량 삭제.
     * `AttendInfoToScheduleConverter` 의 STATUS_DELETE 분기와 동일 동작을 admin 트랜잭션 안에서 인라인.
     * REQUIRES_NEW 격리 회피 — admin 호출 실패 시 함께 롤백.
     */
    private fun cascadeDeleteSchedules(
        employeeCode: String?,
        startDateText: String?,
        endDateText: String?,
        attendType: String?,
    ): Int {
        if (attendType !in AttendType.ANNUAL_LEAVE_CODES) return 0
        if (employeeCode == null) return 0
        val employee = employeeRepository.findByEmployeeCode(employeeCode).orElse(null) ?: return 0
        val start = parseDate(startDateText) ?: return 0
        val end = parseDate(endDateText) ?: start
        val existing = teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
            employee, start, end, WorkingType.ANNUAL_LEAVE
        )
        if (existing.isEmpty()) return 0
        teamMemberScheduleRepository.deleteAll(existing)
        return existing.size
    }

    private fun validateAttendType(code: String) {
        if (AttendType.fromCode(code) == null) {
            throw InvalidAttendInfoTypeException()
        }
    }

    private fun normalizeStatus(status: String): String {
        val upper = status.uppercase()
        if (upper !in ALLOWED_STATUS) {
            throw InvalidAttendInfoStatusException()
        }
        return upper
    }

    private fun validateDateFormat(start: String?, end: String?) {
        if (start != null && parseDate(start) == null) {
            throw InvalidAttendInfoDateException()
        }
        if (end != null && parseDate(end) == null) {
            throw InvalidAttendInfoDateException()
        }
    }

    private fun validateDateRange(start: String?, end: String?) {
        val s = parseDate(start) ?: return
        val e = parseDate(end) ?: return
        if (e.isBefore(s)) {
            throw InvalidAttendInfoDateRangeException()
        }
    }

    private fun parseDate(value: String?): LocalDate? = try {
        value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, DATE_FORMAT) }
    } catch (_: DateTimeParseException) {
        null
    }

    companion object {
        private val ALLOWED_STATUS = setOf("N", "Y")
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 200

        fun normalizePageable(pageable: Pageable): Pageable {
            val size = pageable.pageSize.coerceAtMost(MAX_PAGE_SIZE)
            return if (size != pageable.pageSize) PageRequest.of(pageable.pageNumber, size) else pageable
        }
    }
}
