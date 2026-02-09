package com.otoki.internal.repository

import com.otoki.internal.entity.Notice
import com.otoki.internal.entity.NoticeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * 공지사항 Repository
 */
interface NoticeRepository : JpaRepository<Notice, Long> {

    /**
     * 최근 공지사항 조회
     * 지점공지(해당 지점) + 전체공지를 최신순으로 최대 5개 반환
     */
    @Query(
        """
        SELECT n FROM Notice n
        WHERE n.createdAt >= :since
          AND (n.type = :allType OR (n.type = :branchType AND n.branchName = :branchName))
        ORDER BY n.createdAt DESC
        LIMIT 5
        """
    )
    fun findRecentNotices(
        @Param("branchName") branchName: String,
        @Param("since") since: LocalDateTime,
        @Param("branchType") branchType: NoticeType = NoticeType.BRANCH,
        @Param("allType") allType: NoticeType = NoticeType.ALL
    ): List<Notice>
}
