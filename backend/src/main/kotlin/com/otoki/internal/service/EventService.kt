/*
package com.otoki.internal.service

import com.otoki.internal.dto.request.EventListRequest
import com.otoki.internal.dto.response.DailySalesListResponse
import com.otoki.internal.dto.response.EventDetailResponse
import com.otoki.internal.dto.response.EventListResponse
import com.otoki.internal.exception.EventNotFoundException
import com.otoki.internal.repository.EventProductRepository
import com.otoki.internal.repository.EventRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

/ **
 * 행사 관련 비즈니스 로직
 * /
@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventProductRepository: EventProductRepository,
    private val userRepository: UserRepository
) {

    / **
     * 행사 목록 조회
     * - 현재 로그인 사용자의 담당 행사만 조회
     * - 거래처 필터 (미입력 시 전체)
     * - 기간 필터 (date가 startDate~endDate 사이)
     * - 페이징 처리, 정렬: startDate DESC
     * /
    @Transactional(readOnly = true)
    fun getEvents(userId: Long, request: EventListRequest): EventListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다") }

        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "startDate")
        )

        val date = request.getDateOrToday()
        val eventPage = eventRepository.findEventsByAssignee(
            assigneeId = user.employeeId,
            customerId = request.customerId,
            date = date,
            pageable = pageable
        )

        // TODO: Customer 엔티티가 구현되면 실제 거래처명 조회
        val customerNameMap = eventPage.content
            .associate { it.customerId to it.customerId }

        return EventListResponse.from(eventPage, customerNameMap)
    }

    / **
     * 행사 상세 조회
     * - 행사 기본 정보 + 매출 정보 + 제품 목록 + 오늘 등록 상태
     * - 진행율 계산: min(100, (오늘 - 시작일) / (종료일 - 시작일) * 100)
     * - 달성율 계산: (달성금액 / 목표금액) * 100
     * - canRegisterToday: 오늘이 행사기간 내 AND 현재 사용자가 담당자
     * - isTodayRegistered: 오늘 날짜의 REGISTERED 상태 일매출 존재 여부
     * /
    @Transactional(readOnly = true)
    fun getEventDetail(userId: Long, eventId: String): EventDetailResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다") }

        val event = eventRepository.findByEventId(eventId)
            .orElseThrow { EventNotFoundException() }

        // 제품 목록 조회
        val products = eventProductRepository.findByEventId(eventId)

        // 진행율 계산
        val today = LocalDate.now()
        val totalDays = ChronoUnit.DAYS.between(event.startDate, event.endDate).toDouble()
        val elapsedDays = ChronoUnit.DAYS.between(event.startDate, today).toDouble()
        val progressRate = if (totalDays > 0) {
            min(100.0, (elapsedDays / totalDays) * 100)
        } else {
            0.0
        }

        // TODO: 실제 달성 금액은 DailySales에서 집계해야 함 (F51 구현 후)
        val achievedAmount = 0L
        val achievementRate = if (event.targetAmount > 0) {
            (achievedAmount.toDouble() / event.targetAmount) * 100
        } else {
            0.0
        }

        // 오늘 등록 가능 여부
        val canRegisterToday = today in event.startDate..event.endDate &&
            event.assigneeId == user.employeeId

        // TODO: DailySales 구현 후 실제 등록 여부 확인 (F51)
        val isTodayRegistered = false

        // TODO: Customer 엔티티가 구현되면 실제 거래처명 조회
        val customerName = event.customerId

        return EventDetailResponse(
            event = EventDetailResponse.EventInfo.from(event, customerName),
            salesInfo = EventDetailResponse.SalesInfo(
                targetAmount = event.targetAmount,
                achievedAmount = achievedAmount,
                achievementRate = achievementRate,
                progressRate = progressRate
            ),
            products = EventDetailResponse.ProductsInfo.from(products),
            isTodayRegistered = isTodayRegistered,
            canRegisterToday = canRegisterToday
        )
    }

    / **
     * 일별 매출 목록 조회
     * TODO: DailySales Entity 구현 후 연동 (F51)
     * /
    @Transactional(readOnly = true)
    fun getDailySales(userId: Long, eventId: String): DailySalesListResponse {
        // 행사 존재 확인
        eventRepository.findByEventId(eventId)
            .orElseThrow { EventNotFoundException() }

        // TODO: F51-일매출등록 구현 후 실제 데이터 조회
        return DailySalesListResponse(emptyList())
    }
}
*/
