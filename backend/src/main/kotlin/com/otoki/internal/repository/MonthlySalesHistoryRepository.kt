package com.otoki.internal.repository

import com.otoki.internal.entity.MonthlySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 월매출 이력 Repository
 * V1 스키마 리매핑으로 기존 쿼리 메서드 삭제 (삭제 필드 참조).
 * 대체 메서드는 Service 로직 재작성 시 함께 추가 예정.
 */
interface MonthlySalesHistoryRepository : JpaRepository<MonthlySalesHistory, Long>
