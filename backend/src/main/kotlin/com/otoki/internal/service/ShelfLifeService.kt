package com.otoki.internal.service

import com.otoki.internal.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.dto.response.ShelfLifeItemResponse
import com.otoki.internal.dto.response.ShelfLifeListResponse
import com.otoki.internal.entity.ShelfLife
import com.otoki.internal.exception.*
import com.otoki.internal.repository.ProductRepository
import com.otoki.internal.repository.ShelfLifeRepository
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 유통기한 관리 Service
 * CRUD 비즈니스 로직을 담당한다.
 */
@Service
class ShelfLifeService(
    private val shelfLifeRepository: ShelfLifeRepository,
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val productRepository: ProductRepository
) {

    /**
     * 유통기한 목록 조회
     *
     * 1. fromDate, toDate 유효성 검증 (toDate >= fromDate, 6개월 이내)
     * 2. storeId 유무에 따라 전체/거래처별 조회
     * 3. dDay 기준으로 "유통기한 지남"(dDay <= 0) / "유통기한 전"(dDay > 0) 분리
     * 4. 각 그룹 내 dDay 오름차순 정렬
     */
    @Transactional(readOnly = true)
    fun getShelfLifeList(userId: Long, storeId: Long?, fromDate: String, toDate: String): ShelfLifeListResponse {
        val parsedFromDate = parseDate(fromDate)
        val parsedToDate = parseDate(toDate)

        // 날짜 유효성 검증
        if (parsedToDate.isBefore(parsedFromDate)) {
            throw InvalidShelfLifeDateRangeException("종료일은 시작일 이후여야 합니다")
        }
        if (ChronoUnit.MONTHS.between(parsedFromDate, parsedToDate) > 6) {
            throw InvalidShelfLifeDateRangeException("유통기한 검색 기간은 최대 6개월입니다")
        }

        // 조회
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

        // 그룹 분리: dDay <= 0 (지남) / dDay > 0 (전)
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

    /**
     * 유통기한 단건 조회
     *
     * 1. shelfLifeId로 조회
     * 2. 본인 소유 확인
     * 3. dDay, isExpired 계산하여 반환
     */
    @Transactional(readOnly = true)
    fun getShelfLife(userId: Long, shelfLifeId: Long): ShelfLifeItemResponse {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)
        return ShelfLifeItemResponse.from(shelfLife)
    }

    /**
     * 유통기한 등록
     *
     * 1. 거래처/제품 존재 확인
     * 2. alertDate < expiryDate 검증
     * 3. 중복 등록 확인
     * 4. ShelfLife 생성·저장
     */
    @Transactional
    fun createShelfLife(userId: Long, request: ShelfLifeCreateRequest): ShelfLifeItemResponse {
        val storeId = request.storeId!!
        val productCode = request.productCode!!
        val expiryDate = parseDate(request.expiryDate!!)
        val alertDate = parseDate(request.alertDate!!)

        // alertDate < expiryDate 검증
        if (!alertDate.isBefore(expiryDate)) {
            throw InvalidAlertDateException()
        }

        // 거래처 존재 확인
        val store = storeRepository.findById(storeId)
            .orElseThrow { ShelfLifeStoreNotFoundException() }

        // 제품 존재 확인
        val product = productRepository.findByProductCode(productCode)
            ?: throw ShelfLifeProductNotFoundException()

        // 사용자 존재 확인
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        // 중복 등록 확인
        if (shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(userId, storeId, product.id)) {
            throw DuplicateShelfLifeException()
        }

        val shelfLife = ShelfLife(
            user = user,
            store = store,
            product = product,
            productCode = product.productCode,
            productName = product.productName,
            storeName = store.storeName,
            expiryDate = expiryDate,
            alertDate = alertDate,
            description = request.description
        )

        val saved = shelfLifeRepository.save(shelfLife)
        return ShelfLifeItemResponse.from(saved)
    }

    /**
     * 유통기한 수정
     *
     * 1. 조회 및 본인 소유 확인
     * 2. alertDate < expiryDate 검증
     * 3. expiryDate, alertDate, description 업데이트
     */
    @Transactional
    fun updateShelfLife(userId: Long, shelfLifeId: Long, request: ShelfLifeUpdateRequest): ShelfLifeItemResponse {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)

        val expiryDate = parseDate(request.expiryDate!!)
        val alertDate = parseDate(request.alertDate!!)

        // alertDate < expiryDate 검증
        if (!alertDate.isBefore(expiryDate)) {
            throw InvalidAlertDateException()
        }

        shelfLife.update(expiryDate, alertDate, request.description)
        val updated = shelfLifeRepository.save(shelfLife)
        return ShelfLifeItemResponse.from(updated)
    }

    /**
     * 유통기한 단건 삭제
     *
     * 1. 조회 및 본인 소유 확인
     * 2. 삭제
     */
    @Transactional
    fun deleteShelfLife(userId: Long, shelfLifeId: Long) {
        val shelfLife = findShelfLifeById(shelfLifeId)
        validateOwnership(shelfLife, userId)
        shelfLifeRepository.delete(shelfLife)
    }

    /**
     * 유통기한 일괄 삭제
     *
     * 1. ids로 조회
     * 2. 모든 항목 본인 소유 확인 (타인 데이터 포함 시 FORBIDDEN)
     * 3. 일괄 삭제
     * 4. 실제 삭제된 건수 반환
     */
    @Transactional
    fun deleteShelfLifeBatch(userId: Long, request: ShelfLifeBatchDeleteRequest): ShelfLifeBatchDeleteResponse {
        val ids = request.ids!!

        // 본인 소유 항목만 조회
        val ownedItems = shelfLifeRepository.findByIdInAndUserId(ids, userId)

        // 실제 존재하는 항목 중 타인 소유가 있는지 확인
        val allExistingItems = shelfLifeRepository.findAllById(ids)
        val otherUserItems = allExistingItems.filter { it.user.id != userId }
        if (otherUserItems.isNotEmpty()) {
            throw ShelfLifeForbiddenException()
        }

        // 삭제
        shelfLifeRepository.deleteAll(ownedItems)

        return ShelfLifeBatchDeleteResponse(deletedCount = ownedItems.size)
    }

    // --- Private helper methods ---

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
