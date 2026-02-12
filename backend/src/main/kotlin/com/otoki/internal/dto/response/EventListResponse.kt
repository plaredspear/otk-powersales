package com.otoki.internal.dto.response

import com.otoki.internal.entity.Event
import org.springframework.data.domain.Page

/**
 * 행사 목록 응답 DTO
 */
data class EventListResponse(
    val content: List<EventInfo>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {

    /**
     * 행사 정보
     */
    data class EventInfo(
        val eventId: String,
        val eventType: String,
        val eventName: String,
        val startDate: String,
        val endDate: String,
        val customerId: String,
        val customerName: String
    ) {
        companion object {
            fun from(event: Event, customerName: String): EventInfo {
                return EventInfo(
                    eventId = event.eventId,
                    eventType = event.eventType,
                    eventName = event.eventName,
                    startDate = event.startDate.toString(),
                    endDate = event.endDate.toString(),
                    customerId = event.customerId,
                    customerName = customerName
                )
            }
        }
    }

    companion object {
        fun from(page: Page<Event>, customerNameMap: Map<String, String>): EventListResponse {
            return EventListResponse(
                content = page.content.map { event ->
                    EventInfo.from(event, customerNameMap[event.customerId] ?: "알 수 없음")
                },
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}
