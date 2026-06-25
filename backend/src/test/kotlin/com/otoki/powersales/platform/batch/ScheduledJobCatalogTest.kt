package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.batch.AccountNaverGeocodeBatch
import com.otoki.powersales.platform.batch.AgreementWordCycleBatch
import com.otoki.powersales.platform.batch.ClaimMasterSyncBatch
import com.otoki.powersales.platform.batch.LogisticsClaimMasterSyncBatch
import com.otoki.powersales.platform.batch.TeamMemberScheduleSapOutboundBatch
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
import com.otoki.powersales.platform.batch.ProductExpirationAlertBatch
import com.otoki.powersales.platform.batch.SalesProgressRateMasterSyncBatch
import com.otoki.powersales.platform.batch.StaffReviewSyncBatch
import com.otoki.powersales.platform.batch.SapOutboxBatch
import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import com.otoki.powersales.platform.batch.ScheduledJobRunCleanupBatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.scheduling.support.CronExpression

@DisplayName("ScheduledJobCatalog 테스트")
class ScheduledJobCatalogTest {

    @Test
    @DisplayName("카탈로그의 21개 jobName 이 각 *Batch.JOB_NAME 상수와 1:1 일치한다")
    fun jobNames_alignWithBatchConstants() {
        val expected = setOf(
            AgreementWordCycleBatch.JOB_NAME,
            TeamMemberScheduleSapOutboundBatch.JOB_NAME,
            DisplayMasterSapOutboundBatch.JOB_NAME,
            PPTMasterSapOutboundBatch.JOB_NAME,
            DisplayMasterLastMonthRevenueBatch.JOB_NAME,
            MfeisThisMonthRevenueBatch.JOB_NAME,
            AccountNaverGeocodeBatch.JOB_NAME,
            PPTMasterExpireBatch.JOB_NAME,
            PPTMasterSyncBatch.JOB_NAME,
            PostponedAppointmentBatch.JOB_NAME,
            SalesProgressRateMasterSyncBatch.JOB_NAME,
            ClaimMasterSyncBatch.JOB_NAME,
            LogisticsClaimMasterSyncBatch.JOB_NAME,
            StaffReviewSyncBatch.JOB_NAME,
            SapOutboxBatch.JOB_NAME,
            ScheduledJobRunCleanupBatch.JOB_NAME,
            OroraDailySalesMaterializeBatch.JOB_NAME,
            OroraMonthlySalesMaterializeBatch.JOB_NAME,
            ErpOrderRetentionBatch.JOB_NAME,
            JMartCoordinateBatch.JOB_NAME,
            ProductExpirationAlertBatch.JOB_NAME,
        )

        assertThat(ScheduledJobCatalog.JOB_NAMES.toSet()).isEqualTo(expected)
        assertThat(ScheduledJobCatalog.ENTRIES).hasSize(21)
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

    @Test
    @DisplayName("각 항목의 beanType 이 가리키는 배치 클래스의 JOB_NAME 상수가 entry.jobName 과 일치한다")
    fun beanType_jobNameConstant_matchesEntryJobName() {
        ScheduledJobCatalog.ENTRIES.forEach { entry ->
            // 활성 여부 판정의 정확성은 beanType ↔ jobName 정합에 달려 있으므로,
            // 각 배치 클래스의 companion const JOB_NAME 을 리플렉션으로 읽어 entry.jobName 과 대조한다.
            val constant = entry.beanType.getDeclaredField("JOB_NAME").apply { isAccessible = true }.get(null)
            assertThat(constant)
                .describedAs("beanType=${entry.beanType.simpleName} 의 JOB_NAME 이 entry.jobName=${entry.jobName} 과 일치해야 한다")
                .isEqualTo(entry.jobName)
        }
    }

    @Test
    @DisplayName("전문행사조 배치 cron 이 SF 운영 CronTrigger 와 정합한다 (변경=매시간 44분, 마감=23:30)")
    fun pptMasterCrons_alignWithLegacyOperationalSchedule() {
        val byName = ScheduledJobCatalog.ENTRIES.associateBy { it.jobName }

        // legacy SF "금일 전문행사조 변경" = 0 44 * * * ? (매시간 44분)
        val syncCron = byName.getValue(PPTMasterSyncBatch.JOB_NAME).cron
        assertThat(syncCron).isEqualTo("0 44 * * * *")
        assertThat(CronExpression.isValidExpression(syncCron)).isTrue()

        // legacy SF "금일 전문행사조 마감" = 0 30 23 ? * 1~7 (매일 23:30)
        val expireCron = byName.getValue(PPTMasterExpireBatch.JOB_NAME).cron
        assertThat(expireCron).isEqualTo("0 30 23 * * *")
        assertThat(CronExpression.isValidExpression(expireCron)).isTrue()
    }

    @Test
    @DisplayName("ORORA 매출 적재 배치 cron 이 SF 운영 CronTrigger 와 정합한다 (일별=매일 11시, 월별=첫째 주 목요일 11시)")
    fun ororaSalesCrons_alignWithLegacyOperationalSchedule() {
        val byName = ScheduledJobCatalog.ENTRIES.associateBy { it.jobName }

        // legacy SF "오로라 일별 데이터 수신" = 0 0 11 ? * 1,2,3,4,5,6,7 (매일 11:00 Asia/Seoul)
        val dailyCron = cronDefault(byName.getValue(OroraDailySalesMaterializeBatch.JOB_NAME).cron)
        assertThat(dailyCron).isEqualTo("0 0 11 * * *")
        assertThat(CronExpression.isValidExpression(dailyCron)).isTrue()

        // legacy SF "오로라 월별 매출 이력 수신" = 0 0 11 ? * 5#1 (매월 첫째 주 목요일 11:00 Asia/Seoul)
        val monthlyCron = cronDefault(byName.getValue(OroraMonthlySalesMaterializeBatch.JOB_NAME).cron)
        assertThat(monthlyCron).isEqualTo("0 0 11 ? * THU#1")
        assertThat(CronExpression.isValidExpression(monthlyCron)).isTrue()
    }

    /** `${app.batch.x.cron:DEFAULT}` placeholder 표기에서 운영 기본 cron(DEFAULT) 만 추출. */
    private fun cronDefault(raw: String): String =
        if (raw.startsWith("\${")) raw.substringAfter(':').removeSuffix("}") else raw
}
