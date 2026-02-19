/*
package com.otoki.internal.repository

import com.otoki.internal.entity.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

/ **
 * 행사 Repository
 * /
interface EventRepository : JpaRepository<Event, Long> {

    / **
     * eventId로 행사 조회
     * /
    fun findByEventId(eventId: String): Optional<Event>

    / **
     * 담당자별 행사 목록 조회 (거래처 필터, 기간 필터, 페이징)
     * - assigneeId: 담당자 사번
     * - customerId: 거래처 ID (null이면 전체)
     * - date: 조회 기준일 (startDate <= date <= endDate)
     * - pageable: 페이징 정보 (정렬: startDate DESC)
     * /
    @Query(
        """
        SELECT e FROM Event e
        WHERE e.assigneeId = :assigneeId
          AND (:customerId IS NULL OR e.customerId = :customerId)
          AND :date BETWEEN e.startDate AND e.endDate
        """
    )
    fun findEventsByAssignee(
        @Param("assigneeId") assigneeId: String,
        @Param("customerId") customerId: String?,
        @Param("date") date: LocalDate,
        pageable: Pageable
    ): Page<Event>
}
*/
