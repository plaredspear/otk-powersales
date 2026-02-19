/*
package com.otoki.internal.entity

import jakarta.persistence.*

@Entity
@Table(name = "safety_check_categories")
class SafetyCheckCategory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, unique = true, length = 50)
    val name: String,

    @Column(name = "description", length = 200)
    val description: String? = null,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Column(name = "active", nullable = false)
    val active: Boolean = true,

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    val items: MutableList<SafetyCheckItem> = mutableListOf()
)
*/
