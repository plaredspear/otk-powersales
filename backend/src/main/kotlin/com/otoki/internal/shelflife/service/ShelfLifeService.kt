package com.otoki.internal.shelflife.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.shelflife.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.shelflife.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.shelflife.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.shelflife.dto.response.ShelfLifeItemResponse
import com.otoki.internal.shelflife.entity.ShelfLife
import com.otoki.internal.shelflife.exception.InvalidAlertDateException
import com.otoki.internal.shelflife.exception.InvalidShelfLifeDateRangeException
import com.otoki.internal.shelflife.exception.ShelfLifeForbiddenException
import com.otoki.internal.shelflife.exception.ShelfLifeNotFoundException
import com.otoki.internal.shelflife.repository.ShelfLifeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class ShelfLifeService(
    private val shelfLifeRepository: ShelfLifeRepository,
    private val userRepository: UserRepository
) {

    fun getShelfLifeList(
        userId: Long, accountCode: String?, fromDate: String, toDate: String
    ): List<ShelfLifeItemResponse> {
        val parsedFromDate = parseDate(fromDate)
        val parsedToDate = parseDate(toDate)
        validateDateRange(parsedFromDate, parsedToDate)

        val employeeId = getEmployeeId(userId) ?: return emptyList()

        val items = if (accountCode != null) {
            shelfLifeRepository.findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
                employeeId, accountCode, parsedFromDate, parsedToDate
            )
        } else {
            shelfLifeRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                employeeId, parsedFromDate, parsedToDate
            )
        }

        val today = LocalDate.now()
        return items.map { ShelfLifeItemResponse.from(it, today) }
    }

    @Transactional
    fun createShelfLife(userId: Long, request: ShelfLifeCreateRequest): ShelfLifeItemResponse {
        val employeeId = getEmployeeId(userId)
            ?: throw UserNotFoundException()

        val expirationDate = parseDate(request.expirationDate)
        val alarmDate = parseDate(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        val shelfLife = ShelfLife(
            employeeId = employeeId,
            accountCode = request.accountCode,
            accountId = request.accountName,
            productCode = request.productCode,
            productId = request.productName,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = request.description,
            instDt = LocalDateTime.now()
        )

        val saved = shelfLifeRepository.save(shelfLife)
        return ShelfLifeItemResponse.from(saved)
    }

    @Transactional
    fun updateShelfLife(userId: Long, seq: Int, request: ShelfLifeUpdateRequest): ShelfLifeItemResponse {
        val employeeId = getEmployeeId(userId)
            ?: throw UserNotFoundException()
        val shelfLife = findBySeq(seq)
        validateOwnership(shelfLife, employeeId)

        val expirationDate = parseDate(request.expirationDate)
        val alarmDate = parseDate(request.alarmDate)

        if (!alarmDate.isBefore(expirationDate)) {
            throw InvalidAlertDateException()
        }

        shelfLife.update(expirationDate, alarmDate, request.description)
        return ShelfLifeItemResponse.from(shelfLife)
    }

    @Transactional
    fun deleteShelfLife(userId: Long, seq: Int) {
        val employeeId = getEmployeeId(userId)
            ?: throw UserNotFoundException()
        val shelfLife = findBySeq(seq)
        validateOwnership(shelfLife, employeeId)
        shelfLifeRepository.delete(shelfLife)
    }

    @Transactional
    fun deleteShelfLifeBatch(userId: Long, request: ShelfLifeBatchDeleteRequest): ShelfLifeBatchDeleteResponse {
        val employeeId = getEmployeeId(userId)
            ?: throw UserNotFoundException()

        val items = shelfLifeRepository.findBySeqInAndEmployeeId(request.ids, employeeId)

        if (items.size != request.ids.size) {
            val foundSeqs = items.map { it.seq }.toSet()
            val missingSeqs = request.ids.filter { it !in foundSeqs }

            val allExisting = shelfLifeRepository.findAllById(missingSeqs)
            if (allExisting.isNotEmpty()) {
                throw ShelfLifeForbiddenException()
            }
        }

        shelfLifeRepository.deleteAll(items)
        return ShelfLifeBatchDeleteResponse(deletedCount = items.size)
    }

    private fun getEmployeeId(userId: Long): String? {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        return user.sfid
    }

    private fun findBySeq(seq: Int): ShelfLife {
        return shelfLifeRepository.findById(seq)
            .orElseThrow { ShelfLifeNotFoundException() }
    }

    private fun validateOwnership(shelfLife: ShelfLife, employeeId: String) {
        if (shelfLife.employeeId != employeeId) {
            throw ShelfLifeForbiddenException()
        }
    }

    private fun validateDateRange(fromDate: LocalDate, toDate: LocalDate) {
        if (toDate.isBefore(fromDate)) {
            throw InvalidShelfLifeDateRangeException("날짜 범위가 올바르지 않습니다")
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > 180) {
            throw InvalidShelfLifeDateRangeException("날짜 범위가 올바르지 않습니다")
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            throw InvalidShelfLifeDateRangeException("유효하지 않은 요청입니다")
        }
    }
}
