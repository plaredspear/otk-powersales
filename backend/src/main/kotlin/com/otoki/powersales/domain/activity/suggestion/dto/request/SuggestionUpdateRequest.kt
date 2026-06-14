package com.otoki.powersales.domain.activity.suggestion.dto.request

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 제안 수정 요청 DTO (Spec #664 P2-B §2.3).
 *
 * Category 분기 재검증 (BR1, BR2, BR4, BR5, BR6 — `beforeUpdate` 동등) 은 Service 단에서 수행.
 * BR3 / BR7 (DuplicateProposalNum 관련) 은 `afterUpdate` 동등이라 update 시점에도 동일 검증.
 */
data class SuggestionUpdateRequest(
    @field:NotNull(message = "제안구분은 필수입니다")
    val category: SuggestionCategory?,

    @field:NotBlank(message = "제안 제목은 필수입니다")
    @field:Size(max = 250, message = "제안 제목은 최대 250자입니다")
    val title: String?,

    @field:NotBlank(message = "제안 내용은 필수입니다")
    val content: String?,

    @field:Size(max = 200, message = "클레임 항목은 최대 200자입니다")
    val claimType: String? = null,

    val claimDate: LocalDate? = null,

    @field:Size(max = 20, message = "물류 차량번호는 최대 20자입니다")
    val carNumber: String? = null,

    @field:Size(max = 20, message = "물류책임은 최대 20자입니다")
    val logisticsResponsibility: String? = null,

    @field:Size(max = 255, message = "중복 제안번호는 최대 255자입니다")
    val duplicateProposalNum: String? = null,

    val actionStatus: SuggestionActionStatus? = null
)
