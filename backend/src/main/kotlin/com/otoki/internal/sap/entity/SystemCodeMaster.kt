package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.sap.SAPSource
import com.otoki.internal.common.sap.SAPUpsertKey
import com.otoki.internal.common.sap.SyncMode
import jakarta.persistence.*

@Entity
@Table(name = "system_code_master")
@SAPSource(api = "/sap/SystemCodeMaster", syncMode = SyncMode.UPSERT)
class SystemCodeMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "company_code", nullable = false, length = 10)
    var companyCode: String,

    @Column(name = "group_code", nullable = false, length = 20)
    var groupCode: String,

    @Column(name = "detail_code", nullable = false, length = 20)
    var detailCode: String,

    @Column(name = "group_code_name", length = 100)
    var groupCodeName: String? = null,

    @Column(name = "detail_code_name", length = 100)
    var detailCodeName: String? = null,

    @Column(name = "seq", length = 10)
    var seq: String? = null,

    @SAPUpsertKey(composite = true, components = ["companyCode", "groupCode", "detailCode"])
    @Column(name = "external_key", nullable = false, length = 60, unique = true)
    var externalKey: String
) : BaseEntity()
