package com.otoki.powersales.common.jobrun

import org.springframework.data.jpa.repository.JpaRepository

interface ScheduledJobRunRepository :
    JpaRepository<ScheduledJobRun, Long>,
    ScheduledJobRunRepositoryCustom
