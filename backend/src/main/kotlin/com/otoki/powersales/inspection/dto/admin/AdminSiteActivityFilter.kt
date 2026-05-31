package com.otoki.powersales.inspection.dto.admin

import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import java.time.LocalDate

/**
 * admin 현장점검 목록 검색 조건 wrapper.
 *
 * 기간(activityDate) + 옵션 필터(거래처코드 / 분류 / 현장유형 / 사원명). null/blank 는 무시.
 */
data class AdminSiteActivityFilter(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val category: InspectionCategory? = null,
    val fieldType: InspectionFieldType? = null,
    val employeeName: String? = null,
    val accountCode: String? = null
)
