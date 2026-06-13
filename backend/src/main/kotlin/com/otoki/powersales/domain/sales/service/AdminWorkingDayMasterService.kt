package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.WorkingDayMasterListResponse
import com.otoki.powersales.domain.sales.repository.WorkingDayMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

/**
 * 관리자 웹 영업일관리마스터(SF `WorkingDayMaster__c`) 조회 서비스.
 *
 * 운영이 직접 관리하는 영업일 달력을 연-월 단위로 조회한다. 등록/수정/삭제는 미제공(조회 전용) —
 * 데이터 권위는 SF (SF → RDS 단방향 마이그레이션 Stage1).
 */
@Service
class AdminWorkingDayMasterService(
    private val workingDayMasterRepository: WorkingDayMasterRepository,
) {

    /**
     * 지정 연-월의 영업일 달력 조회. soft-delete row 제외, 일자 오름차순.
     */
    @Transactional(readOnly = true)
    fun getWorkingDayMasters(year: Int, month: Int): WorkingDayMasterListResponse {
        require(month in 1..12) { "연도와 월을 확인해주세요" }
        val yearMonth = YearMonth.of(year, month)
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        val entities = workingDayMasterRepository.findByWorkingDateRange(start, end)
        return WorkingDayMasterListResponse.from(entities)
    }

    /**
     * 임의 일자 구간의 영업일 달력 조회. soft-delete row 제외, 일자 오름차순.
     */
    @Transactional(readOnly = true)
    fun getWorkingDayMasters(start: LocalDate, end: LocalDate): WorkingDayMasterListResponse {
        val entities = workingDayMasterRepository.findByWorkingDateRange(start, end)
        return WorkingDayMasterListResponse.from(entities)
    }
}
