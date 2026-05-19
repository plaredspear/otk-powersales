package com.otoki.powersales.notice.dto.response

import com.otoki.powersales.notice.entity.Notice
import java.time.LocalDateTime

data class NoticeMutationResponse(
    val id: Long,
    val category: String,
    val categoryName: String,
    val title: String,
    val content: String,
    val branch: String?,
    val branchCode: String?,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(entity: Notice): NoticeMutationResponse {
            return NoticeMutationResponse(
                id = entity.id,
                category = entity.category?.apiCode ?: "",
                categoryName = entity.category?.displayName ?: "",
                title = entity.name ?: "",
                content = entity.contents ?: "",
                branch = entity.branch,
                branchCode = entity.branchCode,
                createdAt = entity.createdAt
            )
        }
    }
}
