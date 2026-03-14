package com.otoki.internal.schedule.repository

import java.time.LocalDate

interface DisplayWorkScheduleRepositoryCustom {

    fun findDistinctAccountsByFullNameAndStartDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<String>

    fun findDistinctStartDatesByFullNameAndDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate>
}
