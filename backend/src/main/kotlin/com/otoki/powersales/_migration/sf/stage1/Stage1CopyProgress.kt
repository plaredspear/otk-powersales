package com.otoki.powersales._migration.sf.stage1

import com.otoki.powersales._migration.common.MigrationProgressStore
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Stage 1 S3 → COPY 적재 진행 상태 추적 (1회성 cut-over 도구).
 *
 * 상태는 in-memory 필드로 유지하되, 모든 mutate 후 [MigrationProgressStore] 로 스냅샷을 Redis 에
 * 저장한다 — 다중 인스턴스에서 실행 인스턴스와 `/progress` polling 인스턴스가 갈려도 같은 진행 상태를
 * 읽게 한다 (Redis 미사용 환경은 in-memory 폴백). backend 재시작 시 in-memory 이력은 손실되나
 * [Stage1S3CopyService] 의 INFO 로그가 CloudWatch 에 영구 보관되어 감사 가능.
 *
 * 동시 실행 1회 가정 — controller 가 RUNNING 시 신규 요청을 거부한다.
 *
 * 진행 단위: row 카운트 (CSV 의 data row 기준). totalRows 는 사전 미상 (streaming) 이라
 * UI 는 processed 누적값을 그대로 표시하거나 (totalRows 0 → "indeterminate") 종료 후
 * 최종 카운트를 표시.
 *
 * Batch 모드: 일괄 실행 시 entity 별 EntityResult 를 누적해 [entityResults] 로 노출.
 * 단건 모드는 [entityResults] 비어있고 targetName 만 채워진다.
 */
@Component
class Stage1CopyProgress(
    private val store: MigrationProgressStore,
) {

    enum class Status { IDLE, RUNNING, COMPLETED, FAILED }

    enum class EntityStatus { PENDING, RUNNING, COMPLETED, FAILED, SKIPPED }

    /**
     * 일괄 실행 시 각 entity 의 적재 결과.
     *
     * @param targetName 처리한 entity
     * @param status 처리 결과 (PENDING/RUNNING/COMPLETED/FAILED/SKIPPED)
     * @param s3Key 사용된 S3 key (prefix + csvFileName 자동 조립)
     * @param processedRows CSV 누적 read 수
     * @param filteredOut NOT NULL pre-filter 로 제외된 수
     * @param insertedRows ON CONFLICT DO NOTHING 적용 후 실제 적재된 수
     * @param errorMessage FAILED 시의 실패 사유 (Exception class + message)
     * @param startedAt entity 처리 시작 시각
     * @param finishedAt entity 처리 종료 시각
     */
    data class EntityResult(
        val targetName: String,
        val status: EntityStatus,
        val s3Key: String?,
        val processedRows: Long,
        val filteredOut: Long,
        val insertedRows: Long,
        val errorMessage: String?,
        val startedAt: Instant?,
        val finishedAt: Instant?,
    )

    enum class Mode { SINGLE, BATCH }

    @Volatile var mode: Mode = Mode.SINGLE
        internal set

    @Volatile var status: Status = Status.IDLE
        internal set

    @Volatile var startedAt: Instant? = null
        internal set

    @Volatile var finishedAt: Instant? = null
        internal set

    @Volatile var targetName: String? = null
        internal set

    @Volatile var s3Bucket: String? = null
        internal set

    @Volatile var s3Key: String? = null
        internal set

    private val processedRowsRef = AtomicLong(0L)
    private val filteredOutRef = AtomicLong(0L)
    private val insertedRowsRef = AtomicLong(0L)

    val errors: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    // batch 모드 entity 결과 — list 자체 갱신은 synchronized 로 보호.
    private val entityResultsRef: MutableList<EntityResult> =
        Collections.synchronizedList(mutableListOf())

    val processedRows: Long get() = processedRowsRef.get()
    val filteredOut: Long get() = filteredOutRef.get()
    val insertedRows: Long get() = insertedRowsRef.get()
    val entityResults: List<EntityResult> get() = synchronized(entityResultsRef) { entityResultsRef.toList() }

    fun begin(targetName: String, s3Bucket: String, s3Key: String) {
        this.mode = Mode.SINGLE
        this.targetName = targetName
        this.s3Bucket = s3Bucket
        this.s3Key = s3Key
        this.processedRowsRef.set(0L)
        this.filteredOutRef.set(0L)
        this.insertedRowsRef.set(0L)
        this.errors.clear()
        synchronized(entityResultsRef) { entityResultsRef.clear() }
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
        persist()
    }

    /**
     * Batch 모드 시작 — entity list 를 PENDING 으로 미리 채워 UI 가 즉시 일람을 표시 가능.
     */
    fun beginBatch(s3Bucket: String, plannedTargets: List<String>) {
        this.mode = Mode.BATCH
        this.targetName = null
        this.s3Bucket = s3Bucket
        this.s3Key = null
        this.processedRowsRef.set(0L)
        this.filteredOutRef.set(0L)
        this.insertedRowsRef.set(0L)
        this.errors.clear()
        synchronized(entityResultsRef) {
            entityResultsRef.clear()
            for (t in plannedTargets) {
                entityResultsRef.add(
                    EntityResult(
                        targetName = t,
                        status = EntityStatus.PENDING,
                        s3Key = null,
                        processedRows = 0L,
                        filteredOut = 0L,
                        insertedRows = 0L,
                        errorMessage = null,
                        startedAt = null,
                        finishedAt = null,
                    )
                )
            }
        }
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
        persist()
    }

    /**
     * Batch 의 단일 entity 처리 직전 호출 — 현재 entity name + s3Key 를 갱신 + RUNNING 표시.
     * 누적 카운터 (processed/filtered/inserted) 는 batch 전체 누적으로 유지.
     */
    fun beginEntity(targetName: String, s3Key: String) {
        this.targetName = targetName
        this.s3Key = s3Key
        updateEntity(targetName) {
            it.copy(
                status = EntityStatus.RUNNING,
                s3Key = s3Key,
                startedAt = Instant.now(),
            )
        }
    }

    /**
     * Batch 의 단일 entity 정상 종료 — 누적 갱신 + entity 결과 COMPLETED.
     */
    fun finishEntityOk(targetName: String, processed: Long, filteredOut: Long, inserted: Long) {
        updateEntity(targetName) {
            it.copy(
                status = EntityStatus.COMPLETED,
                processedRows = processed,
                filteredOut = filteredOut,
                insertedRows = inserted,
                finishedAt = Instant.now(),
            )
        }
    }

    /**
     * Batch 의 단일 entity 실패 종료 — entity 결과 FAILED + 실패 사유 기록.
     */
    fun finishEntityFailed(targetName: String, processed: Long, filteredOut: Long, errorMessage: String) {
        updateEntity(targetName) {
            it.copy(
                status = EntityStatus.FAILED,
                processedRows = processed,
                filteredOut = filteredOut,
                errorMessage = errorMessage,
                finishedAt = Instant.now(),
            )
        }
    }

    /**
     * 즉시 중단 모드: 실패 발생 시 나머지 PENDING entity 들을 SKIPPED 로 일괄 마크.
     */
    fun markRemainingAsSkipped() {
        synchronized(entityResultsRef) {
            val updated = entityResultsRef.map {
                if (it.status == EntityStatus.PENDING) it.copy(status = EntityStatus.SKIPPED) else it
            }
            entityResultsRef.clear()
            entityResultsRef.addAll(updated)
        }
        persist()
    }

    private fun updateEntity(targetName: String, transform: (EntityResult) -> EntityResult) {
        synchronized(entityResultsRef) {
            val idx = entityResultsRef.indexOfFirst { it.targetName == targetName }
            if (idx >= 0) entityResultsRef[idx] = transform(entityResultsRef[idx])
        }
        persist()
    }

    fun advanceProcessed(delta: Long = 1L) {
        processedRowsRef.addAndGet(delta)
        persist()
    }

    fun advanceFiltered(delta: Long = 1L) {
        filteredOutRef.addAndGet(delta)
        persist()
    }

    fun setInserted(value: Long) {
        insertedRowsRef.set(value)
        persist()
    }

    fun addInserted(delta: Long) {
        insertedRowsRef.addAndGet(delta)
        persist()
    }

    fun finishOk() {
        this.finishedAt = Instant.now()
        this.status = Status.COMPLETED
        persist()
    }

    fun finishWithFailure(message: String) {
        errors += message
        this.finishedAt = Instant.now()
        this.status = Status.FAILED
        persist()
    }

    /** 현재 in-memory 상태를 응답 DTO 로 스냅샷. */
    fun toResponse(): Stage1CopyProgressResponse = Stage1CopyProgressResponse(
        status = status.name,
        mode = mode.name,
        startedAt = startedAt,
        finishedAt = finishedAt,
        targetName = targetName,
        s3Bucket = s3Bucket,
        s3Key = s3Key,
        processedRows = processedRows,
        filteredOut = filteredOut,
        insertedRows = insertedRows,
        errors = errors.toList(),
        entityResults = entityResults.map {
            Stage1EntityResultResponse(
                targetName = it.targetName,
                status = it.status.name,
                s3Key = it.s3Key,
                processedRows = it.processedRows,
                filteredOut = it.filteredOut,
                insertedRows = it.insertedRows,
                errorMessage = it.errorMessage,
                startedAt = it.startedAt,
                finishedAt = it.finishedAt,
            )
        },
    )

    /**
     * polling 조회용 스냅샷 — Redis 우선 (다중 인스턴스 공유), 부재/미사용 시 in-memory [toResponse].
     */
    fun loadResponse(): Stage1CopyProgressResponse =
        store.load(MigrationProgressStore.SLUG_SF_STAGE1, Stage1CopyProgressResponse::class.java)
            ?: toResponse()

    private fun persist() = store.save(MigrationProgressStore.SLUG_SF_STAGE1, toResponse())
}
