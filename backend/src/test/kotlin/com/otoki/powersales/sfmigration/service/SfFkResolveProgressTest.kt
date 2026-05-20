package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.sfmigration.dto.SubstepResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SfFkResolveProgress 단위 테스트")
class SfFkResolveProgressTest {

    @Test
    @DisplayName("begin → beginTable → advanceChunk → finishTable → finishOk 라이프사이클")
    fun lifecycle() {
        val p = SfFkResolveProgress()

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
        val p = SfFkResolveProgress()
        p.begin(totalTables = 1)
        p.addError("scan 실패")
        p.finishWithFailure("DB 연결 끊김")

        assertThat(p.status).isEqualTo(SfFkResolveProgress.Status.FAILED)
        assertThat(p.errors).containsExactly("scan 실패", "DB 연결 끊김")
        assertThat(p.finishedAt).isNotNull()
    }

    @Test
    @DisplayName("begin 재호출 시 이전 상태 초기화")
    fun reset() {
        val p = SfFkResolveProgress()
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
