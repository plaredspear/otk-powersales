package com.otoki.powersales.domain.activity.productexpiration.service

import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationBatchDeleteRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationCreateRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationUpdateRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.response.ProductExpirationBatchDeleteResponse
import com.otoki.powersales.domain.activity.productexpiration.dto.response.ProductExpirationItemResponse
import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import com.otoki.powersales.domain.activity.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.domain.activity.productexpiration.exception.InvalidProductExpirationDateRangeException
import com.otoki.powersales.domain.activity.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.domain.activity.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ProductExpirationService(
    private val productExpirationRepository: ProductExpirationRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository
) {

    fun getProductExpirationList(
        userId: Long, accountCode: String?, fromDate: String, toDate: String
    ): List<ProductExpirationItemResponse> {
        val parsedFromDate = parseDate(fromDate)
        val parsedToDate = parseDate(toDate)
        validateDateRange(parsedFromDate, parsedToDate)

        val employeeId = getUserId(userId) ?: return emptyList()

        val items = if (accountCode != null) {
            productExpirationRepository.findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
                employeeId, accountCode, parsedFromDate, parsedToDate
            )
        } else {
            productExpirationRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                employeeId, parsedFromDate, parsedToDate
            )
        }

        val today = LocalDate.now()
        return items.map { ProductExpirationItemResponse.from(it, today) }
    }

    @Transactional
    fun createProductExpiration(userId: Long, request: ProductExpirationCreateRequest): ProductExpirationItemResponse {
        val employeeId = getUserId(userId)
            ?: throw EmployeeNotFoundException()

        val expirationDate = parseDate(request.expirationDate)
        val alarmDate = parseDate(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        // FK 기반 등록 — 거래처 코드(SAP거래처코드=account.external_key) / 제품 코드(product.product_code)
        // 로 신규 account/product 를 조회해 FK 를 채운다. 매칭되는 entity 가 없으면 FK 는 NULL 로 두고
        // 코드/명 텍스트만 저장 (레거시 동작과 호환 — 등록 자체는 차단하지 않음).
        val accountId = accountRepository.findByExternalKey(request.accountCode)?.id
        val productId = productRepository.findByProductCode(request.productCode)?.id

        val productExpiration = ProductExpiration(
            employeeId = employeeId,
            accountId = accountId,
            accountCode = request.accountCode,
            accountName = request.accountName,
            productId = productId,
            productCode = request.productCode,
            productName = request.productName,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = request.description
        )

        val saved = productExpirationRepository.save(productExpiration)
        return ProductExpirationItemResponse.from(saved)
    }

    @Transactional
    fun updateProductExpiration(userId: Long, seq: Int, request: ProductExpirationUpdateRequest): ProductExpirationItemResponse {
        val employeeId = getUserId(userId)
            ?: throw EmployeeNotFoundException()
        val productExpiration = findBySeq(seq)
        validateOwnership(productExpiration, employeeId)

        val expirationDate = parseDate(request.expirationDate)
        val alarmDate = parseDate(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        productExpiration.update(expirationDate, alarmDate, request.description)
        return ProductExpirationItemResponse.from(productExpiration)
    }

    @Transactional
    fun deleteProductExpiration(userId: Long, seq: Int) {
        val employeeId = getUserId(userId)
            ?: throw EmployeeNotFoundException()
        val productExpiration = findBySeq(seq)
        validateOwnership(productExpiration, employeeId)
        productExpirationRepository.delete(productExpiration)
    }

    @Transactional
    fun deleteProductExpirationBatch(userId: Long, request: ProductExpirationBatchDeleteRequest): ProductExpirationBatchDeleteResponse {
        val employeeId = getUserId(userId)
            ?: throw EmployeeNotFoundException()

        val items = productExpirationRepository.findBySeqInAndEmployeeId(request.ids, employeeId)

        if (items.size != request.ids.size) {
            val foundSeqs = items.map { it.seq }.toSet()
            val missingSeqs = request.ids.filter { it !in foundSeqs }

            val allExisting = productExpirationRepository.findAllById(missingSeqs)
            if (allExisting.isNotEmpty()) {
                throw ProductExpirationForbiddenException()
            }
        }

        productExpirationRepository.deleteAll(items)
        return ProductExpirationBatchDeleteResponse(deletedCount = items.size)
    }

    private fun getUserId(userId: Long): Long? {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        return employee.id
    }

    private fun findBySeq(seq: Int): ProductExpiration {
        return productExpirationRepository.findById(seq)
            .orElseThrow { ProductExpirationNotFoundException() }
    }

    private fun validateOwnership(productExpiration: ProductExpiration, employeeId: Long) {
        if (productExpiration.employeeId != employeeId) {
            throw ProductExpirationForbiddenException()
        }
    }

    private fun validateDateRange(fromDate: LocalDate, toDate: LocalDate) {
        if (toDate.isBefore(fromDate)) {
            throw InvalidProductExpirationDateRangeException("날짜 범위가 올바르지 않습니다")
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > 180) {
            throw InvalidProductExpirationDateRangeException("날짜 범위가 올바르지 않습니다")
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            throw InvalidProductExpirationDateRangeException("유효하지 않은 요청입니다")
        }
    }
}
