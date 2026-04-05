package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationCreateRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.internal.admin.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationListResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.common.exception.ProductNotFoundException
import com.otoki.internal.productexpiration.entity.ProductExpiration
import com.otoki.internal.productexpiration.exception.InvalidAlertDateException
import com.otoki.internal.productexpiration.exception.ProductExpirationAccountNotFoundException
import com.otoki.internal.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.internal.productexpiration.repository.ProductExpirationRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.repository.ProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminProductExpirationService(
    private val productExpirationRepository: ProductExpirationRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository
) {

    fun getList(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        employeeKeyword: String?,
        accountKeyword: String?,
        status: String?,
        pageable: Pageable
    ): AdminProductExpirationListResponse {
        val today = LocalDate.now()

        val page = productExpirationRepository.findForAdmin(
            fromDate, toDate, employeeKeyword, accountKeyword, status, today, pageable
        )

        val content = page.content.map { AdminProductExpirationResponse.from(it, today) }
        return AdminProductExpirationListResponse(
            content = content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

    fun getDetail(id: Int): AdminProductExpirationResponse {
        val entity = findById(id)
        return AdminProductExpirationResponse.from(entity)
    }

    @Transactional
    fun create(request: AdminProductExpirationCreateRequest): AdminProductExpirationResponse {
        val employee = employeeRepository.findByEmployeeCode(request.employeeCode)
            .orElseThrow { EmployeeNotFoundException() }

        val account = accountRepository.findByExternalKey(request.accountCode)
            ?: throw ProductExpirationAccountNotFoundException()

        val product = productRepository.findByProductCode(request.productCode)
            ?: throw ProductNotFoundException(request.productCode)

        val expirationDate = LocalDate.parse(request.expirationDate)
        val alarmDate = LocalDate.parse(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        val entity = ProductExpiration(
            employeeId = employee.id,
            employeeSfid = employee.sfid,
            accountId = account.id,
            accountName = account.name,
            accountCode = request.accountCode,
            productId = product.id,
            productName = product.name,
            productCode = request.productCode,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = request.description
        )

        val saved = productExpirationRepository.save(entity)
        // Reload to populate employee relation for response
        return getDetail(saved.productExpirationId)
    }

    @Transactional
    fun update(id: Int, request: AdminProductExpirationUpdateRequest): AdminProductExpirationResponse {
        val entity = findById(id)

        val expirationDate = LocalDate.parse(request.expirationDate)
        val alarmDate = LocalDate.parse(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        entity.update(expirationDate, alarmDate, request.description)
        return AdminProductExpirationResponse.from(entity)
    }

    @Transactional
    fun delete(id: Int) {
        val entity = findById(id)
        productExpirationRepository.delete(entity)
    }

    @Transactional
    fun batchDelete(request: AdminProductExpirationBatchDeleteRequest): AdminProductExpirationBatchDeleteResponse {
        val entities = productExpirationRepository.findAllById(request.ids)
        if (entities.size != request.ids.size) {
            throw ProductExpirationNotFoundException()
        }
        productExpirationRepository.deleteAll(entities)
        return AdminProductExpirationBatchDeleteResponse(deletedCount = entities.size)
    }

    fun getSummary(): AdminProductExpirationSummaryResponse {
        return productExpirationRepository.getSummary(LocalDate.now())
    }

    private fun findById(id: Int): ProductExpiration {
        return productExpirationRepository.findById(id)
            .orElseThrow { ProductExpirationNotFoundException() }
    }
}
