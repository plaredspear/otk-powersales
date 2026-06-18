package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.entity.InspectionTheme
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InspectionThemeRepositoryCustom {

    /**
     * 현장점검 등록용 활성 테마 목록(레거시 fieldChk selectTheme 정합).
     * 오늘이 기간 내이고, branch_code 가 공통 화이트리스트이거나 사원 코스트센터([costCenterCode])와 일치하는 테마.
     */
    fun findActiveThemesByDate(targetDate: LocalDate, costCenterCode: String?): List<InspectionTheme>

    /**
     * admin 테마 목록 — 키워드(테마번호/이름/부서) + 부서/지점코드 필터 + 시작일 내림차순 페이징.
     *
     * @param scopeBranchCodes 본인 지점 스코프 코드 목록. `null` 이면 전사(스코프 미적용).
     *   `null` 이 아니면 전사공통 화이트리스트([InspectionThemeRepositoryCustomImpl] COMMON_BRANCH_CODES)
     *   와 합쳐 `branch_code IN (...)` 로 제한한다(모바일 [findActiveThemesByDate] 정합).
     */
    fun searchForAdmin(
        keyword: String?,
        department: String?,
        branchCode: String?,
        scopeBranchCodes: List<String>?,
        pageable: Pageable,
    ): Page<InspectionTheme>

    /** 테마별 하위 현장점검 결과(미삭제) 건수 맵. 목록 N+1 회피용 일괄 조회. */
    fun countSiteActivitiesByThemeIds(themeIds: List<Long>): Map<Long, Long>

    /** 테마번호(`Name`) 채번용 — `TM` prefix 의 최대 일련번호. 없으면 0. */
    fun findMaxThemeNumberSequence(): Long
}
