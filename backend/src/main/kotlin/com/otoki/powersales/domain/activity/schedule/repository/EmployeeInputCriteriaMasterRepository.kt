package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import org.springframework.data.jpa.repository.JpaRepository

interface EmployeeInputCriteriaMasterRepository :
    JpaRepository<EmployeeInputCriteriaMaster, Long>,
    EmployeeInputCriteriaMasterRepositoryCustom {

    fun findByTypeOfWork1AndConfirmedTrueAndIsDeletedNot(
        typeOfWork1: TypeOfWork1,
        isDeleted: Boolean
    ): List<EmployeeInputCriteriaMaster>
}
