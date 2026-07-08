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
    /**
     * 런타임 토글 활성 여부 (Redis). 운영 중 끄고 켜는 토글로, 빈 등록 여부인 [enabled] 와 별개다.
     * `false` 면 자동 스케줄 발화 시 본문을 실행하지 않고 `SKIPPED` 이력만 남긴다.
     * 정적으로 비활성([enabled]=false)인 잡은 애초에 발화하지 않으므로 본 값의 영향을 받지 않는다.
     */
    val runtimeEnabled: Boolean,
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
 * ORORA 매출 수동 적재 **비동기 접수** 결과.
 *
 * 적재는 별도 스레드([com.otoki.powersales.admin.service.OroraMaterializeAsyncRunner])에서 수행되므로,
 * 본 응답은 "접수됨" 만 알린다 (실제 조회/적재 건수는 완료 후 `scheduled_job_run` 이력에 남는다).
 * ORORA view SELECT 가 수십 초~수 분 걸려도 HTTP 요청이 그 시간 동안 열려 있지 않도록 즉시 반환한다.
 *
 * @property jobName 실행을 접수한 스케줄 잡 이름 (이력 탭 필터 값)
 * @property salesMonth 적재 대상 매출월 (`YYYYMM`). 요청에서 미지정 시 서버가 산출한 값.
 * @property accepted 접수 여부 (항상 true — 접수 실패는 예외로 전파)
 * @property message 사용자 안내 문구 (이력 탭에서 진행 확인 유도)
 */
data class OroraMaterializeAcceptedResponse(
    val jobName: String,
    val salesMonth: String,
    val accepted: Boolean = true,
    val message: String,
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

/**
 * ORORA 일매출 거래처 청크 메타 — 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공.
 *
 * 거래처 범위가 일·월 공용이라 [OroraMonthlyChunkCatalogResponse] 와 형태가 동일하다.
 *
 * @property chunkCount 전체 거래처 청크 개수
 * @property chunkSize 청크 1개의 거래처 코드 폭
 * @property chunks 각 청크의 0-based index 와 거래처 코드 경계 (UI 표시용)
 */
data class OroraDailyChunkCatalogResponse(
    val chunkCount: Int,
    val chunkSize: Long,
    val chunks: List<OroraMonthlyChunkInfo>,
)
