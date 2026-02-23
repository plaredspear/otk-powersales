package com.otoki.internal.repository

import com.otoki.internal.entity.ShelfLife
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 유통기한 관리 Repository
 *
 * V1 스키마 리매핑 후: 기존 쿼리 메서드는 @ManyToOne 기반 경로 참조로
 * Entity 변경에 의해 컴파일 오류 발생 → 전체 주석 처리.
 * Service 비활성 상태이므로 호출부 없음.
 */
@Repository
interface ShelfLifeRepository : JpaRepository<ShelfLife, Int>

/* --- 주석 처리: V1 리매핑으로 경로 변경된 기존 쿼리 메서드 ---

    // findByUserIdAndExpiryDateBetween: sl.user.id, sl.expiryDate 참조
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "WHERE sl.user.id = :userId " +
        "AND sl.expiryDate BETWEEN :fromDate AND :toDate " +
        "ORDER BY sl.expiryDate ASC"
    )
    fun findByUserIdAndExpiryDateBetween(
        @Param("userId") userId: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<ShelfLife>

    // findByUserIdAndStoreIdAndExpiryDateBetween: sl.user.id, sl.store.id, sl.expiryDate 참조
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "WHERE sl.user.id = :userId " +
        "AND sl.store.id = :storeId " +
        "AND sl.expiryDate BETWEEN :fromDate AND :toDate " +
        "ORDER BY sl.expiryDate ASC"
    )
    fun findByUserIdAndStoreIdAndExpiryDateBetween(
        @Param("userId") userId: Long,
        @Param("storeId") storeId: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<ShelfLife>

    // existsByUserIdAndStoreIdAndProductId: @ManyToOne 기반 파생 쿼리
    fun existsByUserIdAndStoreIdAndProductId(userId: Long, storeId: Long, productId: Long): Boolean

    // findByAlertDateAndAlertSentFalse: sl.alertSent 필드 V1에 없음
    @Query(
        "SELECT sl FROM ShelfLife sl " +
        "JOIN FETCH sl.user " +
        "WHERE sl.alertDate = :alertDate AND sl.alertSent = false"
    )
    fun findByAlertDateAndAlertSentFalse(
        @Param("alertDate") alertDate: LocalDate
    ): List<ShelfLife>

    // findByIdInAndUserId: id, userId 모두 리매핑 대상
    fun findByIdInAndUserId(ids: List<Long>, userId: Long): List<ShelfLife>

--- */
