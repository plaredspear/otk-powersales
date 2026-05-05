package com.otoki.powersales.schedule.sap

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 진열 마스터(DISPLAY) daily batch 의 SOQL 등가 페이지 조회 Repository (Spec #588 P2-B §1.1).
 *
 * 레거시 `Batch_TeamMemberMasterSchedule.cls:27-28`:
 * ```
 * WHERE ValidData='유효' AND Confirmed=true
 *   AND StartDate<=today AND (EndDate>=today OR EndDate IS NULL)
 * ```
 *
 * 신규 매핑:
 * - `valid_data='유효'` 컬럼은 신규 엔티티에 없음 → `is_deleted IS DISTINCT FROM TRUE` 로 대체
 * - `confirmed = true` 그대로
 * - `start_date <= today AND (end_date >= today OR end_date IS NULL)` 그대로
 * - `employee_number` → `employee.employee_code` (JOIN)
 * - `account_external_key` → `account.external_key` (JOIN)
 */
@Repository
class DisplayMasterRepository(
    @PersistenceContext private val em: EntityManager
) {

    /**
     * 오늘 유효한 진열 마스터 row 를 페이지 단위로 조회한다.
     */
    fun findValidDisplayMasters(
        today: LocalDate,
        limit: Int,
        offset: Int
    ): List<DisplayMasterSapPayloadRow> {
        val sql = """
            SELECT dws.display_work_schedule_id,
                   e.employee_code,
                   a.external_key,
                   dws.type_of_work1,
                   dws.type_of_work3,
                   dws.type_of_work5
              FROM powersales.display_work_schedule dws
              LEFT JOIN powersales.employee e ON e.employee_id = dws.employee_id
              LEFT JOIN powersales.account  a ON a.account_id  = dws.account_id
             WHERE (dws.is_deleted IS DISTINCT FROM TRUE)
               AND dws.confirmed = TRUE
               AND dws.start_date <= ?1
               AND (dws.end_date >= ?1 OR dws.end_date IS NULL)
             ORDER BY dws.display_work_schedule_id
             LIMIT ?2 OFFSET ?3
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(sql)
            .setParameter(1, today)
            .setParameter(2, limit)
            .setParameter(3, offset)
            .resultList as List<Array<Any?>>

        return rows.map { row ->
            DisplayMasterSapPayloadRow(
                displayWorkScheduleId = (row[0] as Number).toLong(),
                employeeCode = row[1] as String?,
                accountExternalKey = row[2] as String?,
                typeOfWork1 = row[3] as String?,
                typeOfWork3 = row[4] as String?,
                typeOfWork5 = row[5] as String?
            )
        }
    }
}
