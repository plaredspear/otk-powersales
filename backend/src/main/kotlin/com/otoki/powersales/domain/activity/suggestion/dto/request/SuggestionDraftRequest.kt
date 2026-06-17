package com.otoki.powersales.domain.activity.suggestion.dto.request

import java.time.LocalDate

/**
 * 제안 임시저장 요청 DTO (multipart/form-data 의 `request` JSON part).
 *
 * 임시저장은 검증을 건너뛰므로 모든 필드가 nullable 이다(레거시 tempSuggestProc 정합).
 * 거래처명/제품명은 prefill 표시용으로 함께 전달받아 저장한다(등록 요청에 없는 정보).
 * 사진(photos)은 컨트롤러에서 `@RequestPart` 로 별도 수신한다(최대 2장).
 *
 * `category` / `actionStatus` 는 검증 없이 enum name 문자열로 저장한다(부분 입력 허용).
 */
data class SuggestionDraftRequest(
    val category: String? = null,
    val title: String? = null,
    val content: String? = null,
    val productCode: String? = null,
    val productName: String? = null,
    val accountId: Long? = null,
    val accountName: String? = null,
    val sapAccountCode: String? = null,
    val claimType: String? = null,
    val claimDate: LocalDate? = null,
    val carNumber: String? = null,
    val logisticsResponsibility: String? = null,
    val duplicateProposalNum: String? = null,
    val actionStatus: String? = null
)
