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

/**
 * [InventorySearchSender.search] 입력 — SAP 재고 조회(SD03070) 원시 호출.
 *
 * 상위 [RealSapInventorySearchClient] 가 accountId → external_key 매핑을 수행하나, 테스트 탭은
 * loan-inquiry 와 동일하게 거래처 SAP 코드(external_key)를 직접 입력받아 raw sender 를 호출한다.
 *
 * @property externalKey SF Account.ExternalKey__c ≡ account.external_key ≡ SAP 거래처 코드
 * @property productCodes 조회할 제품 코드 목록 (요청 라인 전체)
 * @property deliveryDate 납기 요청일 (레거시 DeliveryRequestDate — yyyyMMdd 로 SAP 전송). 미지정 시 오늘.
 */
data class InventorySearchTestRequest(
    val externalKey: String,
    val productCodes: List<String> = emptyList(),
    val deliveryDate: LocalDate? = null,
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

/**
 * 여사원일정 근무 SAP **단건** 테스트 송신 입력.
 *
 * @property scheduleId team_member_schedule.id — 송신할 일정 단건 식별자.
 * @property referenceDate payload 기준일(WorkDate 판정용). 미지정 시 일정 자신의 workingDate 를 사용한다.
 *   `WorkingCategory4`(전일 보정분) 는 `workingDate < referenceDate` 일 때만 채워지므로, 배치 전일 보정분으로
 *   나갔던 payload 를 재현하려면 referenceDate 를 `workingDate + 1일` 로 지정한다 (당일분은 미지정=workingDate).
 */
data class TeamMemberScheduleSingleTestRequest(
    val scheduleId: Long,
    val referenceDate: LocalDate? = null,
)

/** PPTMaster sender 입력. 당월 활성 마스터 조회만 사용. */
data class PPTMasterTestRequest(
    val targetDate: LocalDate? = null,
    val pageSize: Int? = null,
)
