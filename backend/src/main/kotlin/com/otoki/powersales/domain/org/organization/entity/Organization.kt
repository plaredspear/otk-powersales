package com.otoki.powersales.domain.org.organization.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

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
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("조직마스터")
@Entity
@Table(name = "organization")
@SFObject("Org__c")
class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("조직ID")
    @Column(name = "organization_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("CostCenterLevel2__c")
    @FieldName("COST CENTER 코드(2레벨)")
    @Column(name = "cc_cd2", length = 100)
    val costCenterLevel2: String? = null,

    @SFField("OrgCodeLevel2__c")
    @FieldName("HR 조직 코드(2레벨)")
    @Column(name = "org_cd2", length = 100)
    val orgCodeLevel2: String? = null,

    @SFField("OrgNameLevel2__c")
    @FieldName("HR 조직명(2레벨)")
    @Column(name = "org_nm2", length = 100)
    val orgNameLevel2: String? = null,

    @SFField("CostCenterLevel3__c")
    @FieldName("COST CENTER 코드(3레벨)")
    @Column(name = "cc_cd3", length = 100)
    val costCenterLevel3: String? = null,

    @SFField("OrgCodeLevel3__c")
    @FieldName("HR 조직 코드(3레벨)")
    @Column(name = "org_cd3", length = 100)
    val orgCodeLevel3: String? = null,

    @SFField("OrgNameLevel3__c")
    @FieldName("HR 조직명(3레벨)")
    @Column(name = "org_nm3", length = 100)
    val orgNameLevel3: String? = null,

    @SFField("CostCenterLevel4__c")
    @FieldName("COST CENTER 코드(4레벨)")
    @Column(name = "cc_cd4", length = 100)
    val costCenterLevel4: String? = null,

    @SFField("OrgCodeLevel4__c")
    @FieldName("HR 조직 코드(4레벨)")
    @Column(name = "org_cd4", length = 100)
    val orgCodeLevel4: String? = null,

    @SFField("OrgNameLevel4__c")
    @FieldName("HR 조직명(4레벨)")
    @Column(name = "org_nm4", length = 100)
    val orgNameLevel4: String? = null,

    @SFField("CostCenterLevel5__c")
    @FieldName("COST CENTER 코드(5레벨)")
    @Column(name = "cc_cd5", length = 100)
    val costCenterLevel5: String? = null,

    @SFField("OrgCodeLevel5__c")
    @FieldName("HR 조직 코드(5레벨)")
    @Column(name = "org_cd5", length = 100)
    val orgCodeLevel5: String? = null,

    @SFField("OrgNameLevel5__c")
    @FieldName("HR 조직명(5레벨)")
    @Column(name = "org_nm5", length = 100)
    val orgNameLevel5: String? = null,

    @SFField("ExternalKey__c")
    @FieldName("ExternalKey")
    @Column(name = "external_key", length = 100)
    val externalKey: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null
) : BaseEntity()
