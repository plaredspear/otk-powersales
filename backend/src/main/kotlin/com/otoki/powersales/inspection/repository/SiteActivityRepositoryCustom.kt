package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import java.time.LocalDate

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
}
