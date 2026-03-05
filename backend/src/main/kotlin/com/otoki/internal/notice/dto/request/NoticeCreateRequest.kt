package com.otoki.internal.notice.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NoticeCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다")
    val title: String,

    @field:NotBlank(message = "카테고리는 필수입니다")
    val category: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    val branch: String? = null,
    val branchCode: String? = null
)
