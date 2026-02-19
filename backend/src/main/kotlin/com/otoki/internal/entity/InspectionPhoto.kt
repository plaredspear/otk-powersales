/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/ **
 * 현장 점검 사진 Entity
 * 현장 점검 시 첨부된 사진 정보를 관리한다.
 * /
@Entity
@Table(name = "inspection_photos")
class InspectionPhoto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    val inspection: Inspection,

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
*/
