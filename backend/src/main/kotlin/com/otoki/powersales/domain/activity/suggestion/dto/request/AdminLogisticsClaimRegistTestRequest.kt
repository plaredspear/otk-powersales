package com.otoki.powersales.domain.activity.suggestion.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * SF 물류 클레임 등록(ProposalRegist) 전송 테스트 요청 DTO (개발자 도구 — 외부 API 테스트).
 *
 * 레거시 SF Apex REST `IF_REST_MOBILE_ProposalRegist.cls#doPost` 의 입력 JSON(Input 클래스) 과
 * 동일한 key 셋을 구성하기 위한 입력. 신규 운영 등록([com.otoki.powersales.domain.activity.suggestion.service.SuggestionService.create])
 * 은 SF 전송 없이 신규 DB 에만 적재하므로, 본 테스트는 **모바일이 보내는 물류 클레임 등록 정보로
 * SF 전송 payload(apiMap) 미리보기만 구성** 한다 (실제 SF POST 는 추후 API 정보 확보 후 추가).
 *
 * Category 는 `물류 클레임` 으로 고정 — claimList/logclaimDate/carNumber 가 의미 있는 분기.
 * 자동 파생 필드(ProductId/AccountId/WERK 등) 는 SF 서버 측 SOQL 파생이라 미리보기에서 제외.
 */
data class AdminLogisticsClaimRegistTestRequest(
    @field:NotBlank(message = "거래처 SAP 코드는 필수입니다")
    @field:Size(max = 100, message = "거래처 SAP 코드는 최대 100자입니다")
    val sapAccountCode: String?,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    @field:Size(max = 20, message = "제품 코드는 최대 20자입니다")
    val productCode: String?,

    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String?,

    @field:NotBlank(message = "클레임 항목은 필수입니다")
    @field:Size(max = 200, message = "클레임 항목은 최대 200자입니다")
    val claimType: String?,

    @field:NotBlank(message = "발생일자는 필수입니다")
    val claimDate: String?,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 250, message = "제목은 최대 250자입니다")
    val title: String?,

    @field:NotBlank(message = "내용은 필수입니다")
    val description: String?,

    @field:Size(max = 20, message = "차량번호는 최대 20자입니다")
    val carNumber: String? = null,
)
