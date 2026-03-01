package com.otoki.internal.notice.repository

import com.otoki.internal.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    @Query(
        value = """
            SELECT n FROM Notice n
            WHERE (n.isDeleted IS NULL OR n.isDeleted = false)
              AND (
                (:category IS NULL AND (n.category = 'ALL' OR (n.category = 'BRANCH' AND n.branch = :branch)))
                OR (:category = 'COMPANY' AND n.category = 'ALL')
                OR (:category = 'BRANCH' AND n.category = 'BRANCH' AND n.branch = :branch)
              )
              AND (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.contents) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY n.createdDate DESC
        """,
        countQuery = """
            SELECT COUNT(n) FROM Notice n
            WHERE (n.isDeleted IS NULL OR n.isDeleted = false)
              AND (
                (:category IS NULL AND (n.category = 'ALL' OR (n.category = 'BRANCH' AND n.branch = :branch)))
                OR (:category = 'COMPANY' AND n.category = 'ALL')
                OR (:category = 'BRANCH' AND n.category = 'BRANCH' AND n.branch = :branch)
              )
              AND (:search IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.contents) LIKE LOWER(CONCAT('%', :search, '%')))
        """
    )
    fun findNotices(
        @Param("category") category: String?,
        @Param("search") search: String?,
        @Param("branch") branch: String,
        pageable: Pageable
    ): Page<Notice>
}
