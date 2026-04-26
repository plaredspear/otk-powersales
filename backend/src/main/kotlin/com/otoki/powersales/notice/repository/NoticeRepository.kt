package com.otoki.powersales.notice.repository

import com.otoki.powersales.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long>, NoticeRepositoryCustom
