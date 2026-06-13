package com.otoki.powersales.domain.support.agreement.dto.response

import com.otoki.powersales.common.entity.AgreementWord
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 관리자 웹 신규 약관 등록 응답 DTO. (Spec #658 P1-B)
 *
 * 등록 직후 entity 의 핵심 필드 노출. `active` 는 항상 false, `activeDate` 는 항상 null —
 * cycle batch (#654) 도래 전까지 비활성 상태로 적재된다.
 */
data class AdminAgreementWordCreateResponse(
    val agreementWordId: Int,
    val name: String,
    val afterActiveDate: LocalDate?,
    val active: Boolean,
    val activeDate: LocalDate?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: AgreementWord): AdminAgreementWordCreateResponse = AdminAgreementWordCreateResponse(
            agreementWordId = entity.id,
            name = entity.name ?: "",
            afterActiveDate = entity.afterActiveDate,
            active = entity.active ?: false,
            activeDate = entity.activeDate,
            createdAt = entity.createdAt
        )
    }
}

/**
 * 관리자 웹 활성 약관 조회 응답 DTO. (Spec #658 P1-B)
 *
 * Web 측 `/admin/agreement-words` 화면 상단 미리보기 카드용. 활성 약관 부재 시 컨트롤러는
 * `ApiResponse.data == null` 로 응답한다.
 */
data class AdminAgreementWordActiveResponse(
    val agreementWordId: Int,
    val name: String,
    val contents: String,
    val activeDate: LocalDate?,
    val afterActiveDate: LocalDate?
) {
    companion object {
        fun from(entity: AgreementWord): AdminAgreementWordActiveResponse = AdminAgreementWordActiveResponse(
            agreementWordId = entity.id,
            name = entity.name ?: "",
            contents = entity.contents ?: "",
            activeDate = entity.activeDate,
            afterActiveDate = entity.afterActiveDate
        )
    }
}
