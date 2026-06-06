package com.otoki.powersales.herokumigration.stage1

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Heroku Stage 1 S3 → COPY 적재 진행 상태 in-memory 추적 (1회성 cut-over 도구).
 *
 * SF [com.otoki.powersales.sfmigration.stage1.Stage1CopyProgress] 와 동형. 단일 backend
 * 인스턴스 가정 — backend 재시작 시 이력 손실되나 [HerokuStage1S3CopyService] 의 INFO 로그가
 * CloudWatch 에 영구 보관되어 감사 가능. 동시 실행 1회 가정 (controller 가 RUNNING 시 거부).
 *
 * Heroku 고유 — EmployeeInfo 처럼 적재 시점 PK resolve 가 필요한 entity 는 employee_code 가
 * employee 에 없으면 INSERT 불가하므로, 그 미매칭 수를 [unmatchedRows] 로 누적해 UI 가 확인.
 */
@Component
class HerokuStage1CopyProgress {

    enum class Status { IDLE, RUNNING, COMPLETED, FAILED }

    enum class EntityStatus { PENDING, RUNNING, COMPLETED, FAILED, SKIPPED }

    /**
     * 일괄 실행 시 각 entity 의 적재 결과.
     *
     * @param unmatchedRows 적재 시점 PK resolve 실패 (employee_code 가 employee 에 없음) 로 INSERT 못 한 수
     */
    data class EntityResult(
        val targetName: String,
        val status: EntityStatus,
        val s3Key: String?,
        val processedRows: Long,
        val filteredOut: Long,
        val insertedRows: Long,
        val unmatchedRows: Long,
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
    private val unmatchedRowsRef = AtomicLong(0L)

    val errors: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    private val entityResultsRef: MutableList<EntityResult> =
        Collections.synchronizedList(mutableListOf())

    val processedRows: Long get() = processedRowsRef.get()
    val filteredOut: Long get() = filteredOutRef.get()
    val insertedRows: Long get() = insertedRowsRef.get()
    val unmatchedRows: Long get() = unmatchedRowsRef.get()
    val entityResults: List<EntityResult> get() = synchronized(entityResultsRef) { entityResultsRef.toList() }

    fun begin(targetName: String, s3Bucket: String, s3Key: String) {
        this.mode = Mode.SINGLE
        this.targetName = targetName
        this.s3Bucket = s3Bucket
        this.s3Key = s3Key
        this.processedRowsRef.set(0L)
        this.filteredOutRef.set(0L)
        this.insertedRowsRef.set(0L)
        this.unmatchedRowsRef.set(0L)
        this.errors.clear()
        synchronized(entityResultsRef) { entityResultsRef.clear() }
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
    }

    /** Batch 모드 시작 — entity list 를 PENDING 으로 미리 채워 UI 가 즉시 일람 표시 가능. */
    fun beginBatch(s3Bucket: String, plannedTargets: List<String>) {
        this.mode = Mode.BATCH
        this.targetName = null
        this.s3Bucket = s3Bucket
        this.s3Key = null
        this.processedRowsRef.set(0L)
        this.filteredOutRef.set(0L)
        this.insertedRowsRef.set(0L)
        this.unmatchedRowsRef.set(0L)
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
                        unmatchedRows = 0L,
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
    }

    fun beginEntity(targetName: String, s3Key: String) {
        this.targetName = targetName
        this.s3Key = s3Key
        updateEntity(targetName) {
            it.copy(status = EntityStatus.RUNNING, s3Key = s3Key, startedAt = Instant.now())
        }
    }

    fun finishEntityOk(
        targetName: String,
        processed: Long,
        filteredOut: Long,
        inserted: Long,
        unmatched: Long,
    ) {
        updateEntity(targetName) {
            it.copy(
                status = EntityStatus.COMPLETED,
                processedRows = processed,
                filteredOut = filteredOut,
                insertedRows = inserted,
                unmatchedRows = unmatched,
                finishedAt = Instant.now(),
            )
        }
    }

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
     * 단건 entity 를 SKIPPED 로 마크 — batch 모드에서 S3 에 CSV 가 없는(404) entity 를 건너뛸 때 사용.
     * 실패(FAILED)와 달리 batch 를 중단시키지 않으며, 사유는 errorMessage 에 기록해 UI 가 구분 가능.
     */
    fun skipEntity(targetName: String, s3Key: String, reason: String) {
        updateEntity(targetName) {
            it.copy(
                status = EntityStatus.SKIPPED,
                s3Key = s3Key,
                errorMessage = reason,
                finishedAt = Instant.now(),
            )
        }
    }

    /** 즉시 중단 모드: 실패 발생 시 나머지 PENDING entity 들을 SKIPPED 로 일괄 마크. */
    fun markRemainingAsSkipped() {
        synchronized(entityResultsRef) {
            val updated = entityResultsRef.map {
                if (it.status == EntityStatus.PENDING) it.copy(status = EntityStatus.SKIPPED) else it
            }
            entityResultsRef.clear()
            entityResultsRef.addAll(updated)
        }
    }

    private fun updateEntity(targetName: String, transform: (EntityResult) -> EntityResult) {
        synchronized(entityResultsRef) {
            val idx = entityResultsRef.indexOfFirst { it.targetName == targetName }
            if (idx >= 0) entityResultsRef[idx] = transform(entityResultsRef[idx])
        }
    }

    fun advanceProcessed(delta: Long = 1L) {
        processedRowsRef.addAndGet(delta)
    }

    fun advanceFiltered(delta: Long = 1L) {
        filteredOutRef.addAndGet(delta)
    }

    fun setInserted(value: Long) {
        insertedRowsRef.set(value)
    }

    fun addInserted(delta: Long) {
        insertedRowsRef.addAndGet(delta)
    }

    fun setUnmatched(value: Long) {
        unmatchedRowsRef.set(value)
    }

    fun addUnmatched(delta: Long) {
        unmatchedRowsRef.addAndGet(delta)
    }

    fun finishOk() {
        this.finishedAt = Instant.now()
        this.status = Status.COMPLETED
    }

    fun finishWithFailure(message: String) {
        errors += message
        this.finishedAt = Instant.now()
        this.status = Status.FAILED
    }
}
