package com.otoki.powersales.common.jobrun

import com.otoki.powersales.common.jobrun.QScheduledJobRun.Companion.scheduledJobRun
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

open class ScheduledJobRunRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ScheduledJobRunRepositoryCustom {

    @Transactional
    override fun deleteByStartedAtBefore(threshold: Instant): Long {
        return queryFactory
            .delete(scheduledJobRun)
            .where(scheduledJobRun.startedAt.lt(threshold))
            .execute()
    }
}
