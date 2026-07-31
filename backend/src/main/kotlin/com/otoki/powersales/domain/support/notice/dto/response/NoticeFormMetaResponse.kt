package com.otoki.powersales.domain.support.notice.dto.response

data class NoticeFormMetaResponse(
    val scopes: List<ScopeOption>,
    /** 작성/수정 폼의 분류 선택지 — 권한별로 제한된다(조장/지점장은 지점공지만). */
    val categories: List<CategoryOption>,
    /**
     * 목록 화면의 분류 **조회** 선택지 — 전 분류 고정.
     *
     * 작성 권한([categories])과 분리한다. 조장/지점장은 지점공지만 작성할 수 있지만, 목록에는 본인 지점공지
     * 외에 회사공지/교육(지점 소속이 없는 전사 공지)도 함께 나오므로 그 분류로 조회도 할 수 있어야 한다.
     */
    val searchCategories: List<CategoryOption>,
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
