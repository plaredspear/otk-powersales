package com.otoki.powersales.domain.sales.materialize

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ORORA 매출이력 적재의 진입 facade (Spec #855).
 *
 * 배치 / 수동 트리거 공통으로 사용하는 거래처 범위 조립 + 대상 월 결정(동적 산출, Q2) 을 한 곳에 둔다.
 * 거래처 범위 기본값은 레거시 `OroraDailyAccountRange__mdt` 실측(from 1000000 / to 1100000 / range 2000) 정합.
 */
@Service
class OroraSalesMaterializeFacade(
    private val monthlyService: OroraMonthlySalesMaterializeService,
    private val dailyService: OroraDailySalesMaterializeService,
    @Value("\${app.batch.orora.account-range.from:1000000}") private val rangeFrom: Long,
    @Value("\${app.batch.orora.account-range.to:1100000}") private val rangeTo: Long,
    @Value("\${app.batch.orora.account-range.chunk-size:2000}") private val chunkSize: Long,
) {

    /**
     * 월별 적재 — `salesMonth` null 이면 전월(前月) 동적 산출.
     *
     * 레거시 ORORA 월배치(`IF_REST_ORORA_ReceiveMonthlySalesHistory`)는 익월 초에 1회 실행되어
     * **전월 마감분**을 적재했다 (`OroraYearMonth__mdt` 에 전월 `YYYYMM` 수동 설정 + 운영자 수동 실행).
     * 운영 DB `created_at` 분포 실측에서도 매월 익월 초 ≈8,900건이 전월 영업월로 적재됨을 확인 →
     * 본 배치는 월 1회 cron 으로 전월을 동적 산출한다 (daily 의 당월 산출과 분리).
     */
    fun materializeMonthly(salesMonth: String? = null): OroraMonthlyMaterializeResult =
        monthlyService.materialize(resolvePreviousSalesMonth(salesMonth), accountRange())

    /**
     * 월별 적재 — 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 실행.
     *
     * 전체 거래처 범위를 도는 [materializeMonthly] 대신, 운영자가 선택한 단일 청크의 거래처 구간만
     * 적재한다 (특정 거래처 구간 재적재 / 부분 점검용). `salesMonth` null 이면 전월 동적 산출.
     *
     * @throws IndexOutOfBoundsException `chunkIndex` 가 `[0, monthlyChunkCount())` 밖일 때
     */
    fun materializeMonthlyChunk(chunkIndex: Int, salesMonth: String? = null): OroraMonthlyMaterializeResult =
        monthlyService.materialize(resolvePreviousSalesMonth(salesMonth), accountRange().singleChunk(chunkIndex))

    /**
     * 월별 적재의 전체 거래처 청크 개수 — 수동 트리거 UI 가 "전체 N개 중 몇 번째" 를 선택하도록 노출.
     */
    fun monthlyChunkCount(): Int = accountRange().chunkCount()

    /**
     * 월별 적재의 거래처 청크 경계 목록 — 수동 트리거 UI 의 청크 선택 표시용.
     *
     * 각 항목은 (from, to) 거래처 코드 (ORORA view 원본 형식, 선행 `000` 포함). 0-based index 는 리스트 순서.
     */
    fun monthlyChunkBoundaries(): List<Pair<String, String>> = accountRange().toChunks()

    /** 청크 선택 수동 트리거가 참조하는 청크 폭 (거래처 코드 폭). */
    fun chunkSize(): Long = chunkSize

    /**
     * 일별 적재 + 월별 합계 갱신 — `salesMonth` null 이면 당월 동적 산출 (Q2 옵션 1).
     */
    fun materializeDaily(salesMonth: String? = null): OroraDailyMaterializeResult =
        dailyService.materialize(resolveSalesMonth(salesMonth), accountRange())

    private fun accountRange(): OroraAccountRange = OroraAccountRange(rangeFrom, rangeTo, chunkSize)

    /**
     * 대상 매출월 결정 (일별) — 명시값 우선, 없으면 현재 시각 기준 당월 `YYYYMM`.
     */
    private fun resolveSalesMonth(salesMonth: String?): String =
        salesMonth?.takeIf { it.isNotBlank() } ?: LocalDate.now().format(YYYYMM)

    /**
     * 대상 매출월 결정 (월별) — 명시값 우선, 없으면 현재 시각 기준 전월 `YYYYMM`.
     *
     * 월배치는 익월 초에 실행되어 전월 마감분을 적재하므로 `now().minusMonths(1)` 산출.
     */
    private fun resolvePreviousSalesMonth(salesMonth: String?): String =
        salesMonth?.takeIf { it.isNotBlank() } ?: LocalDate.now().minusMonths(1).format(YYYYMM)

    companion object {
        private val YYYYMM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
    }
}
