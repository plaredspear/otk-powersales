package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

/**
 * 조직 마스터 Entity
 * Salesforce 조직(Organization) 커스텀 오브젝트 — SAP HR 조직 마스터 동기화 대상 테이블.
 */
@Entity
@Table(name = "organization")
@SFObject("Organization__c")
class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @SFField("CostCenterLevel2__c")
    @Comment("COST CENTER 코드(2레벨)")
    @Column(name = "cc_cd2", length = 100)
    val costCenterLevel2: String? = null,

    @SFField("OrgCodeLevel2__c")
    @Comment("HR 조직 코드(2레벨)")
    @Column(name = "org_cd2", length = 100)
    val orgCodeLevel2: String? = null,

    @SFField("OrgNameLevel2__c")
    @Comment("HR 조직명(2레벨)")
    @Column(name = "org_nm2", length = 100)
    val orgNameLevel2: String? = null,

    @SFField("CostCenterLevel3__c")
    @Comment("COST CENTER 코드(3레벨)")
    @Column(name = "cc_cd3", length = 100)
    val costCenterLevel3: String? = null,

    @SFField("OrgCodeLevel3__c")
    @Comment("HR 조직 코드(3레벨)")
    @Column(name = "org_cd3", length = 100)
    val orgCodeLevel3: String? = null,

    @SFField("OrgNameLevel3__c")
    @Comment("HR 조직명(3레벨)")
    @Column(name = "org_nm3", length = 100)
    val orgNameLevel3: String? = null,

    @SFField("CostCenterLevel4__c")
    @Comment("COST CENTER 코드(4레벨)")
    @Column(name = "cc_cd4", length = 100)
    val costCenterLevel4: String? = null,

    @SFField("OrgCodeLevel4__c")
    @Comment("HR 조직 코드(4레벨)")
    @Column(name = "org_cd4", length = 100)
    val orgCodeLevel4: String? = null,

    @SFField("OrgNameLevel4__c")
    @Comment("HR 조직명(4레벨)")
    @Column(name = "org_nm4", length = 100)
    val orgNameLevel4: String? = null,

    @SFField("CostCenterLevel5__c")
    @Comment("COST CENTER 코드(5레벨)")
    @Column(name = "cc_cd5", length = 100)
    val costCenterLevel5: String? = null,

    @SFField("OrgCodeLevel5__c")
    @Comment("HR 조직 코드(5레벨)")
    @Column(name = "org_cd5", length = 100)
    val orgCodeLevel5: String? = null,

    @SFField("OrgNameLevel5__c")
    @Comment("HR 조직명(5레벨)")
    @Column(name = "org_nm5", length = 100)
    val orgNameLevel5: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
