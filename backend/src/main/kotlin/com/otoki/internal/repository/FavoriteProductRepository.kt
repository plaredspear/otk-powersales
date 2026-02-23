package com.otoki.internal.repository

import com.otoki.internal.entity.FavoriteProduct
import com.otoki.internal.entity.ProductFavoriteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 즐겨찾기 제품 Repository
 *
 * V1 스키마 리매핑 후: 기존 쿼리 메서드는 @ManyToOne 기반 경로 참조로
 * Entity 변경에 의해 컴파일 오류 발생 → 전체 주석 처리.
 * Service 비활성 상태이므로 호출부 없음.
 */
@Repository
interface FavoriteProductRepository : JpaRepository<FavoriteProduct, ProductFavoriteId>

/* --- 주석 처리: V1 리매핑으로 경로 변경된 기존 쿼리 메서드 ---

    // findByUserIdWithProduct: f.user.id, JOIN FETCH f.product, f.createdAt 참조
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

    // existsByUserIdAndProductCode: userId → employeeCode 변경
    fun existsByUserIdAndProductCode(userId: Long, productCode: String): Boolean

    // findByUserIdAndProductCode: userId → employeeCode 변경
    fun findByUserIdAndProductCode(userId: Long, productCode: String): FavoriteProduct?

--- */
