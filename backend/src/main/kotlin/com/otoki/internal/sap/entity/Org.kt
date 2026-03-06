package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "org")
class Org(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "cc_cd2", length = 10)
    val costCenterLevel2: String? = null,

    @Column(name = "org_cd2", length = 20)
    val orgCodeLevel2: String? = null,

    @Column(name = "org_nm2", length = 100)
    val orgNameLevel2: String? = null,

    @Column(name = "cc_cd3", length = 10)
    val costCenterLevel3: String? = null,

    @Column(name = "org_cd3", length = 20)
    val orgCodeLevel3: String? = null,

    @Column(name = "org_nm3", length = 100)
    val orgNameLevel3: String? = null,

    @Column(name = "cc_cd4", length = 10)
    val costCenterLevel4: String? = null,

    @Column(name = "org_cd4", length = 20)
    val orgCodeLevel4: String? = null,

    @Column(name = "org_nm4", length = 100)
    val orgNameLevel4: String? = null,

    @Column(name = "cc_cd5", length = 10)
    val costCenterLevel5: String? = null,

    @Column(name = "org_cd5", length = 20)
    val orgCodeLevel5: String? = null,

    @Column(name = "org_nm5", length = 100)
    val orgNameLevel5: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
