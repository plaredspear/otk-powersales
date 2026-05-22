package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.core.types.Predicate

/**
 * MFEIS Custom Repository — SF Sharing Rule 정책 적용 (spec #782 P4-B).
 *
 * 정책 차원: CostCenterCode__c sharingRule (62) + CreatedById 예외 6 rule (특정 User 3명) —
 * DisplayWorkScheduleMaster__c 와 구조 동일.
 *
 * Service layer 가 SharingRulePolicyEvaluator.buildPredicate 결과를 [policyPredicate] 로 전달.
 */
interface MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    fun findAllAccessibleByPolicy(policyPredicate: Predicate): List<MonthlyFemaleEmployeeIntegrationSchedule>
}
