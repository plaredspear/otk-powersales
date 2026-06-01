package com.otoki.powersales.sales.repository

import com.otoki.powersales.sales.entity.SalesProgressRateMaster
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처목표등록마스터 Repository (SF `SalesProgressRateMaster__c`).
 *
 * SF → RDS 마이그레이션 적재 대상 entity 의 기본 JPA Repository.
 * 현재 소비처는 마이그레이션 적재(Stage1) 뿐이므로 기본 CRUD 만 제공한다.
 */
interface SalesProgressRateMasterRepository : JpaRepository<SalesProgressRateMaster, Long>
