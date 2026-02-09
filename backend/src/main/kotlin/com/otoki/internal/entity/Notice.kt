package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 공지사항 Entity
 */
@Entity
@Table(name = "notices")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "content", columnDefinition = "TEXT")
    val content: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: NoticeType,

    @Column(name = "branch_name", length = 50)
    val branchName: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
