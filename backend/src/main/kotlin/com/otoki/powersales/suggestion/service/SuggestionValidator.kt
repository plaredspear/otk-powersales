package com.otoki.powersales.suggestion.service

import com.otoki.powersales.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import com.otoki.powersales.suggestion.exception.SuggestionValidationException
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
 */
@Component
class SuggestionValidator {

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
