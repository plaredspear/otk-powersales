package com.otoki.powersales.domain.support.agreement.dto.request

import jakarta.validation.constraints.AssertFalse
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 관리자 웹 신규 약관 등록 요청 DTO. (Spec #658 P1-B)
 *
 * `active` / `activeDate` 는 입력 차단 — cycle batch (#654) 가 활성 토글 단독 권한자.
 * Service 단에서도 fallback 으로 false / null 강제 적재 (DTO 누락 / 우회 케이스 안전망).
 */
data class AdminAgreementWordCreateRequest(

    @field:NotBlank(message = "약관 이름은 필수입니다")
    @field:Size(max = 80, message = "약관 이름은 80자 이내여야 합니다")
    val name: String,

    @field:NotBlank(message = "약관 본문은 필수입니다")
    @field:Size(max = 8000, message = "약관 본문은 8000자 이내여야 합니다")
    val contents: String,

    @field:NotNull(message = "다음 시행일자는 필수입니다")
    @field:Future(message = "다음 시행일자는 미래 일자여야 합니다")
    val afterActiveDate: LocalDate?,

    @field:AssertFalse(message = "active 는 입력할 수 없습니다")
    val active: Boolean = false,

    @field:Null(message = "activeDate 는 입력할 수 없습니다")
    val activeDate: LocalDate? = null
)
