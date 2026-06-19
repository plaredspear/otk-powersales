package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryItem
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

/**
 * 여사원 상세 페이지 — 근무이력(TeamMemberSchedule) 시간순서별 조회.
 *
 * 동일 working_date 다건 대비 created_at 보조 정렬.
 */
@Service
@Transactional(readOnly = true)
class EmployeeWorkHistoryService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    fun getRecentHistory(employeeId: Long, limit: Int): EmployeeWorkHistoryResponse {
        val employee = employeeRepository.findById(employeeId).orElseThrow {
            EmployeeNotFoundException(employeeId)
        }
        val schedules = teamMemberScheduleRepository
            .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(employee, PageRequest.of(0, limit))
        return EmployeeWorkHistoryResponse(items = schedules.map { EmployeeWorkHistoryItem.from(it) })
    }

    /**
     * 근무기간 조회(월별) — 특정 인원 1명의 지정 월 근무내역을 일자 오름차순으로 반환.
     * "어디서(거래처/지점)/어떻게(근무유형·진열·행사·고정·순회)" 표현 + 캘린더/요약 인사이트 렌더용.
     */
    fun getMonthlyHistory(employeeId: Long, yearMonth: YearMonth): EmployeeWorkHistoryResponse {
        val employee = employeeRepository.findById(employeeId).orElseThrow {
            EmployeeNotFoundException(employeeId)
        }
        val schedules = teamMemberScheduleRepository
            .findByEmployeeAndWorkingDateBetweenOrderByWorkingDateAscCreatedAtAsc(
                employee,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth(),
            )
        return EmployeeWorkHistoryResponse(items = schedules.map { EmployeeWorkHistoryItem.from(it) })
    }

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}
