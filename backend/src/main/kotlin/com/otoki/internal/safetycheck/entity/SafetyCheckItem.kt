package com.otoki.internal.safetycheck.entity

import jakarta.persistence.*

@Entity
@Table(name = "safetycheck_list")
@IdClass(SafetyCheckItemId::class)
class SafetyCheckItem(

    @Id
    @Column(name = "question_num", nullable = false)
    val questionNum: Int = 0,

    @Id
    @Column(name = "seq_num", nullable = false)
    val seqNum: Int = 0,

    @Column(name = "contents", nullable = false, length = 500)
    val contents: String = "",

    @Column(name = "use_yn", length = 1)
    val useYn: String? = "Y"

    // Phase2: 기존 V2 필드 주석 처리
    // val id: Long = 0,                    // auto-increment PK → 복합 키로 대체
    // val categoryId: Long = 0,            // V1에 없음
    // val label: String,                   // → contents로 대체
    // val sortOrder: Int,                  // V1에 없음
    // val required: Boolean = true,        // V1에 없음
    // val active: Boolean = true           // → useYn (String 'Y'/'N')으로 대체
)
