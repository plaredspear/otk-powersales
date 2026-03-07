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

        fun from(entity: Notice): NoticeMutationResponse {
            return NoticeMutationResponse(
                id = entity.id,
                category = entity.category?.apiCode ?: "",
                categoryName = entity.category?.displayName ?: "",
                title = entity.name ?: "",
                content = entity.contents ?: "",
                branch = entity.branch,
                branchCode = entity.branchCode,
                createdAt = entity.createdDate?.format(DATE_TIME_FORMATTER) ?: ""
            )
        }
    }
}
