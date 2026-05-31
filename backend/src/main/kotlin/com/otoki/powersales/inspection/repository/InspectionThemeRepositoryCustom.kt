package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.InspectionTheme
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InspectionThemeRepositoryCustom {

    fun findActiveThemesByDate(targetDate: LocalDate): List<InspectionTheme>

    /** admin 테마 목록 — 키워드(테마번호/이름/부서) + 부서/지점코드 필터 + 시작일 내림차순 페이징. */
    fun searchForAdmin(
        keyword: String?,
        department: String?,
        branchCode: String?,
        pageable: Pageable,
    ): Page<InspectionTheme>

    /** 테마별 하위 현장점검 결과(미삭제) 건수 맵. 목록 N+1 회피용 일괄 조회. */
    fun countSiteActivitiesByThemeIds(themeIds: List<Long>): Map<Long, Long>

    /** 테마번호(`Name`) 채번용 — `TM` prefix 의 최대 일련번호. 없으면 0. */
    fun findMaxThemeNumberSequence(): Long
}
