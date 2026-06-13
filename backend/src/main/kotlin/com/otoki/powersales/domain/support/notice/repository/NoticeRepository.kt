package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long>, NoticeRepositoryCustom
