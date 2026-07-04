package com.otoki.powersales._migration.heroku.stage1

import com.otoki.powersales._migration.common.MigrationProgressStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * HerokuStage1CopyProgress.skipEntity — batch 모드에서 S3 에 CSV 가 없는(404) entity 건너뛰기 검증.
 *
 * cut-over 시 일부 테이블 CSV 가 아직 준비 안 된 경우, 404 는 batch 를 FAILED 로 중단시키지 않고
 * 해당 entity 만 SKIPPED 로 마크 후 다음 target 을 계속 적재해야 한다 (사용자 결정).
 *
 * DB/S3 의존 없는 순수 상태 단위 테스트.
 */
@DisplayName("HerokuStage1CopyProgress.skipEntity — 404 SKIPPED")
class HerokuStage1CopyProgressTest {

    @Test
    @DisplayName("skipEntity 는 해당 entity 를 SKIPPED 로 마크하고 사유를 errorMessage 에 기록")
    fun skipEntityMarksSkipped() {
        val progress = HerokuStage1CopyProgress(MigrationProgressStore.noop())
        progress.beginBatch("bucket", listOf("EducationCode", "LoginHistory", "ProductExpiration"))
        progress.beginEntity("LoginHistory", "heroku-migration/input/employee_his.csv")

        progress.skipEntity(
            targetName = "LoginHistory",
            s3Key = "heroku-migration/input/employee_his.csv",
            reason = "S3 에 CSV 없음 (404)",
        )

        val result = progress.entityResults.first { it.targetName == "LoginHistory" }
        assertThat(result.status).isEqualTo(HerokuStage1CopyProgress.EntityStatus.SKIPPED)
        assertThat(result.errorMessage).contains("404")
        assertThat(result.finishedAt).isNotNull
    }

    @Test
    @DisplayName("skipEntity 는 전체 status 를 FAILED 로 바꾸지 않는다 — batch 계속 진행")
    fun skipEntityDoesNotFailBatch() {
        val progress = HerokuStage1CopyProgress(MigrationProgressStore.noop())
        progress.beginBatch("bucket", listOf("EducationCode", "LoginHistory"))
        progress.beginEntity("LoginHistory", "k")
        progress.skipEntity("LoginHistory", "k", "404")

        // skip 직후에도 batch 는 여전히 RUNNING (다음 target 진행 가능).
        assertThat(progress.status).isEqualTo(HerokuStage1CopyProgress.Status.RUNNING)
        // 다른 entity 는 그대로 PENDING — markRemainingAsSkipped 와 달리 일괄 SKIP 안 함.
        val edu = progress.entityResults.first { it.targetName == "EducationCode" }
        assertThat(edu.status).isEqualTo(HerokuStage1CopyProgress.EntityStatus.PENDING)
    }

    @Test
    @DisplayName("skip 후 다른 entity 정상 완료 시 finishOk → COMPLETED (1개 SKIPPED 공존)")
    fun skipThenCompleteOthers() {
        val progress = HerokuStage1CopyProgress(MigrationProgressStore.noop())
        progress.beginBatch("bucket", listOf("EducationCode", "LoginHistory"))

        progress.beginEntity("EducationCode", "edu.csv")
        progress.finishEntityOk("EducationCode", processed = 10, filteredOut = 0, inserted = 10, unmatched = 0)

        progress.beginEntity("LoginHistory", "his.csv")
        progress.skipEntity("LoginHistory", "his.csv", "404")

        progress.finishOk()

        assertThat(progress.status).isEqualTo(HerokuStage1CopyProgress.Status.COMPLETED)
        val statuses = progress.entityResults.associate { it.targetName to it.status }
        assertThat(statuses["EducationCode"]).isEqualTo(HerokuStage1CopyProgress.EntityStatus.COMPLETED)
        assertThat(statuses["LoginHistory"]).isEqualTo(HerokuStage1CopyProgress.EntityStatus.SKIPPED)
    }
}
