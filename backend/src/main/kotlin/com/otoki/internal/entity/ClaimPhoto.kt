package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 클레임 사진 Entity
 * 클레임 등록 시 첨부된 사진 정보를 관리한다.
 * 사진 유형: 불량 사진, 일부인 사진, 구매 영수증 사진
 */
@Entity
@Table(name = "claim_photos")
class ClaimPhoto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    val claim: Claim,

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type", nullable = false, length = 20)
    val photoType: ClaimPhotoType,

    @Column(name = "url", nullable = false, length = 500)
    val url: String,

    @Column(name = "original_file_name", nullable = false, length = 255)
    val originalFileName: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "content_type", nullable = false, length = 50)
    val contentType: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
