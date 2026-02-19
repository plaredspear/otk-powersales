/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/ **
 * 제안 사진 Entity
 * 제안 등록 시 첨부된 사진 정보를 관리한다. (최대 2장)
 * /
@Entity
@Table(name = "suggestion_photos")
class SuggestionPhoto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggestion_id", nullable = false)
    val suggestion: Suggestion,

    @Column(name = "url", nullable = false, length = 500)
    val url: String,

    @Column(name = "original_file_name", nullable = false, length = 255)
    val originalFileName: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "content_type", nullable = false, length = 50)
    val contentType: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
*/
