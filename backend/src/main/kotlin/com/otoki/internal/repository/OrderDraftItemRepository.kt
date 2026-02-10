package com.otoki.internal.repository

import com.otoki.internal.entity.OrderDraftItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 임시저장 주문서 제품 항목 Repository
 */
@Repository
interface OrderDraftItemRepository : JpaRepository<OrderDraftItem, Long>
