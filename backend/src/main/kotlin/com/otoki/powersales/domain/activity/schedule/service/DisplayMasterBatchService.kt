package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterSapPayloadRow
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.external.sap.outbound.sender.DisplayMasterSapSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 진열 마스터 SAP전송 배치 (SD03131) 의 일배치 실행 서비스.
 *
 * 레거시 `Batch_TeamMemberMasterSchedule.cls` (interface `IF_REST_SAP_TeamMemberMasterSchedule`) 동등.
 * 여사원일정 배치([TeamMemberScheduleSapBatchService], SD03130) 와 **별개** — 대상 객체 / 페이로드 키가 다름.
 *
 * **실행 시각 정합 (검토: 2026-06-18)**
 * - 레거시 운영 cron = `0 0 23 ? * 1-7` (매일 23:00 KST, CronTrigger "여사원 진열마스터 스케쥴", TZ=Asia/Seoul).
 *   (레거시 테스트 코드 `Batch_TEST.cls` 의 `0 0 23` 도 우연히 운영값과 일치 — 단 신뢰 근거는 운영 CronTrigger.)
 * - 신규 cron = 매일 23:00 KST (JVM TZ=Asia/Seoul 고정) — 레거시 운영값에 맞춤.
 * - WorkDate(= 실행일 today)를 레거시와 같은 달력일로 송신하기 위함. 01시 실행 시 자정을 넘겨 +1일 라벨이 됨.
 *
 * **날짜 조회 조건 정합 (검토: 2026-06-18)**
 * - 레거시 WHERE `ValidData__c='유효' AND Confirmed__c=true AND StartDate__c<=오늘 AND (EndDate__c>=오늘 OR null)`.
 * - 신규 [DisplayWorkScheduleRepository.findValidForDisplayMasterSapPaged] 가 동일 4조건 재현
 *   (`confirmed=true` + `startDate<=date` + `endDate>=date or null` + `validDataEqualsValid` 풀이 절).
 * - 레거시는 날짜를 `{year}-{month}-{day}` 문자열로 직접 조립(SOQL TODAY 미사용)하나, 신규 [LocalDate.now]
 *   단일 today 와 의미 동일. 여사원일정과 달리 어제(Yesterday) 분기가 없어 today/yesterday 이중 윈도우 부재.
 * - 페이로드 키 = `CompanyCode`(상수 1000) / `EmployeeCode` / `SAPAccountCode` / `WorkDate`(실행일 yyyyMMdd)
 *   / `WorkingCategory1·3·5` — 레거시와 동일(2·4 없음). 상세는 [DisplayMasterPayloadFactory] 참조.
 */
@Service
class DisplayMasterBatchService(
    private val repository: DisplayWorkScheduleRepository,
    private val payloadFactory: DisplayMasterPayloadFactory,
    private val sender: DisplayMasterSapSender,
    @Value("\${app.sap.outbound.display.page-size:100}") private val pageSize: Int,
) {

    private val log = LoggerFactory.getLogger(DisplayMasterBatchService::class.java)

    fun runDaily(context: ScheduledJobRunContext? = null) {
        runDaily(LocalDate.now(), context)
    }

    internal fun runDaily(today: LocalDate, context: ScheduledJobRunContext? = null) {
        var pageIndex = 0
        var totalRows = 0
        var sentPages = 0
        var failedPages = 0

        while (true) {
            val entities = repository.findValidForDisplayMasterSapPaged(
                date = today,
                limit = pageSize,
                offset = pageIndex * pageSize
            )
            if (entities.isEmpty()) break

            val rows = entities.map { it.toSapPayloadRow() }
            totalRows += rows.size
            val payload = payloadFactory.build(rows, today)
            val ok = sender.sendPage(payload)
            if (ok) sentPages++ else failedPages++

            if (entities.size < pageSize) break
            pageIndex++
        }

        val totalPages = sentPages + failedPages
        log.info(
            "DISPLAY_SAP_BATCH today={} totalRows={} pages={} sent={} failed={}",
            today, totalRows, totalPages, sentPages, failedPages
        )
        context?.metadata(
            mapOf(
                "totalRows" to totalRows,
                "totalPages" to totalPages,
                "sentPages" to sentPages,
                "failedPages" to failedPages
            )
        )
    }

    private fun DisplayWorkSchedule.toSapPayloadRow(): DisplayMasterSapPayloadRow =
        DisplayMasterSapPayloadRow(
            displayWorkScheduleId = id,
            employeeCode = employee?.employeeCode,
            accountExternalKey = account?.externalKey,
            typeOfWork1 = typeOfWork1?.displayName,
            typeOfWork3 = typeOfWork3?.displayName,
            typeOfWork5 = typeOfWork5?.displayName
        )
}
