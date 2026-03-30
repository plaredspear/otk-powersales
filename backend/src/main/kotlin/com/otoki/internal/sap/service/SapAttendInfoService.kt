package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapAttendInfoRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.AttendInfo
import com.otoki.internal.sap.entity.AttendType
import com.otoki.internal.sap.repository.AttendInfoRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SapAttendInfoService(
    private val attendInfoRepository: AttendInfoRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val employeeRepository: EmployeeRepository
) : SapSyncService<SapAttendInfoRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CHUNK_SIZE = 5_000
        const val WORKING_TYPE_ANNUAL_LEAVE = "연차"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    override fun sync(items: List<SapAttendInfoRequest.ReqItem>): SapSyncResult {
        var totalSuccess = 0
        val allErrors = mutableListOf<SapSyncError>()

        items.chunked(CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
            try {
                val result = syncChunk(chunk, chunkIndex * CHUNK_SIZE)
                totalSuccess += result.successCount
                allErrors.addAll(result.errors)
            } catch (e: Exception) {
                log.error("출퇴근 청크 처리 실패: chunkIndex={}, error={}", chunkIndex, e.message)
                chunk.forEachIndexed { index, item ->
                    allErrors.add(
                        SapSyncError(
                            index = chunkIndex * CHUNK_SIZE + index,
                            field = "chunk",
                            value = item.employeeCode,
                            error = "Chunk failed: ${e.message}"
                        )
                    )
                }
            }
        }

        return SapSyncResult(
            successCount = totalSuccess,
            failCount = allErrors.size,
            errors = allErrors
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun syncChunk(chunk: List<SapAttendInfoRequest.ReqItem>, baseIndex: Int): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        chunk.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("출퇴근 동기화 실패: index={}, employeeCode={}, error={}",
                    baseIndex + index, item.employeeCode, e.message)
                errors.add(
                    SapSyncError(
                        index = baseIndex + index,
                        field = "employee_code",
                        value = item.employeeCode,
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        return SapSyncResult(
            successCount = successCount,
            failCount = errors.size,
            errors = errors
        )
    }

    private fun syncItem(item: SapAttendInfoRequest.ReqItem) {
        val employeeCode = item.employeeCode
            ?: throw IllegalArgumentException("employee_code is required")
        val startDate = item.startDate
            ?: throw IllegalArgumentException("start_date is required")

        val attendInfo = AttendInfo(
            employeeCode = employeeCode,
            startDate = startDate,
            endDate = item.endDate,
            attendType = item.attendType,
            status = item.status
        )

        attendInfoRepository.save(attendInfo)

        processAnnualLeaveSchedule(attendInfo)
    }

    private fun processAnnualLeaveSchedule(attendInfo: AttendInfo) {
        val attendType = attendInfo.attendType ?: return
        val type = AttendType.fromCode(attendType) ?: return
        if (!type.isAnnualLeave) return

        val dates = parseDateRange(attendInfo.startDate, attendInfo.endDate)
        if (dates.isEmpty()) return

        val employeeCode = attendInfo.employeeCode
        val employee = employeeRepository.findByEmployeeCode(employeeCode).orElse(null)
        if (employee == null) {
            log.warn("연차 스케줄 처리 실패 - 사원 조회 불가: employeeCode={}", employeeCode)
            return
        }

        if (attendInfo.status == "Y") {
            deleteAnnualLeaveSchedules(employee.id, employeeCode, dates)
        } else {
            createAnnualLeaveSchedules(employee.id, employeeCode, dates)
        }
    }

    private fun createAnnualLeaveSchedules(employeeId: Long, employeeCode: String, dates: List<LocalDate>) {
        for (date in dates) {
            val exists = teamMemberScheduleRepository.existsByEmployeeIdAndWorkingDateAndWorkingType(
                employeeId, date, WORKING_TYPE_ANNUAL_LEAVE
            )
            if (exists) {
                log.debug("연차 스케줄 이미 존재: employeeCode={}, date={}", employeeCode, date)
                continue
            }

            val schedule = TeamMemberSchedule(
                employeeId = employeeId,
                workingDate = date,
                workingType = WORKING_TYPE_ANNUAL_LEAVE
            )
            teamMemberScheduleRepository.save(schedule)
            log.debug("연차 스케줄 생성: employeeCode={}, date={}", employeeCode, date)
        }
    }

    private fun deleteAnnualLeaveSchedules(employeeId: Long, employeeCode: String, dates: List<LocalDate>) {
        val from = dates.min()
        val to = dates.max()
        val deletedCount = teamMemberScheduleRepository.deleteAnnualLeaveByEmployeeIdAndDateRange(
            employeeId, from, to
        )
        log.info("연차 스케줄 삭제: employeeCode={}, from={}, to={}, deletedCount={}", employeeCode, from, to, deletedCount)
    }

    private fun parseDateRange(startDateStr: String, endDateStr: String?): List<LocalDate> {
        return try {
            val startDate = LocalDate.parse(startDateStr, DATE_FORMAT)
            val endDate = if (endDateStr.isNullOrBlank()) startDate else LocalDate.parse(endDateStr, DATE_FORMAT)
            generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(endDate) }
                .toList()
        } catch (e: Exception) {
            log.warn("날짜 파싱 실패: startDate={}, endDate={}, error={}", startDateStr, endDateStr, e.message)
            emptyList()
        }
    }
}
