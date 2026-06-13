package com.otoki.powersales.domain.support.agreement.repository

import com.otoki.powersales.domain.support.agreement.entity.AgreementWord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AgreementWordRepository : JpaRepository<AgreementWord, Int>, AgreementWordRepositoryCustom {

    fun findFirstByActiveTrueAndIsDeletedFalse(): Optional<AgreementWord>

    fun findByNameAndIsDeletedFalse(name: String): Optional<AgreementWord>
}
