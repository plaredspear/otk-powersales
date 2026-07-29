package com.otoki.powersales.domain.sales.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import java.time.LocalDateTime

/**
 * ORORA 월매출 목록 행 — `monthly_sales_history` 1건 (SF `MonthlySalesHistory__c` 동등).
 *
 * 금액 10종은 ORORA 월별 적재 배치(`OroraMonthlySalesChunkProcessor.applyMonthly`)가 채우는 컬럼
 * 전량이다 (온도대별 전산마감 4종 + 물류마감 4종 + 각 합계 2종). 일매출 화면이 온도대별 3분할 컬럼을
 * 감춘 것과 반대인데, 월별 적재 경로는 이 컬럼들을 실제로 채우기 때문이다.
 *
 * 운영 입력 컬럼(당월목표 / 비고 / 마감확정)은 적재 배치가 보존만 하고 건드리지 않으므로,
 * 적재 결과 확인 화면인 여기서는 노출하지 않는다.
 */
data class MonthlySalesHistoryListItem(
    val id: Long,
    /** 매출발생년 (`yyyy`). */
    val salesYear: String?,
    /** 매출발생월 (`MM`). */
    val salesMonth: String?,
    val sapAccountCode: String?,
    /** `거래처코드 + yyyy + MM` — 적재 upsert 키. */
    val externalKey: String?,
    /** 전산마감실적_상온 (원). */
    val abcClosingAmount1: Double?,
    /** 전산마감실적_라면 (원). */
    val abcClosingAmount2: Double?,
    /** 전산마감실적_냉장냉동 (원). */
    val abcClosingAmount3: Double?,
    /** 전산마감실적_유지 (원). */
    val abcClosingAmount4: Double?,
    /** 전산마감실적_합계 (원) — ORORA view 가 내려주는 값이며, 1~4 재합산이 아니다. */
    val abcClosingSumAmount: Double?,
    /** 물류마감실적_상온 (원). */
    val shipClosingAmount1: Double?,
    /** 물류마감실적_라면 (원). */
    val shipClosingAmount2: Double?,
    /** 물류마감실적_냉장냉동 (원). */
    val shipClosingAmount3: Double?,
    /** 물류마감실적_유지 (원). */
    val shipClosingAmount4: Double?,
    /** 물류마감실적_합계 (원) — ORORA view 가 내려주는 값이며, 1~4 재합산이 아니다. */
    val shipClosingSumAmount: Double?,
    /**
     * SF `IsDeleted` soft-delete 여부.
     *
     * 적재 결과 확인 화면이라 삭제 row 도 숨기지 않고 노출하되, 화면이 정상 매출과 구분할 수 있도록
     * 필드로 내린다 (합계에서는 제외 — [MonthlySalesHistoryListResponse] 참조).
     *
     * `@get:JsonProperty` 로 JSON 필드명을 고정한다 — Jackson 은 Kotlin `Boolean` 프로퍼티의 `is`
     * prefix 를 떼어 `deleted` 로 직렬화하므로, 명시하지 않으면 클라이언트의 `isDeleted` 가 항상
     * undefined 가 되어 삭제 표시가 조용히 동작하지 않는다.
     */
    @get:JsonProperty("isDeleted")
    val isDeleted: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: MonthlySalesHistory): MonthlySalesHistoryListItem =
            MonthlySalesHistoryListItem(
                id = entity.id,
                salesYear = entity.salesYear?.value,
                salesMonth = entity.salesMonth?.value,
                sapAccountCode = entity.sapAccountCode,
                externalKey = entity.externalkeyC,
                abcClosingAmount1 = entity.abcClosingAmount1,
                abcClosingAmount2 = entity.abcClosingAmount2,
                abcClosingAmount3 = entity.abcClosingAmount3,
                abcClosingAmount4 = entity.abcClosingAmount4,
                abcClosingSumAmount = entity.abcClosingSumAmount,
                shipClosingAmount1 = entity.shipClosingAmount1,
                shipClosingAmount2 = entity.shipClosingAmount2,
                shipClosingAmount3 = entity.shipClosingAmount3,
                shipClosingAmount4 = entity.shipClosingAmount4,
                shipClosingSumAmount = entity.shipClosingSumAmount,
                isDeleted = entity.isDeleted == true,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

/**
 * ORORA 월매출 목록 응답 — 거래처 1곳 + 매출년월 1개의 적재 행 + 합계.
 *
 * 정상 데이터라면 `content` 는 1건이다 (적재 upsert 키가 거래처+년월 단위). 2건 이상이면 SF 이관분의
 * 중복 row 이므로 합계는 전 행 기준으로 낸다.
 *
 * 합계 2종은 적재된 `abcClosingSumAmount` / `shipClosingSumAmount` 컬럼의 합이며, 온도대별 1~4 의
 * 재합산이 아니다 — SF 정합상 개별 온도대 컬럼이 비어 있고 합계 컬럼에만 값이 든 거래처/월이 존재해
 * 재합산하면 매출이 누락된다.
 *
 * ## soft-delete (`IsDeleted`) 취급
 * 삭제 row 를 **목록에서는 감추지 않고 합계에서만 제외** 한다. 일매출과 달리
 * [MonthlySalesHistory] 에는 SF `IsDeleted` 컬럼이 있어(일매출 entity 에는 없다) 정책 판단이 필요하다.
 * - 목록 노출: 적재 결과 확인이 목적인 화면이라 삭제 row 를 감추면 "적재됐는데 안 보인다" 가 되어
 *   화면의 목적을 해친다. 대신 [MonthlySalesHistoryListItem.isDeleted] 로 구분 가능하게 내린다.
 * - 합계 제외: 대시보드 등 기존 read 경로([MonthlySalesHistoryQueryGateway]) 가 `isDeleted != true` 를
 *   필터하므로, 합계까지 삭제 row 를 포함하면 같은 거래처/월의 금액이 화면마다 달라진다.
 */
data class MonthlySalesHistoryListResponse(
    /** 조회 매출년월 (`yyyyMM`). */
    val salesMonth: String,
    val sapAccountCode: String,
    val accountName: String?,
    val branchName: String?,
    val content: List<MonthlySalesHistoryListItem>,
    /** 전산마감실적 합계 (원) — 적재된 합계 컬럼 기준, soft-delete row 제외. */
    val totalAbcClosingAmount: Double,
    /** 물류마감실적 합계 (원) — 적재된 합계 컬럼 기준, soft-delete row 제외. */
    val totalShipClosingAmount: Double,
    /**
     * 조회한 거래처 + 매출년월 행의 마지막 적재 시각 (= `max(updated_at)`). 결과가 0건이면 null.
     *
     * 조회 월 기준으로 항상 정확한 유일한 출처다 — 배치 실행 이력(`scheduled_job_run`) 은 대상 월이
     * metadata JSON 에만 있고 보존이 90일이며 SF 이관분에는 이력 자체가 없어 쓰지 않는다
     * (일매출 화면과 동일 판단).
     */
    val lastMaterializedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            salesMonth: String,
            sapAccountCode: String,
            accountName: String?,
            branchName: String?,
            entities: List<MonthlySalesHistory>,
        ): MonthlySalesHistoryListResponse {
            // 합계 모수는 soft-delete 를 제외한 행 — 기존 read 경로(MonthlySalesHistoryQueryGateway) 와
            // 금액이 갈리지 않게 한다. 목록(content) 은 삭제 행도 그대로 노출한다.
            val liveRows = entities.filter { it.isDeleted != true }
            return MonthlySalesHistoryListResponse(
                salesMonth = salesMonth,
                sapAccountCode = sapAccountCode,
                accountName = accountName,
                branchName = branchName,
                content = entities.map { MonthlySalesHistoryListItem.from(it) },
                totalAbcClosingAmount = liveRows.sumOf { it.abcClosingSumAmount ?: 0.0 },
                totalShipClosingAmount = liveRows.sumOf { it.shipClosingSumAmount ?: 0.0 },
                lastMaterializedAt = entities.mapNotNull { it.updatedAt }.maxOrNull(),
            )
        }
    }
}
