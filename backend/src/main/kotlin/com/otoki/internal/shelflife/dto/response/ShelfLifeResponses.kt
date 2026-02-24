package com.otoki.internal.shelflife.dto.response

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ShelfLife Entity 구조 변경으로 from() 변환 로직이 컴파일 오류 → 주석 처리.

import com.otoki.internal.shelflife.entity.ShelfLife
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        fun from(entity: ShelfLife): ShelfLifeItemResponse {
            return from(entity, LocalDate.now())
        }

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

data class ShelfLifeListResponse(
    val totalCount: Int,
    val expiredItems: List<ShelfLifeItemResponse>,
    val upcomingItems: List<ShelfLifeItemResponse>
)

data class ShelfLifeBatchDeleteResponse(
    val deletedCount: Int
)

--- */
