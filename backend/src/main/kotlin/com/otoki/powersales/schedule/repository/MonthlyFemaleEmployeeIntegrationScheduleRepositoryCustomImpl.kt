package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory

class MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    override fun findAllAccessibleByPolicy(policyPredicate: Predicate): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        return queryFactory
            .selectFrom(monthlyFemaleEmployeeIntegrationSchedule)
            .where(policyPredicate)
            .fetch()
    }
}
