package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendanceLogSearchRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendanceLogDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AdminAttendanceLogListItemResponse
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceLogNotFoundException
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 출근현황 (AttendanceLog / DKRetail__CommuteLog__c) admin 조회 서비스.
 *
 * 본 객체는 mobile 의 출근 등록 (`AttendanceService.register`) 이 INSERT 하는 row 의
 * admin 조회 전용. 수정/삭제 API 는 제공하지 않는다 — 등록 이력 성격.
 */
@Service
@Transactional(readOnly = true)
class AdminAttendanceLogService(
    private val attendanceLogRepository: AttendanceLogRepository,
) {

    fun search(filter: AdminAttendanceLogSearchRequest, pageable: Pageable): Page<AdminAttendanceLogListItemResponse> =
        attendanceLogRepository.searchByFilter(filter, pageable)
            .map { AdminAttendanceLogListItemResponse.from(it) }

    fun get(id: Long): AdminAttendanceLogDetailResponse {
        val entity = attendanceLogRepository.findById(id).orElseThrow { AttendanceLogNotFoundException() }
        return AdminAttendanceLogDetailResponse.from(entity)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 200

        fun normalizePageable(pageable: Pageable): Pageable {
            val size = pageable.pageSize.coerceAtMost(MAX_PAGE_SIZE)
            return if (size != pageable.pageSize) PageRequest.of(pageable.pageNumber, size) else pageable
        }
    }
}
