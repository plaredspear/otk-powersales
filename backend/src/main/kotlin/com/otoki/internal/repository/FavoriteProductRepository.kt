package com.otoki.internal.repository

import com.otoki.internal.entity.FavoriteProduct
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 즐겨찾기 제품 Repository
 */
@Repository
interface FavoriteProductRepository : JpaRepository<FavoriteProduct, Long> {

    /**
     * 사용자의 즐겨찾기 제품 목록 조회 (Product JOIN FETCH)
     */
    @Query(
        "SELECT f FROM FavoriteProduct f " +
        "JOIN FETCH f.product " +
        "WHERE f.user.id = :userId " +
        "ORDER BY f.createdAt DESC",
        countQuery = "SELECT COUNT(f) FROM FavoriteProduct f WHERE f.user.id = :userId"
    )
    fun findByUserIdWithProduct(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<FavoriteProduct>

    /**
     * 사용자가 특정 제품을 즐겨찾기에 추가했는지 확인
     */
    fun existsByUserIdAndProductCode(userId: Long, productCode: String): Boolean

    /**
     * 사용자와 제품코드로 즐겨찾기 조회
     */
    fun findByUserIdAndProductCode(userId: Long, productCode: String): FavoriteProduct?
}
