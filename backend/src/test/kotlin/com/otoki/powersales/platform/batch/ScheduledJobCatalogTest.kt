package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.batch.AccountNaverGeocodeBatch
import com.otoki.powersales.platform.batch.AgreementWordCycleBatch
import com.otoki.powersales.platform.batch.AttendanceSapOutboundBatch
import com.otoki.powersales.platform.batch.DisplayMasterLastMonthRevenueBatch
import com.otoki.powersales.platform.batch.DisplayMasterSapOutboundBatch
import com.otoki.powersales.platform.batch.ErpOrderRetentionBatch
import com.otoki.powersales.platform.batch.JMartCoordinateBatch
import com.otoki.powersales.platform.batch.MfeisThisMonthRevenueBatch
import com.otoki.powersales.platform.batch.OroraDailySalesMaterializeBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.platform.batch.PPTMasterExpireBatch
import com.otoki.powersales.platform.batch.PPTMasterSapOutboundBatch
import com.otoki.powersales.platform.batch.PPTMasterSyncBatch
import com.otoki.powersales.platform.batch.PostponedAppointmentBatch
import com.otoki.powersales.platform.batch.SalesProgressRateMasterSyncBatch
import com.otoki.powersales.platform.batch.StaffReviewSyncBatch
import com.otoki.powersales.platform.batch.SapOutboxBatch
import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import com.otoki.powersales.platform.batch.ScheduledJobRunCleanupBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScheduledJobCatalog 테스트")
class ScheduledJobCatalogTest {

    @Test
    @DisplayName("카탈로그의 18개 jobName 이 각 *Batch.JOB_NAME 상수와 1:1 일치한다")
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
            StaffReviewSyncBatch.JOB_NAME,
            SapOutboxBatch.JOB_NAME,
            ScheduledJobRunCleanupBatch.JOB_NAME,
            OroraDailySalesMaterializeBatch.JOB_NAME,
            OroraMonthlySalesMaterializeBatch.JOB_NAME,
            ErpOrderRetentionBatch.JOB_NAME,
            JMartCoordinateBatch.JOB_NAME,
        )

        assertThat(ScheduledJobCatalog.JOB_NAMES.toSet()).isEqualTo(expected)
        assertThat(ScheduledJobCatalog.ENTRIES).hasSize(18)
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
