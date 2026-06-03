package com.otoki.powersales.savedsearch.entity

/**
 * 저장된 검색의 공유 스코프.
 *
 * - [PRIVATE]: 개인 검색 — 소유자(owner) 본인에게만 노출. SF ListView 의 "나만 보기" 동등.
 * - [SHARED]: 공용 검색 — 전체 사용자에게 노출. SF ListView 의 "조직 공유" 동등.
 *   생성/수정/삭제는 `saved_search` EDIT 권한 보유자만 가능.
 */
enum class SavedSearchScope {
    PRIVATE,
    SHARED,
}
