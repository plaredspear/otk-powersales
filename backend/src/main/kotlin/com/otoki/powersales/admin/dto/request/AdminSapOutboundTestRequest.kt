package com.otoki.powersales.admin.dto.request

import java.time.LocalDate

/**
 * Admin SAP outbound 테스트 요청 DTO 묶음.
 *
 * 각 sender 의 실제 호출 시그니처에 맞춰 1:1 매핑된다.
 * preview 와 send 두 endpoint 가 동일 request body 를 받는다.
 */

/** [LoanInquirySender.inquire] 입력. */
data class LoanInquiryTestRequest(
    val externalKey: String,
)

/** [OrderRequestDetailSapSender.fetchDetail] 입력. */
data class OrderRequestDetailTestRequest(
    val requestNumber: String,
)

/** [OrderCancelService.cancel] 의 SAP 송신 부 입력. orderProductIds 가 비어 있으면 미취소 라인 전체. */
data class OrderRequestCancelTestRequest(
    val orderRequestId: Long,
    val orderProductIds: List<Long> = emptyList(),
)

/** [OrderRequestRegisterSender.enqueue] 입력. 실 송신은 outbox 적재만. */
data class OrderRequestRegisterTestRequest(
    val orderRequestId: Long,
)

/** Batch 류 sender (Attendance / DisplayMaster) 입력. */
data class BatchDateTestRequest(
    val targetDate: LocalDate,
    val pageSize: Int? = null,
)

/** PPTMaster sender 입력. 당월 활성 마스터 조회만 사용. */
data class PPTMasterTestRequest(
    val targetDate: LocalDate? = null,
    val pageSize: Int? = null,
)
