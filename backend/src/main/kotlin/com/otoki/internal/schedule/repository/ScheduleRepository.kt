package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface ScheduleRepository : JpaRepository<Schedule, Long> {

    /**
     * 사원 sfid와 근무 날짜로 일정 조회
     */
    fun findByEmployeeIdAndWorkingDate(employeeId: String, workingDate: LocalDate): List<Schedule>

    /**
     * 복수 사원 sfid와 근무 날짜로 일정 일괄 조회 (조장 팀 전체 조회용)
     */
    fun findByWorkingDateAndEmployeeIdIn(workingDate: LocalDate, employeeIds: List<String>): List<Schedule>

    /**
     * sfid로 스케줄 단건 조회 (출근 등록용)
     */
    fun findBySfid(sfid: String): Schedule?

    /**
     * commuteLogId 업데이트 (출근 등록 시 'OK' 설정)
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.commuteLogId = :commuteLogId WHERE s.sfid = :sfid")
    fun updateCommuteLogId(sfid: String, commuteLogId: String)
}
