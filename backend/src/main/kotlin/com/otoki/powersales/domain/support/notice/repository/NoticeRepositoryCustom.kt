package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
interface NoticeRepositoryCustom {

    fun findNotices(
        category: NoticeCategory?,
        search: String?,
        branchCode: String,
        pageable: Pageable
    ): Page<Notice>

    fun findAllNotices(
        category: NoticeCategory?,
        search: String?,
        pageable: Pageable
    ): Page<Notice>

    fun findRecentNotices(
        branchCode: String
    ): List<Notice>
}
