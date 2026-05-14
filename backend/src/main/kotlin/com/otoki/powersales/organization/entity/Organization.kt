package com.otoki.powersales.organization.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
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
@SFObject("Org__c")
@HCTable("org__c")
class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("CostCenterLevel2__c")
    @HCColumn("costcenterlevel2__c")
    @Comment("COST CENTER 코드(2레벨)")
    @Column(name = "cc_cd2", length = 100)
    val costCenterLevel2: String? = null,

    @SFField("OrgCodeLevel2__c")
    @HCColumn("orgcodelevel2__c")
    @Comment("HR 조직 코드(2레벨)")
    @Column(name = "org_cd2", length = 100)
    val orgCodeLevel2: String? = null,

    @SFField("OrgNameLevel2__c")
    @HCColumn("orgnamelevel2__c")
    @Comment("HR 조직명(2레벨)")
    @Column(name = "org_nm2", length = 100)
    val orgNameLevel2: String? = null,

    @SFField("CostCenterLevel3__c")
    @HCColumn("costcenterlevel3__c")
    @Comment("COST CENTER 코드(3레벨)")
    @Column(name = "cc_cd3", length = 100)
    val costCenterLevel3: String? = null,

    @SFField("OrgCodeLevel3__c")
    @HCColumn("orgcodelevel3__c")
    @Comment("HR 조직 코드(3레벨)")
    @Column(name = "org_cd3", length = 100)
    val orgCodeLevel3: String? = null,

    @SFField("OrgNameLevel3__c")
    @HCColumn("orgnamelevel3__c")
    @Comment("HR 조직명(3레벨)")
    @Column(name = "org_nm3", length = 100)
    val orgNameLevel3: String? = null,

    @SFField("CostCenterLevel4__c")
    @HCColumn("costcenterlevel4__c")
    @Comment("COST CENTER 코드(4레벨)")
    @Column(name = "cc_cd4", length = 100)
    val costCenterLevel4: String? = null,

    @SFField("OrgCodeLevel4__c")
    @HCColumn("orgcodelevel4__c")
    @Comment("HR 조직 코드(4레벨)")
    @Column(name = "org_cd4", length = 100)
    val orgCodeLevel4: String? = null,

    @SFField("OrgNameLevel4__c")
    @HCColumn("orgnamelevel4__c")
    @Comment("HR 조직명(4레벨)")
    @Column(name = "org_nm4", length = 100)
    val orgNameLevel4: String? = null,

    @SFField("CostCenterLevel5__c")
    @HCColumn("costcenterlevel5__c")
    @Comment("COST CENTER 코드(5레벨)")
    @Column(name = "cc_cd5", length = 100)
    val costCenterLevel5: String? = null,

    @SFField("OrgCodeLevel5__c")
    @HCColumn("orgcodelevel5__c")
    @Comment("HR 조직 코드(5레벨)")
    @Column(name = "org_cd5", length = 100)
    val orgCodeLevel5: String? = null,

    @SFField("OrgNameLevel5__c")
    @HCColumn("orgnamelevel5__c")
    @Comment("HR 조직명(5레벨)")
    @Column(name = "org_nm5", length = 100)
    val orgNameLevel5: String? = null,

    @SFField("ExternalKey__c")
    @HCColumn("externalkey__c")
    @Comment("외부 ID — SF 측 외부 ID(텍스트(100), 고유). Q1 옵션 2: UNIQUE 제약 미부착(정합 검증 후속)")
    @Column(name = "external_key", length = 100)
    val externalKey: String? = null,

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null
) : BaseEntity()
