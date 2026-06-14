package com.otoki.powersales.domain.activity.schedule.dto.response

import java.math.BigDecimal

data class MonthlyIntegrationScheduleResponse(
    val year: Int,
    val month: Int,
    val items: List<MonthlyIntegrationScheduleItem>,
    val totalCount: Int
)

data class MonthlyIntegrationScheduleItem(
    val branchName: String,
    val accountBranchName: String?,
    val accountCode: String,
    val accountName: String,
    val employeeCode: String,
    val title: String?,
    val employeeName: String,
    val workingCategory1: String,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val totalInputCount: Int,
    val equivalentWorkingDays: BigDecimal,
    val convertedHeadcount: BigDecimal,
    val avgClosingAmount: Long
)

data class CategoryScheduleResponse(
    val year: Int,
    val month: Int,
    val items: List<CategoryScheduleItem>
)

data class CategoryScheduleItem(
    val branchName: String,
    val currentMonthTotal: BigDecimal,
    val previousMonthTotal: BigDecimal,
    val totalChange: BigDecimal,
    val displayFixed: BigDecimal,
    val displayAlternate: BigDecimal,
    val displayPatrol: BigDecimal,
    val currentMonthDisplayTotal: BigDecimal,
    val previousMonthDisplayTotal: BigDecimal,
    val displayChange: BigDecimal,
    val eventAmbient: BigDecimal,
    val eventFrozenChilled: BigDecimal,
    val currentMonthEventTotal: BigDecimal,
    val previousMonthEventTotal: BigDecimal,
    val eventChange: BigDecimal
)
