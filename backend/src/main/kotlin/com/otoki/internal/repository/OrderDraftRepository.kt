package com.otoki.internal.repository

import com.otoki.internal.entity.OrderDraft
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 임시저장 주문서 Repository
 */
@Repository
interface OrderDraftRepository : JpaRepository<OrderDraft, Long> {

    /**
     * 사용자의 임시저장 주문서 조회 (store JOIN FETCH 포함)
     */
    @Query(
        "SELECT d FROM OrderDraft d " +
        "JOIN FETCH d.store " +
        "LEFT JOIN FETCH d.items " +
        "WHERE d.user.id = :userId"
    )
    fun findByUserIdWithItems(@Param("userId") userId: Long): OrderDraft?

    /**
     * 사용자의 임시저장 주문서 존재 여부 확인
     */
    fun existsByUserId(userId: Long): Boolean

    /**
     * 사용자의 기존 임시저장 주문서 삭제
     */
    fun deleteByUserId(userId: Long)
}
