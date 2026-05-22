package com.otoki.powersales.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.QDisplayWorkSchedule.Companion.displayWorkSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Schedule 도메인 (DisplayWorkSchedule + MFEIS) 의 SF Sharing Rule 정책 적용 Service (spec #782 P4-B).
 *
 * 두 SObject 모두 sharingModel = Private + CostCenterCode__c sharingRule + CreatedById 예외 6 rule 의
 * 구조 동일. 본 Service 가 [SharingRulePolicyEvaluator.buildPredicate] 결과 Predicate 를 각 Repository 에
 * 전달하여 가시 record 일람 반환.
 *
 * 기존 admin Service (`AdminMonthlyIntegrationService` 등) 와 별도 진입점 — backward compat.
 * 운영 안정화 시점에 기존 Service 본문에 통합 가능.
 */
@Service
@Transactional(readOnly = true)
class AdminScheduleSharingPolicyService(
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
) {

    /**
     * SF Sharing Rule 정책이 합성된 가시 DisplayWorkSchedule 일람.
     *
     * 정책 차원: CostCenterCode__c sharingRule (62) + CreatedById 예외 6 rule (특정 User 3명).
     */
    fun getAccessibleDisplayWorkSchedulesByPolicy(scope: DataScope): List<DisplayWorkSchedule> {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "DisplayWorkScheduleMaster__c",
            entityPath = displayWorkSchedule,
        )
        return displayWorkScheduleRepository.findAllAccessibleByPolicy(policyPredicate)
    }

    /**
     * SF Sharing Rule 정책이 합성된 가시 MFEIS 일람.
     *
     * DisplayWorkScheduleMaster__c 와 정책 구조 동일.
     */
    fun getAccessibleMfeisByPolicy(scope: DataScope): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "MonthlyFemaleEmployeeIntegrationSchedule__c",
            entityPath = monthlyFemaleEmployeeIntegrationSchedule,
        )
        return mfeisRepository.findAllAccessibleByPolicy(policyPredicate)
    }
}
