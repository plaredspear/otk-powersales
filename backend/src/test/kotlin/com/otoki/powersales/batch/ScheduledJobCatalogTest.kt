package com.otoki.powersales.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScheduledJobCatalog 테스트")
class ScheduledJobCatalogTest {

    @Test
    @DisplayName("카탈로그의 13개 jobName 이 각 *Batch.JOB_NAME 상수와 1:1 일치한다")
    fun jobNames_alignWithBatchConstants() {
        val expected = setOf(
            AgreementWordCycleBatch.JOB_NAME,
            AttendanceSapOutboundBatch.JOB_NAME,
            DisplayMasterSapOutboundBatch.JOB_NAME,
            PPTMasterSapOutboundBatch.JOB_NAME,
            DisplayMasterLastMonthRevenueBatch.JOB_NAME,
            MfeisThisMonthRevenueBatch.JOB_NAME,
            AccountNaverGeocodeBatch.JOB_NAME,
            PPTMasterExpireBatch.JOB_NAME,
            PPTMasterSyncBatch.JOB_NAME,
            PostponedAppointmentBatch.JOB_NAME,
            SalesProgressRateMasterSyncBatch.JOB_NAME,
            SapOutboxBatch.JOB_NAME,
            ScheduledJobRunCleanupBatch.JOB_NAME,
        )

        assertThat(ScheduledJobCatalog.JOB_NAMES.toSet()).isEqualTo(expected)
        assertThat(ScheduledJobCatalog.ENTRIES).hasSize(13)
        assertThat(ScheduledJobCatalog.ENTRIES.map { it.jobName }).doesNotHaveDuplicates()
    }

    @Test
    @DisplayName("모든 항목에 cron 표현식과 한글 설명이 채워져 있다")
    fun entries_haveCronAndDescription() {
        ScheduledJobCatalog.ENTRIES.forEach { entry ->
            assertThat(entry.cron).isNotBlank()
            assertThat(entry.description).isNotBlank()
        }
    }
}
