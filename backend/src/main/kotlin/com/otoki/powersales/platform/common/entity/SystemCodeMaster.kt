package com.otoki.powersales.platform.common.entity

import jakarta.persistence.*

@DomainName("시스템코드마스터")
@Entity
@Table(name = "system_code_master")
class SystemCodeMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // SF nillable=true 정합 — SAP inbound 무검증 raw 적재 (IF_REST_SAP_SystemCodeMaster).
    // 식별은 합성 키 external_key(NOT NULL+UNIQUE)가 담당한다.
    @FieldName("회사코드")
    @Column(name = "company_code", length = 10)
    var companyCode: String? = null,

    @FieldName("그룹코드")
    @Column(name = "group_code", length = 20)
    var groupCode: String? = null,

    @FieldName("상세코드")
    @Column(name = "detail_code", length = 20)
    var detailCode: String? = null,

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
