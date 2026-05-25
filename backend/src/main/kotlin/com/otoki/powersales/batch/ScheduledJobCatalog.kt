package com.otoki.powersales.batch

/**
 * 운영 환경에 등록된 `@Scheduled` 잡의 정적 카탈로그.
 *
 * 각 항목의 `jobName` 은 해당 배치 클래스의 `companion object { const val JOB_NAME }` 상수를
 * 그대로 참조하여 `scheduled_job_run.job_name` 컬럼과 1:1 정합을 보장한다.
 *
 * cron 표현식은 `@Scheduled(cron = ...)` 원문을 그대로 표기한다.
 * placeholder (`${...:default}`) 가 포함된 경우 default 값이 운영 기본 schedule 이지만,
 * application.yml override 가능성을 운영자가 인지할 수 있도록 원문 그대로 노출한다.
 */
object ScheduledJobCatalog {

    data class Entry(
        val jobName: String,
        val cron: String,
        val description: String,
    )

    val ENTRIES: List<Entry> = listOf(
        Entry(
            jobName = AgreementWordCycleBatch.JOB_NAME,
            cron = "0 0 9 * * *",
            description = "동의 약관 단어 주기 리셋 (매일 09시)",
        ),
        Entry(
            jobName = AttendanceSapOutboundBatch.JOB_NAME,
            cron = "\${app.sap.outbound.attendance.cron:0 0 1 * * *}",
            description = "근태 SAP outbound 전송 (기본 매일 01시)",
        ),
        Entry(
            jobName = DisplayMasterSapOutboundBatch.JOB_NAME,
            cron = "\${app.sap.outbound.display.cron:0 0 1 * * *}",
            description = "진열마스터 SAP outbound 전송 (기본 매일 01시)",
        ),
        Entry(
            jobName = DisplayMasterLastMonthRevenueBatch.JOB_NAME,
            cron = "\${app.batch.display.last-month-revenue.cron:0 0 2 * * *}",
            description = "진열마스터 전월 매출 일괄 갱신 (기본 매일 02시) — legacy UpdateLastMonthRevenueBatch 동등",
        ),
        Entry(
            jobName = AccountNaverGeocodeBatch.JOB_NAME,
            cron = AccountNaverGeocodeBatch.CRON,
            description = "거래처 주소 Naver geocode 변환 (매일 02시)",
        ),
        Entry(
            jobName = PPTMasterExpireBatch.JOB_NAME,
            cron = "0 0 23 * * *",
            description = "전문행사조 마스터 만료 처리 (매일 23시)",
        ),
        Entry(
            jobName = PPTMasterSyncBatch.JOB_NAME,
            cron = "0 0 5 * * *",
            description = "전문행사조 마스터 유효 sync (매일 05시)",
        ),
        Entry(
            jobName = PostponedAppointmentBatch.JOB_NAME,
            cron = "0 0 0 * * *",
            description = "연기된 SAP 예약 일괄 처리 (매일 자정)",
        ),
        Entry(
            jobName = SapOutboxBatch.JOB_NAME,
            cron = "\${app.sap.outbox.cron:*/30 * * * * *}",
            description = "SAP outbox 메시지 worker (기본 30초 주기)",
        ),
        Entry(
            jobName = ScheduledJobRunCleanupBatch.JOB_NAME,
            cron = "0 0 4 * * *",
            description = "scheduled_job_run 90일 초과 이력 정리 (매일 04시)",
        ),
    )

    val JOB_NAMES: List<String> = ENTRIES.map { it.jobName }
}
