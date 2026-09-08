package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmployeeInputCriteriaMasterRepository :
    JpaRepository<EmployeeInputCriteriaMaster, Long>,
    EmployeeInputCriteriaMasterRepositoryCustom {

    /**
     * 근무유형1 + 확정 + 미삭제 투입기준마스터.
     *
     * 미삭제 조건은 `is_deleted IS NULL OR = false` — `IsDeletedNot(true)` 파생 쿼리는
     * NULL 행을 통째로 탈락시킨다 ([com.otoki.powersales.domain.foundation.account.repository.AccountRepository] KDoc 참조).
     */
    @Query(
        """
        SELECT m FROM EmployeeInputCriteriaMaster m
        WHERE m.typeOfWork1 = :typeOfWork1
          AND m.confirmed = true
          AND (m.isDeleted IS NULL OR m.isDeleted = false)
        """
    )
    fun findByTypeOfWork1AndConfirmedTrueAndNotDeleted(
        @Param("typeOfWork1") typeOfWork1: TypeOfWork1,
    ): List<EmployeeInputCriteriaMaster>
}
