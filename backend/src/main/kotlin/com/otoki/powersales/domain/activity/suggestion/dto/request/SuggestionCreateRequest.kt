package com.otoki.powersales.domain.activity.suggestion.dto.request

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 제안 등록 요청 DTO (Spec #664 P2-B §2.2).
 *
 * multipart/form-data 의 `request` form field 로 전송된다 (Spring `@RequestPart` JSON binding).
 * 사진 파일은 `photos[]` MultipartFile 로 별도 처리.
 *
 * Category 분기 검증 (BR1~BR7) 은 Service 단에서 수행 — DTO 의 Bean Validation 은 기본 필드만.
 *
 * 등록 시 action_status / logistics_responsibility 는 SF picklist default(미확인 / 확인중)로 Service 가 고정 설정하므로
 * 요청 필드에 포함하지 않는다(SF beforeInsertProposal 정합). 따라서 BR3(중복접수 시 중복번호 필수)는 등록에선 발동하지 않고
 * 조치 단계 update 에서만 발동한다.
 */
data class SuggestionCreateRequest(
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

    @field:Size(max = 255, message = "중복 제안번호는 최대 255자입니다")
    val duplicateProposalNum: String? = null
)
