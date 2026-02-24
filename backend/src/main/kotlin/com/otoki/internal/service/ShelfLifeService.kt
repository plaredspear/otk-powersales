package com.otoki.internal.service

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ShelfLife Entity가 V1 스키마로 리매핑되어 @ManyToOne 관계(user, store, product)가
 * raw String 컬럼으로 변환됨. 기존 비즈니스 로직이 V2 Entity 구조를 직접 참조하므로
 * 컴파일 오류 발생 → 전체 주석 처리.
 * Service/Controller 비즈니스 로직 재작성은 별도 스펙에서 수행.

import com.otoki.internal.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.dto.response.ShelfLifeItemResponse
import com.otoki.internal.dto.response.ShelfLifeListResponse
import com.otoki.internal.entity.ShelfLife
import com.otoki.internal.exception.*
import com.otoki.internal.common.exception.*
import com.otoki.internal.product.repository.ProductRepository
import com.otoki.internal.repository.ShelfLifeRepository
import com.otoki.internal.repository.AccountRepository
import com.otoki.internal.common.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Service
class ShelfLifeService(
    private val shelfLifeRepository: ShelfLifeRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository
) {

    @Transactional(readOnly = true)
    fun getShelfLifeList(userId: Long, storeId: Long?, fromDate: String, toDate: String): ShelfLifeListResponse {
        val parsedFromDate = parseDate(fromDate)
        val parsedToDate = parseDate(toDate)

        if (parsedToDate.isBefore(parsedFromDate)) {
            throw InvalidShelfLifeDateRangeException("종료일은 시작일 이후여야 합니다")
        }
        if (ChronoUnit.MONTHS.between(parsedFromDate, parsedToDate) > 6) {
            throw InvalidShelfLifeDateRangeException("유통기한 검색 기간은 최대 6개월입니다")
        }

        val items = if (storeId != null) {
            shelfLifeRepository.findByUserIdAndStoreIdAndExpiryDateBetween(
                userId, storeId, parsedFromDate, parsedToDate
            )
        } else {
            shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                userId, parsedFromDate, parsedToDate
            )
        }

        val today = LocalDate.now()
        val responses = items.map { ShelfLifeItemResponse.from(it, today) }

        val expiredItems = responses
            .filter { it.isExpired }
            .sortedBy { it.dDay }
        val upcomingItems = responses
            .filter { !it.isExpired }
            .sortedBy { it.dDay }

        return ShelfLifeListResponse(
            totalCount = responses.size,
            expiredItems = expiredItems,
            upcomingItems = upcomingItems
        )
    }

    @Transactional(readOnly = true)
    fun getShelfLife(userId: Long, shelfLifeId: Long): ShelfLifeItemResponse {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)
        return ShelfLifeItemResponse.from(shelfLife)
    }

    @Transactional
    fun createShelfLife(userId: Long, request: ShelfLifeCreateRequest): ShelfLifeItemResponse {
        val storeId = request.storeId!!
        val productCode = request.productCode!!
        val expiryDate = parseDate(request.expiryDate!!)
        val alertDate = parseDate(request.alertDate!!)

        if (!alertDate.isBefore(expiryDate)) {
            throw InvalidAlertDateException()
        }

        val store = accountRepository.findById(storeId)
            .orElseThrow { ShelfLifeStoreNotFoundException() }

        val product = productRepository.findByProductCode(productCode)
            ?: throw ShelfLifeProductNotFoundException()

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        if (shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(userId, storeId, product.id)) {
            throw DuplicateShelfLifeException()
        }

        val shelfLife = ShelfLife(
            user = user,
            store = store,
            product = product,
            productCode = product.productCode ?: "",
            productName = product.name ?: "",
            storeName = store.name ?: "",
            expiryDate = expiryDate,
            alertDate = alertDate,
            description = request.description
        )

        val saved = shelfLifeRepository.save(shelfLife)
        return ShelfLifeItemResponse.from(saved)
    }

    @Transactional
    fun updateShelfLife(userId: Long, shelfLifeId: Long, request: ShelfLifeUpdateRequest): ShelfLifeItemResponse {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)

        val expiryDate = parseDate(request.expiryDate!!)
        val alertDate = parseDate(request.alertDate!!)

        if (!alertDate.isBefore(expiryDate)) {
            throw InvalidAlertDateException()
        }

        shelfLife.update(expiryDate, alertDate, request.description)
        val updated = shelfLifeRepository.save(shelfLife)
        return ShelfLifeItemResponse.from(updated)
    }

    @Transactional
    fun deleteShelfLife(userId: Long, shelfLifeId: Long) {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)
        shelfLifeRepository.delete(shelfLife)
    }

    @Transactional
    fun deleteShelfLifeBatch(userId: Long, request: ShelfLifeBatchDeleteRequest): ShelfLifeBatchDeleteResponse {
        val ids = request.ids!!

        val ownedItems = shelfLifeRepository.findByIdInAndUserId(ids, userId)

        val allExistingItems = shelfLifeRepository.findAllById(ids)
        val otherUserItems = allExistingItems.filter { it.user.id != userId }
        if (otherUserItems.isNotEmpty()) {
            throw ShelfLifeForbiddenException()
        }

        shelfLifeRepository.deleteAll(ownedItems)

        return ShelfLifeBatchDeleteResponse(deletedCount = ownedItems.size)
    }

    private fun findShelfLifeById(shelfLifeId: Long): ShelfLife {
        return shelfLifeRepository.findById(shelfLifeId)
            .orElseThrow { ShelfLifeNotFoundException() }
    }

    private fun validateOwnership(shelfLife: ShelfLife, userId: Long) {
        if (shelfLife.user.id != userId) {
            throw ShelfLifeForbiddenException()
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            throw InvalidShelfLifeDateRangeException("날짜 형식이 올바르지 않습니다: $dateStr")
        }
    }
}

--- */
