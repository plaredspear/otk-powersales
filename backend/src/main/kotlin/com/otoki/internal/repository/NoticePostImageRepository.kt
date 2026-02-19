/*
package com.otoki.internal.repository

import com.otoki.internal.entity.NoticePostImage
import org.springframework.data.jpa.repository.JpaRepository

/ **
 * 공지사항 게시물 이미지 Repository
 * /
interface NoticePostImageRepository : JpaRepository<NoticePostImage, Long> {

    / **
     * 게시물별 이미지 조회 (정렬 순서대로)
     * /
    fun findByPostIdOrderBySortOrderAsc(postId: Long): List<NoticePostImage>
}
*/
