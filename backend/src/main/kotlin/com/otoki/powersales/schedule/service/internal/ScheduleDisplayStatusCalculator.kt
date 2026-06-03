package com.otoki.powersales.schedule.service.internal

import com.otoki.powersales.employee.entity.Employee
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 진열사원 스케줄 마스터 화면의 SF formula 필드 (`ValidConditionData__c`(재직상태) /
 * `ValidData__c`(유효데이터) / `Valid__c`(유효)) 를 신규 시스템에서 조회 시점에 계산한다.
 *
 * SF 에서는 이들이 calculated formula 라 DB 컬럼이 없으며 레코드 조회 때마다 평가된다.
 * 신규 시스템도 동일하게 Employee(여사원) + Schedule(시작·종료일) 조인 값으로 그때그때 계산한다.
 *
 * 원본 formula:
 *   - `DisplayWorkScheduleMaster__c.ValidConditionData__c` (재직상태)
 *   - `DisplayWorkScheduleMaster__c.ValidData__c` (유효데이터)
 *   - `DisplayWorkScheduleMaster__c.Valid__c` (유효 — 신호등 이미지, ValidData 기반)
 * 의 frozen snapshot 정의를 1:1 포팅.
 */
@Component
class ScheduleDisplayStatusCalculator {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 재직상태 (`ValidConditionData__c`).
     *   - (status=퇴직 OR appLoginActive=false) AND empEndDate < TODAY → "퇴직{empEndDate}"
     *   - (status=퇴직 OR appLoginActive=false) AND empEndDate > TODAY → "퇴직예정{empEndDate}"
     *   - status=휴직 → "휴직"
     *   - 그 외 → "재직"
     */
    fun employmentStatus(employee: Employee?, today: LocalDate = LocalDate.now()): String? {
        if (employee == null) return null
        val status = employee.status
        val appLoginActive = employee.appLoginActive == true
        val empEndDate = employee.endDate
        val resigned = status == "퇴직" || !appLoginActive

        return when {
            resigned && empEndDate != null && empEndDate.isBefore(today) ->
                "퇴직" + empEndDate.format(dateFormat)
            resigned && empEndDate != null && empEndDate.isAfter(today) ->
                "퇴직예정" + empEndDate.format(dateFormat)
            status == "휴직" -> "휴직"
            else -> "재직"
        }
    }

    /**
     * 유효데이터 (`ValidData__c`) — "유효" / "예정" / "종료" 중 하나.
     *
     * @param scheduleStart 스케줄 시작일 (`StartDate__c`)
     * @param scheduleEnd   스케줄 종료일 (`EndDate__c`, nullable)
     */
    fun validData(
        employee: Employee?,
        scheduleStart: LocalDate?,
        scheduleEnd: LocalDate?,
        today: LocalDate = LocalDate.now(),
    ): String? {
        if (employee == null || scheduleStart == null) return null
        val status = employee.status
        val appLoginActive = employee.appLoginActive == true
        val empEndDate = employee.endDate
        val resigned = status == "퇴직" || !appLoginActive

        // 스케줄 기간이 현재 진행 중인지 (시작 <= TODAY AND (종료 없음 OR TODAY <= 종료))
        val periodActive = !scheduleStart.isAfter(today) &&
            (scheduleEnd == null || !today.isAfter(scheduleEnd))

        // "유효": (재직 + 기간진행) OR (퇴직/비활성 + 기간진행 + empEndDate >= TODAY)
        val validCase =
            (status == "재직" && periodActive) ||
                (resigned && periodActive && empEndDate != null && !empEndDate.isBefore(today))
        if (validCase) return "유효"

        // "예정": status != 퇴직 AND (시작 > TODAY OR (휴직 + 기간진행))
        val plannedCase = status != "퇴직" &&
            (scheduleStart.isAfter(today) || (status == "휴직" && periodActive))
        if (plannedCase) return "예정"

        // "종료": 종료일 < TODAY AND status in (퇴직/휴직/재직)
        if (scheduleEnd != null && scheduleEnd.isBefore(today) &&
            (status == "퇴직" || status == "휴직" || status == "재직")
        ) {
            return "종료"
        }

        // 잔여 분기: 퇴직/비활성 시나리오 — "예정" 또는 "종료"
        if (resigned) {
            val plannedTail = empEndDate != null && empEndDate.isAfter(today) && scheduleStart.isAfter(today)
            return if (plannedTail) "예정" else "종료"
        }

        return "종료"
    }

    /**
     * 유효 신호등 색상 (`Valid__c`) — ValidData 기반.
     *   - "유효" → GREEN
     *   - "예정" → YELLOW
     *   - "종료" → RED
     *   - 그 외 → null
     */
    fun validLight(validData: String?): ScheduleValidLight? = when (validData) {
        "유효" -> ScheduleValidLight.GREEN
        "예정" -> ScheduleValidLight.YELLOW
        "종료" -> ScheduleValidLight.RED
        else -> null
    }
}

enum class ScheduleValidLight {
    GREEN, YELLOW, RED
}
