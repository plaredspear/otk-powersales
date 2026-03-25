package com.otoki.internal.draft.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "tmp_claim_code")
@HCTable("tmp_claim_code")
class TmpClaimCode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_claim_code_id")
    val id: Long = 0,

    @HCColumn("claim1_code")
    @Column(name = "claim1_code", length = 80)
    var claim1Code: String? = null,

    @HCColumn("claim1_name")
    @Column(name = "claim1_name", length = 80)
    var claim1Name: String? = null,

    @HCColumn("claim2_code")
    @Column(name = "claim2_code", length = 80)
    var claim2Code: String? = null,

    @HCColumn("claim2_name")
    @Column(name = "claim2_name", length = 80)
    var claim2Name: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
