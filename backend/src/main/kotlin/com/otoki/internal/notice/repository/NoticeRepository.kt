package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long>, NoticeRepositoryCustom
