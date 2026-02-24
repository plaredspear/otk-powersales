package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NoticeRepository : JpaRepository<Notice, Long> {

    @Query(
        """
        SELECT n FROM Notice n
        WHERE n.createdDate >= :since
          AND (n.category = :allCategory OR (n.category = :branchCategory AND n.branch = :branch))
        ORDER BY n.createdDate DESC
        LIMIT 5
        """
    )
    fun findRecentNotices(
        @Param("branch") branch: String,
        @Param("since") since: LocalDateTime,
        @Param("branchCategory") branchCategory: String = "BRANCH",
        @Param("allCategory") allCategory: String = "ALL"
    ): List<Notice>
}
