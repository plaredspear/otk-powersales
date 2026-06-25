package com.otoki.powersales.admin.dto.response

import java.time.LocalDateTime

data class ScheduledJobRunDto(
    val id: Long,
    val jobName: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val durationMs: Long?,
    val status: String,
    val errorMessage: String?,
    val metadata: String?,
)

data class ScheduledJobRunListResponse(
    val items: List<ScheduledJobRunDto>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)

data class ScheduledJobSummaryResponse(
    val windowFrom: LocalDateTime,
    val windowTo: LocalDateTime,
    val totalCount: Long,
    val runningCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val distinctJobNames: List<String>,
)

data class RegisteredScheduledJobDto(
    val jobName: String,
    val cron: String,
    val description: String,
    /**
     * 현재 환경에서 해당 배치 빈이 실제로 등록(스케줄링)되어 있는지 여부.
     *
     * 각 배치의 `@ConditionalOnProperty(enabled)` + `@Profile(dev|prod)` 평가 결과를 그대로 반영한다
     * (빈 등록 = 활성). local 등 프로필이나 enabled=false 설정으로 비활성화된 잡은 `false`.
     */
    val enabled: Boolean,
)

/**
 * `@Scheduled` 잡 수동 실행 트리거 결과.
 *
 * 수동 실행도 자동 스케줄과 동일하게 `ScheduledJobRunner` 로 감싸 `scheduled_job_run` 에 이력이
 * 남는다. 본 엔드포인트는 동기 실행으로, 본문이 예외 없이 끝나면 `SUCCESS` 를 반환한다 (실패 시
 * 예외가 전파되어 에러 응답). 화면은 본 응답으로 즉시 결과를 띄우고 이력 탭을 새로고침한다.
 *
 * @property jobName 실행한 잡 이름
 * @property status 실행 결과 상태 (정상 반환 시 항상 `SUCCESS`)
 */
data class ScheduledJobManualTriggerResponse(
    val jobName: String,
    val status: String,
)

/**
 * ORORA 월매출 수동 적재 트리거 결과.
 *
 * @property salesMonth 적재한 대상 매출월 (`YYYYMM`)
 * @property fetchedCount ORORA view 에서 조회된 row 수
 * @property upsertedCount RDS 에 적재(신규+갱신)된 row 수
 * @property skippedAccountUnmatchedCount account 미매칭으로 account_id=null 적재된 row 수
 */
data class OroraMonthlyMaterializeTriggerResponse(
    val salesMonth: String,
    val fetchedCount: Int,
    val upsertedCount: Int,
    val skippedAccountUnmatchedCount: Int,
)

/**
 * ORORA 월매출 거래처 청크 메타 — 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공.
 *
 * @property chunkCount 전체 거래처 청크 개수
 * @property chunkSize 청크 1개의 거래처 코드 폭
 * @property chunks 각 청크의 0-based index 와 거래처 코드 경계 (UI 표시용)
 */
data class OroraMonthlyChunkCatalogResponse(
    val chunkCount: Int,
    val chunkSize: Long,
    val chunks: List<OroraMonthlyChunkInfo>,
)

/**
 * 거래처 청크 1개의 메타.
 *
 * @property chunkIndex 0-based 청크 번호
 * @property fromAccountCode 청크 시작 거래처 코드 (ORORA view 원본 형식, 선행 `000` 포함)
 * @property toAccountCode 청크 끝 거래처 코드 (ORORA view 원본 형식, 선행 `000` 포함)
 */
data class OroraMonthlyChunkInfo(
    val chunkIndex: Int,
    val fromAccountCode: String,
    val toAccountCode: String,
)
