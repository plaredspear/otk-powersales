/*
package com.otoki.internal.repository

import com.otoki.internal.entity.Attendance
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AttendanceRepository : JpaRepository<Attendance, Long> {

    / **
     * 사용자의 특정 날짜 출근등록 목록 조회
     * /
    fun findByUserIdAndAttendanceDate(userId: Long, attendanceDate: LocalDate): List<Attendance>

    / **
     * 사용자의 특정 날짜 출근등록 건수
     * /
    fun countByUserIdAndAttendanceDate(userId: Long, attendanceDate: LocalDate): Long

    / **
     * 사용자의 특정 거래처 + 특정 날짜 출근등록 존재 여부
     * /
    fun existsByUserIdAndStoreIdAndAttendanceDate(userId: Long, storeId: Long, attendanceDate: LocalDate): Boolean
}
*/
