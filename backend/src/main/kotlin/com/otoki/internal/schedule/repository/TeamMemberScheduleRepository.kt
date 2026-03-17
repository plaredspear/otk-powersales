package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

/**
 * 일정 Repository
 */
interface TeamMemberScheduleRepository : JpaRepository<TeamMemberSchedule, Long>, TeamMemberScheduleRepositoryCustom {

    /**
     * 사번 employeeId 와 근무 날짜로 일정 조회
     */
    fun findByEmployeeIdAndWorkingDate(employeeId: String, workingDate: LocalDate): List<TeamMemberSchedule>

    /**
     * 복수 사원 sfid와 근무 날짜로 일정 일괄 조회 (조장 팀 전체 조회용)
     */
    fun findByWorkingDateAndEmployeeIdIn(workingDate: LocalDate, employeeIds: List<String>): List<TeamMemberSchedule>

    /**
     * ID 목록으로 스케줄 일괄 삭제 (행사 연쇄 삭제용)
     */
    fun deleteAllByIdIn(ids: List<Long>)

    /**
     * promotionEmpIdExt 목록으로 스케줄 일괄 조회 (행사 확정 Upsert용)
     */
    fun findByPromotionEmpIdExtIn(promotionEmpIdExts: List<String>): List<TeamMemberSchedule>

    /**
     * 복수 사원 + 복수 날짜 스케줄 일괄 조회 (행사 확정 검증용)
     */
    fun findByEmployeeIdInAndWorkingDateIn(employeeIds: List<String>, workingDates: List<LocalDate>): List<TeamMemberSchedule>

    /**
     * 사원 ID 목록 + 기간 범위로 월간 일정 조회 (여사원 일정관리)
     */
    @Query("SELECT s FROM TeamMemberSchedule s WHERE s.employeeId IN :employeeIds AND s.workingDate BETWEEN :from AND :to AND (s.isDeleted IS NULL OR s.isDeleted = false)")
    fun findMonthlyByEmployeeIds(
        @Param("employeeIds") employeeIds: List<String>,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): List<TeamMemberSchedule>

    /**
     * 거래처 ID 목록 + 기간 범위로 월간 일정 조회 (여사원 일정관리)
     */
    @Query("SELECT s FROM TeamMemberSchedule s WHERE s.accountId IN :accountIds AND s.workingDate BETWEEN :from AND :to AND (s.isDeleted IS NULL OR s.isDeleted = false)")
    fun findMonthlyByAccountIds(
        @Param("accountIds") accountIds: List<String>,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): List<TeamMemberSchedule>

    /**
     * 사원 + 날짜로 기존 일정 조회 (중복 검증용, 삭제 제외)
     */
    @Query("SELECT s FROM TeamMemberSchedule s WHERE s.employeeId = :employeeId AND s.workingDate = :workingDate AND (s.isDeleted IS NULL OR s.isDeleted = false)")
    fun findActiveByEmployeeIdAndDate(
        @Param("employeeId") employeeId: String,
        @Param("workingDate") workingDate: LocalDate
    ): List<TeamMemberSchedule>
}
