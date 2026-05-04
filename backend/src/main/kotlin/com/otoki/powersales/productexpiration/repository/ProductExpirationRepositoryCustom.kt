package com.otoki.powersales.productexpiration.repository

import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ProductExpirationRepositoryCustom {

    fun findForAdmin(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        employeeKeyword: String?,
        accountKeyword: String?,
        status: String?,
        today: LocalDate,
        pageable: Pageable,
        employeeIds: List<Long>? = null
    ): Page<ProductExpiration>

    fun getSummary(today: LocalDate, employeeIds: List<Long>? = null): AdminProductExpirationSummaryResponse
}
