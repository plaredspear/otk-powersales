package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 영업일관리마스터 Repository (SF `WorkingDayMaster__c`).
 *
 * 월매출 현황 "기준 진도율" 산출용 영업일수 count 를 제공한다.
 */
interface WorkingDayMasterRepository :
    JpaRepository<WorkingDayMaster, Long>,
    WorkingDayMasterRepositoryCustom
