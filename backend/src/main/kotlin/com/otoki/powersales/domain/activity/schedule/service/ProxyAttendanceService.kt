package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.request.ProxyAttendanceRegisterRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceBranchNotAllowedException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceNotAllowedException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceNotBranchMemberException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * AccountViewAll 대리출근 서비스 — 지점 선택형 대리출근.
 *
 * ## 레거시 매핑
 * 조장 대리출근(레거시 `mngDaily addScheduleProc`) 과 동일한 SF WorkReport 등록 경로를 공유하되,
 * 실행 주체가 조장이 아니라 AccountViewAll(전체 거래처 조회 권한) 사용자다. 조장은 본인 소속
 * costCenterCode 로 팀이 자동 고정되지만, AccountViewAll 은 전사 성격이라 **지점을 직접 선택**한 뒤
 * 그 지점 여사원을 대상으로 대리출근을 등록한다.
 *
 * ## 레거시 동작 요약
 * - 입력: 선택 지점(branchCode) + 대상 여사원(targetEmployeeId) + 진열/행사 식별자.
 * - 분기: 권한(AccountViewAll) 검증 → 지점 허용(IDOR) 검증 → 대상 여사원이 그 지점 소속인지 검증.
 * - 외부 호출: 실제 출근 등록은 [AttendanceService.registerProxy] 위임 (Orora WorkReport, GPS 스킵).
 * - 부수 효과: attendance_log 생성 + TMS 백링크 + 안전점검 stamp (registerProxy 내부).
 *
 * ## 신규 차이
 * - 지점 스코프가 조장 costCenterCode(자동) → 선택 지점(사용자 지정 + 서버 IDOR 재검증) 으로 변경.
 * - 권한 가드가 조장(LEADER) → AccountViewAll 로 변경.
 * - 지점 목록/여사원 스코프 = 대시보드 34개 화이트리스트 기준([ProxyAttendanceBranchResolver]).
 */
@Service
@Transactional(readOnly = true)
class ProxyAttendanceService(
    private val employeeRepository: EmployeeRepository,
    private val proxyAttendanceBranchResolver: ProxyAttendanceBranchResolver,
    private val teamDailyStatusCalculator: TeamDailyStatusCalculator,
    private val attendanceService: AttendanceService,
) {

    /** 대리출근 지점 선택 옵션 조회 — AccountViewAll 만 허용. */
    fun getBranches(registrantId: Long): List<BranchResponse> {
        requireAccountViewAll(findRegistrant(registrantId))
        return proxyAttendanceBranchResolver.resolveBranches()
    }

    /**
     * 선택 지점의 여사원 목록 조회 — 조장·지점장 제외(역필터), 퇴직자 제외, 이름순.
     * 지점(branchCode)이 대리출근 허용 목록에 없으면 IDOR 차단.
     */
    fun getTeamMembers(registrantId: Long, branchCode: String): List<LeaderTeamMemberListResponse> {
        requireAccountViewAll(findRegistrant(registrantId))
        requireBranchAllowed(branchCode)

        return employeeRepository
            .findByCostCenterCodeAndRoleNotIn(
                branchCode,
                listOf(AppAuthority.LEADER, AppAuthority.BRANCH_MANAGER)
            )
            .filter { it.status != EmploymentStatus.RESIGNED.code }
            .sortedBy { it.name }
            .map { LeaderTeamMemberListResponse.from(it) }
    }

    /**
     * 선택 지점 여사원의 특정 날짜 일별 현황(진열/행사/연차 + 요약) 조회.
     * 조장 일별현황과 동일한 [TeamDailyStatusCalculator] 코어를 지점 인원 기준으로 호출.
     */
    fun getDailyStatus(registrantId: Long, branchCode: String, date: LocalDate): LeaderDailyStatusResponse {
        requireAccountViewAll(findRegistrant(registrantId))
        requireBranchAllowed(branchCode)

        val branchEmployeeIds = employeeRepository
            .findByCostCenterCodeIn(listOf(branchCode))
            .mapNotNull { it.id.takeIf { v -> v != 0L } }

        return teamDailyStatusCalculator.computeDailyStatus(branchEmployeeIds, date)
    }

    /**
     * 대리출근 등록. 권한(AccountViewAll) + 지점 허용(IDOR) + 대상 여사원 지점 소속 검증 후,
     * 실제 출근 등록은 [AttendanceService.registerProxy] 에 위임 (GPS 스킵).
     */
    @Transactional
    fun registerProxyAttendance(
        registrantId: Long,
        request: ProxyAttendanceRegisterRequest
    ): AttendanceRegisterResponse {
        requireAccountViewAll(findRegistrant(registrantId))

        val branchCode = request.branchCode?.takeIf { it.isNotBlank() }
            ?: throw ProxyAttendanceBranchNotAllowedException()
        requireBranchAllowed(branchCode)

        val targetEmployeeId = request.targetEmployeeId
            ?: throw LeaderScheduleTargetEmployeeNotFoundException()
        val targetEmployee = employeeRepository.findById(targetEmployeeId)
            .orElseThrow { LeaderScheduleTargetEmployeeNotFoundException() }

        // 대상 여사원이 선택 지점 소속인지 검증 (타 지점 여사원 대리출근 차단)
        if (targetEmployee.costCenterCode != branchCode) {
            throw ProxyAttendanceNotBranchMemberException()
        }

        return attendanceService.registerProxy(
            targetEmployee = targetEmployee,
            scheduleId = request.scheduleId,
            displayWorkScheduleId = request.displayWorkScheduleId
        )
    }

    private fun findRegistrant(registrantId: Long): Employee =
        employeeRepository.findById(registrantId)
            .orElseThrow { EmployeeNotFoundException() }

    private fun requireAccountViewAll(employee: Employee) {
        if (employee.role != AppAuthority.ACCOUNT_VIEW_ALL) {
            throw ProxyAttendanceNotAllowedException()
        }
    }

    private fun requireBranchAllowed(branchCode: String) {
        if (!proxyAttendanceBranchResolver.isBranchAllowed(branchCode)) {
            throw ProxyAttendanceBranchNotAllowedException()
        }
    }
}
