package com.otoki.powersales.common.jobrun

import java.time.Instant

interface ScheduledJobRunRepositoryCustom {
    /**
     * `started_at < threshold` 인 row 를 단일 bulk DELETE 로 삭제하고 영향받은 row 수를 반환한다.
     * 보존 정책 잡(`ScheduledJobRunCleanupJob`) 에서 매일 04:00 호출된다.
     */
    fun deleteByStartedAtBefore(threshold: Instant): Long
}
