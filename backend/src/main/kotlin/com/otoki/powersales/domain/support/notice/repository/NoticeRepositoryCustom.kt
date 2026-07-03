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

    /**
     * 공지 push 대상 사원의 FCM 토큰을 조회한다.
     *
     * 대상 = 해당 공지가 앱 목록에 노출되는 사용자와 동일 (조회 노출 규칙 정합):
     * - 회사공지(COMPANY)/교육(EDUCATION): FCM 토큰 보유 전 사용자
     * - 지점공지(BRANCH): costCenterCode 가 공지 branchCode 와 일치하는 사용자만
     *
     * @param category 공지 카테고리
     * @param branchCode 지점공지일 때 매칭할 지점코드 (그 외 카테고리는 무시)
     */
    fun findPushTargetTokens(
        category: NoticeCategory,
        branchCode: String?
    ): List<String>
}
