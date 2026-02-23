package com.otoki.internal.repository

import com.otoki.internal.entity.AgreementWord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AgreementWordRepository : JpaRepository<AgreementWord, Int> {

    fun findFirstByActiveTrueAndIsDeletedFalse(): Optional<AgreementWord>
}
