/*
package com.otoki.internal.repository

import com.otoki.internal.entity.NoticeCategory
import com.otoki.internal.entity.NoticePost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/ **
 * 공지사항 게시물 Repository
 * /
interface NoticePostRepository : JpaRepository<NoticePost, Long> {

    / **
     * 전체 활성 게시물 조회 (페이지네이션)
     * isActive가 true인 게시물만 조회하며, createdAt 내림차순 정렬
     * /
    fun findByIsActiveTrueOrderByCreatedAtDesc(
        pageable: Pageable
    ): Page<NoticePost>

    / **
     * 분류별 활성 게시물 조회 (페이지네이션)
     * isActive가 true인 게시물만 조회하며, createdAt 내림차순 정렬
     * /
    fun findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
        category: NoticeCategory,
        pageable: Pageable
    ): Page<NoticePost>

    / **
     * 전체 활성 게시물 검색 (타이틀 + 내용 LIKE 검색, 페이지네이션)
     * isActive가 true이고, title 또는 content에 검색 키워드가 포함된 게시물을 조회
     * /
    @Query(
        """
        SELECT p FROM NoticePost p
        WHERE p.isActive = true
          AND (p.title LIKE %:search% OR p.content LIKE %:search%)
        ORDER BY p.createdAt DESC
        """
    )
    fun findBySearchWithPaging(
        search: String,
        pageable: Pageable
    ): Page<NoticePost>

    / **
     * 분류별 활성 게시물 검색 (타이틀 + 내용 LIKE 검색, 페이지네이션)
     * isActive가 true이고, category가 일치하며, title 또는 content에 검색 키워드가 포함된 게시물을 조회
     * /
    @Query(
        """
        SELECT p FROM NoticePost p
        WHERE p.category = :category
          AND p.isActive = true
          AND (p.title LIKE %:search% OR p.content LIKE %:search%)
        ORDER BY p.createdAt DESC
        """
    )
    fun findByCategoryAndSearchWithPaging(
        category: NoticeCategory,
        search: String,
        pageable: Pageable
    ): Page<NoticePost>

    / **
     * ID로 활성 게시물 조회
     * /
    fun findByIdAndIsActiveTrue(id: Long): NoticePost?
}
*/
