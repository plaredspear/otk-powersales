package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClaimRepository : JpaRepository<Claim, Long>, ClaimRepositoryCustom {

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

    /** SAP 인바운드 일괄 조회 (Spec #561) */
    fun findAllByNameIn(names: Collection<String>): List<Claim>
}
