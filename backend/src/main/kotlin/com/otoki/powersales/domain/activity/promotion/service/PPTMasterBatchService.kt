package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.collections.get

@Service
class PPTMasterBatchService(
    private val pptMasterRepository: PPTMasterRepository,
    private val employeeRepository: EmployeeRepository,
    private val adminPPTMasterService: AdminPPTMasterService,
) {

    /**
     * @param triggeredBy 실행 출처. 자동 스케줄 실행은 `null`(이력 metadata 에 표기 안 함),
     *   운영자 수동 실행은 `"MANUAL"` 을 넘겨 이력 metadata 에 함께 기록한다.
     */
    @Transactional
    fun syncValidMasters(context: ScheduledJobRunContext? = null, triggeredBy: String? = null) {
        val today = LocalDate.now()
        val validMasters = pptMasterRepository.findValidMasters(today)

        val employeeIds = validMasters.mapNotNull { it.employeeId }.distinct()
        val employeeMap = employeeRepository.findAllById(employeeIds).associateBy { it.id }

        var updated = 0
        for (master in validMasters) {
            val employee = employeeMap[master.employeeId] ?: continue
            if (employee.professionalPromotionTeam != master.teamType) {
                adminPPTMasterService.updateEmployeeTeam(employee, master.teamType)
                updated++
            }
        }
        context?.metadata(
            buildMetadata(triggeredBy, "scanned" to validMasters.size, "updated" to updated),
        )
    }

    /**
     * @param triggeredBy 실행 출처. 자동 스케줄 실행은 `null`, 운영자 수동 실행은 `"MANUAL"`.
     */
    @Transactional
    fun expireMasters(context: ScheduledJobRunContext? = null, triggeredBy: String? = null) {
        val today = LocalDate.now()
        val expiringMasters = pptMasterRepository.findExpiringMasters(today)

        val employeeIds = expiringMasters.mapNotNull { it.employeeId }.distinct()

        // legacy `Batch_PPTMaster2` 동등: 오늘 종료되는 마스터의 사원은 잔여 유효 마스터 유무와 무관하게
        // 무조건 행사조 소속을 해제한다 (레거시는 사원 팀을 '일반' 으로 덮어씀 — 신규 모델에선 미배정=null).
        // 다음 날 sync 배치(pptMaster.syncValid)가 잔여 유효 마스터 기준으로 팀을 재정합한다.
        var reverted = 0
        for (employeeId in employeeIds) {
            val employee = employeeRepository.findById(employeeId).orElse(null) ?: continue
            if (employee.professionalPromotionTeam != null) {
                adminPPTMasterService.updateEmployeeTeam(employee, null)
                reverted++
            }
        }
        context?.metadata(
            buildMetadata(triggeredBy, "expiringEmployees" to employeeIds.size, "reverted" to reverted),
        )
    }

    /** 통계 metadata 에 (수동 실행 시) `triggeredBy` 를 합성한다. */
    private fun buildMetadata(triggeredBy: String?, vararg stats: Pair<String, Any?>): Map<String, Any?> {
        val base = linkedMapOf(*stats)
        if (triggeredBy != null) base["triggeredBy"] = triggeredBy
        return base
    }
}
