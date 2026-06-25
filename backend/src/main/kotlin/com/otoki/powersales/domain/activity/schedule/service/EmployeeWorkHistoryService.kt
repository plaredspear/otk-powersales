package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryItem
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
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
    private val excelExporter: EmployeeWorkHistoryExcelExporter,
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
            .findByEmployeeAndWorkingDateBetweenAndAttendanceLogIsNotNullOrderByWorkingDateAscCreatedAtAsc(
                employee,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth(),
            )
        return EmployeeWorkHistoryResponse(items = schedules.map { EmployeeWorkHistoryItem.from(it) })
    }

    /**
     * 근무기간 조회(월별) 엑셀 export — 목록 탭과 동일 데이터/컬럼. 파일명: 월별근무내역_{사번}_{yyyyMM}.xlsx
     */
    fun exportMonthlyHistory(employeeId: Long, yearMonth: YearMonth): ExcelResult {
        val employee = employeeRepository.findById(employeeId).orElseThrow {
            EmployeeNotFoundException(employeeId)
        }
        val items = teamMemberScheduleRepository
            .findByEmployeeAndWorkingDateBetweenAndAttendanceLogIsNotNullOrderByWorkingDateAscCreatedAtAsc(
                employee,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth(),
            )
            .map { EmployeeWorkHistoryItem.from(it) }
        val filename = "월별근무내역_${employee.employeeCode.orEmpty()}_${yearMonth.format(FILENAME_MONTH_FMT)}.xlsx"
        return excelExporter.export(items, filename)
    }

    companion object {
        const val DEFAULT_LIMIT = 10
        private val FILENAME_MONTH_FMT: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyyMM")
    }
}
