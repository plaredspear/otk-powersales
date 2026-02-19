/*
package com.otoki.internal.entity

import jakarta.persistence.*

@Entity
@Table(name = "safety_check_submission_items")
class SafetyCheckSubmissionItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    val submission: SafetyCheckSubmission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: SafetyCheckItem,

    @Column(name = "checked", nullable = false)
    val checked: Boolean = true
)
*/
