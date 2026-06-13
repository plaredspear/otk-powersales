package com.otoki.powersales.domain.support.notice.dto.response

data class NoticeFormMetaResponse(
    val scopes: List<ScopeOption>,
    val categories: List<CategoryOption>,
    val branches: List<BranchOption>
)

data class ScopeOption(
    val code: String,
    val name: String
)

data class CategoryOption(
    val code: String,
    val name: String
)

data class BranchOption(
    val branchCode: String,
    val branchName: String
)
