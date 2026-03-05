package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.AgreementWord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AgreementWordRepository : JpaRepository<AgreementWord, Int> {

    fun findFirstByActiveTrueAndIsDeletedFalse(): Optional<AgreementWord>

    fun findByNameAndIsDeletedFalse(name: String): Optional<AgreementWord>
}
