package com.otoki.powersales.sfmigration.stage1

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Stage 1 S3 → COPY 적재 진행 상태 in-memory 추적 (1회성 cut-over 도구).
 *
 * 단일 backend 인스턴스 가정. backend 재시작 시 이력 손실되나 [Stage1S3CopyService] 의
 * INFO 로그가 CloudWatch 에 영구 보관되어 감사 가능.
 *
 * 동시 실행 1회 가정 — controller 가 RUNNING 시 신규 요청을 거부한다.
 *
 * 진행 단위: row 카운트 (CSV 의 data row 기준). totalRows 는 사전 미상 (streaming) 이라
 * UI 는 processed 누적값을 그대로 표시하거나 (totalRows 0 → "indeterminate") 종료 후
 * 최종 카운트를 표시.
 */
@Component
class Stage1CopyProgress {

    enum class Status { IDLE, RUNNING, COMPLETED, FAILED }

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

    val processedRows: Long get() = processedRowsRef.get()
    val filteredOut: Long get() = filteredOutRef.get()
    val insertedRows: Long get() = insertedRowsRef.get()

    fun begin(targetName: String, s3Bucket: String, s3Key: String) {
        this.targetName = targetName
        this.s3Bucket = s3Bucket
        this.s3Key = s3Key
        this.processedRowsRef.set(0L)
        this.filteredOutRef.set(0L)
        this.insertedRowsRef.set(0L)
        this.errors.clear()
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
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
