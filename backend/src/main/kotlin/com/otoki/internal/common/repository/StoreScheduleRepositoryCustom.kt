package com.otoki.internal.common.repository

import java.time.LocalDate

interface StoreScheduleRepositoryCustom {

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
