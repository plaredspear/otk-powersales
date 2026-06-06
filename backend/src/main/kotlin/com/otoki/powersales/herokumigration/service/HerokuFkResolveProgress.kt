package com.otoki.powersales.herokumigration.service

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Heroku Stage 2 FK Resolve 진행 상태 in-memory 추적 (1회성 cut-over 도구).
 *
 * SF [com.otoki.powersales.sfmigration.service.SfFkResolveProgress] 패턴. 단일 backend 인스턴스
 * 가정 — 재시작 시 이력 손실되나 [HerokuFkResolveService] 의 INFO 로그가 CloudWatch 에 보관.
 * 동시 실행 1회 가정 (controller 가 RUNNING 시 거부).
 */
@Component
class HerokuFkResolveProgress {

    enum class Status { IDLE, RUNNING, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED }

    /** 테이블·컬럼별 채운 row 수. */
    data class TableResult(val table: String, val column: String, val rowsAffected: Long)

    /** 자연 키 매칭 실패 (FK NULL 유지) 집계. */
    data class Unmatched(val table: String, val column: String, val naturalKey: String, val unmatchedCount: Long)

    @Volatile var status: Status = Status.IDLE
        internal set

    @Volatile var startedAt: Instant? = null
        internal set

    @Volatile var finishedAt: Instant? = null
        internal set

    @Volatile var totalTables: Int = 0
        internal set

    private val completedTables = AtomicInteger(0)

    @Volatile var currentTable: String? = null
        internal set

    private val totalRowsAffectedRef = AtomicLong(0L)

    val tableResults: ConcurrentLinkedQueue<TableResult> = ConcurrentLinkedQueue()
    val unmatched: ConcurrentLinkedQueue<Unmatched> = ConcurrentLinkedQueue()
    val errors: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    val completedTablesCount: Int get() = completedTables.get()
    val totalRowsAffected: Long get() = totalRowsAffectedRef.get()

    fun begin(totalTables: Int) {
        this.totalTables = totalTables
        this.completedTables.set(0)
        this.currentTable = null
        this.tableResults.clear()
        this.unmatched.clear()
        this.errors.clear()
        this.totalRowsAffectedRef.set(0L)
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
    }

    fun beginTable(tableName: String) {
        this.currentTable = tableName
    }

    fun finishTable(result: TableResult) {
        tableResults += result
        totalRowsAffectedRef.addAndGet(result.rowsAffected)
        completedTables.incrementAndGet()
    }

    fun addUnmatched(u: Unmatched) {
        if (u.unmatchedCount > 0) unmatched += u
    }

    fun addError(message: String) {
        errors += message
    }

    fun finishOk() {
        this.finishedAt = Instant.now()
        this.currentTable = null
        // unmatched 가 있으면 status 만으로 누락을 놓치지 않도록 COMPLETED_WITH_WARNINGS.
        this.status =
            if (errors.isEmpty() && unmatched.isEmpty()) Status.COMPLETED else Status.COMPLETED_WITH_WARNINGS
    }

    fun finishWithFailure(message: String) {
        errors += message
        this.finishedAt = Instant.now()
        this.currentTable = null
        this.status = Status.FAILED
    }
}
