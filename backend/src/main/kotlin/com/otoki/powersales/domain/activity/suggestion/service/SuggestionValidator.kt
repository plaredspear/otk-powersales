package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.exception.SuggestionValidationException
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Spec #664 P2-B §2.4 — Category 분기 비즈니스 룰 BR1~BR7.
 *
 * ## 레거시 매핑
 * - SF Apex: `ProposalTriggerHandler.cls#beforeInsertProposal:50-101` + `#beforeUpdateProposal:103-129` + `#afterUpdateProposal:131-152`
 * - origin spec: #664 P2-B
 *
 * ## 레거시 동작 요약
 * 레거시 SF Trigger 의 `addError` 호출이 검증 위반 시 SF 운영의 SObject save 를 차단. 신규에서는 동일 룰을
 * `@Transactional` 시작 전 validate 단계로 이식하여 `SuggestionValidationException` (400 Bad Request) 으로 변환.
 *
 * | BR  | 분기 | 검증 | 위반 시 메시지 |
 * |-----|-----|------|-------------|
 * | BR1 | LOGISTICS_CLAIM (insert/update) | claim_type 필수 | "제안구분이 물류 클레임일 경우 클레임 항목을 기입하셔야 합니다." |
 * | BR2 | LOGISTICS_CLAIM (insert/update) | claim_date 필수 | "제안구분이 물류 클레임일 경우 물류 클레임 발생일자를 기입하셔야 합니다." |
 * | BR3 | LOGISTICS_CLAIM AND action=DUPLICATE_RECEPTION (insert/afterUpdate) | duplicate_proposal_num 필수 | "제안구분이 물류 클레임이면서 중복접수를 선택하셨을경우 중복 제안번호를 기입하셔야 합니다." |
 * | BR4 | != LOGISTICS_CLAIM (insert/update) | claim_type null 의무 | "제안구분이 물류 클레임이 아닐 경우 클레임 항목을 기입할 수 없습니다." |
 * | BR5 | != LOGISTICS_CLAIM (insert/update) | claim_date null 의무 | "제안구분이 물류 클레임이 아닐 경우 물류 클레임 발생일자를 기입할 수 없습니다." |
 * | BR6 | != LOGISTICS_CLAIM (insert/update) | car_number null 의무 | "제안구분이 물류 클레임이 아닐 경우 물류 차량번호를 기입할 수 없습니다." |
 * | BR7 | != LOGISTICS_CLAIM (insert/afterUpdate) | duplicate_proposal_num null 의무 | "제안구분이 물류 클레임이 아닐 경우 중복 제안번호를 기입할 수 없습니다." |
 *
 * ## 등록 전용 — 제품 필수 (BR8)
 * [validateProductRequired] 는 SF Trigger 가 아니라 **레거시 Heroku 화면 검증**(`write.jsp:372-373`)
 * 을 서버로 승격한 룰이라 등록 경로에서만 호출한다 ([validate] 와 분리한 이유).
 */
@Component
class SuggestionValidator {

    /**
     * BR8 — 등록 시 제품(productCode) 필수. **분류 무관 전 분기 적용**.
     *
     * ## 레거시 매핑
     * - Heroku: `write.jsp#send:372-373` (`if ($('.productNmCd').val() == '' || ... ) alert("제품을 선택하세요.")`)
     * - SF Apex: `IF_REST_MOBILE_ProposalRegist.cls:136-142`
     *
     * ## 레거시 동작 요약
     * 레거시 화면의 전송 버튼 검증 중 **제품 선택만 카테고리 조건이 없다** — 거래처/사진/발생일자 검증은
     * 모두 `&& category == "claim"` 이 붙지만 제품은 무조건이다. 원래는 신제품 제안 시 필수 표시(`*`)를
     * 지웠으나(`write.jsp:648-657` 주석 이력), 2022-11-07 영업지원실 요청으로 전 분기 필수가 되었다.
     *
     * SF 는 등록 payload 의 `ProductCode` 로 `DKRetail__Product__c` 를 조회해 **없으면 Category 분기 없이**
     * `RESULT_MSG='잘못된 값입니다. (ProductCode)'` 로 거부한다. 레거시는 이 검증이 화면에만 있어 우회 시
     * SF 원문 오류가 사용자에게 그대로 노출됐다 — 신규는 SF 호출 전에 서버에서 막는다.
     */
    fun validateProductRequired(productCode: String?) {
        if (productCode.isNullOrBlank()) {
            throw SuggestionValidationException("제품을 선택해주세요.")
        }
    }

    fun validate(
        category: SuggestionCategory,
        claimType: String?,
        claimDate: LocalDate?,
        carNumber: String?,
        duplicateProposalNum: String?,
        actionStatus: SuggestionActionStatus?
    ) {
        if (category == SuggestionCategory.LOGISTICS_CLAIM) {
            // BR1
            if (claimType.isNullOrBlank()) {
                throw SuggestionValidationException("제안구분이 물류 클레임일 경우 클레임 항목을 기입하셔야 합니다.")
            }
            // BR2
            if (claimDate == null) {
                throw SuggestionValidationException("제안구분이 물류 클레임일 경우 물류 클레임 발생일자를 기입하셔야 합니다.")
            }
            // BR3
            if (actionStatus == SuggestionActionStatus.DUPLICATE_RECEPTION && duplicateProposalNum.isNullOrBlank()) {
                throw SuggestionValidationException("제안구분이 물류 클레임이면서 중복접수를 선택하셨을경우 중복 제안번호를 기입하셔야 합니다.")
            }
        } else {
            // BR4
            if (!claimType.isNullOrBlank()) {
                throw SuggestionValidationException("제안구분이 물류 클레임이 아닐 경우 클레임 항목을 기입할 수 없습니다.")
            }
            // BR5
            if (claimDate != null) {
                throw SuggestionValidationException("제안구분이 물류 클레임이 아닐 경우 물류 클레임 발생일자를 기입할 수 없습니다.")
            }
            // BR6
            if (!carNumber.isNullOrBlank()) {
                throw SuggestionValidationException("제안구분이 물류 클레임이 아닐 경우 물류 차량번호를 기입할 수 없습니다.")
            }
            // BR7
            if (!duplicateProposalNum.isNullOrBlank()) {
                throw SuggestionValidationException("제안구분이 물류 클레임이 아닐 경우 중복 제안번호를 기입할 수 없습니다.")
            }
        }
    }
}
