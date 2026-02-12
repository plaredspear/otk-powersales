package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 제안 Entity
 * 사용자가 등록한 제안(신제품, 기존제품 개선) 정보를 관리한다.
 */
@Entity
@Table(
    name = "suggestions",
    indexes = [
        Index(name = "idx_suggestion_user_created", columnList = "user_id,created_at")
    ]
)
class Suggestion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    val category: SuggestionCategory,

    @Column(name = "product_code", length = 20)
    val productCode: String? = null,

    @Column(name = "product_name", length = 200)
    val productName: String? = null,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "content", nullable = false, length = 2000)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: SuggestionStatus = SuggestionStatus.SUBMITTED,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
