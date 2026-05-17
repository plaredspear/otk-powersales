package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface EmployeeInputCriteriaMasterRepository : JpaRepository<EmployeeInputCriteriaMaster, Long> {
    fun findByTypeOfWork1AndConfirmedTrueAndIsDeletedNot(
        typeOfWork1: TypeOfWork1,
        isDeleted: Boolean
    ): List<EmployeeInputCriteriaMaster>

    @Query(
        """
        SELECT e FROM EmployeeInputCriteriaMaster e
        LEFT JOIN FETCH e.category
        WHERE (e.isDeleted IS NULL OR e.isDeleted = false)
        ORDER BY e.startDate DESC, e.id DESC
        """
    )
    fun findAllNotDeleted(): List<EmployeeInputCriteriaMaster>

    /**
     * SF Trigger duplicateCheck() 정합 — 동일 거래처유형 + 동일 근무형태1 의 기간 겹침 존재 여부.
     * 종료일 null = 무기한.
     * `excludeId` 는 update 시 자기 자신 제외용 (생성 시 -1 같은 값 전달).
     */
    @Query(
        """
        SELECT COUNT(e) > 0 FROM EmployeeInputCriteriaMaster e
        WHERE (e.isDeleted IS NULL OR e.isDeleted = false)
          AND e.category.id = :categoryId
          AND ((:typeOfWork1 IS NULL AND e.typeOfWork1 IS NULL) OR e.typeOfWork1 = :typeOfWork1)
          AND e.id <> :excludeId
          AND (e.endDate IS NULL OR e.endDate >= :startDate)
          AND (:endDate IS NULL OR e.startDate <= :endDate)
        """
    )
    fun existsOverlapping(
        @Param("categoryId") categoryId: Long,
        @Param("typeOfWork1") typeOfWork1: TypeOfWork1?,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate?,
        @Param("excludeId") excludeId: Long,
    ): Boolean
}
