package com.otoki.powersales.productexpiration.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationBatchDeleteRequest
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationCreateRequest
import com.otoki.powersales.productexpiration.dto.request.ProductExpirationUpdateRequest
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationBatchDeleteResponse
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationItemResponse
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.InvalidProductExpirationDateRangeException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ProductExpirationService(
    private val productExpirationRepository: ProductExpirationRepository,
    private val employeeRepository: EmployeeRepository
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

        val productExpiration = ProductExpiration(
            employeeId = employeeId,
            accountCode = request.accountCode,
            accountName = request.accountName,
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
