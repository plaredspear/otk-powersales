package com.otoki.powersales.domain.activity.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName
@DomainName("임시저장 클레임코드")
@Entity
@Table(name = "tmp_claim_code")
@HerokuOnly("tmp_claimcode")
class TmpClaimCode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장클레임코드ID")
    @Column(name = "tmp_claim_code_id")
    val id: Long = 0,

    @HCColumn("claim1_code")
    @FieldName("클레임유형1코드")
    @Column(name = "claim1_code", length = 80)
    var claim1Code: String? = null,

    @HCColumn("claim1_name")
    @FieldName("클레임유형1명")
    @Column(name = "claim1_name", length = 80)
    var claim1Name: String? = null,

    @HCColumn("claim2_code")
    @FieldName("클레임유형2코드")
    @Column(name = "claim2_code", length = 80)
    var claim2Code: String? = null,

    @HCColumn("claim2_name")
    @FieldName("클레임유형2명")
    @Column(name = "claim2_name", length = 80)
    var claim2Name: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()