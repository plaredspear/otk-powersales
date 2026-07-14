package com.otoki.powersales.admin.service

import com.otoki.powersales.platform.batch.AccountNaverGeocodeBatch
import com.otoki.powersales.platform.batch.ClaimMasterSyncBatch
import com.otoki.powersales.platform.batch.DisplayMasterLastMonthRevenueBatch
import com.otoki.powersales.platform.batch.DisplayMasterSapOutboundBatch
import com.otoki.powersales.platform.batch.MfeisThisMonthRevenueBatch
import com.otoki.powersales.platform.batch.OroraDailySalesMaterializeBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.platform.batch.PPTMasterExpireBatch
import com.otoki.powersales.platform.batch.PPTMasterSapOutboundBatch
import com.otoki.powersales.platform.batch.PPTMasterSyncBatch
import com.otoki.powersales.platform.batch.PostponedAppointmentBatch
import com.otoki.powersales.platform.batch.SalesProgressRateMasterSyncBatch
import com.otoki.powersales.platform.batch.TeamMemberScheduleSapOutboundBatch

/**
 * 대시보드 "일별 스케줄 실행현황" 위젯이 노출하는 대상 스케줄 목록 (표시 순서 고정).
 *
 * 각 항목의 `jobName` 은 배치 클래스의 `JOB_NAME` 상수를 참조해 `scheduled_job_run.job_name` 과
 * 1:1 정합을 보장한다. `expectedCount`(예상 발화 횟수) 는 [ScheduledJobCronResolver] 가 런타임에
 * 해석한 실제 cron 으로 계산하므로, 여기서는 표시 라벨/주기 텍스트/안내 문구만 정적으로 둔다.
 */
object DashboardScheduledJobTargets {

    /**
     * @property jobName `scheduled_job_run.job_name` 과 매칭되는 잡 이름
     * @property label 화면 표시용 한글 라벨
     * @property scheduleText 사람이 읽는 실행 주기 (참고 표기 — 실제 발화 횟수는 cron 파싱으로 산출)
     * @property note 추가 안내 문구 (없으면 null)
     */
    data class Target(
        val jobName: String,
        val label: String,
        val scheduleText: String,
        val note: String? = null,
    )

    val TARGETS: List<Target> = listOf(
        Target(TeamMemberScheduleSapOutboundBatch.JOB_NAME, "여사원일정 SAP전송", "매일 01시"),
        Target(DisplayMasterSapOutboundBatch.JOB_NAME, "진열마스터 SAP전송", "매일 23시"),
        Target(PPTMasterSapOutboundBatch.JOB_NAME, "PPT마스터 SAP전송", "매일 12시"),
        Target(DisplayMasterLastMonthRevenueBatch.JOB_NAME, "진열 전월매출", "매일 02시"),
        Target(MfeisThisMonthRevenueBatch.JOB_NAME, "일정 평균매출", "매월 1일 03시"),
        Target(AccountNaverGeocodeBatch.JOB_NAME, "거래처 좌표변환", "매일 02시"),
        Target(PPTMasterExpireBatch.JOB_NAME, "금일 전문행사조 마감", "매일 23:30"),
        Target(PPTMasterSyncBatch.JOB_NAME, "금일 전문행사조 반영", "매시간 44분"),
        Target(PostponedAppointmentBatch.JOB_NAME, "연기예약 처리", "매일 자정"),
        Target(SalesProgressRateMasterSyncBatch.JOB_NAME, "목표마스터 sync", "매시간 정각"),
        Target(ClaimMasterSyncBatch.JOB_NAME, "클레임 업데이트", "매시간 정각"),
        Target(OroraDailySalesMaterializeBatch.JOB_NAME, "ORORA 일매출", "매일 04:30"),
        Target(
            OroraMonthlySalesMaterializeBatch.JOB_NAME,
            "ORORA 월매출",
            "매월 3일 05시",
            note = "매월 3일에만 실행됩니다. 조회일이 매월 3일이 아니면 예상 실행 횟수는 0회입니다.",
        ),
    )

    val JOB_NAMES: List<String> = TARGETS.map { it.jobName }
}
