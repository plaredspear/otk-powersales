package com.otoki.powersales.platform.common.entity

import jakarta.persistence.*

@DomainName("시스템코드마스터")
@Entity
@Table(name = "system_code_master")
class SystemCodeMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @FieldName("회사코드")
    @Column(name = "company_code", nullable = false, length = 10)
    var companyCode: String,

    @FieldName("그룹코드")
    @Column(name = "group_code", nullable = false, length = 20)
    var groupCode: String,

    @FieldName("상세코드")
    @Column(name = "detail_code", nullable = false, length = 20)
    var detailCode: String,

    @FieldName("그룹코드명")
    @Column(name = "group_code_name", length = 100)
    var groupCodeName: String? = null,

    @FieldName("상세코드명")
    @Column(name = "detail_code_name", length = 100)
    var detailCodeName: String? = null,

    @FieldName("순번")
    @Column(name = "seq", length = 10)
    var seq: String? = null,

    @FieldName("ExternalKey")
    @Column(name = "external_key", nullable = false, length = 60, unique = true)
    var externalKey: String
) : BaseEntity()
