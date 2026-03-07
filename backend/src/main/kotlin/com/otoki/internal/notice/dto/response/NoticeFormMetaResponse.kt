package com.otoki.internal.notice.dto.response

data class NoticeFormMetaResponse(
    val categories: List<CategoryOption>,
    val branches: List<BranchOption>
)

data class CategoryOption(
    val code: String,
    val name: String
)

data class BranchOption(
    val branchCode: String,
    val branchName: String
)
