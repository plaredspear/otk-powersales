package com.otoki.powersales._migration.sf.service

import com.otoki.powersales._migration.sf.dto.SubstepResult
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Stage 2-A FK Resolve 진행 상태 in-memory 추적 (1회성 cut-over 도구).
 *
 * 단일 backend 인스턴스 가정. backend 재시작 시 이력 손실되나 [SfMigrationStage2FkService] 의
 * INFO 로그 (`[fk] ...`) 가 CloudWatch 에 영구 보관되어 감사 가능.
 *
 * 동시 실행 1회 가정 — runFkResolve 호출 전 [status] 가 RUNNING 이면 거부해야 한다 (controller 담당).
 */
@Component
class SfFkResolveProgress {

    enum class Status { IDLE, RUNNING, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED }

    @Volatile var status: Status = Status.IDLE
        internal set

    /**
     * 동시 실행 차단용 원자적 게이트. 요청 스레드가 비동기 작업을 submit 하기 **전에** 호출해 즉시
     * 실행 권한을 선점한다.
     *
     * 기존 가드 (`status == RUNNING` 체크 → submit) 는 워커 스레드가 [begin] 으로 RUNNING 을 세팅하기
     * 전까지 status 가 옛 값(COMPLETED 등)이라, 두 요청이 모두 가드를 통과하는 TOCTOU 구멍이 있었다.
     * 본 메서드로 요청 스레드에서 원자적으로 RUNNING 을 선점하면, 두 번째 요청은 반드시 false 를 받는다.
     *
     * 선점 성공 시 이전 실행의 결과(테이블 결과 / 경고)를 즉시 비워, submit 직후 반환하는 202 응답이
     * 옛 실행의 stale 진행 상태를 노출하지 않도록 한다. 실제 totalTables / 시작시각은 워커가 [begin]
     * 에서 확정한다.
     *
     * @return 선점 성공 시 true. 이미 RUNNING 이면 false (호출자는 CONFLICT 반환).
     */
    @Synchronized
    fun tryAcquire(): Boolean {
        if (status == Status.RUNNING) return false
        status = Status.RUNNING
        completedTables.set(0)
        currentTable = null
        currentTableTotalChunks = 0
        currentTableChunkNo.set(0)
        tableResults.clear()
        errors.clear()
        totalRowsAffectedRef.set(0L)
        totalTables = 0
        startedAt = Instant.now()
        finishedAt = null
        return true
    }

    /**
     * [tryAcquire] 로 선점한 락을, 비동기 작업을 끝내 submit 하지 못한 경우(스레드풀 거부 등) 되돌린다.
     */
    @Synchronized
    fun releaseWithoutRun() {
        status = Status.IDLE
        startedAt = null
    }

    @Volatile var startedAt: Instant? = null
        internal set

    @Volatile var finishedAt: Instant? = null
        internal set

    @Volatile var totalTables: Int = 0
        internal set

    private val completedTables = AtomicInteger(0)

    @Volatile var currentTable: String? = null
        internal set

    @Volatile var currentTableTotalChunks: Int = 0
        internal set

    private val currentTableChunkNo = AtomicInteger(0)

    val tableResults: ConcurrentLinkedQueue<SubstepResult> = ConcurrentLinkedQueue()
    val errors: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    private val totalRowsAffectedRef = AtomicReference(0L)

    val completedTablesCount: Int get() = completedTables.get()
    val currentTableChunk: Int get() = currentTableChunkNo.get()
    val totalRowsAffected: Long get() = totalRowsAffectedRef.get()

    fun begin(totalTables: Int) {
        this.totalTables = totalTables
        this.completedTables.set(0)
        this.currentTable = null
        this.currentTableTotalChunks = 0
        this.currentTableChunkNo.set(0)
        this.tableResults.clear()
        this.errors.clear()
        this.totalRowsAffectedRef.set(0L)
        this.startedAt = Instant.now()
        this.finishedAt = null
        this.status = Status.RUNNING
    }

    fun beginTable(tableName: String, totalChunks: Int) {
        this.currentTable = tableName
        this.currentTableTotalChunks = totalChunks
        this.currentTableChunkNo.set(0)
    }

    fun advanceChunk(rowsThisChunk: Int) {
        currentTableChunkNo.incrementAndGet()
        totalRowsAffectedRef.updateAndGet { it + rowsThisChunk }
    }

    fun finishTable(result: SubstepResult) {
        tableResults += result
        completedTables.incrementAndGet()
    }

    fun addError(message: String) {
        errors += message
    }

    fun finishOk() {
        this.finishedAt = Instant.now()
        this.currentTable = null
        // errors (FK 컬럼 매핑 실패 / chunk 실패 / dangling 잔존) 가 있으면 COMPLETED 가 아니라
        // COMPLETED_WITH_WARNINGS — status 만 보고 누락을 놓치지 않도록 (errors 별도 확인 유도).
        this.status = if (errors.isEmpty()) Status.COMPLETED else Status.COMPLETED_WITH_WARNINGS
    }

    fun finishWithFailure(message: String) {
        errors += message
        this.finishedAt = Instant.now()
        this.currentTable = null
        this.status = Status.FAILED
    }
}
