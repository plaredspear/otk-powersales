/*
package com.otoki.internal.repository

import com.otoki.internal.entity.ExpiryProduct
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

/ **
 * 유통기한 관리 제품 Repository
 * /
interface ExpiryProductRepository : JpaRepository<ExpiryProduct, Long> {

    / **
     * 사용자 ID와 유통기한 범위로 제품 건수 조회
     * (오늘 ~ 7일 후 범위의 임박 제품 건수 계산용)
     * /
    fun countByUserIdAndExpiryDateBetween(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long
}
*/
