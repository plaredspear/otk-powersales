package com.otoki.powersales.domain.activity.claim.dto.response

import com.otoki.powersales.domain.activity.claim.entity.Claim
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

data class ClaimListItemResponse(
    val claimId: Long,
    val claimNo: String? = null,
    val accountName: String?,
    val productName: String?,
    val productCode: String?,
    val categoryValue: String?,
    val categoryLabel: String?,
    val subcategoryValue: String?,
    val subcategoryLabel: String?,
    val defectQuantity: BigDecimal?,
    val defectDescription: String? = null,
    // status/statusLabel : SF DKRetail__Status__c (코스모스 전송상태) 원문 — 앱 화면 표시에는 쓰지 않는다
    // (신규 시스템은 전이 경로가 없어 앱 등록분이 영구히 "임시저장"). 화면 표시는 actionStatusLabel.
    val status: String?,
    val statusLabel: String?,
    // 앱 목록 카드 뱃지 문구 — 코스모스 조치상태(Claim.actionStatus) 원문, 미회신이면 "미확인".
    val actionStatus: String? = null,
    val actionStatusLabel: String? = null,
    // sfSendStatus/sfSendStatusLabel : 신규→SF 전송상태. SF origin 마이그레이션 건은 null.
    val sfSendStatus: String? = null,
    val sfSendStatusLabel: String? = null,
    // 발생일자(SF ClaimDate) — 레거시 list.jsp 의 목록 표시/필터 기준 날짜.
    val date: LocalDate? = null,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(claim: Claim): ClaimListItemResponse = ClaimListItemResponse(
            claimId = claim.id,
            claimNo = claim.name,
            accountName = claim.account?.name,
            productName = claim.product?.name,
            productCode = claim.product?.productCode,
            categoryValue = claim.claimType1?.value,
            categoryLabel = claim.claimType1?.label,
            subcategoryValue = claim.claimType2?.value,
            subcategoryLabel = claim.claimType2?.label,
            defectQuantity = claim.defectQuantity,
            defectDescription = claim.defectDescription,
            status = claim.status?.name,
            statusLabel = claim.status?.displayName,
            actionStatus = claim.actionStatus,
            actionStatusLabel = claim.actionStatusLabel(),
            sfSendStatus = claim.sfSendStatus?.name,
            sfSendStatusLabel = claim.sfSendStatus?.displayName,
            date = claim.date,
            createdAt = claim.createdAt
        )
    }
}
