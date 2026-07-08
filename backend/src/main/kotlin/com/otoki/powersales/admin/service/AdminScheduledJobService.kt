package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.response.OroraDailyChunkCatalogResponse
import com.otoki.powersales.admin.dto.response.OroraMaterializeAcceptedResponse
import com.otoki.powersales.admin.dto.response.OroraMonthlyChunkCatalogResponse
import com.otoki.powersales.admin.dto.response.OroraMonthlyChunkInfo
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.platform.batch.OroraDailySalesMaterializeBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import com.otoki.powersales.platform.batch.toggle.ScheduledJobToggleStore
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRun
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunRepository
import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 운영자 admin 화면용 `@Scheduled` 잡 실행 이력 조회 서비스.
 *
 * 시각 필드는 모두 UTC wall clock `LocalDateTime` 으로 처리한다 (전사 컨벤션 — 스펙 #564).
 * 요약 위젯의 `windowFrom`/`windowTo` 는 서버 `LocalDateTime.now()` 기준이며 응답에 그대로 포함되어
 * 화면이 사용자에게 윈도우 시각을 표기할 수 있다.
 */
@Service
@Transactional(readOnly = true)
class AdminScheduledJobService(
    private val scheduledJobRunRepository: ScheduledJobRunRepository,
    private val ororaSalesMaterializeFacade: OroraSalesMaterializeFacade,
    private val ororaMaterializeAsyncRunner: OroraMaterializeAsyncRunner,
    private val beanFactory: ListableBeanFactory,
    private val scheduledJobToggleStore: ScheduledJobToggleStore,
) {

    fun search(query: AdminScheduledJobQuery): ScheduledJobRunListResponse {
        val page = (query.page - 1).coerceAtLeast(0)
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, size)

        val result = scheduledJobRunRepository.search(
            jobName = query.jobName,
            status = query.status,
            from = query.from,
            to = query.to,
            pageable = pageable,
        )

        return ScheduledJobRunListResponse(
            items = result.content.map { it.toDto() },
            totalCount = result.totalElements,
            currentPage = query.page,
            pageSize = size,
        )
    }

    fun summary(windowHours: Long): ScheduledJobSummaryResponse {
        val to = LocalDateTime.now()
        val from = to.minus(windowHours.coerceIn(1L, MAX_WINDOW_HOURS), java.time.temporal.ChronoUnit.HOURS)
        val counts = scheduledJobRunRepository.countByStatusWithin(from, to)
        val running = counts[ScheduledJobRun.STATUS_RUNNING] ?: 0L
        val success = counts[ScheduledJobRun.STATUS_SUCCESS] ?: 0L
        val failure = counts[ScheduledJobRun.STATUS_FAILURE] ?: 0L

        val distinctFromTable = scheduledJobRunRepository.findDistinctJobNames()
        val distinctMerged = (ScheduledJobCatalog.JOB_NAMES + distinctFromTable).distinct().sorted()

        return ScheduledJobSummaryResponse(
            windowFrom = from,
            windowTo = to,
            totalCount = running + success + failure,
            runningCount = running,
            successCount = success,
            failureCount = failure,
            distinctJobNames = distinctMerged,
        )
    }

    /**
     * 등록된 `@Scheduled` 잡 카탈로그 + 현재 환경 활성 여부.
     *
     * 활성 여부는 각 Entry 의 배치 빈 타입이 [beanFactory] 에 등록되어 있는지로 판정한다. 배치 클래스는
     * `@ConditionalOnProperty(enabled)` + `@Profile(dev|prod)` 로 조건부 등록되므로, 빈 존재 여부가 곧
     * 현재 환경의 실제 스케줄링 활성 여부와 일치한다 (yml override 포함 반영).
     */
    fun catalog(): List<RegisteredScheduledJobDto> {
        val runtimeStates = scheduledJobToggleStore.getAllStates()
        return ScheduledJobCatalog.ENTRIES.map {
            RegisteredScheduledJobDto(
                jobName = it.jobName,
                cron = it.cron,
                description = it.description,
                enabled = beanFactory.getBeanNamesForType(it.beanType).isNotEmpty(),
                runtimeEnabled = runtimeStates[it.jobName] ?: true,
            )
        }
    }

    /**
     * 스케줄 잡의 런타임 활성/비활성 설정. 카탈로그에 없는 jobName 은 거절.
     */
    fun setRuntimeEnabled(jobName: String, enabled: Boolean) {
        val exists = ScheduledJobCatalog.JOB_NAMES.contains(jobName)
        require(exists) { "존재하지 않는 스케줄 잡: $jobName" }
        scheduledJobToggleStore.setEnabled(jobName, enabled)
    }

    /**
     * ORORA 월매출 적재를 특정 월(`YYYYMM`)로 **비동기** 수동 실행 접수한다.
     *
     * 입력 월을 검증한 뒤 실제 적재는 [OroraMaterializeAsyncRunner.runMonthly] 로 별도 스레드에 위임하고
     * 즉시 접수 응답을 돌려준다 — ORORA view SELECT 가 수십 초~수 분 걸려도 HTTP 요청이 매달리지 않도록.
     * 조회/적재 건수 등 결과는 완료 후 `scheduled_job_run` 이력에 남으며 이력 탭에서 확인한다.
     * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     *
     * @param salesMonth `YYYYMM` 6자 (예: `202604`). null/blank 면 전월 자동 산출.
     */
    fun triggerOroraMonthly(salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = validateSalesMonth(salesMonth)
        ororaMaterializeAsyncRunner.runMonthly(normalized)
        return acceptedResponse(
            jobName = OroraMonthlySalesMaterializeBatch.JOB_NAME,
            salesMonth = ororaSalesMaterializeFacade.previewMonthlySalesMonth(normalized),
        )
    }

    /**
     * ORORA 월매출 거래처 청크 메타 — 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공.
     *
     * 청크 경계는 정적 거래처 범위(`app.batch.orora.account-range.*`) 에서 산출되므로 ORORA 호출 없이 즉시 반환된다.
     */
    fun ororaMonthlyChunkCatalog(): OroraMonthlyChunkCatalogResponse {
        val boundaries = ororaSalesMaterializeFacade.monthlyChunkBoundaries()
        return OroraMonthlyChunkCatalogResponse(
            chunkCount = boundaries.size,
            chunkSize = ororaSalesMaterializeFacade.chunkSize(),
            chunks = boundaries.mapIndexed { index, (from, to) ->
                OroraMonthlyChunkInfo(chunkIndex = index, fromAccountCode = from, toAccountCode = to)
            },
        )
    }

    /**
     * ORORA 월매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다.
     *
     * 전체 범위를 도는 [triggerOroraMonthly] 와 달리 선택 청크의 거래처 구간만 적재한다 (특정 구간 재적재 / 부분 점검).
     * 스케줄 배치와 동일한 `JOB_NAME` 으로 [ScheduledJobRunner] 로 감싸 `scheduled_job_run` 이력에 남긴다 —
     * 화면 이력 탭에서 chunk 수동 실행도 함께 조회된다. metadata 의 `trigger=manual-chunk` + `chunkIndex` 로 구분.
     *
     * @param chunkIndex 적재 대상 청크 번호 (0-based). `[0, chunkCount)` 범위.
     * @param salesMonth `YYYYMM` 6자. null/blank 면 전월 자동 산출.
     */
    fun triggerOroraMonthlyChunk(chunkIndex: Int, salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = validateSalesMonth(salesMonth)
        val chunkCount = ororaSalesMaterializeFacade.monthlyChunkCount()
        require(chunkIndex in 0 until chunkCount) {
            "chunkIndex 는 0 이상 $chunkCount 미만이어야 합니다: $chunkIndex"
        }
        ororaMaterializeAsyncRunner.runMonthlyChunk(chunkIndex, normalized)
        return acceptedResponse(
            jobName = OroraMonthlySalesMaterializeBatch.JOB_NAME,
            salesMonth = ororaSalesMaterializeFacade.previewMonthlySalesMonth(normalized),
        )
    }

    /**
     * ORORA 일매출 적재를 특정 월(`YYYYMM`)로 수동 실행한다.
     *
     * 입력 월을 검증한 뒤 [OroraSalesMaterializeFacade.materializeDaily] 에 명시 월로 위임한다 (당월
     * 자동 산출 대신 운영자 지정 월 재적재). 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     * facade 내부 chunk processor 가 별도 트랜잭션으로 적재하므로 본 서비스의 readOnly 경계와 무관.
     *
     * @param salesMonth `YYYYMM` 6자 (예: `202606`). null/blank 면 당월 자동 산출.
     */
    fun triggerOroraDaily(salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = validateSalesMonth(salesMonth)
        ororaMaterializeAsyncRunner.runDaily(normalized)
        return acceptedResponse(
            jobName = OroraDailySalesMaterializeBatch.JOB_NAME,
            salesMonth = ororaSalesMaterializeFacade.previewDailySalesMonth(normalized),
        )
    }

    /**
     * ORORA 일매출 거래처 청크 메타 — 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공.
     *
     * 청크 경계는 정적 거래처 범위(`app.batch.orora.account-range.*`) 에서 산출되므로 ORORA 호출 없이 즉시 반환된다.
     * 거래처 범위가 일·월 공용이라 [ororaMonthlyChunkCatalog] 와 같은 경계를 반환한다.
     */
    fun ororaDailyChunkCatalog(): OroraDailyChunkCatalogResponse {
        val boundaries = ororaSalesMaterializeFacade.dailyChunkBoundaries()
        return OroraDailyChunkCatalogResponse(
            chunkCount = boundaries.size,
            chunkSize = ororaSalesMaterializeFacade.chunkSize(),
            chunks = boundaries.mapIndexed { index, (from, to) ->
                OroraMonthlyChunkInfo(chunkIndex = index, fromAccountCode = from, toAccountCode = to)
            },
        )
    }

    /**
     * ORORA 일매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다.
     *
     * 전체 범위를 도는 [triggerOroraDaily] 와 달리 선택 청크의 거래처 구간만 적재한다 (특정 구간 재적재 / 부분 점검).
     * 스케줄 배치와 동일한 `JOB_NAME` 으로 [ScheduledJobRunner] 로 감싸 `scheduled_job_run` 이력에 남긴다 —
     * 화면 이력 탭에서 chunk 수동 실행도 함께 조회된다. metadata 의 `trigger=manual-chunk` + `chunkIndex` 로 구분.
     *
     * @param chunkIndex 적재 대상 청크 번호 (0-based). `[0, chunkCount)` 범위.
     * @param salesMonth `YYYYMM` 6자. null/blank 면 당월 자동 산출.
     */
    fun triggerOroraDailyChunk(chunkIndex: Int, salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = validateSalesMonth(salesMonth)
        val chunkCount = ororaSalesMaterializeFacade.dailyChunkCount()
        require(chunkIndex in 0 until chunkCount) {
            "chunkIndex 는 0 이상 $chunkCount 미만이어야 합니다: $chunkIndex"
        }
        ororaMaterializeAsyncRunner.runDailyChunk(chunkIndex, normalized)
        return acceptedResponse(
            jobName = OroraDailySalesMaterializeBatch.JOB_NAME,
            salesMonth = ororaSalesMaterializeFacade.previewDailySalesMonth(normalized),
        )
    }

    /**
     * 월별 합계 재집계를 특정 월(`YYYYMM`)로 **비동기** 수동 실행 접수한다.
     *
     * ORORA 조회 없이 이미 적재된 `daily_sales_history` 만으로 `monthly_sales_history` 합계를 재계산한다
     * (외부 연동 없음, RDS read+upsert 만). daily 배치와 동일 JOB_NAME 으로 이력에 남으며 metadata
     * `trigger=manual-reaggregate` 로 구분된다. RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     *
     * @param salesMonth `YYYYMM` 6자. **필수** — 재집계는 자동 산출하지 않고 대상 월을 반드시 지정한다.
     */
    fun triggerMonthlyReaggregate(salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = requireSalesMonth(salesMonth)
        ororaMaterializeAsyncRunner.runMonthlyReaggregate(normalized)
        return acceptedResponse(jobName = OroraDailySalesMaterializeBatch.JOB_NAME, salesMonth = normalized)
    }

    /**
     * 월별 합계 재집계를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다. `salesMonth` 필수.
     */
    fun triggerMonthlyReaggregateChunk(chunkIndex: Int, salesMonth: String?): OroraMaterializeAcceptedResponse {
        val normalized = requireSalesMonth(salesMonth)
        val chunkCount = ororaSalesMaterializeFacade.dailyChunkCount()
        require(chunkIndex in 0 until chunkCount) {
            "chunkIndex 는 0 이상 $chunkCount 미만이어야 합니다: $chunkIndex"
        }
        ororaMaterializeAsyncRunner.runMonthlyReaggregateChunk(chunkIndex, normalized)
        return acceptedResponse(jobName = OroraDailySalesMaterializeBatch.JOB_NAME, salesMonth = normalized)
    }

    /** salesMonth 입력 정규화 + `YYYYMM` 6자리 숫자 검증. null/blank 면 null 반환(자동 산출 위임). */
    private fun validateSalesMonth(salesMonth: String?): String? {
        val normalized = salesMonth?.trim()?.takeIf { it.isNotBlank() } ?: return null
        require(normalized.length == 6 && normalized.all { it.isDigit() }) {
            "salesMonth 는 YYYYMM 6자리 숫자여야 합니다: $salesMonth"
        }
        return normalized
    }

    /** salesMonth 필수 검증 — null/blank 면 예외. 재집계는 자동 산출하지 않고 대상 월 지정을 강제한다. */
    private fun requireSalesMonth(salesMonth: String?): String {
        val normalized = validateSalesMonth(salesMonth)
        requireNotNull(normalized) { "salesMonth 는 필수입니다 (재집계 대상 월을 지정하세요)." }
        return normalized
    }

    /** ORORA 매출 비동기 적재 접수 공통 응답 조립. */
    private fun acceptedResponse(jobName: String, salesMonth: String): OroraMaterializeAcceptedResponse =
        OroraMaterializeAcceptedResponse(
            jobName = jobName,
            salesMonth = salesMonth,
            message = "$salesMonth 적재를 시작했습니다. 진행/결과는 실행 이력 탭에서 확인하세요.",
        )

    private fun ScheduledJobRun.toDto(): ScheduledJobRunDto {
        val durationMs = endedAt?.let { end ->
            java.time.Duration.between(startedAt, end).toMillis()
        }
        return ScheduledJobRunDto(
            id = id,
            jobName = jobName,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMs = durationMs,
            status = status,
            errorMessage = errorMessage,
            metadata = metadata,
        )
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
        const val MAX_WINDOW_HOURS = 24L * 90
    }
}
