package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 월매출 이력 Repository
 *
 * SF `MonthlySalesHistory__c` → RDS 복제 적재 대상 entity 의 기본 JPA Repository.
 * 조회 소비처는 ORORA view (`OroraMonthlySalesHistory`) 로 일원화되어 있어 본 Repository 는
 * 적재 흐름의 기본 CRUD 만 제공한다.
 */
interface MonthlySalesHistoryRepository : JpaRepository<MonthlySalesHistory, Long>
