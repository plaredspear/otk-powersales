/*
package com.otoki.internal.repository

import com.otoki.internal.entity.DailySales
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*

/ **
 * 일매출 Repository
 * /
interface DailySalesRepository : JpaRepository<DailySales, Long> {

    / **
     * 특정 행사, 사원, 날짜, 상태로 일매출 존재 여부 확인
     * 중복 등록 방지를 위해 사용
     * /
    fun existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
        eventId: String,
        employeeId: String,
        salesDate: LocalDate,
        status: String
    ): Boolean

    / **
     * 특정 행사, 사원, 날짜, 상태로 일매출 조회
     * 임시저장 데이터 조회 및 업데이트를 위해 사용
     * /
    fun findByEventIdAndEmployeeIdAndSalesDateAndStatus(
        eventId: String,
        employeeId: String,
        salesDate: LocalDate,
        status: String
    ): Optional<DailySales>
}
*/
