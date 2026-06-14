package com.otoki.powersales.domain.activity.suggestion.dto.admin

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * admin 제안 등록 요청 DTO (Spec #830 P1-B §2.7).
 *
 * mobile [SuggestionCreateRequest] 와 동일 필드 + admin 이 다른 사원 대신 등록할 수 있도록 employeeId 명시.
 * employeeId = null 이면 admin 본인이 작성자.
 */
data class AdminSuggestionCreateRequest(
    @field:NotNull(message = "제안구분은 필수입니다")
    val category: SuggestionCategory?,

    @field:NotBlank(message = "제안 제목은 필수입니다")
    @field:Size(max = 250, message = "제안 제목은 최대 250자입니다")
    val title: String?,

    @field:NotBlank(message = "제안 내용은 필수입니다")
    val content: String?,

    @field:Size(max = 20, message = "제품코드는 최대 20자입니다")
    val productCode: String? = null,

    val accountId: Long? = null,

    @field:Size(max = 100, message = "SAP 거래처 코드는 최대 100자입니다")
    val sapAccountCode: String? = null,

    @field:Size(max = 200, message = "클레임 항목은 최대 200자입니다")
    val claimType: String? = null,

    val claimDate: LocalDate? = null,

    @field:Size(max = 20, message = "물류 차량번호는 최대 20자입니다")
    val carNumber: String? = null,

    @field:Size(max = 20, message = "물류책임은 최대 20자입니다")
    val logisticsResponsibility: String? = null,

    @field:Size(max = 255, message = "중복 제안번호는 최대 255자입니다")
    val duplicateProposalNum: String? = null,

    val actionStatus: SuggestionActionStatus? = null,

    val employeeId: Long? = null
)

/**
 * admin 제안 수정 요청 DTO (Spec #830 P1-B §2.7).
 *
 * mobile [SuggestionUpdateRequest] 와 동일 필드 (admin 은 actionStatus + duplicateProposalNum 도 수정 가능 — mobile 도 가능).
 * 조치내용/조치담당자/조치번호 는 #832 위임.
 */
data class AdminSuggestionUpdateRequest(
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
