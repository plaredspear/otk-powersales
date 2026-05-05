package com.otoki.powersales.schedule.sap

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.sql.Date as SqlDate
import java.time.LocalDate

/**
 * 일반 출근(REGULAR) daily batch 의 SOQL 등가 페이지 조회 Repository (Spec #588 P1-B §1.1).
 *
 * 레거시 `Batch_TeamMemberSchedule.cls:24-30`:
 * ```
 * WHERE WorkingType='근무' AND WorkingDate=today
 *   + (CommuteLogId != null AND WorkingDate=Yesterday)
 * ```
 *
 * 신규 매핑:
 * - `attendance_type='REGULAR'` 필터 (spec #587 추가 컬럼)
 * - `working_type='근무'` 필터
 * - `working_date IN (today, yesterday)` 윈도우 (어제 보정 — 레거시 동등)
 * - `tms.commute_log_id` ↔ `al.sfid` 또는 `al.attendance_log_id` 문자열 표현
 *   (spec #587 P1-B §1.4 — sfid/id 어느 쪽이 채워지든 매칭)
 */
@Repository
class AttendanceSapBatchRepository(
    @PersistenceContext private val em: EntityManager
) {

    /**
     * 어제/오늘 일반 출근 row 를 페이지 단위로 조회한다.
     *
     * @param today 기준일 (배치 실행일)
     * @param yesterday 어제 보정 대상일 (보통 today.minusDays(1))
     * @param limit 페이지 사이즈 (기본 100)
     * @param offset 페이지 오프셋 (0, limit, 2*limit, ...)
     */
    fun findRegularAttendances(
        today: LocalDate,
        yesterday: LocalDate,
        limit: Int,
        offset: Int
    ): List<AttendanceSapPayloadRow> {
        val sql = """
            SELECT al.attendance_log_id,
                   tms.working_date,
                   e.employee_code,
                   a.external_key,
                   tms.working_category1,
                   tms.working_category2,
                   tms.working_category3,
                   al.second_work_type
              FROM powersales.attendance_log al
              JOIN powersales.team_member_schedule tms
                ON (tms.commute_log_id = al.sfid
                    OR tms.commute_log_id = CAST(al.attendance_log_id AS VARCHAR))
              JOIN powersales.employee e ON e.employee_id = al.employee_id
              JOIN powersales.account  a ON a.account_id  = al.account_id
             WHERE al.attendance_type = 'REGULAR'
               AND tms.working_type   = '근무'
               AND tms.working_date IN (?1, ?2)
             ORDER BY al.attendance_log_id
             LIMIT ?3 OFFSET ?4
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(sql)
            .setParameter(1, today)
            .setParameter(2, yesterday)
            .setParameter(3, limit)
            .setParameter(4, offset)
            .resultList as List<Array<Any?>>

        return rows.map { row ->
            AttendanceSapPayloadRow(
                attendanceLogId = (row[0] as Number).toLong(),
                workingDate = toLocalDate(row[1]),
                employeeCode = row[2] as String,
                accountExternalKey = row[3] as String?,
                workingCategory1 = row[4] as String?,
                workingCategory2 = row[5] as String?,
                workingCategory3 = row[6] as String?,
                secondWorkType = row[7] as String?
            )
        }
    }

    private fun toLocalDate(value: Any?): LocalDate = when (value) {
        is LocalDate -> value
        is SqlDate -> value.toLocalDate()
        else -> error("Unsupported date type: ${value?.javaClass}")
    }
}
