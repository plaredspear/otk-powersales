package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryItem
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}
