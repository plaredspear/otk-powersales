package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityFilter
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
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
        accountId: Int?,
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
}
