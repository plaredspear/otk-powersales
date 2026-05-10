package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.entity.QAgreementWord.Companion.agreementWord
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class AgreementWordRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : AgreementWordRepositoryCustom {

    override fun findActiveOrDueCandidates(today: LocalDate): List<AgreementWord> {
        val notDeleted = agreementWord.isDeleted.isNull.or(agreementWord.isDeleted.isFalse)
        val isActive = agreementWord.active.isTrue
        val isDueCandidate = agreementWord.activeDate.isNull
            .and(agreementWord.afterActiveDate.eq(today))

        return queryFactory
            .selectFrom(agreementWord)
            .where(notDeleted.and(isActive.or(isDueCandidate)))
            .fetch()
    }
}
