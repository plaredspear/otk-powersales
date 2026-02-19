/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/ **
 * 공지사항 게시물 Entity
 * /
@Entity
@Table(
    name = "notice_posts",
    indexes = [
        Index(name = "idx_notice_post_category_created", columnList = "category,created_at"),
        Index(name = "idx_notice_post_active", columnList = "is_active"),
        Index(name = "idx_notice_post_created", columnList = "created_at")
    ]
)
class NoticePost(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    val category: NoticeCategory,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
*/
