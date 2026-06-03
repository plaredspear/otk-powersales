package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.SalesProgressRateMaster
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처목표등록마스터 Repository (SF `SalesProgressRateMaster__c`).
 *
 * SF → RDS 마이그레이션 적재 대상 entity 의 JPA Repository.
 * 마이그레이션 적재(Stage1) + admin 목록/상세 조회([SalesProgressRateMasterRepositoryCustom]) 소비.
 */
interface SalesProgressRateMasterRepository :
    JpaRepository<SalesProgressRateMaster, Long>,
    SalesProgressRateMasterRepositoryCustom
