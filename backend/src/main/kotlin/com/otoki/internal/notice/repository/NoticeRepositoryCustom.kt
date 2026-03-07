package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface NoticeRepositoryCustom {

    fun findNotices(
        category: NoticeCategory?,
        search: String?,
        branch: String,
        pageable: Pageable
    ): Page<Notice>

    fun findAllNotices(
        category: NoticeCategory?,
        search: String?,
        pageable: Pageable
    ): Page<Notice>

    fun findRecentNotices(
        branch: String,
        since: LocalDateTime
    ): List<Notice>
}
