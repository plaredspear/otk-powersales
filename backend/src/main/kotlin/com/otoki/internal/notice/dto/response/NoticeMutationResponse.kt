package com.otoki.internal.notice.dto.response

import com.otoki.internal.notice.entity.Notice
import java.time.format.DateTimeFormatter

data class NoticeMutationResponse(
    val id: Long,
    val category: String,
    val categoryName: String,
    val title: String,
    val content: String,
    val branch: String?,
    val branchCode: String?,
    val createdAt: String
) {
    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        private val CATEGORY_DISPLAY_MAP = mapOf(
            "ALL" to ("COMPANY" to "전체공지"),
            "BRANCH" to ("BRANCH" to "지점공지")
        )

        fun from(entity: Notice): NoticeMutationResponse {
            val (categoryCode, categoryName) = CATEGORY_DISPLAY_MAP[entity.category]
                ?: (entity.category.orEmpty() to entity.category.orEmpty())
            return NoticeMutationResponse(
                id = entity.id,
                category = categoryCode,
                categoryName = categoryName,
                title = entity.name ?: "",
                content = entity.contents ?: "",
                branch = entity.branch,
                branchCode = entity.branchCode,
                createdAt = entity.createdDate?.format(DATE_TIME_FORMATTER) ?: ""
            )
        }
    }
}
