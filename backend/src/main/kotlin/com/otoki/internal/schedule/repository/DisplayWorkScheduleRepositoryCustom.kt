package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface DisplayWorkScheduleRepositoryCustom {

    fun findDistinctAccountIdsByFullNameAndStartDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Int>

    fun findDistinctStartDatesByFullNameAndDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate>

    fun findByFullNameInAndNotDeleted(fullNames: List<String>): List<DisplayWorkSchedule>

    fun findScheduleList(
        employeeCode: String?,
        accountIds: List<Int>?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        pageable: Pageable
    ): Page<DisplayWorkSchedule>
}
