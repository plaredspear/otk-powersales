package com.otoki.powersales.notice.repository

import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.notice.enums.NoticeCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        branch: String
    ): List<Notice>
}
