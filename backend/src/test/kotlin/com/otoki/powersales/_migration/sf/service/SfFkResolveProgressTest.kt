package com.otoki.powersales._migration.sf.service

import com.otoki.powersales._migration.common.MigrationProgressStore
import com.otoki.powersales._migration.sf.dto.SubstepResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SfFkResolveProgress 단위 테스트")
class SfFkResolveProgressTest {

    @Test
    @DisplayName("begin → beginTable → advanceChunk → finishTable → finishOk 라이프사이클")
    fun lifecycle() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())

        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.IDLE)

        p.begin(totalTables = 2)
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.RUNNING)
        assertThat(p.startedAt).isNotNull()
        assertThat(p.totalTables).isEqualTo(2)
        assertThat(p.completedTablesCount).isZero()

        p.beginTable("erp_order_product", totalChunks = 3)
        assertThat(p.currentTable).isEqualTo("erp_order_product")
        assertThat(p.currentTableTotalChunks).isEqualTo(3)
        assertThat(p.currentTableChunk).isZero()

        p.advanceChunk(100)
        p.advanceChunk(200)
        p.advanceChunk(0)
        assertThat(p.currentTableChunk).isEqualTo(3)
        assertThat(p.totalRowsAffected).isEqualTo(300L)

        p.finishTable(SubstepResult("erp_order_product (3 FK)", 300))
        assertThat(p.completedTablesCount).isEqualTo(1)
        assertThat(p.tableResults).hasSize(1)

        p.beginTable("user", totalChunks = 1)
        p.advanceChunk(50)
        p.finishTable(SubstepResult("user (2 FK)", 50))

        p.finishOk()
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.COMPLETED)
        assertThat(p.finishedAt).isNotNull()
        assertThat(p.currentTable).isNull()
        assertThat(p.completedTablesCount).isEqualTo(2)
        assertThat(p.totalRowsAffected).isEqualTo(350L)
    }

    @Test
    @DisplayName("finishWithFailure 시 status FAILED + error 누적")
    fun failure() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())
        p.begin(totalTables = 1)
        p.addError("scan 실패")
        p.finishWithFailure("DB 연결 끊김")

        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.FAILED)
        assertThat(p.errors).containsExactly("scan 실패", "DB 연결 끊김")
        assertThat(p.finishedAt).isNotNull()
    }

    @Test
    @DisplayName("tryAcquire — 첫 호출만 true, RUNNING 중 재호출은 false (동시 실행 차단)")
    fun tryAcquireBlocksConcurrent() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())

        assertThat(p.tryAcquire()).isTrue()
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.RUNNING)

        // 진행 중 재선점은 거부.
        assertThat(p.tryAcquire()).isFalse()
        assertThat(p.tryAcquire()).isFalse()

        // 완료 후에는 다시 선점 가능.
        p.finishOk()
        assertThat(p.tryAcquire()).isTrue()
    }

    @Test
    @DisplayName("tryAcquire — 직전 실행의 결과/경고를 비워 stale 노출 방지")
    fun tryAcquireClearsStaleResult() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())
        p.begin(totalTables = 1)
        p.beginTable("erp_order_product", 1)
        p.advanceChunk(6300)
        p.finishTable(SubstepResult("erp_order_product (4 FK + polymorphic owner)", 6300))
        p.addError("[erp_order_product] erp_order_sfid → erp_order_id 미해소 6300 건")
        p.finishOk()

        assertThat(p.tableResults).isNotEmpty()
        assertThat(p.errors).isNotEmpty()

        // 새 실행 선점 즉시 직전 결과가 비워져야 한다 (202 응답이 옛 결과를 노출하지 않도록).
        assertThat(p.tryAcquire()).isTrue()
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.RUNNING)
        assertThat(p.tableResults).isEmpty()
        assertThat(p.errors).isEmpty()
        assertThat(p.totalRowsAffected).isZero()
        assertThat(p.completedTablesCount).isZero()
        assertThat(p.finishedAt).isNull()
    }

    @Test
    @DisplayName("releaseWithoutRun — 선점 락을 IDLE 로 되돌려 다음 선점 허용")
    fun releaseWithoutRunResetsLock() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())
        assertThat(p.tryAcquire()).isTrue()

        p.releaseWithoutRun()
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.IDLE)
        assertThat(p.startedAt).isNull()

        assertThat(p.tryAcquire()).isTrue()
    }

    @Test
    @DisplayName("begin 재호출 시 이전 상태 초기화")
    fun reset() {
        val p = SfFkResolveProgress(MigrationProgressStore.noop())
        p.begin(totalTables = 1)
        p.beginTable("a", 1)
        p.advanceChunk(100)
        p.finishTable(SubstepResult("a", 100))
        p.addError("warn")
        p.finishOk()

        p.begin(totalTables = 5)
        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.RUNNING)
        assertThat(p.completedTablesCount).isZero()
        assertThat(p.tableResults).isEmpty()
        assertThat(p.errors).isEmpty()
        assertThat(p.totalRowsAffected).isZero()
        assertThat(p.finishedAt).isNull()
    }
}
