package com.otoki.internal.repository

import com.otoki.internal.entity.Account
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처 마스터 Repository
 */
interface AccountRepository : JpaRepository<Account, Long> {

    /**
     * 거래처 외부키(SAP 코드)로 조회
     */
    fun findByExternalKey(externalKey: String): Account?

    /**
     * 거래처 ID 목록으로 일괄 조회
     */
    fun findByIdIn(ids: List<Long>): List<Account>
}
