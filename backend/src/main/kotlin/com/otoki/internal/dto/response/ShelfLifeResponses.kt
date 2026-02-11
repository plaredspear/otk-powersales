package com.otoki.internal.dto.response

import com.otoki.internal.entity.ShelfLife
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 유통기한 항목 Response DTO (목록/상세 공통)
 */
data class ShelfLifeItemResponse(
    val id: Long,
    val productCode: String,
    val productName: String,
    val storeName: String,
    val storeId: Long,
    val expiryDate: String,
    val alertDate: String,
    val dDay: Int,
    val description: String?,
    val isExpired: Boolean
) {
    companion object {
        /**
         * Entity → Response 변환
         * dDay와 isExpired는 오늘 날짜 기준으로 계산한다.
         */
        fun from(entity: ShelfLife): ShelfLifeItemResponse {
            return from(entity, LocalDate.now())
        }

        /**
         * Entity → Response 변환 (기준일 지정, 테스트용)
         */
        fun from(entity: ShelfLife, today: LocalDate): ShelfLifeItemResponse {
            val dDay = ChronoUnit.DAYS.between(today, entity.expiryDate).toInt()
            return ShelfLifeItemResponse(
                id = entity.id,
                productCode = entity.productCode,
                productName = entity.productName,
                storeName = entity.storeName,
                storeId = entity.store.id,
                expiryDate = entity.expiryDate.toString(),
                alertDate = entity.alertDate.toString(),
                dDay = dDay,
                description = entity.description,
                isExpired = dDay <= 0
            )
        }
    }
}

/**
 * 유통기한 목록 조회 Response DTO
 */
data class ShelfLifeListResponse(
    val totalCount: Int,
    val expiredItems: List<ShelfLifeItemResponse>,
    val upcomingItems: List<ShelfLifeItemResponse>
)

/**
 * 유통기한 일괄 삭제 Response DTO
 */
data class ShelfLifeBatchDeleteResponse(
    val deletedCount: Int
)
