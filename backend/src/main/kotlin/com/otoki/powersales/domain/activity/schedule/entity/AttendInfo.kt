package com.otoki.powersales.domain.activity.schedule.entity

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

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("출근정보")
@Entity
@SFObject("AttendInfo__c")
@Table(
    name = "attend_info",
    indexes = [
        Index(name = "idx_attend_info_employee", columnList = "employee_code"),
        Index(name = "idx_attend_info_start_date", columnList = "start_date")
    ]
)
class AttendInfo(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attend_info_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("EmployeeCode__c")
    @Column(name = "employee_code", nullable = false, length = 100)
    val employeeCode: String,

    @SFField("StartDate__c")
    @Column(name = "start_date", nullable = false, length = 100)
    var startDate: String,

    @SFField("EndDate__c")
    @Column(name = "end_date", length = 100)
    var endDate: String? = null,

    @SFField("AttendType__c")
    @Column(name = "attend_type", length = 100)
    var attendType: String? = null,

    @SFField("Status__c")
    @Column(name = "status", length = 100)
    var status: String? = null,

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
