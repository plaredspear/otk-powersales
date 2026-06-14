package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminSiteActivityFilter
import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import com.otoki.powersales.domain.activity.inspection.enums.InspectionCategory
import com.querydsl.core.types.Predicate
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SiteActivityRepositoryCustom {

    /**
     * 사원 본인의 현장점검 목록 검색.
     *
     * 기간(activityDate) 필수, accountId / category(productType) 옵션 필터. activityDate DESC 정렬.
     */
    fun searchByEmployee(
        employeeId: Long,
        accountId: Long?,
        category: InspectionCategory?,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<SiteActivity>

    /**
     * admin 현장점검 목록 검색 — SharingRule 가시 범위(policyPredicate) + 필터 + 페이징.
     */
    fun searchForAdmin(
        policyPredicate: Predicate,
        filter: AdminSiteActivityFilter,
        pageable: Pageable
    ): Page<SiteActivity>

    /**
     * 가시 범위(policyPredicate) 내에 해당 id 의 미삭제 현장점검이 존재하는지.
     */
    fun existsVisibleById(policyPredicate: Predicate, id: Long): Boolean

    /**
     * 테마(InspectionTheme) 하위 현장점검 결과 목록 — 미삭제 + account/employee/product fetch join.
     * 테마 상세 관련목록 + 엑셀 export 공용. activityDate DESC, id DESC 정렬.
     */
    fun findByInspectionThemeIdForAdmin(themeId: Long): List<SiteActivity>
}
