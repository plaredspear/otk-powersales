package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface NoticeRepositoryCustom {

    fun findNotices(
        category: String?,
        search: String?,
        branch: String,
        pageable: Pageable
    ): Page<Notice>

    fun findAllNotices(
        category: String?,
        search: String?,
        pageable: Pageable
    ): Page<Notice>

    fun findRecentNotices(
        branch: String,
        since: LocalDateTime,
        branchCategory: String = "BRANCH",
        allCategory: String = "ALL"
    ): List<Notice>
}
