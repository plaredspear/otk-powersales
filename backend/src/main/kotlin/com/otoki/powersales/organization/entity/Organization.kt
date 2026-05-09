package com.otoki.powersales.organization.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Comment

/**
 * 조직 마스터 Entity
 * Salesforce 조직(Organization) 커스텀 오브젝트 — SAP HR 조직 마스터 동기화 대상 테이블.
 *
 * **주의: organization_id(PK)를 FK로 참조하지 마세요.**
 * 이 테이블은 SAP 동기화 시 전체 삭제 후 재삽입(DELETE_INSERT)되므로
 * PK가 매 동기화마다 변경됩니다. 다른 테이블에서 organization_id를
 * FK로 참조하면 데이터 무결성이 깨집니다.
 * 조직 정보를 참조할 때는 [costCenterLevel5](cc_cd5) 등 코드값을 문자열로 사용하세요.
 */
@Entity
@Table(name = "organization")
@SFObject("Organization__c")
class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
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

    @SFField("ExternalKey__c")
    @Comment("외부 ID — SF 측 외부 ID(텍스트(100), 고유). Q1 옵션 2: UNIQUE 제약 미부착(정합 검증 후속)")
    @Column(name = "external_key", length = 100)
    val externalKey: String? = null
) : BaseEntity()
